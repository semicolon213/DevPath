package com.devpath.repository.domain;

import java.time.Instant;
import java.util.Objects;

public record RepositoryActivityEvent(
    String eventType,
    String sourceReference,
    Instant occurredAt
) {
    public RepositoryActivityEvent {
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(sourceReference);
        Objects.requireNonNull(occurredAt);
        if (!eventType.matches("COMMIT|PULL_REQUEST_OPENED|PULL_REQUEST_CLOSED|PULL_REQUEST_MERGED|ISSUE_OPENED|ISSUE_CLOSED")
            || sourceReference.isBlank() || sourceReference.length() > 128) {
            throw new IllegalArgumentException("Repository activity event is invalid");
        }
    }
}
