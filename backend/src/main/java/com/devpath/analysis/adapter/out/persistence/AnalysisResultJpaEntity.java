package com.devpath.analysis.adapter.out.persistence;

import com.devpath.analysis.domain.CompletedAnalysis;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_results")
class AnalysisResultJpaEntity {
    @Id @Column(name = "analysis_id") private UUID id;
    @Column(name = "job_id", nullable = false, updatable = false) private UUID jobId;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "repository_id", nullable = false, updatable = false) private UUID repositoryId;
    @Column(name = "snapshot_id", nullable = false, updatable = false) private UUID snapshotId;
    @Column(name = "evaluation_id", nullable = false, updatable = false) private UUID evaluationId;
    @Column(name = "skill_matrix_id", nullable = false, updatable = false) private UUID skillMatrixId;
    @Column(name = "analysis_scope", nullable = false, updatable = false, length = 32) private String analysisScope;
    @Column(name = "completed_at", nullable = false, updatable = false) private Instant completedAt;

    protected AnalysisResultJpaEntity() {}

    AnalysisResultJpaEntity(CompletedAnalysis value) {
        id = value.id(); jobId = value.jobId(); userId = value.userId(); repositoryId = value.repositoryId();
        snapshotId = value.snapshotId(); evaluationId = value.evaluationId(); skillMatrixId = value.skillMatrixId();
        analysisScope = value.analysisScope(); completedAt = value.completedAt();
    }

    CompletedAnalysis toDomain() {
        return new CompletedAnalysis(id, jobId, userId, repositoryId, snapshotId, evaluationId,
            skillMatrixId, analysisScope, completedAt);
    }
}
