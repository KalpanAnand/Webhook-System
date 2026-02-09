package com.example.webhook.client;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class QueueClient {

    private final RestTemplate restTemplate;

    @Value("${queue.base-url}")
    private String baseUrl;

    @Value("${queue.api-token}")
    private String apiToken;

    public QueueClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Data
    public static class QueueResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("queue_id")
        private String queueId;
        @com.fasterxml.jackson.annotation.JsonProperty("post_url")
        private String postUrl;
        @com.fasterxml.jackson.annotation.JsonProperty("get_url")
        private String getUrl;
    }

    /**
     * Create a queue (required)
     */
    public QueueResponse createQueue(String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"name\": \"" + name + "\"}";
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<QueueResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/queues",
                request,
                QueueResponse.class);

        return response.getBody();
    }

    /**
     * Push a delivery message to the queue
     */
    public void push(String queueId, String payload) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("payload", payload);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        String pushUrl = baseUrl + "/queue/" + queueId + "/push";

        ResponseEntity<Void> response = restTemplate.postForEntity(pushUrl, request, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Queue push failed: " + response.getStatusCode());
        }

        System.out.println("✅ PUSHED TO QUEUE [" + queueId + "]");
    }

    /**
     * Pop a message from the queue
     */
    public String pop(String queueId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        String popUrl = baseUrl + "/queue/" + queueId + "/pop";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    popUrl,
                    HttpMethod.GET,
                    request,
                    String.class);
            // System.out.println("🧪 RAW QUEUE RESPONSE = [" + response + "]");

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String body = response.getBody();
                // System.out.println("🔍 RAW POP RESPONSE for " + queueId + ": " + body); //
                // Enable for debugging

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(body);

                if (node.has("message") && !node.get("message").isNull()) {
                    com.fasterxml.jackson.databind.JsonNode msgNode = node.get("message");
                    return msgNode.isContainerNode() ? msgNode.toString() : msgNode.asText();
                } else if (node.has("payload") && !node.get("payload").isNull()) {
                    com.fasterxml.jackson.databind.JsonNode payloadNode = node.get("payload");
                    return payloadNode.isContainerNode() ? payloadNode.toString() : payloadNode.asText();
                } else {
                    // Queue might be returning empty JSON {} if empty
                }
            }

        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // Queue not found (404), likely expired or deleted. Return null.
            return null;
        } catch (Exception e) {
            System.err.println("⚠️ Queue pop error for [" + queueId + "]: " + e.getMessage());
        }

        return null;
    }

    // @Data
    // public static class MessageResponse {

    // @com.fasterxml.jackson.annotation.JsonProperty("payload")
    // private String payload;
    // }
}