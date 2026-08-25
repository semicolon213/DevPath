package com.devpath.dashboard.application;

import java.time.Instant;
import java.util.UUID;

public interface DashboardAuditPort {
    void record(DashboardAuditEvent event, UUID userId, Instant occurredAt);
}
