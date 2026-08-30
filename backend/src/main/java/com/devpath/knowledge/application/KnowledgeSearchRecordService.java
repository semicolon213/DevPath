package com.devpath.knowledge.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class KnowledgeSearchRecordService {
    private final KnowledgeSearchPersistencePort persistence;
    private final KnowledgeAuditPort audit;

    KnowledgeSearchRecordService(KnowledgeSearchPersistencePort persistence, KnowledgeAuditPort audit) {
        this.persistence = persistence;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    List<KnowledgeSearchCandidate> retrieve(UUID userId, EmbeddingVector embedding,
        KnowledgeSearchFilters filters, int limit, double minimumRelevance) {
        return persistence.search(userId, embedding, filters, limit, minimumRelevance);
    }

    @Transactional(readOnly = true)
    List<KnowledgeSearchCandidate> retainAuthorized(UUID userId, List<KnowledgeSearchCandidate> candidates) {
        if (candidates.isEmpty()) return List.of();
        var allowed = persistence.findAuthorizedCurrentChunkIds(userId,
            candidates.stream().map(KnowledgeSearchCandidate::chunkId).toList());
        return candidates.stream().filter(candidate -> allowed.contains(candidate.chunkId())).toList();
    }

    @Transactional
    void record(KnowledgeRetrievalRecord record) {
        persistence.record(record);
        audit.recordRetrieval(record.userId(), record.resultId(), new KnowledgeRetrievalAuditDetails(
            String.join(",", record.filters().sourceTypes()), record.filters().documentIds().size(),
            record.items().size(), record.policyVersion(), record.contextPurpose()), record.completedAt());
    }
}
