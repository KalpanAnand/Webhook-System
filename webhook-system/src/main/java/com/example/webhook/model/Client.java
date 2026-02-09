package com.example.webhook.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "CLIENT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    @Id
    @Column(name = "client_id")
    private String clientId;

    @Column(name = "organization_name")
    private String organizationName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (clientId == null || clientId.isBlank()) {
            clientId = "c-" + java.util.UUID
                    .randomUUID()
                    .toString()
                    .substring(0, 3);
        }
    }
}
