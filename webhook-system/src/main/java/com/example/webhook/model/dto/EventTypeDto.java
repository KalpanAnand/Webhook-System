package com.example.webhook.model.dto;

import com.example.webhook.model.EventType;
import lombok.Data;

public class EventTypeDto {

    @Data
    public static class CreateRequest {
        private String eventName;
        private String description;
    }

    @Data
    public static class UpdateRequest {
        private String description;
    }

    @Data
    public static class Response {
        private String eventTypeId;
        private String eventName;
        private String description;
        private String message; // Optional, for legacy support or info messages

        public Response(EventType eventType) {
            this.eventTypeId = eventType.getEventTypeId();
            this.eventName = eventType.getEventName();
            this.description = eventType.getDescription();
        }

        public Response(EventType eventType, String message) {
            this(eventType);
            this.message = message;
        }
    }
}
