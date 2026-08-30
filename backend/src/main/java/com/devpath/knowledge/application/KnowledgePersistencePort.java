package com.devpath.knowledge.application;

import com.devpath.knowledge.domain.KnowledgeChunk;
import com.devpath.knowledge.domain.KnowledgeDocument;
import com.devpath.knowledge.domain.KnowledgeIngestionJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgePersistencePort {
    Optional<KnowledgeIngestionJob> findJobByOwnerAndKey(UUID userId, String key);
    Optional<KnowledgeIngestionJob> findJobByIdAndOwner(UUID jobId, UUID userId);
    Optional<KnowledgeIngestionJob> findActiveJob(UUID userId, String sourceObjectId);
    Optional<KnowledgeIngestionJob> findNextClaimable(Instant now);
    KnowledgeIngestionJob saveJob(KnowledgeIngestionJob job);
    Optional<KnowledgeDocument> findDocumentByOwnerAndSource(UUID userId, String sourceObjectId);
    Optional<KnowledgeDocument> findDocumentByIdAndOwner(UUID documentId, UUID userId);
    List<KnowledgeDocument> findDocumentsByOwner(UUID userId);
    int countCurrentChunks(UUID documentId, UUID userId);
    List<KnowledgeChunk> findCurrentChunks(UUID documentId, UUID userId);
    void complete(KnowledgeIngestionJob job, PreparedKnowledgeDocument document, Instant now);
    KnowledgeDocument archive(KnowledgeDocument document, Instant now);
}
