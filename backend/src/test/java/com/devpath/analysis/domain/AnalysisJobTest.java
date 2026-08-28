package com.devpath.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnalysisJobTest {
    private static final Instant NOW = Instant.parse("2026-08-27T00:00:00Z");

    @Test
    void recoversAnExpiredLeaseAndPersistsTheTerminalAttempt() {
        AnalysisJob first = AnalysisJob.queue(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "analysis-worker-recovery", "REPOSITORY_BASELINE", NOW).claim(NOW, Duration.ofMinutes(5));
        AnalysisJob second = first.claim(NOW.plusSeconds(301), Duration.ofMinutes(5));
        AnalysisJob third = second.claim(NOW.plusSeconds(602), Duration.ofMinutes(5));
        AnalysisJob exhausted = third.claim(NOW.plusSeconds(903), Duration.ofMinutes(5));

        assertThat(second.status()).isEqualTo(AnalysisJobStatus.RUNNING);
        assertThat(second.attemptCount()).isEqualTo(2);
        assertThat(second.errorCode()).isEqualTo("WORKER_LEASE_RECOVERED");
        assertThat(exhausted.status()).isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(exhausted.attemptCount()).isEqualTo(3);
        assertThat(exhausted.errorCode()).isEqualTo("WORKER_LEASE_EXPIRED");
    }

    @Test
    void appliesIncreasingBoundedBackoffToTransientFailures() {
        AnalysisJob queued = AnalysisJob.queue(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "analysis-bounded-backoff", "REPOSITORY_BASELINE", NOW);
        AnalysisJob firstRetry = queued.start(NOW).failOrRetry("ANALYSIS_FAILED", "safe", NOW);
        AnalysisJob secondRetry = firstRetry.start(firstRetry.nextAttemptAt())
            .failOrRetry("ANALYSIS_FAILED", "safe", firstRetry.nextAttemptAt());

        Duration firstDelay = Duration.between(NOW, firstRetry.nextAttemptAt());
        Duration secondDelay = Duration.between(firstRetry.nextAttemptAt(), secondRetry.nextAttemptAt());
        assertThat(firstDelay).isBetween(Duration.ofSeconds(30), Duration.ofSeconds(37));
        assertThat(secondDelay).isGreaterThan(firstDelay);
        assertThat(secondDelay).isLessThanOrEqualTo(Duration.ofMinutes(5));
    }
}
