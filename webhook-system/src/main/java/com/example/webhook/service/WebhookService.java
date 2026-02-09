package com.example.webhook.service;

import com.example.webhook.client.QueueClient;
import com.example.webhook.model.*;
import com.example.webhook.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WebhookService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private QueueClient queueClient;

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    /**
     * Ingest event and enqueue deliveries
     */
    @Transactional
    public Event ingestEvent(String eventTypeId, String payload) {
        System.out.println("🚀 ingestEvent CALLED | eventType=" + eventTypeId);
        /*
         * -------------------------------------------------
         * 1. Persist incoming event
         * -------------------------------------------------
         */
        Event event = new Event();
        event.setEventTypeId(eventTypeId);
        event.setPayload(payload);
        event.setReceivedAt(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);

        /*
         * -------------------------------------------------
         * 2. Fetch active subscriptions for this event
         * -------------------------------------------------
         */
        List<Subscription> subscriptions = subscriptionRepository.findByEventTypeIdAndStatus(
                eventTypeId, SubscriptionStatus.ACTIVE);

        /*
         * -------------------------------------------------
         * 3. Create delivery + push to each subscriber queue
         * -------------------------------------------------
         */
        System.out.println("🚀 ingestEvent CALLED | eventType=" + eventTypeId);
        for (Subscription sub : subscriptions) {

            // Create delivery record
            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setEventId(savedEvent.getEventId());
            delivery.setSubscriptionId(sub.getSubscriptionId());
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setNextAttemptAt(LocalDateTime.now());

            webhookDeliveryRepository.save(delivery);

            // Validate queueId
            String queueId = sub.getQueueId();
            if (queueId == null || queueId.isBlank()) {
                delivery.setStatus(DeliveryStatus.FAILED);
                webhookDeliveryRepository.save(delivery);

                System.err.println(
                        "Skipping queue push for subscription "
                                + sub.getSubscriptionId()
                                + " : queueId missing");
                continue;
            }

            // Push deliveryId into queue
            try {
                String message = String.format("{\"deliveryId\":\"%s\"}", delivery.getId());

                queueClient.push(queueId, message);

            } catch (Exception e) {

                delivery.setStatus(DeliveryStatus.FAILED);
                webhookDeliveryRepository.save(delivery);

                System.err.println(
                        "❌ Failed to push delivery "
                                + delivery.getId()
                                + " to queue "
                                + queueId
                                + " : " + e.getMessage());
            }
        }

        return savedEvent;
    }
}