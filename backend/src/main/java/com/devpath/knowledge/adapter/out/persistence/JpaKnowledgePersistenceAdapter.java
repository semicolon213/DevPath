package com.devpath.knowledge.adapter.out.persistence;

import com.devpath.knowledge.application.KnowledgePersistencePort;
import com.devpath.knowledge.application.PreparedKnowledgeDocument;
import com.devpath.knowledge.domain.KnowledgeChunk;
import com.devpath.knowledge.domain.KnowledgeDocument;
import com.devpath.knowledge.domain.KnowledgeIngestionJob;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JpaKnowledgePersistenceAdapter implements KnowledgePersistencePort {
    private static final Collection<String> ACTIVE = List.of("QUEUED", "RUNNING");
    private final KnowledgeIngestionJobJpaRepository jobs;
    private final KnowledgeDocumentJpaRepository documents;
    private final KnowledgeDocumentVersionJpaRepository versions;
    private final KnowledgeChunkJpaRepository chunks;
    private final JdbcTemplate jdbc;

    public JpaKnowledgePersistenceAdapter(KnowledgeIngestionJobJpaRepository jobs,
        KnowledgeDocumentJpaRepository documents, KnowledgeDocumentVersionJpaRepository versions,
        KnowledgeChunkJpaRepository chunks, JdbcTemplate jdbc) {
        this.jobs=jobs; this.documents=documents; this.versions=versions; this.chunks=chunks; this.jdbc=jdbc;
    }

    @Override public Optional<KnowledgeIngestionJob> findJobByOwnerAndKey(UUID userId, String key) {
        return jobs.findByUserIdAndIdempotencyKey(userId,key).map(KnowledgeIngestionJobJpaEntity::toDomain);
    }
    @Override public Optional<KnowledgeIngestionJob> findJobByIdAndOwner(UUID jobId, UUID userId) {
        return jobs.findByIdAndUserId(jobId,userId).map(KnowledgeIngestionJobJpaEntity::toDomain);
    }
    @Override public Optional<KnowledgeIngestionJob> findActiveJob(UUID userId, String sourceObjectId) {
        return jobs.findFirstByUserIdAndSourceObjectIdAndStatusInOrderBySubmittedAtDesc(userId,sourceObjectId,ACTIVE)
            .map(KnowledgeIngestionJobJpaEntity::toDomain);
    }
    @Override public Optional<KnowledgeIngestionJob> findNextClaimable(Instant now) {
        return jobs.findNextClaimable(now).map(KnowledgeIngestionJobJpaEntity::toDomain);
    }
    @Override public KnowledgeIngestionJob saveJob(KnowledgeIngestionJob job) {
        KnowledgeIngestionJobJpaEntity entity = jobs.findById(job.id()).orElse(null);
        if (entity == null) entity = new KnowledgeIngestionJobJpaEntity(job); else entity.update(job);
        return jobs.saveAndFlush(entity).toDomain();
    }
    @Override public Optional<KnowledgeDocument> findDocumentByOwnerAndSource(UUID userId, String sourceObjectId) {
        return documents.findByUserIdAndSourceTypeAndSourceObjectId(userId,"NOTION",sourceObjectId)
            .map(KnowledgeDocumentJpaEntity::toDomain);
    }
    @Override public Optional<KnowledgeDocument> findDocumentByIdAndOwner(UUID documentId, UUID userId) {
        return documents.findByIdAndUserId(documentId,userId).map(KnowledgeDocumentJpaEntity::toDomain);
    }
    @Override public List<KnowledgeDocument> findDocumentsByOwner(UUID userId) {
        return documents.findAllByUserIdOrderByUpdatedAtDescIdAsc(userId).stream()
            .map(KnowledgeDocumentJpaEntity::toDomain).toList();
    }
    @Override public int countCurrentChunks(UUID documentId, UUID userId) {
        return documents.findByIdAndUserId(documentId,userId).map(KnowledgeDocumentJpaEntity::currentVersionId)
            .map(chunks::countByDocumentVersionId).orElse(0);
    }
    @Override public List<KnowledgeChunk> findCurrentChunks(UUID documentId, UUID userId) {
        return documents.findByIdAndUserId(documentId,userId).map(KnowledgeDocumentJpaEntity::currentVersionId)
            .map(chunks::findAllByDocumentVersionIdOrderByPositionAsc).orElseGet(List::of).stream()
            .map(KnowledgeChunkJpaEntity::toDomain).toList();
    }

    @Override
    public void complete(KnowledgeIngestionJob job, PreparedKnowledgeDocument prepared, Instant now) {
        KnowledgeDocumentJpaEntity document = documents.findByIdAndUserId(job.documentId(),job.userId()).orElse(null);
        if (document == null) {
            document = documents.saveAndFlush(new KnowledgeDocumentJpaEntity(job.documentId(),job.userId(),job.connectionId(),
                job.sourceObjectId(),prepared.title(),now));
        }
        UUID previousVersionId = document.currentVersionId();
        if (previousVersionId != null) {
            versions.findByIdAndDocumentId(previousVersionId, document.id()).ifPresent(version -> {
                version.supersede(); versions.save(version);
            });
            chunks.findAllByDocumentVersionIdOrderByPositionAsc(previousVersionId).forEach(chunk -> {
                chunk.stale(); chunks.save(chunk);
            });
            jdbc.update("UPDATE embedding_records SET embedding_status='STALE' WHERE user_id=? AND knowledge_chunk_id IN " +
                "(SELECT knowledge_chunk_id FROM knowledge_chunks WHERE knowledge_document_version_id=?)",
                job.userId(), previousVersionId);
        }
        versions.saveAndFlush(new KnowledgeDocumentVersionJpaEntity(prepared.versionId(),document.id(),job.userId(),
            versions.maxVersionNumber(document.id())+1,prepared.contentHash(),prepared.objectReference(),
            prepared.sourceUpdatedAt(),now));
        for (var chunk : prepared.chunks()) {
            chunks.saveAndFlush(new KnowledgeChunkJpaEntity(chunk.chunkId(),prepared.versionId(),job.userId(),chunk.position(),
                chunk.heading(),chunk.objectReference(),chunk.contentHash(),chunk.tokenEstimate(),now));
            var vector = chunk.embedding().values().stream().map(String::valueOf).collect(Collectors.joining(",","[","]"));
            jdbc.update("INSERT INTO embedding_records (embedding_record_id,knowledge_chunk_id,user_id,provider,model," +
                "model_version,dimension,content_hash,embedding,embedding_status,created_at) " +
                "VALUES (?,?,?,?,?,?,?,?,CAST(? AS vector),'ACTIVE',?)",
                UUID.randomUUID(),chunk.chunkId(),job.userId(),chunk.embedding().provider(),chunk.embedding().model(),
                chunk.embedding().modelVersion(),chunk.embedding().dimension(),chunk.contentHash(),vector,
                java.sql.Timestamp.from(now));
        }
        document.activate(prepared.title(),prepared.versionId(),now);
        documents.saveAndFlush(document);
    }

    @Override public KnowledgeDocument archive(KnowledgeDocument value, Instant now) {
        KnowledgeDocumentJpaEntity document = documents.findByIdAndUserId(value.id(),value.userId())
            .orElseThrow();
        document.archive(now);
        if (document.currentVersionId()!=null) {
            chunks.findAllByDocumentVersionIdOrderByPositionAsc(document.currentVersionId()).forEach(chunk -> {
                chunk.stale(); chunks.save(chunk);
            });
            jdbc.update("UPDATE embedding_records SET embedding_status='STALE' WHERE user_id=? AND knowledge_chunk_id IN " +
                "(SELECT knowledge_chunk_id FROM knowledge_chunks WHERE knowledge_document_version_id=?)",
                value.userId(),document.currentVersionId());
        }
        return documents.saveAndFlush(document).toDomain();
    }
}
