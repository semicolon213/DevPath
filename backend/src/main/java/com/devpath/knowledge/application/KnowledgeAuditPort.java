package com.devpath.knowledge.application;

import java.time.Instant;
import java.util.UUID;

public interface KnowledgeAuditPort {
    void record(KnowledgeAuditEvent event, UUID userId, UUID resourceId, Instant occurredAt);
    void recordRetrieval(UUID userId, UUID resultId, KnowledgeRetrievalAuditDetails details, Instant occurredAt);
}
