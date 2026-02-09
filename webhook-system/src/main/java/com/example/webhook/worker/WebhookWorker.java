package com.example.webhook.worker;

import com.example.webhook.model.*;
import com.example.webhook.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class WebhookWorker {

    @Autowired
    private DeliveryAttemptRepository deliveryAttemptRepository;

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private DeadLetterQueueRepository deadLetterQueueRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${delivery.max-retries:5}")
    private int maxRetries;

    @Async
    public void processDelivery(WebhookDelivery delivery) {
        System.out.println("⚡️ Async Worker started for delivery: " + delivery.getId() + " on thread: "
                + Thread.currentThread().getName());

        // Double check timing
        if (delivery.getNextAttemptAt() != null &&
                delivery.getNextAttemptAt().isAfter(LocalDateTime.now())) {
            return;
        }

        int attemptNumber = deliveryAttemptRepository.countByWebhookDeliveryId(delivery.getId()) + 1;

        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.setWebhookDeliveryId(delivery.getId());
        attempt.setAttemptNumber(attemptNumber);
        attempt.setAttemptAt(LocalDateTime.now());
        attempt.setStatus("PENDING");

        attempt = deliveryAttemptRepository.save(attempt);

        try {
            Optional<Event> eventOpt = eventRepository.findById(delivery.getEventId());
            Optional<Subscription> subOpt = subscriptionRepository.findById(delivery.getSubscriptionId());

            if (eventOpt.isEmpty() || subOpt.isEmpty()) {
                failAttempt(delivery, attempt, "Event or Subscription not found");
                return;
            }

            Event event = eventOpt.get();
            Subscription sub = subOpt.get();

            String webhookUrl = sub.getWebhookUrl();
            if (webhookUrl == null || webhookUrl.isBlank() || !webhookUrl.startsWith("http")) {
                failAttempt(delivery, attempt, "Invalid webhook URL");
                return;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(event.getPayload()))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            attempt.setResponseCode(response.statusCode());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                attempt.setStatus("SUCCESS");
                delivery.setStatus(DeliveryStatus.SUCCESS);

                deliveryAttemptRepository.save(attempt);
                webhookDeliveryRepository.save(delivery);

                System.out.println("✅ Delivery SUCCESS: " + delivery.getId());

                // --- CHAIN REACTION ---
                triggerNext(sub.getSubscriptionId());

            } else {
                retryDelivery(delivery, attempt, "Non-2xx response: " + response.statusCode(), attemptNumber);
            }

        } catch (Exception e) {
            retryDelivery(delivery, attempt, e.getMessage(), attemptNumber);
        }
    }

    private void failAttempt(WebhookDelivery delivery, DeliveryAttempt attempt, String reason) {
        attempt.setStatus("FAILED");
        attempt.setErrorMessage(reason);
        deliveryAttemptRepository.save(attempt);

        delivery.setStatus(DeliveryStatus.FAILED);
        webhookDeliveryRepository.save(delivery);
    }

    private void retryDelivery(WebhookDelivery delivery, DeliveryAttempt attempt, String reason, int attemptNumber) {
        attempt.setStatus("FAILED");
        attempt.setErrorMessage(reason);
        deliveryAttemptRepository.save(attempt);

        if (attemptNumber >= maxRetries) {
            delivery.setStatus(DeliveryStatus.FAILED);
            webhookDeliveryRepository.save(delivery);

            DeadLetterQueue dlq = new DeadLetterQueue();
            dlq.setEventId(delivery.getEventId());
            dlq.setSubscriptionId(delivery.getSubscriptionId());
            dlq.setFailureReason(reason);
            deadLetterQueueRepository.save(dlq);

            System.err.println("💀 Moved to DLQ: " + delivery.getId());
            triggerNext(delivery.getSubscriptionId());

        } else {
            long backoffSeconds = 5 * (long) Math.pow(2, attemptNumber - 1);
            delivery.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds));
            webhookDeliveryRepository.save(delivery);

            System.out.println("zzZ Retry scheduled for " + delivery.getId() + " in " + backoffSeconds + "s");
        }
    }

    private void triggerNext(String subscriptionId) {

        Optional<WebhookDelivery> next = webhookDeliveryRepository
                .findTopBySubscriptionIdAndStatusInOrderByCreatedAtAsc(
                        subscriptionId,
                        java.util.List.of(DeliveryStatus.PENDING));

        if (next.isPresent()) {
            WebhookDelivery delivery = next.get();
            if (delivery.getNextAttemptAt() == null ||
                    delivery.getNextAttemptAt().isBefore(LocalDateTime.now())) {
                System.out.println("🔗 Chain Reaction: Triggering next delivery " + delivery.getId());
                self.processDelivery(delivery);
            }
        }
    }

    @Autowired
    @org.springframework.context.annotation.Lazy
    private WebhookWorker self;
}
