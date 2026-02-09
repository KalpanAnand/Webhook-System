package com.example.webhook.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "SUBSCRIPTION")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @Column(name = "subscription_id")
    private String subscriptionId;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "event_type_id", nullable = false)
    private String eventTypeId;

    @Column(name = "webhook_url", nullable = false)
    private String webhookUrl;

    // Only this is required for QueueMock
    @Column(name = "queue_id")
    private String queueId;

    @Column(name = "queue_post_url")
    private String queuePostUrl;

    @Column(name = "queue_get_url")
    private String queueGetUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SubscriptionStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        if (subscriptionId == null || subscriptionId.isBlank()) {
            subscriptionId = "sub-" + UUID.randomUUID().toString().substring(0, 8);
        }

        if (status == null) {
            status = SubscriptionStatus.ACTIVE;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}