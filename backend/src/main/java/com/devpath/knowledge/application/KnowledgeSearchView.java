package com.devpath.knowledge.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record KnowledgeSearchView(
    UUID retrievalResultId,
    String retrievalType,
    String policyVersion,
    String contextPurpose,
    KnowledgeSearchFilters appliedFilters,
    List<KnowledgeSearchResultItemView> results,
    int resultCount,
    long durationMs,
    Instant generatedAt
) {
    public KnowledgeSearchView { results = List.copyOf(results); }
}
