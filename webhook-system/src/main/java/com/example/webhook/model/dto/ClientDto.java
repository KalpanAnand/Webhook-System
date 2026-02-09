package com.example.webhook.model.dto;

import com.example.webhook.model.Client;
import lombok.Data;

public class ClientDto {

    @Data
    public static class CreateRequest {
        private String organizationName;
        private String contactEmail;
    }

    @Data
    public static class Response {
        private String clientId;
        private String organizationName;
        private String contactEmail;
        private String message;

        public Response(Client client) {
            this.clientId = client.getClientId();
            this.organizationName = client.getOrganizationName();
            this.contactEmail = client.getContactEmail();
        }

        public Response(Client client, String message) {
            this(client);
            this.message = message;
        }
    }
}
