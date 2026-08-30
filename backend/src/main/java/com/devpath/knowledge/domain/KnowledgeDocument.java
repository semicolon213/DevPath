package com.devpath.knowledge.domain;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocument(
    UUID id, UUID userId, UUID connectionId, String sourceObjectId, String title, String status,
    UUID currentVersionId, Instant createdAt, Instant updatedAt, long version
) {}
