package com.devpath.integration.application;

public class NotionIntegrationUnavailableException extends RuntimeException {
    public NotionIntegrationUnavailableException(String message) { super(message); }
    public NotionIntegrationUnavailableException(String message, Throwable cause) { super(message, cause); }
}
