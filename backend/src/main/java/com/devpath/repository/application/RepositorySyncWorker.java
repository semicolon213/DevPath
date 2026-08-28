package com.devpath.repository.application;

import com.devpath.integration.application.GitHubConnectionPort;
import com.devpath.integration.application.GitHubCollectionLimitExceededException;
import com.devpath.integration.application.GitHubIntegrationUnavailableException;
import com.devpath.integration.application.GitHubRateLimitExceededException;
import com.devpath.shared.infrastructure.WorkerShutdownGate;
import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "devpath.runtime.worker-enabled", havingValue = "true")
class RepositorySyncWorker {
    private final RepositorySynchronizationTransaction transactions;
    private final GitHubConnectionPort github;
    private final Clock clock;
    private final WorkerShutdownGate shutdownGate;

    RepositorySyncWorker(
        RepositorySynchronizationTransaction transactions,
        GitHubConnectionPort github,
        Clock clock,
        WorkerShutdownGate shutdownGate
    ) {
        this.transactions = transactions;
        this.github = github;
        this.clock = clock;
        this.shutdownGate = shutdownGate;
    }

    @Scheduled(
        fixedDelayString = "${devpath.jobs.repository-sync.poll-interval:1000}",
        initialDelayString = "${devpath.jobs.repository-sync.initial-delay:1000}",
        scheduler = "workerTaskScheduler"
    )
    void processNext() {
        if (!shutdownGate.acceptingClaims()) {
            return;
        }
        transactions.claim(clock.instant()).ifPresent(item -> {
            try {
                var collected = github.collectRepository(
                    item.job().userId(), item.repository().providerRepositoryId(),
                    item.repository().defaultBranch(), clock.instant()
                );
                transactions.complete(item, collected, clock.instant());
            } catch (GitHubRateLimitExceededException exception) {
                Instant now = clock.instant();
                transactions.rateLimited(item, exception.retryAt(now), now);
            } catch (GitHubCollectionLimitExceededException exception) {
                transactions.failTerminal(
                    item, "COLLECTION_LIMIT_EXCEEDED",
                    "Repository facts exceed the current safe collection limit; no partial snapshot was created.",
                    clock.instant()
                );
            } catch (GitHubIntegrationUnavailableException exception) {
                transactions.fail(item, "DEPENDENCY_UNAVAILABLE", "GitHub repository collection is temporarily unavailable.", clock.instant());
            } catch (RuntimeException exception) {
                transactions.fail(item, "SYNCHRONIZATION_FAILED", "Repository synchronization failed safely.", clock.instant());
            }
        });
    }
}
