package com.devpath.rule.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SkillMatrixAuditPort {
    void record(SkillMatrixAuditEvent event, UUID userId, List<UUID> matrixIds, Instant occurredAt);
    void record(SkillMatrixAuditEvent event, UUID userId, UUID skillId, Instant occurredAt);
}
