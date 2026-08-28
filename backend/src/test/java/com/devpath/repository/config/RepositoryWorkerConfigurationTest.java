package com.devpath.repository.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RepositoryWorkerConfigurationTest {
    private final RepositoryWorkerConfiguration configuration = new RepositoryWorkerConfiguration();

    @Test
    void createsADedicatedTwoThreadWorkerScheduler() {
        var scheduler = configuration.workerTaskScheduler(Duration.ofMinutes(10));
        scheduler.initialize();
        try {
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(2);
            assertThat(scheduler.getThreadNamePrefix()).isEqualTo("devpath-worker-");
        } finally {
            scheduler.destroy();
        }
    }

    @Test
    void keepsShutdownWaitingInsideThePersistedLeaseWindow() {
        assertThatThrownBy(() -> configuration.workerTaskScheduler(Duration.ZERO))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> configuration.workerTaskScheduler(Duration.ofMinutes(16)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void waitsForAlreadyClaimedWorkButDoesNotWaitBeyondTheConfiguredWindow() throws Exception {
        var scheduler = configuration.workerTaskScheduler(Duration.ofSeconds(2));
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var completed = new CountDownLatch(1);
        scheduler.initialize();
        scheduler.schedule(() -> {
            started.countDown();
            try {
                release.await();
                completed.countDown();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, Instant.now());
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<Void> shutdown = CompletableFuture.runAsync(scheduler::destroy);
        assertThat(shutdown.isDone()).isFalse();
        release.countDown();
        shutdown.get(1, TimeUnit.SECONDS);

        assertThat(completed.getCount()).isZero();
    }
}
