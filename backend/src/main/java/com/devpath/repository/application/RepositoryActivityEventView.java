package com.devpath.repository.application;

import com.devpath.repository.domain.RepositoryActivityEvent;
import java.time.Instant;

public record RepositoryActivityEventView(String eventType, String sourceReference, Instant occurredAt) {
    static RepositoryActivityEventView from(RepositoryActivityEvent value) {
        return new RepositoryActivityEventView(value.eventType(), value.sourceReference(), value.occurredAt());
    }
}
