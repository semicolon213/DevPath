package com.devpath.repository.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositorySynchronizationApplicationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-11T00:00:00Z");

    @Test
    void acceptsTheDurableJobWithoutCallingTheProviderOnTheRequestThread() {
        UUID userId = UUID.randomUUID();
        UUID repositoryId = UUID.randomUUID();
        var transactions = mock(RepositorySynchronizationTransaction.class);
        var service = new RepositorySynchronizationApplicationService(
            transactions, Clock.fixed(NOW, ZoneOffset.UTC)
        );

        service.request(userId, repositoryId, "sync-request-1");

        verify(transactions).request(userId, repositoryId, "sync-request-1", NOW);
    }

    @Test
    void recordsTheMeasuredTimeWhenCurrentEvidenceIsViewed() {
        UUID userId = UUID.randomUUID();
        UUID repositoryId = UUID.randomUUID();
        var transactions = mock(RepositorySynchronizationTransaction.class);
        var service = new RepositorySynchronizationApplicationService(
            transactions, Clock.fixed(NOW, ZoneOffset.UTC)
        );

        service.getEvidence(userId, repositoryId);

        verify(transactions).getEvidence(userId, repositoryId, NOW);
    }
}
