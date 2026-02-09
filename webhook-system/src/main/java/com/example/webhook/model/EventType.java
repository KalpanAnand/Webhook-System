package com.example.webhook.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "EVENT_TYPE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventType {
    @Id
    @Column(name = "event_type_id")
    private String eventTypeId;

    @Column(name = "event_name", unique = true, nullable = false)
    private String eventName;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (eventTypeId == null || eventTypeId.isBlank()) {
            eventTypeId = "et-" + java.util.UUID
                    .randomUUID()
                    .toString()
                    .substring(0, 3);
        }
    }
}
