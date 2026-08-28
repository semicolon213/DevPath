package com.devpath.integration.application;

public class NotionRateLimitExceededException extends RuntimeException {
    private final Long retryAfterSeconds;

    public NotionRateLimitExceededException(Long retryAfterSeconds) {
        super("Notion request limit was reached");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long retryAfterSeconds() { return retryAfterSeconds; }
}
