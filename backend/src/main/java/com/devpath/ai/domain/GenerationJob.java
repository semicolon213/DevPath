package com.devpath.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record GenerationJob(
    UUID id, UUID userId, UUID promptContextId, String idempotencyKey, String taskType,
    GenerationJobStatus status, String validationStatus, int attemptCount, String failureCode,
    Instant requestedAt, Instant startedAt, Instant completedAt
) {
    public static GenerationJob queue(
        UUID userId, UUID promptContextId, String idempotencyKey, String taskType, Instant now
    ) {
        return new GenerationJob(UUID.randomUUID(), userId, promptContextId, idempotencyKey, taskType,
            GenerationJobStatus.QUEUED, "PENDING", 0, null, now, null, null);
    }

    public GenerationJob start(Instant now) {
        if (status != GenerationJobStatus.QUEUED) throw new IllegalStateException("Generation job is not queued");
        return new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
            GenerationJobStatus.RUNNING, "PENDING", attemptCount + 1, null, requestedAt, now, null);
    }

    public GenerationJob retryOrFail(String code, int maxAttempts, Instant now) {
        if (status != GenerationJobStatus.RUNNING) return this;
        boolean retry = attemptCount < maxAttempts;
        return new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
            retry ? GenerationJobStatus.QUEUED : GenerationJobStatus.FAILED,
            "PENDING", attemptCount, code, requestedAt, startedAt, retry ? null : now);
    }

    public GenerationJob rejectOrRetry(int maxAttempts, Instant now) {
        return attemptCount < maxAttempts
            ? new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
                GenerationJobStatus.QUEUED, "PENDING", attemptCount, "RESPONSE_VALIDATION_REJECTED",
                requestedAt, startedAt, null)
            : new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
                GenerationJobStatus.FAILED, "REJECTED", attemptCount, "RESPONSE_VALIDATION_REJECTED",
                requestedAt, startedAt, now);
    }

    public GenerationJob succeed(Instant now) {
        return new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
            GenerationJobStatus.SUCCEEDED, "PASSED", attemptCount, null, requestedAt, startedAt, now);
    }

    public GenerationJob cancel(Instant now) {
        if (status == GenerationJobStatus.SUCCEEDED || status == GenerationJobStatus.FAILED
            || status == GenerationJobStatus.CANCELED) return this;
        return new GenerationJob(id, userId, promptContextId, idempotencyKey, taskType,
            GenerationJobStatus.CANCELED, validationStatus, attemptCount, "CANCELED",
            requestedAt, startedAt, now);
    }
}
