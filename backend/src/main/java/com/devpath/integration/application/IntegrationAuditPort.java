package com.devpath.integration.application;

import java.time.Instant;
import java.util.UUID;

public interface IntegrationAuditPort {
    void record(IntegrationAuditEvent event, UUID userId, String resourceId, Instant occurredAt);
}
