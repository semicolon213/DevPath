package com.devpath.repository.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositorySyncJobTest {
    private static final Instant NOW = Instant.parse("2026-08-11T00:00:00Z");

    @Test
    void persistsRetryStateBeforeTerminalFailure() {
        RepositorySyncJob job = RepositorySyncJob.queue(UUID.randomUUID(), UUID.randomUUID(), "request-1", NOW);

        RepositorySyncJob firstRetry = job.start(NOW).failOrRetry("DEPENDENCY_UNAVAILABLE", "safe", NOW);
        Instant secondAttemptAt = firstRetry.nextAttemptAt();
        RepositorySyncJob secondRetry = firstRetry.start(secondAttemptAt)
            .failOrRetry("DEPENDENCY_UNAVAILABLE", "safe", secondAttemptAt);
        Instant thirdAttemptAt = secondRetry.nextAttemptAt();
        RepositorySyncJob failed = secondRetry.start(thirdAttemptAt)
            .failOrRetry("DEPENDENCY_UNAVAILABLE", "safe", thirdAttemptAt);

        assertThat(firstRetry.status()).isEqualTo(RepositorySyncJobStatus.QUEUED);
        assertThat(firstRetry.phase()).isEqualTo("RETRY_WAIT");
        assertThat(failed.status()).isEqualTo(RepositorySyncJobStatus.FAILED);
        assertThat(failed.attemptCount()).isEqualTo(3);
        assertThat(failed.completedAt()).isNotNull();
    }

    @Test
    void waitsUntilTheProviderResetInsteadOfUsingTheGenericRetryDelay() {
        RepositorySyncJob running = RepositorySyncJob
            .queue(UUID.randomUUID(), UUID.randomUUID(), "request-rate-limit", NOW)
            .start(NOW);
        Instant resetAt = NOW.plusSeconds(900);

        RepositorySyncJob waiting = running.waitForRateLimit(resetAt, NOW);

        assertThat(waiting.status()).isEqualTo(RepositorySyncJobStatus.QUEUED);
        assertThat(waiting.phase()).isEqualTo("RETRY_WAIT");
        assertThat(waiting.nextAttemptAt()).isEqualTo(resetAt);
        assertThat(waiting.errorCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void recoversAnExpiredWorkerLeaseAndStopsAfterTheBoundedAttemptCount() {
        RepositorySyncJob firstClaim = RepositorySyncJob
            .queue(UUID.randomUUID(), UUID.randomUUID(), "request-worker-recovery", NOW)
            .claim(NOW, Duration.ofMinutes(5));

        RepositorySyncJob secondClaim = firstClaim.claim(NOW.plusSeconds(301), Duration.ofMinutes(5));
        RepositorySyncJob thirdClaim = secondClaim.claim(NOW.plusSeconds(602), Duration.ofMinutes(5));
        RepositorySyncJob exhausted = thirdClaim.claim(NOW.plusSeconds(903), Duration.ofMinutes(5));

        assertThat(secondClaim.status()).isEqualTo(RepositorySyncJobStatus.RUNNING);
        assertThat(secondClaim.attemptCount()).isEqualTo(2);
        assertThat(secondClaim.errorCode()).isEqualTo("WORKER_LEASE_RECOVERED");
        assertThat(exhausted.status()).isEqualTo(RepositorySyncJobStatus.FAILED);
        assertThat(exhausted.attemptCount()).isEqualTo(3);
        assertThat(exhausted.errorCode()).isEqualTo("WORKER_LEASE_EXPIRED");
        assertThat(exhausted.completedAt()).isEqualTo(NOW.plusSeconds(903));
    }

    @Test
    void usesIncreasingRetryDelayWithAFiveMinuteUpperBound() {
        RepositorySyncJob first = RepositorySyncJob.queue(
            UUID.randomUUID(), UUID.randomUUID(), "request-bounded-backoff", NOW);
        RepositorySyncJob firstRetry = first.start(NOW).failOrRetry("DEPENDENCY_UNAVAILABLE", "safe", NOW);
        Instant secondAttemptAt = firstRetry.nextAttemptAt();
        RepositorySyncJob secondRetry = firstRetry.start(secondAttemptAt)
            .failOrRetry("DEPENDENCY_UNAVAILABLE", "safe", secondAttemptAt);

        Duration firstDelay = Duration.between(NOW, firstRetry.nextAttemptAt());
        Duration secondDelay = Duration.between(secondAttemptAt, secondRetry.nextAttemptAt());
        assertThat(firstDelay).isBetween(Duration.ofSeconds(30), Duration.ofSeconds(37));
        assertThat(secondDelay).isGreaterThan(firstDelay);
        assertThat(secondDelay).isLessThanOrEqualTo(Duration.ofMinutes(5));
    }
}
