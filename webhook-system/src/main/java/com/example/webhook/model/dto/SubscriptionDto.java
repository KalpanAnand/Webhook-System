package com.example.webhook.model.dto;

import com.example.webhook.model.Subscription;
import lombok.Data;

public class SubscriptionDto {

    @Data
    public static class CreateRequest {
        private String clientId;
        private String webhookUrl;
        private String eventTypeId;
    }

    @Data
    public static class ModifyRequest {
        private String eventTypeId;
    }

    @Data
    public static class Response {
        private String subscriptionId;
        private String clientId;
        private String eventTypeId;
        private String webhookUrl;
        private String status;

        public Response(Subscription sub) {
            this.subscriptionId = sub.getSubscriptionId();
            this.clientId = sub.getClientId();
            this.eventTypeId = sub.getEventTypeId();
            this.webhookUrl = sub.getWebhookUrl();
            this.status = sub.getStatus().name();
        }
    }
}
