package com.devpath.repository.application;

import java.time.Instant;
import java.util.UUID;

public interface RepositoryAuditPort {
    void record(RepositoryAuditEvent event, UUID userId, UUID repositoryId, Instant occurredAt);
}
