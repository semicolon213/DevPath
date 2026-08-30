package com.devpath.knowledge.domain;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeChunk(
    UUID id, UUID documentVersionId, UUID userId, int position, String heading, String objectReference,
    String contentHash, int tokenEstimate, String status, Instant createdAt
) {}
