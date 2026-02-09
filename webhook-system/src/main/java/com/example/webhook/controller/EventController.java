package com.example.webhook.controller;

import com.example.webhook.model.Event;
import com.example.webhook.service.WebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private com.example.webhook.repository.EventRepository eventRepository;

    @PostMapping
    public ResponseEntity<Event> ingestEvent(@RequestBody IngestRequest request) {
        // Requirement 11: POST /api/events with body { eventTypeId: "...", payload:
        // {...} }
        // Payload can be any JSON, so we'll take it as Object or String.
        // WebhookService expects String payload.
        String payloadStr = convertPayloadToString(request.getPayload());
        Event event = webhookService.ingestEvent(request.getEventTypeId(), payloadStr);
        return ResponseEntity.ok(event);
    }

    @GetMapping
    public ResponseEntity<?> listEvents() {
        // Requirement 12: List Events
        return ResponseEntity.ok(eventRepository.findAll());
    }

    private String convertPayloadToString(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return String.valueOf(payload);
        }
    }

    public static class IngestRequest {
        private String eventTypeId;
        private Object payload;

        public String getEventTypeId() {
            return eventTypeId;
        }

        public void setEventTypeId(String eventTypeId) {
            this.eventTypeId = eventTypeId;
        }

        public Object getPayload() {
            return payload;
        }

        public void setPayload(Object payload) {
            this.payload = payload;
        }
    }
}
