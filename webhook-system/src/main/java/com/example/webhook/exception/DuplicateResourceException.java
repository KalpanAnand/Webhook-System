package com.example.webhook.exception;

public class DuplicateResourceException extends RuntimeException {
    private String resourceId;

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, String resourceId) {
        super(message);
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }
}
