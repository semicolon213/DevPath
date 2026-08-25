package com.devpath.analysis.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CompletedAnalysis(
    UUID id, UUID jobId, UUID userId, UUID repositoryId, UUID snapshotId,
    UUID evaluationId, UUID skillMatrixId, String analysisScope, Instant completedAt
) {
    public CompletedAnalysis {
        Objects.requireNonNull(id); Objects.requireNonNull(jobId); Objects.requireNonNull(userId);
        Objects.requireNonNull(repositoryId); Objects.requireNonNull(snapshotId); Objects.requireNonNull(evaluationId);
        Objects.requireNonNull(skillMatrixId); Objects.requireNonNull(completedAt);
        if (!"REPOSITORY_BASELINE".equals(analysisScope)) {
            throw new IllegalArgumentException("Analysis scope is invalid");
        }
    }
}
