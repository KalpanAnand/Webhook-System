package com.example.webhook.repository;

import com.example.webhook.model.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, String> {
    List<DeliveryAttempt> findByStatus(String status);

    int countByWebhookDeliveryId(String webhookDeliveryId);
    
    List<DeliveryAttempt> findByWebhookDeliveryId(String webhookDeliveryId);
}
