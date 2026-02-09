package com.example.webhook.worker;

import com.example.webhook.model.*;
import com.example.webhook.repository.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DispatcherService {

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private com.example.webhook.client.QueueClient queueClient;

    @Autowired
    private WebhookWorker webhookWorker;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 1. Poll queues for Real-Time triggers
     */
    @Scheduled(fixedDelay = 15000)
    public void dispatch() {
        List<Subscription> activeSubs = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);

        for (Subscription sub : activeSubs) {
            processSubscription(sub);
        }
    }

    /**
     * 2. Poll DB for Retries / Stuck items (The "Recovery" Loop)
     */
    @Scheduled(fixedDelay = 30000)
    public void processPendingRetries() {
        List<Subscription> activeSubs = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);

        for (Subscription sub : activeSubs) {
            // Check the HEAD of the line
            Optional<WebhookDelivery> oldestPending = webhookDeliveryRepository
                    .findTopBySubscriptionIdAndStatusInOrderByCreatedAtAsc(
                            sub.getSubscriptionId(),
                            List.of(DeliveryStatus.PENDING, DeliveryStatus.PROCESSING));

            if (oldestPending.isPresent()) {
                WebhookDelivery delivery = oldestPending.get();
                // If it's ready for retry/attempt
                if (delivery.getNextAttemptAt() == null ||
                        delivery.getNextAttemptAt().isBefore(LocalDateTime.now())) {
                    System.out.println("⏰ Poller picked up delivery: " + delivery.getId());
                    webhookWorker.processDelivery(delivery);
                }
            }
        }
    }

    /**
     * Poll one subscription queue
     */
    private void processSubscription(Subscription sub) {
        String queueId = sub.getQueueId();
        if (queueId == null || queueId.isBlank())
            return;

        String message = queueClient.pop(queueId);
        if (message == null || message.isBlank())
            return;

        System.out.println("📨 Message received from " + queueId + ": " + message);

        try {
            JsonNode root = mapper.readTree(message);
            if (!root.hasNonNull("deliveryId")) {
                System.err.println("⚠️ Message missing deliveryId: " + message);
                return;
            }

            String deliveryId = root.get("deliveryId").asText();
            Optional<WebhookDelivery> deliveryOpt = webhookDeliveryRepository.findById(deliveryId);

            if (deliveryOpt.isPresent()) {
                WebhookDelivery delivery = deliveryOpt.get();

                // --- ORDERING GUARD ---
                // We must ensure this is the Oldest PENDING or PROCESSING delivery.
                // If there's an older one (failed/retrying), we SKIP this one.
                Optional<WebhookDelivery> oldest = webhookDeliveryRepository
                        .findTopBySubscriptionIdAndStatusInOrderByCreatedAtAsc(
                                sub.getSubscriptionId(),
                                List.of(DeliveryStatus.PENDING, DeliveryStatus.PROCESSING));

                if (oldest.isPresent() && !oldest.get().getId().equals(delivery.getId())) {
                    System.out.println("⛔ Skipping delivery " + delivery.getId()
                            + " because older delivery " + oldest.get().getId() + " is pending/processing.");
                    return; // Drop it. The older one will unblock us later.
                }

                // MARK AS PROCESSING
                // This prevents other threads/instances from picking it up immediately
                delivery.setStatus(DeliveryStatus.PROCESSING);
                delivery.setNextAttemptAt(LocalDateTime.now().plusMinutes(2)); // Safety timeout
                webhookDeliveryRepository.save(delivery);

                webhookWorker.processDelivery(delivery);
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to parse queue message: " + e.getMessage());
        }
    }
}