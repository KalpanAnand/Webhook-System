package com.example.webhook.service;

import com.example.webhook.exception.ResourceNotFoundException;
import com.example.webhook.model.Subscription;
import com.example.webhook.model.SubscriptionStatus;
import com.example.webhook.model.dto.SubscriptionDto;
import com.example.webhook.repository.ClientRepository;
import com.example.webhook.repository.EventTypeRepository;
import com.example.webhook.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ClientRepository clientRepository;
    private final EventTypeRepository eventTypeRepository;
    private final com.example.webhook.client.QueueClient queueClient;
    private final com.example.webhook.repository.WebhookDeliveryRepository webhookDeliveryRepository;
    private final com.example.webhook.repository.DeadLetterQueueRepository deadLetterQueueRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
            ClientRepository clientRepository,
            EventTypeRepository eventTypeRepository,
            com.example.webhook.client.QueueClient queueClient,
            com.example.webhook.repository.WebhookDeliveryRepository webhookDeliveryRepository,
            com.example.webhook.repository.DeadLetterQueueRepository deadLetterQueueRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.clientRepository = clientRepository;
        this.eventTypeRepository = eventTypeRepository;
        this.queueClient = queueClient;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.deadLetterQueueRepository = deadLetterQueueRepository;
    }

    @Transactional
    public Subscription createSubscription(SubscriptionDto.CreateRequest request) {
        // ... (existing checks) ...
        if (!clientRepository.existsById(request.getClientId())) {
            throw new ResourceNotFoundException("Client not found: " + request.getClientId());
        }
        if (!eventTypeRepository.existsById(request.getEventTypeId())) {
            throw new ResourceNotFoundException("EventType not found: " + request.getEventTypeId());
        }

        // Check for existing subscription
        List<Subscription> existingSubscriptions = subscriptionRepository
                .findByClientIdAndEventTypeId(request.getClientId(), request.getEventTypeId());

        if (!existingSubscriptions.isEmpty()) {
            // Check if any is active
            for (Subscription existing : existingSubscriptions) {
                if (existing.getStatus() == SubscriptionStatus.ACTIVE) {
                    throw new com.example.webhook.exception.DuplicateResourceException(
                            "Subscription already exists for this Event Type",
                            existing.getSubscriptionId());
                }
            }

            // All exist but are inactive - reactivate the first one
            Subscription existing = existingSubscriptions.get(0);

            // Setup queue (Network Call - Outside Lock)
            existing.setStatus(SubscriptionStatus.ACTIVE);
            existing.setWebhookUrl(request.getWebhookUrl());
            if (existing.getQueueId() == null) {
                setupQueueForSubscription(existing);
            }

            // Save (Write - Inside Lock)
            return subscriptionRepository.save(existing);
        }

        Subscription sub = new Subscription();
        sub.setClientId(request.getClientId());
        sub.setEventTypeId(request.getEventTypeId());
        sub.setWebhookUrl(request.getWebhookUrl());
        sub.setStatus(SubscriptionStatus.ACTIVE);

        // Setup queue (Network Call - Outside Lock)
        setupQueueForSubscription(sub);

        // Save (Write - Inside Lock)
        return subscriptionRepository.save(sub);
    }

    private void setupQueueForSubscription(Subscription sub) {
        // Generate unique queue name per subscriber
        String queueName = "sub-" + java.util.UUID.randomUUID().toString();
        try {
            com.example.webhook.client.QueueClient.QueueResponse response = queueClient.createQueue(queueName);

            sub.setQueueId(response.getQueueId());
            sub.setQueuePostUrl(response.getPostUrl());
            sub.setQueueGetUrl(response.getGetUrl());

        } catch (Exception e) {
            throw new RuntimeException("Failed to create external queue: " + e.getMessage());
        }
    }

    @Transactional
    public Subscription updateSubscription(String subscriptionId,
            SubscriptionDto.ModifyRequest request) {

        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        if (!eventTypeRepository.existsById(request.getEventTypeId())) {
            throw new IllegalArgumentException(
                    "EventType not found: " + request.getEventTypeId());
        }

        sub.setEventTypeId(request.getEventTypeId());
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public Subscription unsubscribe(String subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));

        // 1. Set Status to INACTIVE
        sub.setStatus(SubscriptionStatus.INACTIVE);
        Subscription savedSub = subscriptionRepository.save(sub);

        // 2. Find PENDING deliveries
        List<com.example.webhook.model.WebhookDelivery> pendingDeliveries = webhookDeliveryRepository
                .findBySubscriptionIdAndStatus(subscriptionId, com.example.webhook.model.DeliveryStatus.PENDING);

        // 3. Move to DLQ
        for (com.example.webhook.model.WebhookDelivery delivery : pendingDeliveries) {
            delivery.setStatus(com.example.webhook.model.DeliveryStatus.FAILED);
            webhookDeliveryRepository.save(delivery);

            com.example.webhook.model.DeadLetterQueue dlq = new com.example.webhook.model.DeadLetterQueue();
            dlq.setEventId(delivery.getEventId());
            dlq.setSubscriptionId(delivery.getSubscriptionId());
            dlq.setFailureReason("Subscription Unsubscribed");
            deadLetterQueueRepository.save(dlq);

            System.out.println("💀 Unsubscribe Cleanup: Moved delivery " + delivery.getId() + " to DLQ");
        }

        return savedSub;
    }

    public List<Subscription> listSubscribers() {
        return subscriptionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Subscription> listUnsubscribed(String clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client not found: " + clientId);
        }

        return subscriptionRepository.findByClientIdAndStatus(clientId, SubscriptionStatus.INACTIVE);
    }

}
