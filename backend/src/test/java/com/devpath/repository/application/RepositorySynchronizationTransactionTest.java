package com.devpath.repository.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.devpath.repository.domain.Repository;
import com.devpath.repository.domain.RepositoryBranch;
import com.devpath.repository.domain.RepositoryCommit;
import com.devpath.repository.domain.RepositorySnapshot;
import com.devpath.repository.domain.RepositorySyncJob;
import com.devpath.repository.domain.RepositorySyncJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RepositorySynchronizationTransactionTest {
    @Test
    void acceptsOneHundredDistinctJobsIntoTheDurableQueue() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        var repositories = mock(RepositoryPersistencePort.class);
        var synchronization = mock(RepositorySynchronizationPersistencePort.class);
        var audit = mock(RepositoryAuditPort.class);
        when(synchronization.saveJob(any(RepositorySyncJob.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        var transaction = new RepositorySynchronizationTransaction(repositories, synchronization, audit);
        var accepted = new ArrayList<RepositorySyncJobView>();

        for (int index = 0; index < 100; index++) {
            Repository repository = Repository.discover(userId, UUID.randomUUID(), Integer.toString(index),
                "repo-" + index, "owner/repo-" + index, "owner", false, "main", false,
                "https://github.com/owner/repo-" + index, now);
            when(repositories.findByIdAndOwner(repository.id(), userId)).thenReturn(Optional.of(repository));
            accepted.add(transaction.request(userId, repository.id(), "bulk-sync-" + index, now));
        }

        assertThat(accepted).hasSize(100);
        assertThat(accepted).extracting(RepositorySyncJobView::jobId).doesNotHaveDuplicates();
        assertThat(accepted).allSatisfy(job -> {
            assertThat(job.status()).isEqualTo("queued");
            assertThat(job.attemptCount()).isZero();
        });
    }

    @Test
    void returnsOwnerScopedCurrentSnapshotActivityAndWritesADurableReadAudit() {
        UUID userId = UUID.randomUUID();
        Instant capturedAt = Instant.parse("2026-08-11T00:00:00Z");
        String revision = "a".repeat(40);
        Repository discovered = Repository.discover(
            userId, UUID.randomUUID(), "42", "devpath", "owner/devpath", "owner",
            true, "main", false, "https://github.com/owner/devpath", capturedAt.minusSeconds(172_800)
        );
        RepositorySnapshot snapshot = RepositorySnapshot.ready(
            discovered.id(), userId, revision, capturedAt,
            List.of(new RepositoryBranch("main", revision, true)),
            List.of(new RepositoryCommit(revision, "owner", capturedAt.minusSeconds(86_400), "hidden message"))
        );
        Repository repository = discovered.markSynchronized(snapshot.id(), capturedAt);
        var repositories = mock(RepositoryPersistencePort.class);
        var synchronization = mock(RepositorySynchronizationPersistencePort.class);
        var audit = mock(RepositoryAuditPort.class);
        when(repositories.findByIdAndOwner(repository.id(), userId)).thenReturn(Optional.of(repository));
        when(synchronization.findSnapshot(userId, repository.id(), snapshot.id())).thenReturn(Optional.of(snapshot));
        var transactions = new RepositorySynchronizationTransaction(repositories, synchronization, audit);

        RepositoryEvidenceSummaryView view = transactions.getEvidence(userId, repository.id(), capturedAt);

        assertThat(view.activityTimeline().events()).singleElement().satisfies(event -> {
            assertThat(event.eventType()).isEqualTo("COMMIT");
            assertThat(event.sourceReference()).isEqualTo(revision);
        });
        assertThat(view.activityTimeline().daysSinceLatestActivity()).isEqualTo(1L);
        verify(audit).record(RepositoryAuditEvent.REPOSITORY_EVIDENCE_VIEWED, userId, repository.id(), capturedAt);
    }

    @Test
    void persistsCollectionLimitAsANonRetryableTerminalFailureWithoutASnapshot() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        Repository repository = Repository.discover(
            userId, UUID.randomUUID(), "42", "devpath", "owner/devpath", "owner",
            true, "main", false, "https://github.com/owner/devpath", now.minusSeconds(60)
        );
        RepositorySyncJob running = RepositorySyncJob.queue(userId, repository.id(), "sync-large", now.minusSeconds(2))
            .start(now.minusSeconds(1));
        var repositories = mock(RepositoryPersistencePort.class);
        var synchronization = mock(RepositorySynchronizationPersistencePort.class);
        var audit = mock(RepositoryAuditPort.class);
        when(synchronization.findByIdAndOwner(running.id(), userId)).thenReturn(Optional.of(running));
        when(repositories.findByIdAndOwner(repository.id(), userId)).thenReturn(Optional.of(repository));
        var transactions = new RepositorySynchronizationTransaction(repositories, synchronization, audit);

        transactions.failTerminal(
            new RepositorySyncWorkItem(running, repository), "COLLECTION_LIMIT_EXCEEDED",
            "Repository facts exceed the current safe collection limit; no partial snapshot was created.", now
        );

        var jobCaptor = ArgumentCaptor.forClass(RepositorySyncJob.class);
        verify(synchronization).saveJob(jobCaptor.capture());
        RepositorySyncJob failed = jobCaptor.getValue();
        assertThat(failed.status()).isEqualTo(RepositorySyncJobStatus.FAILED);
        assertThat(failed.attemptCount()).isEqualTo(1);
        assertThat(failed.resultSnapshotId()).isNull();
        assertThat(RepositorySyncJobView.from(failed).retryable()).isFalse();
        verify(audit).record(RepositoryAuditEvent.REPOSITORY_SYNC_FAILED, userId, repository.id(), now);
    }
}
