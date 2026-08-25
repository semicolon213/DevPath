package com.devpath.repository.application;

import com.devpath.repository.domain.RepositorySyncJob;
import java.time.Instant;
import java.util.UUID;

public record RepositorySyncJobView(
    UUID jobId,
    String jobType,
    String status,
    String phase,
    int progressPercent,
    int attemptCount,
    int maxAttempts,
    Instant submittedAt,
    Instant startedAt,
    Instant completedAt,
    String pollingUrl,
    String resultResourceUrl,
    String errorCode,
    String errorMessage,
    boolean retryable
) {
    static RepositorySyncJobView from(RepositorySyncJob job) {
        return new RepositorySyncJobView(
            job.id(), "REPOSITORY_SYNC", job.status().name().toLowerCase(), job.phase(),
            job.progressPercent(), job.attemptCount(), job.maxAttempts(), job.submittedAt(),
            job.startedAt(), job.completedAt(), "/api/v1/repository-sync-jobs/" + job.id(),
            job.resultSnapshotId() == null ? null : "/api/v1/repositories/" + job.repositoryId()
                + "/snapshots/" + job.resultSnapshotId(),
            job.errorCode(), job.errorMessage(), job.status().name().equals("FAILED")
        );
    }
}
