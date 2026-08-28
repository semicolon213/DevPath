package com.devpath.repository.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.devpath.integration.application.GitHubConnectionPort;
import com.devpath.integration.application.GitHubCollectionLimitExceededException;
import com.devpath.integration.application.GitHubRateLimitExceededException;
import com.devpath.repository.domain.Repository;
import com.devpath.repository.domain.RepositorySyncJob;
import com.devpath.shared.infrastructure.WorkerShutdownGate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.ContextClosedEvent;

class RepositorySyncWorkerTest {
    @Test
    void doesNotClaimNewWorkAfterShutdownBegins() {
        var transactions = mock(RepositorySynchronizationTransaction.class);
        var github = mock(GitHubConnectionPort.class);
        var gate = new WorkerShutdownGate();
        gate.stopAcceptingClaims(mock(ContextClosedEvent.class));

        new RepositorySyncWorker(transactions, github, Clock.systemUTC(), gate).processNext();

        verifyNoInteractions(transactions, github);
    }

    @Test
    void persistsProviderResetTimeWhenCollectionIsRateLimited() {
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        Instant resetAt = now.plusSeconds(900);
        var transactions = mock(RepositorySynchronizationTransaction.class);
        var github = mock(GitHubConnectionPort.class);
        var job = mock(RepositorySyncJob.class);
        var repository = mock(Repository.class);
        UUID userId = UUID.randomUUID();
        when(job.userId()).thenReturn(userId);
        when(repository.providerRepositoryId()).thenReturn("42");
        when(repository.defaultBranch()).thenReturn("main");
        var item = new RepositorySyncWorkItem(job, repository);
        when(transactions.claim(now)).thenReturn(Optional.of(item));
        when(github.collectRepository(userId, "42", "main", now)).thenThrow(
            new GitHubRateLimitExceededException(resetAt, null, new RuntimeException("rate limited"))
        );

        new RepositorySyncWorker(transactions, github, Clock.fixed(now, ZoneOffset.UTC),
            new WorkerShutdownGate()).processNext();

        verify(transactions).rateLimited(item, resetAt, now);
    }

    @Test
    void failsImmediatelyWithoutRetryWhenTheRepositoryExceedsACollectionLimit() {
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        var transactions = mock(RepositorySynchronizationTransaction.class);
        var github = mock(GitHubConnectionPort.class);
        var job = mock(RepositorySyncJob.class);
        var repository = mock(Repository.class);
        UUID userId = UUID.randomUUID();
        when(job.userId()).thenReturn(userId);
        when(repository.providerRepositoryId()).thenReturn("42");
        when(repository.defaultBranch()).thenReturn("main");
        var item = new RepositorySyncWorkItem(job, repository);
        when(transactions.claim(now)).thenReturn(Optional.of(item));
        when(github.collectRepository(userId, "42", "main", now))
            .thenThrow(new GitHubCollectionLimitExceededException("tree truncated"));

        new RepositorySyncWorker(transactions, github, Clock.fixed(now, ZoneOffset.UTC),
            new WorkerShutdownGate()).processNext();

        verify(transactions).failTerminal(
            item, "COLLECTION_LIMIT_EXCEEDED",
            "Repository facts exceed the current safe collection limit; no partial snapshot was created.", now
        );
    }
}
