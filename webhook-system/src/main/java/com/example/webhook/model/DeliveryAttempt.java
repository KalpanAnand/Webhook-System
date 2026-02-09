package com.example.webhook.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_attempt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAttempt {
    @Id
    @Column(name = "_id")
    private String id;

    @Column(name = "webhook_delivery_id")
    private String webhookDeliveryId;

    @Column(name = "attempt_number")
    private Integer attemptNumber;

    @Column(name = "attempt_at")
    private LocalDateTime attemptAt;

    @Column(name = "status")
    private String status; // SUCCESS, FAILED

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "response_code")
    private Integer responseCode;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (attemptAt == null) {
            attemptAt = LocalDateTime.now();
        }
    }
}
