package com.devpath.analysis.application;

import com.devpath.analysis.domain.AnalysisJob;
import java.time.Instant;
import java.util.UUID;

public record AnalysisJobView(
    UUID jobId, String jobType, String status, String phase, int progressPercent,
    int attemptCount, int maxAttempts, Instant submittedAt, Instant startedAt,
    Instant completedAt, String pollingUrl, String resultResourceUrl,
    String errorCode, String errorMessage, boolean retryable
) {
    static AnalysisJobView from(AnalysisJob job) {
        return new AnalysisJobView(job.id(), "REPOSITORY_ANALYSIS", job.status().name().toLowerCase(),
            job.phase(), job.progressPercent(), job.attemptCount(), job.maxAttempts(), job.submittedAt(),
            job.startedAt(), job.completedAt(), "/api/v1/analysis-jobs/" + job.id(),
            job.resultAnalysisId() == null ? null : "/api/v1/analyses/" + job.resultAnalysisId(),
            job.errorCode(), job.errorMessage(), "FAILED".equals(job.status().name()));
    }
}
