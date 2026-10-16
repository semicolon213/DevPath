package com.devpath.repository.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.slf4j.LoggerFactory;
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

    @Test
    void logsUnexpectedFailureWithoutExceptionDetails() {
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        var transactions = mock(RepositorySynchronizationTransaction.class);
        var github = mock(GitHubConnectionPort.class);
        var job = mock(RepositorySyncJob.class);
        var repository = mock(Repository.class);
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(job.id()).thenReturn(jobId);
        when(job.userId()).thenReturn(userId);
        when(repository.providerRepositoryId()).thenReturn("42");
        when(repository.defaultBranch()).thenReturn("main");
        var item = new RepositorySyncWorkItem(job, repository);
        when(transactions.claim(now)).thenReturn(Optional.of(item));
        when(github.collectRepository(userId, "42", "main", now))
            .thenThrow(new IllegalStateException("sensitive failure"));
        var logs = appender();

        try {
            new RepositorySyncWorker(transactions, github, Clock.fixed(now, ZoneOffset.UTC),
                new WorkerShutdownGate()).processNext();
        } finally {
            logger().detachAppender(logs);
            logs.stop();
        }

        verify(transactions).fail(item, "SYNCHRONIZATION_FAILED",
            "Repository synchronization failed safely.", now);
        assertThat(logs.list).singleElement().satisfies(event -> {
            assertThat(event.getFormattedMessage()).isEqualTo(
                "Repository synchronization attempt failed job_id=" + jobId + " error_type=IllegalStateException");
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    private static Logger logger() {
        return (Logger) LoggerFactory.getLogger(RepositorySyncWorker.class);
    }

    private static ListAppender<ILoggingEvent> appender() {
        var logs = new ListAppender<ILoggingEvent>();
        logs.start();
        logger().addAppender(logs);
        return logs;
    }
}
