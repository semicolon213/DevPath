package com.devpath.knowledge.application;

import com.devpath.knowledge.domain.KnowledgeChunk;
import java.util.UUID;

public record KnowledgeChunkSummaryView(
    UUID chunkId, int position, String heading, String contentHash, int tokenEstimate, String status
) {
    static KnowledgeChunkSummaryView from(KnowledgeChunk chunk) {
        return new KnowledgeChunkSummaryView(chunk.id(), chunk.position(), chunk.heading(), chunk.contentHash(),
            chunk.tokenEstimate(), chunk.status());
    }
}
