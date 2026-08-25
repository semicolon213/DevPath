package com.devpath.analysis.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AnalysisJob(
    UUID id, UUID userId, UUID repositoryId, UUID snapshotId, String idempotencyKey,
    String analysisScope, AnalysisJobStatus status, String phase, int progressPercent,
    int attemptCount, int maxAttempts, Instant submittedAt, Instant startedAt,
    Instant completedAt, Instant nextAttemptAt, UUID resultAnalysisId,
    String errorCode, String errorMessage, long version
) {
    public AnalysisJob {
        Objects.requireNonNull(id); Objects.requireNonNull(userId); Objects.requireNonNull(repositoryId);
        Objects.requireNonNull(snapshotId); Objects.requireNonNull(status); Objects.requireNonNull(submittedAt);
        Objects.requireNonNull(nextAttemptAt);
        idempotencyKey = required(idempotencyKey, 128);
        analysisScope = required(analysisScope, 32);
        phase = required(phase, 32);
        if (!"REPOSITORY_BASELINE".equals(analysisScope) || progressPercent < 0 || progressPercent > 100
            || attemptCount < 0 || maxAttempts < 1 || attemptCount > maxAttempts) {
            throw new IllegalArgumentException("Analysis job state is invalid");
        }
    }

    public static AnalysisJob queue(
        UUID userId, UUID repositoryId, UUID snapshotId, String idempotencyKey, String scope, Instant now
    ) {
        return new AnalysisJob(UUID.randomUUID(), userId, repositoryId, snapshotId, idempotencyKey, scope,
            AnalysisJobStatus.QUEUED, "QUEUED", 0, 0, 3, now, null, null, now,
            null, null, null, 0);
    }

    public AnalysisJob start(Instant now) {
        if (status != AnalysisJobStatus.QUEUED) return this;
        return copy(AnalysisJobStatus.RUNNING, "EVALUATING_RULES", 20, attemptCount + 1,
            startedAt == null ? now : startedAt, null, nextAttemptAt, null, null, null);
    }

    public AnalysisJob succeed(UUID analysisId, Instant now) {
        if (status != AnalysisJobStatus.RUNNING) throw new IllegalStateException("Only a running analysis job can succeed");
        return copy(AnalysisJobStatus.SUCCEEDED, "COMPLETED", 100, attemptCount, startedAt, now,
            nextAttemptAt, analysisId, null, null);
    }

    public AnalysisJob failOrRetry(String code, String message, Instant now) {
        if (status != AnalysisJobStatus.RUNNING) return this;
        String safeMessage = message == null || message.isBlank() ? "Analysis failed safely" : message;
        if (safeMessage.length() > 500) safeMessage = safeMessage.substring(0, 500);
        if (attemptCount < maxAttempts) {
            return copy(AnalysisJobStatus.QUEUED, "RETRY_WAIT", 0, attemptCount, startedAt, null,
                now.plus(Duration.ofSeconds(30)), null, code, safeMessage);
        }
        return copy(AnalysisJobStatus.FAILED, "FAILED", progressPercent, attemptCount, startedAt, now,
            nextAttemptAt, null, code, safeMessage);
    }

    private AnalysisJob copy(AnalysisJobStatus newStatus, String newPhase, int progress, int attempts,
        Instant started, Instant completed, Instant nextAttempt, UUID analysisId, String code, String message) {
        return new AnalysisJob(id, userId, repositoryId, snapshotId, idempotencyKey, analysisScope, newStatus,
            newPhase, progress, attempts, maxAttempts, submittedAt, started, completed, nextAttempt,
            analysisId, code, message, version);
    }

    private static String required(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException("Analysis job value is invalid");
        }
        return value;
    }
}
