package com.devpath.knowledge.application;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeSearchCandidate(
    UUID chunkId,
    UUID documentId,
    String documentTitle,
    String sourceObjectId,
    String sourceUrl,
    String heading,
    String objectReference,
    int tokenEstimate,
    double relevance,
    Instant sourceUpdatedAt
) {}
