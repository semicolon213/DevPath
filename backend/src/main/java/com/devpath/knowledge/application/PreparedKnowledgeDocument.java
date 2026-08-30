package com.devpath.knowledge.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PreparedKnowledgeDocument(
    UUID documentId, UUID versionId, String title, String sourceObjectId, Instant sourceUpdatedAt,
    String contentHash, String objectReference, List<PreparedKnowledgeChunk> chunks
) {
    public PreparedKnowledgeDocument { chunks = List.copyOf(chunks); }
}
