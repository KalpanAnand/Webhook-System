package com.example.webhook.repository;

import com.example.webhook.model.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, String> {
        List<WebhookDelivery> findByStatus(com.example.webhook.model.DeliveryStatus status);

        List<WebhookDelivery> findByStatusAndNextAttemptAtBefore(com.example.webhook.model.DeliveryStatus status,
                        java.time.LocalDateTime timestamp);

        // For checking ordered delivery per subscription
        java.util.Optional<WebhookDelivery> findTopBySubscriptionIdAndStatusInOrderByCreatedAtAsc(String subscriptionId,
                        java.util.List<com.example.webhook.model.DeliveryStatus> statuses);

        List<WebhookDelivery> findBySubscriptionIdAndStatus(String subscriptionId,
                        com.example.webhook.model.DeliveryStatus status);
}
