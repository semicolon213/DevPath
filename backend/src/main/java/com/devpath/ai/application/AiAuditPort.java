package com.devpath.ai.application;

import java.time.Instant;
import java.util.UUID;

public interface AiAuditPort {
    void record(AiAuditEvent event, UUID userId, UUID resourceId, Instant occurredAt);
}
