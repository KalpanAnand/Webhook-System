package com.example.webhook.controller;

import com.example.webhook.model.DeliveryAttempt;
import com.example.webhook.model.WebhookDelivery;
import com.example.webhook.repository.DeliveryAttemptRepository;
import com.example.webhook.repository.WebhookDeliveryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/webhook-deliveries")
public class WebhookDeliveryController {

    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;

    public WebhookDeliveryController(
            WebhookDeliveryRepository webhookDeliveryRepository,
            DeliveryAttemptRepository deliveryAttemptRepository) {
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listDeliveries() {
        List<WebhookDelivery> deliveries = webhookDeliveryRepository.findAll();
        
        List<Map<String, Object>> result = deliveries.stream().map(delivery -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", delivery.getId());
            map.put("eventId", delivery.getEventId());
            map.put("subscriptionId", delivery.getSubscriptionId());
            map.put("status", delivery.getStatus() != null ? delivery.getStatus().name() : "UNKNOWN");
            map.put("nextAttemptAt", delivery.getNextAttemptAt());
            map.put("createdAt", delivery.getCreatedAt());
            
            // Get attempt count
            int attemptCount = deliveryAttemptRepository.countByWebhookDeliveryId(delivery.getId());
            map.put("attemptCount", attemptCount);
            
            // Get latest attempt
            List<DeliveryAttempt> attempts = deliveryAttemptRepository.findByWebhookDeliveryId(delivery.getId());
            if (!attempts.isEmpty()) {
                // Sort by attemptAt descending to get latest
                DeliveryAttempt latest = attempts.stream()
                    .sorted((a, b) -> {
                        if (a.getAttemptAt() == null) return 1;
                        if (b.getAttemptAt() == null) return -1;
                        return b.getAttemptAt().compareTo(a.getAttemptAt());
                    })
                    .findFirst()
                    .orElse(attempts.get(0));
                map.put("lastAttemptAt", latest.getAttemptAt());
                map.put("lastAttemptStatus", latest.getStatus());
                map.put("lastAttemptError", latest.getErrorMessage());
                map.put("lastAttemptResponseCode", latest.getResponseCode());
            }
            
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/attempts")
    public ResponseEntity<List<DeliveryAttempt>> getDeliveryAttempts(@PathVariable String id) {
        List<DeliveryAttempt> attempts = deliveryAttemptRepository.findByWebhookDeliveryId(id);
        return ResponseEntity.ok(attempts);
    }
}
