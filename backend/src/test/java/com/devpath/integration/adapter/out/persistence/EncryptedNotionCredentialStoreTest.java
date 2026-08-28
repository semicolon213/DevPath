package com.devpath.integration.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.devpath.integration.config.NotionIntegrationProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EncryptedNotionCredentialStoreTest {
    @Test
    void connectionSummaryDoesNotReportScopesAfterRevocation() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        var entity = new NotionWorkspaceConnectionJpaEntity(UUID.randomUUID(), userId, "workspace-1",
            "bot-1", "DevPath", null, new byte[] {1}, new byte[12], new byte[] {2},
            new byte[12], "local-v1", now);
        entity.revoke(new byte[] {9}, new byte[12], "local-v1", now.plusSeconds(60));
        var repository = mock(NotionWorkspaceConnectionJpaRepository.class);
        when(repository.findAllByUserIdOrderByConnectedAtAsc(userId)).thenReturn(List.of(entity));
        var store = new EncryptedNotionCredentialStore(repository,
            mock(NotionIntegrationProperties.class));

        assertThat(store.findAllViews(userId)).singleElement().satisfies(view -> {
            assertThat(view.status()).isEqualTo("REVOKED");
            assertThat(view.scopes()).isEmpty();
        });
    }
}
