package com.devpath.knowledge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devpath.integration.application.NotionConnectionPort;
import com.devpath.knowledge.domain.KnowledgeIngestionJob;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeApplicationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-30T00:00:00Z");
    @Mock KnowledgePersistencePort persistence;
    @Mock NotionConnectionPort notion;
    @Mock KnowledgeAuditPort audit;

    @Test
    void replaysAnAcceptedOwnerScopedCommandBeforeRecheckingProviderPermission() {
        UUID userId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        KnowledgeIngestionJob accepted = KnowledgeIngestionJob.queue(
            userId, connectionId, "page-1", UUID.randomUUID(), "stable-key", NOW);
        when(persistence.findJobByOwnerAndKey(userId, "stable-key")).thenReturn(Optional.of(accepted));
        var service = new KnowledgeApplicationService(persistence, notion, audit,
            Clock.fixed(NOW, ZoneOffset.UTC));

        KnowledgeIngestionJobView replay = service.importNotion(userId, connectionId, "page-1", "stable-key");

        assertThat(replay.jobId()).isEqualTo(accepted.id());
        verify(notion, never()).verifyPageAccess(userId, connectionId, "page-1");
        verify(persistence, never()).saveJob(org.mockito.ArgumentMatchers.any());
    }
}
