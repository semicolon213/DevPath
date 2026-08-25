package com.devpath.analysis.application;

import com.devpath.analysis.domain.CompletedAnalysis;
import java.time.Instant;
import java.util.UUID;

public record AnalysisResultView(
    UUID analysisId, UUID repositoryId, UUID snapshotId, UUID evaluationId,
    UUID skillMatrixId, String analysisScope, boolean currentForRepository, Instant completedAt
) {
    static AnalysisResultView from(CompletedAnalysis analysis, boolean currentForRepository) {
        return new AnalysisResultView(analysis.id(), analysis.repositoryId(), analysis.snapshotId(),
            analysis.evaluationId(), analysis.skillMatrixId(), analysis.analysisScope(), currentForRepository,
            analysis.completedAt());
    }
}
