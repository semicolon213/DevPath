package com.devpath.integration.application;

public class NotionConnectionNotFoundException extends RuntimeException {
    public NotionConnectionNotFoundException() {
        super("Notion connection was not found");
    }
}
