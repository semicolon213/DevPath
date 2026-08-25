package com.devpath.learning.application;

import java.time.Instant;
import java.util.UUID;

public interface LearningAuditPort {
    void record(LearningAuditEvent event, UUID userId, UUID roadmapId, Instant occurredAt);
}
