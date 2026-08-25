package com.devpath.analysis.application;

import java.time.Instant;
import java.util.UUID;

public interface AnalysisAuditPort {
    void record(AnalysisAuditEvent event, UUID userId, String resourceId, Instant occurredAt);
}
