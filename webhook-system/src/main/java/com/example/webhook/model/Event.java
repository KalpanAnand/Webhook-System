package com.example.webhook.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "EVENT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "event_type_id")
    private String eventTypeId;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        if (eventId == null || eventId.isBlank()) {
            eventId = "evt-" + java.util.UUID
                    .randomUUID()
                    .toString()
                    .substring(0, 3);
        }
    }
}
