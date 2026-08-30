package com.devpath.knowledge.adapter.out.persistence;

import com.devpath.knowledge.domain.KnowledgeIngestionJob;
import com.devpath.knowledge.domain.KnowledgeIngestionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_ingestion_jobs")
class KnowledgeIngestionJobJpaEntity {
    @Id @Column(name = "knowledge_ingestion_job_id") private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "source_connection_id", nullable = false, updatable = false) private UUID connectionId;
    @Column(name = "source_object_id", nullable = false, updatable = false) private String sourceObjectId;
    @Column(name = "knowledge_document_id", nullable = false, updatable = false) private UUID documentId;
    @Column(name = "idempotency_key", nullable = false, updatable = false) private String idempotencyKey;
    @Column(name = "status", nullable = false) private String status;
    @Column(name = "phase", nullable = false) private String phase;
    @Column(name = "progress_percent", nullable = false) private int progressPercent;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "max_attempts", nullable = false) private int maxAttempts;
    @Column(name = "submitted_at", nullable = false, updatable = false) private Instant submittedAt;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "error_code") private String errorCode;
    @Column(name = "error_message") private String errorMessage;
    @Version @Column(name = "version", nullable = false) private long version;

    protected KnowledgeIngestionJobJpaEntity() {}
    KnowledgeIngestionJobJpaEntity(KnowledgeIngestionJob job) { copy(job); }
    void update(KnowledgeIngestionJob job) { copy(job); }
    private void copy(KnowledgeIngestionJob job) {
        id=job.id(); userId=job.userId(); connectionId=job.connectionId(); sourceObjectId=job.sourceObjectId();
        documentId=job.documentId(); idempotencyKey=job.idempotencyKey(); status=job.status().name(); phase=job.phase();
        progressPercent=job.progressPercent(); attemptCount=job.attemptCount(); maxAttempts=job.maxAttempts();
        submittedAt=job.submittedAt(); startedAt=job.startedAt(); completedAt=job.completedAt();
        nextAttemptAt=job.nextAttemptAt(); errorCode=job.errorCode(); errorMessage=job.errorMessage();
    }
    KnowledgeIngestionJob toDomain() { return new KnowledgeIngestionJob(id,userId,connectionId,sourceObjectId,documentId,
        idempotencyKey,KnowledgeIngestionStatus.valueOf(status),phase,progressPercent,attemptCount,maxAttempts,
        submittedAt,startedAt,completedAt,nextAttemptAt,errorCode,errorMessage,version); }
}
