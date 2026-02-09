package com.example.webhook.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_delivery")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDelivery {

    @Id
    @Column(name = "_id")
    private String id;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "subscription_id")
    private String subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeliveryStatus status; // PENDING, SUCCESS, FAILED

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DeliveryStatus.PENDING;
        }
    }
}
