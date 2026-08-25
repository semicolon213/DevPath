package com.devpath.repository.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositorySyncJobTest {
    private static final Instant NOW = Instant.parse("2026-08-11T00:00:00Z");

    @Test
    void persistsRetryStateBeforeTerminalFailure() {
        RepositorySyncJob job = RepositorySyncJob.queue(UUID.randomUUID(), UUID.randomUUID(), "request-1", NOW);

        RepositorySyncJob firstRetry = job.start(NOW).failOrRetry("DEPENDENCY_UNAVAILABLE", "safe", NOW);
        RepositorySyncJob secondRetry = firstRetry.start(NOW.plusSeconds(30))
            .failOrRetry("DEPENDENCY_UNAVAILABLE", "safe", NOW.plusSeconds(30));
        RepositorySyncJob failed = secondRetry.start(NOW.plusSeconds(60))
            .failOrRetry("DEPENDENCY_UNAVAILABLE", "safe", NOW.plusSeconds(60));

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
}
