package com.devpath.repository.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devpath.repository.domain.Repository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositoryRegistrationTransactionTest {
    @Test
    void persistsAndAuditsANewCanonicalRepositoryInTheRegistrationTransaction() {
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        UUID userId = UUID.randomUUID();
        Repository candidate = Repository.discover(
            userId, UUID.randomUUID(), "42", "devpath", "owner/devpath", "owner",
            false, "main", false, "https://github.com/owner/devpath", now
        );
        var persistence = mock(RepositoryPersistencePort.class);
        var audit = mock(RepositoryAuditPort.class);
        when(persistence.findByOwnerAndProviderReference(userId, "GITHUB", "42"))
            .thenReturn(Optional.empty());
        when(persistence.save(candidate)).thenReturn(candidate);
        var transaction = new RepositoryRegistrationTransaction(persistence, audit);

        RepositoryView result = transaction.register(candidate, now);

        assertThat(result.repositoryId()).isEqualTo(candidate.id());
        verify(persistence).save(candidate);
        verify(audit).record(RepositoryAuditEvent.REPOSITORY_IMPORTED, userId, candidate.id(), now);
    }

    @Test
    void duplicateRegistrationReturnsTheCanonicalRepositoryWithoutAnotherAuditEvent() {
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        UUID userId = UUID.randomUUID();
        Repository existing = Repository.discover(
            userId, UUID.randomUUID(), "42", "devpath", "owner/devpath", "owner",
            false, "main", false, "https://github.com/owner/devpath", now
        );
        var persistence = mock(RepositoryPersistencePort.class);
        var audit = mock(RepositoryAuditPort.class);
        when(persistence.findByOwnerAndProviderReference(userId, "GITHUB", "42"))
            .thenReturn(Optional.of(existing));
        var transaction = new RepositoryRegistrationTransaction(persistence, audit);

        RepositoryView result = transaction.register(existing, now);

        assertThat(result.repositoryId()).isEqualTo(existing.id());
        verify(persistence, never()).save(existing);
        verify(audit, never()).record(
            RepositoryAuditEvent.REPOSITORY_IMPORTED, userId, existing.id(), now
        );
    }

    @Test
    void archiveAndRestorePersistAndAuditOnlyActualOwnerScopedStateChanges() {
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        UUID userId = UUID.randomUUID();
        Repository repository = Repository.discover(
            userId, UUID.randomUUID(), "42", "devpath", "owner/devpath", "owner",
            false, "main", false, "https://github.com/owner/devpath", now
        );
        var persistence = mock(RepositoryPersistencePort.class);
        var audit = mock(RepositoryAuditPort.class);
        when(persistence.findByIdAndOwner(repository.id(), userId))
            .thenReturn(Optional.of(repository), Optional.of(repository.archive(now.plusSeconds(1))));
        when(persistence.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        var transaction = new RepositoryRegistrationTransaction(persistence, audit);

        RepositoryView archived = transaction.archive(userId, repository.id(), now.plusSeconds(1));
        RepositoryView restored = transaction.restore(userId, repository.id(), now.plusSeconds(2));

        assertThat(archived.lifecycle()).isEqualTo("ARCHIVED");
        assertThat(restored.lifecycle()).isEqualTo("DISCOVERED");
        verify(audit).record(RepositoryAuditEvent.REPOSITORY_ARCHIVED, userId, repository.id(), now.plusSeconds(1));
        verify(audit).record(RepositoryAuditEvent.REPOSITORY_RESTORED, userId, repository.id(), now.plusSeconds(2));
    }
}
