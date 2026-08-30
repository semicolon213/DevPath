package com.devpath.knowledge.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record KnowledgeRetrievalRecord(
    UUID requestId,
    UUID resultId,
    UUID userId,
    String queryHash,
    String contextPurpose,
    KnowledgeSearchFilters filters,
    int requestedLimit,
    String policyVersion,
    long durationMs,
    Instant completedAt,
    List<Item> items
) {
    public KnowledgeRetrievalRecord { items = List.copyOf(items); }
    public record Item(int position, UUID chunkId, double relevance) {}
}
