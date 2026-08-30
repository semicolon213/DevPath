package com.devpath.knowledge.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface KnowledgeSearchPersistencePort {
    List<KnowledgeSearchCandidate> search(UUID userId, EmbeddingVector queryEmbedding,
        KnowledgeSearchFilters filters, int limit, double minimumRelevance);
    Set<UUID> findAuthorizedCurrentChunkIds(UUID userId, List<UUID> chunkIds);
    void record(KnowledgeRetrievalRecord record);
}
