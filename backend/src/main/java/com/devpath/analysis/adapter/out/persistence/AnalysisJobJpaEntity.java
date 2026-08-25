package com.devpath.analysis.adapter.out.persistence;

import com.devpath.analysis.domain.AnalysisJob;
import com.devpath.analysis.domain.AnalysisJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_jobs")
class AnalysisJobJpaEntity {
    @Id @Column(name = "job_id") private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "repository_id", nullable = false, updatable = false) private UUID repositoryId;
    @Column(name = "snapshot_id", nullable = false, updatable = false) private UUID snapshotId;
    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 128) private String idempotencyKey;
    @Column(name = "analysis_scope", nullable = false, updatable = false, length = 32) private String analysisScope;
    @Column(name = "status", nullable = false, length = 16) private String status;
    @Column(name = "phase", nullable = false, length = 32) private String phase;
    @Column(name = "progress_percent", nullable = false) private int progressPercent;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "max_attempts", nullable = false) private int maxAttempts;
    @Column(name = "submitted_at", nullable = false, updatable = false) private Instant submittedAt;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "result_analysis_id") private UUID resultAnalysisId;
    @Column(name = "error_code", length = 64) private String errorCode;
    @Column(name = "error_message", length = 500) private String errorMessage;
    @Version @Column(name = "version", nullable = false) private long version;

    protected AnalysisJobJpaEntity() {}

    AnalysisJobJpaEntity(AnalysisJob job) {
        id = job.id(); userId = job.userId(); repositoryId = job.repositoryId(); snapshotId = job.snapshotId();
        idempotencyKey = job.idempotencyKey(); analysisScope = job.analysisScope(); status = job.status().name();
        phase = job.phase(); progressPercent = job.progressPercent(); attemptCount = job.attemptCount();
        maxAttempts = job.maxAttempts(); submittedAt = job.submittedAt(); startedAt = job.startedAt();
        completedAt = job.completedAt(); nextAttemptAt = job.nextAttemptAt();
        resultAnalysisId = job.resultAnalysisId(); errorCode = job.errorCode(); errorMessage = job.errorMessage();
        version = job.version();
    }

    AnalysisJob toDomain() {
        return new AnalysisJob(id, userId, repositoryId, snapshotId, idempotencyKey, analysisScope,
            AnalysisJobStatus.valueOf(status), phase, progressPercent, attemptCount, maxAttempts,
            submittedAt, startedAt, completedAt, nextAttemptAt, resultAnalysisId, errorCode, errorMessage, version);
    }
}
