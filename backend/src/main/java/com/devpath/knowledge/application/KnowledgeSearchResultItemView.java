package com.devpath.knowledge.application;

import java.util.UUID;

public record KnowledgeSearchResultItemView(
    UUID chunkId,
    UUID documentId,
    String documentTitle,
    String sourceType,
    String sourceObjectId,
    String sourceUrl,
    String heading,
    String excerpt,
    double relevance,
    int tokenEstimate,
    String freshness
) {}
