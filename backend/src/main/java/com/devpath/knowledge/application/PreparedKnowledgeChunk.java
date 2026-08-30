package com.devpath.knowledge.application;

import java.util.UUID;

public record PreparedKnowledgeChunk(
    UUID chunkId, int position, String heading, String objectReference, String contentHash,
    int tokenEstimate, EmbeddingVector embedding
) {}
