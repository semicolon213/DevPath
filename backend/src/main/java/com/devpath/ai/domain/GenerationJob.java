package com.devpath.ai.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GenerationJob(
    UUID id, UUID userId, UUID promptContextId, String idempotencyKey, String taskType,
    GenerationJobStatus status, String validationStatus, int attemptCount, String failureCode,
    Instant requestedAt, Instant startedAt, Instant completedAt, Instant nextAttemptAt
) {
    private static final Duration DEFAULT_LEASE = Duration.ofMinutes(15);
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);

    public static GenerationJob queue(
        UUID userId, UUID promptContextId, String idempotencyKey, String taskType, Instant now
    ) {
        return new GenerationJob(UUID.randomUUID(), userId, promptContextId, idempotencyKey, taskType,
            GenerationJobStatus.QUEUED, "PENDING", 0, null, now, null, null, now);
    }

    public GenerationJob start(Instant now) {
        if (status != GenerationJobStatus.QUEUED) throw new IllegalStateException("Generation job is not queued");
        return claim(now, DEFAULT_LEASE, 2);
    }

    public GenerationJob claim(Instant now, Duration leaseDuration, int maxAttempts) {
        Objects.requireNonNull(now); Objects.requireNonNull(leaseDuration);
        if (leaseDuration.isZero() || leaseDuration.isNegative() || maxAttempts < 1 || maxAttempts > 2) {
            throw new IllegalArgumentException("Generation job lease is invalid");
        }
        boolean queued = status == GenerationJobStatus.QUEUED && !nextAttemptAt.isAfter(now);
        boolean stale = status == GenerationJobStatus.RUNNING && !nextAttemptAt.isAfter(now);
        if (!queued && !stale) return this;
        if (stale && attemptCount >= maxAttempts) {
            return new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
                GenerationJobStatus.FAILED, "PENDING", attemptCount, "WORKER_LEASE_EXPIRED",
                requestedAt, startedAt, now, nextAttemptAt);
        }
        return new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
            GenerationJobStatus.RUNNING, "PENDING", attemptCount + 1,
            stale ? "WORKER_LEASE_RECOVERED" : null, requestedAt,
            startedAt == null ? now : startedAt, null, now.plus(leaseDuration));
    }

    public GenerationJob retryOrFail(String code, int maxAttempts, Instant now) {
        if (status != GenerationJobStatus.RUNNING) return this;
        boolean retry = attemptCount < maxAttempts;
        return new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
            retry ? GenerationJobStatus.QUEUED : GenerationJobStatus.FAILED,
            "PENDING", attemptCount, code, requestedAt, startedAt, retry ? null : now,
            retry ? now.plus(retryDelay()) : nextAttemptAt);
    }

    public GenerationJob rejectOrRetry(int maxAttempts, Instant now) {
        return attemptCount < maxAttempts
            ? new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
                GenerationJobStatus.QUEUED, "PENDING", attemptCount, "RESPONSE_VALIDATION_REJECTED",
                requestedAt, startedAt, null, now.plus(retryDelay()))
            : new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
                GenerationJobStatus.FAILED, "REJECTED", attemptCount, "RESPONSE_VALIDATION_REJECTED",
                requestedAt, startedAt, now, nextAttemptAt);
    }

    public GenerationJob succeed(Instant now) {
        return new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
            GenerationJobStatus.SUCCEEDED, "PASSED", attemptCount, null, requestedAt, startedAt, now,
            nextAttemptAt);
    }

    public GenerationJob cancel(Instant now) {
        if (status == GenerationJobStatus.SUCCEEDED || status == GenerationJobStatus.FAILED
            || status == GenerationJobStatus.CANCELED) return this;
        return new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
            GenerationJobStatus.CANCELED, validationStatus, attemptCount, "CANCELED",
            requestedAt, startedAt, now, nextAttemptAt);
    }

    private Duration retryDelay() {
        long exponentialSeconds = 30L << Math.max(0, attemptCount - 1);
        long boundedSeconds = Math.min(exponentialSeconds, MAX_RETRY_DELAY.toSeconds());
        long jitterRange = Math.max(1, boundedSeconds / 4);
        long jitterSeconds = Math.floorMod(Objects.hash(id, attemptCount), jitterRange);
        return Duration.ofSeconds(Math.min(MAX_RETRY_DELAY.toSeconds(), boundedSeconds + jitterSeconds));
    }
}
