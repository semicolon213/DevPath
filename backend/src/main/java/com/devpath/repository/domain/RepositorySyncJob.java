package com.devpath.repository.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RepositorySyncJob(
    UUID id,
    UUID userId,
    UUID repositoryId,
    String idempotencyKey,
    RepositorySyncJobStatus status,
    String phase,
    int progressPercent,
    int attemptCount,
    int maxAttempts,
    Instant submittedAt,
    Instant startedAt,
    Instant completedAt,
    Instant nextAttemptAt,
    UUID resultSnapshotId,
    String errorCode,
    String errorMessage,
    long version
) {
    private static final Duration DEFAULT_LEASE = Duration.ofMinutes(15);
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);
    public RepositorySyncJob {
        Objects.requireNonNull(id);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(repositoryId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(submittedAt);
        Objects.requireNonNull(nextAttemptAt);
        idempotencyKey = required(idempotencyKey, 128);
        phase = required(phase, 32);
        if (progressPercent < 0 || progressPercent > 100 || attemptCount < 0 || maxAttempts < 1 || attemptCount > maxAttempts) {
            throw new IllegalArgumentException("Repository sync job state is invalid");
        }
    }

    public static RepositorySyncJob queue(UUID userId, UUID repositoryId, String key, Instant now) {
        return new RepositorySyncJob(
            UUID.randomUUID(), userId, repositoryId, key, RepositorySyncJobStatus.QUEUED,
            "QUEUED", 0, 0, 3, now, null, null, now, null, null, null, 0
        );
    }

    public RepositorySyncJob start(Instant now) {
        return claim(now, DEFAULT_LEASE);
    }

    public RepositorySyncJob claim(Instant now, Duration leaseDuration) {
        Objects.requireNonNull(now);
        Objects.requireNonNull(leaseDuration);
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("Repository sync job lease must be positive");
        }
        boolean queued = status == RepositorySyncJobStatus.QUEUED && !nextAttemptAt.isAfter(now);
        boolean stale = status == RepositorySyncJobStatus.RUNNING && !nextAttemptAt.isAfter(now);
        if (!queued && !stale) {
            return this;
        }
        if (stale && attemptCount >= maxAttempts) {
            return copy(RepositorySyncJobStatus.FAILED, "FAILED", progressPercent, attemptCount,
                startedAt, now, nextAttemptAt, null, "WORKER_LEASE_EXPIRED",
                "Repository synchronization stopped after its worker lease expired.");
        }
        return copy(RepositorySyncJobStatus.RUNNING, "COLLECTING", 10, attemptCount + 1,
            startedAt == null ? now : startedAt, null, now.plus(leaseDuration), null,
            stale ? "WORKER_LEASE_RECOVERED" : null,
            stale ? "Repository synchronization resumed after a worker interruption." : null);
    }

    public RepositorySyncJob succeed(UUID snapshotId, Instant now) {
        if (status != RepositorySyncJobStatus.RUNNING) {
            throw new IllegalStateException("Only a running repository sync job can succeed");
        }
        return copy(RepositorySyncJobStatus.SUCCEEDED, "COMPLETED", 100, attemptCount,
            startedAt, now, nextAttemptAt, snapshotId, null, null);
    }

    public RepositorySyncJob failOrRetry(String code, String message, Instant now) {
        if (status != RepositorySyncJobStatus.RUNNING) {
            return this;
        }
        String safeMessage = message == null || message.isBlank() ? "Repository synchronization failed" : message;
        if (safeMessage.length() > 500) {
            safeMessage = safeMessage.substring(0, 500);
        }
        if (attemptCount < maxAttempts) {
            return copy(RepositorySyncJobStatus.QUEUED, "RETRY_WAIT", 0, attemptCount,
                startedAt, null, now.plus(retryDelay()), null, code, safeMessage);
        }
        return copy(RepositorySyncJobStatus.FAILED, "FAILED", progressPercent, attemptCount,
            startedAt, now, nextAttemptAt, null, code, safeMessage);
    }

    private Duration retryDelay() {
        long exponentialSeconds = 30L << Math.max(0, attemptCount - 1);
        long boundedSeconds = Math.min(exponentialSeconds, MAX_RETRY_DELAY.toSeconds());
        long jitterRange = Math.max(1, boundedSeconds / 4);
        long jitterSeconds = Math.floorMod(Objects.hash(id, attemptCount), jitterRange);
        return Duration.ofSeconds(Math.min(MAX_RETRY_DELAY.toSeconds(), boundedSeconds + jitterSeconds));
    }

    public RepositorySyncJob waitForRateLimit(Instant retryAt, Instant now) {
        Objects.requireNonNull(retryAt);
        Objects.requireNonNull(now);
        if (status != RepositorySyncJobStatus.RUNNING) {
            return this;
        }
        if (attemptCount >= maxAttempts) {
            return failTerminal(
                "RATE_LIMIT_EXCEEDED",
                "GitHub request limit remained exhausted after the maximum attempts.",
                now
            );
        }
        Instant safeRetryAt = retryAt.isAfter(now) ? retryAt : now.plusSeconds(60);
        return copy(RepositorySyncJobStatus.QUEUED, "RETRY_WAIT", 0, attemptCount,
            startedAt, null, safeRetryAt, null, "RATE_LIMIT_EXCEEDED",
            "GitHub request limit reached; synchronization will resume after reset.");
    }

    public RepositorySyncJob failTerminal(String code, String message, Instant now) {
        if (status != RepositorySyncJobStatus.RUNNING) {
            return this;
        }
        String safeMessage = message == null || message.isBlank() ? "Repository synchronization failed" : message;
        if (safeMessage.length() > 500) {
            safeMessage = safeMessage.substring(0, 500);
        }
        return copy(RepositorySyncJobStatus.FAILED, "FAILED", progressPercent, attemptCount,
            startedAt, now, nextAttemptAt, null, code, safeMessage);
    }

    private RepositorySyncJob copy(
        RepositorySyncJobStatus newStatus,
        String newPhase,
        int progress,
        int attempts,
        Instant started,
        Instant completed,
        Instant nextAttempt,
        UUID snapshotId,
        String code,
        String message
    ) {
        return new RepositorySyncJob(
            id, userId, repositoryId, idempotencyKey, newStatus, newPhase, progress, attempts,
            maxAttempts, submittedAt, started, completed, nextAttempt, snapshotId, code, message, version
        );
    }

    private static String required(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException("Repository sync job value is invalid");
        }
        return value;
    }
}
