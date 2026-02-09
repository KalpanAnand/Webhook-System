package com.example.webhook.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "DEAD_LETTER_QUEUE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterQueue {
    @Id
    @Column(name = "dlq_id")
    private String dlqId;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "subscription_id")
    private String subscriptionId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }
        if (dlqId == null) {
            dlqId = "dlq-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
        }
    }
}
