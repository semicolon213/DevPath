package com.devpath.knowledge.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record KnowledgeIngestionJob(
    UUID id, UUID userId, UUID connectionId, String sourceObjectId, UUID documentId,
    String idempotencyKey, KnowledgeIngestionStatus status, String phase, int progressPercent,
    int attemptCount, int maxAttempts, Instant submittedAt, Instant startedAt, Instant completedAt,
    Instant nextAttemptAt, String errorCode, String errorMessage, long version
) {
    private static final Duration LEASE = Duration.ofMinutes(15);

    public KnowledgeIngestionJob {
        Objects.requireNonNull(id); Objects.requireNonNull(userId); Objects.requireNonNull(connectionId);
        Objects.requireNonNull(documentId); Objects.requireNonNull(status); Objects.requireNonNull(submittedAt);
        Objects.requireNonNull(nextAttemptAt);
        sourceObjectId = required(sourceObjectId, 255); idempotencyKey = required(idempotencyKey, 128);
        phase = required(phase, 32);
        if (progressPercent < 0 || progressPercent > 100 || attemptCount < 0 || maxAttempts < 1 || attemptCount > maxAttempts) {
            throw new IllegalArgumentException("Knowledge ingestion job state is invalid");
        }
    }

    public static KnowledgeIngestionJob queue(UUID userId, UUID connectionId, String sourceObjectId,
        UUID documentId, String key, Instant now) {
        return new KnowledgeIngestionJob(UUID.randomUUID(), userId, connectionId, sourceObjectId, documentId,
            key, KnowledgeIngestionStatus.QUEUED, "QUEUED", 0, 0, 3, now, null, null, now, null, null, 0);
    }

    public KnowledgeIngestionJob claim(Instant now) {
        boolean queued = status == KnowledgeIngestionStatus.QUEUED && !nextAttemptAt.isAfter(now);
        boolean stale = status == KnowledgeIngestionStatus.RUNNING && !nextAttemptAt.isAfter(now);
        if (!queued && !stale) return this;
        if (stale && attemptCount >= maxAttempts) return copy(KnowledgeIngestionStatus.FAILED, "FAILED", progressPercent,
            attemptCount, startedAt, now, nextAttemptAt, "WORKER_LEASE_EXPIRED", "Knowledge ingestion worker lease expired.");
        return copy(KnowledgeIngestionStatus.RUNNING, "COLLECTING", 10, attemptCount + 1,
            startedAt == null ? now : startedAt, null, now.plus(LEASE), null, null);
    }

    public KnowledgeIngestionJob succeed(Instant now) {
        if (status != KnowledgeIngestionStatus.RUNNING) throw new IllegalStateException("Only running ingestion can succeed");
        return copy(KnowledgeIngestionStatus.SUCCEEDED, "COMPLETED", 100, attemptCount, startedAt, now, nextAttemptAt, null, null);
    }

    public KnowledgeIngestionJob failOrRetry(String code, String message, Instant now) {
        if (status != KnowledgeIngestionStatus.RUNNING) return this;
        String safe = message == null || message.isBlank() ? "Knowledge ingestion failed safely." : message;
        if (safe.length() > 500) safe = safe.substring(0, 500);
        if (attemptCount < maxAttempts) return copy(KnowledgeIngestionStatus.QUEUED, "RETRY_WAIT", 0,
            attemptCount, startedAt, null, now.plusSeconds(30L * attemptCount), code, safe);
        return copy(KnowledgeIngestionStatus.FAILED, "FAILED", progressPercent, attemptCount,
            startedAt, now, nextAttemptAt, code, safe);
    }

    private KnowledgeIngestionJob copy(KnowledgeIngestionStatus newStatus, String newPhase, int progress,
        int attempts, Instant started, Instant completed, Instant retryAt, String code, String message) {
        return new KnowledgeIngestionJob(id, userId, connectionId, sourceObjectId, documentId, idempotencyKey,
            newStatus, newPhase, progress, attempts, maxAttempts, submittedAt, started, completed, retryAt,
            code, message, version);
    }

    private static String required(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException("Knowledge job value is invalid");
        return value;
    }
}
