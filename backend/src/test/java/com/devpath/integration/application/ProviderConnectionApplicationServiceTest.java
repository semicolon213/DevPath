package com.devpath.integration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderConnectionApplicationServiceTest {
    @Test
    void returnsOnlyTheOwningUsersActiveConnectionSummaries() {
        UUID userId = UUID.randomUUID();
        var port = mock(ProviderCredentialSummaryPort.class);
        var connection = new ConnectedAccountView(
            UUID.randomUUID(), "GITHUB", "ACTIVE", List.of("repo"),
            Instant.parse("2026-08-11T00:00:00Z"), null
        );
        when(port.findActiveByUserId(userId)).thenReturn(List.of(connection));

        var result = new ProviderConnectionApplicationService(
            port,
            Clock.fixed(Instant.parse("2026-08-11T01:00:00Z"), ZoneOffset.UTC)
        ).listFor(userId);

        assertThat(result.connections()).containsExactly(connection);
    }

    @Test
    void hidesCredentialsThatHaveExpiredEvenBeforeStatusCleanupRuns() {
        UUID userId = UUID.randomUUID();
        var port = mock(ProviderCredentialSummaryPort.class);
        var expired = new ConnectedAccountView(
            UUID.randomUUID(), "GITHUB", "ACTIVE", List.of("repo"),
            Instant.parse("2026-08-10T00:00:00Z"), Instant.parse("2026-08-11T00:00:00Z")
        );
        when(port.findActiveByUserId(userId)).thenReturn(List.of(expired));

        var result = new ProviderConnectionApplicationService(
            port,
            Clock.fixed(Instant.parse("2026-08-11T01:00:00Z"), ZoneOffset.UTC)
        ).listFor(userId);

        assertThat(result.connections()).isEmpty();
    }
}
