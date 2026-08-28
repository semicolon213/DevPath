package com.devpath.integration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotionIntegrationApplicationServiceTest {
    @Test
    void completesAndDisconnectsTheAuthenticatedUsersWorkspaceWithAuditRecords() {
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        UUID userId = UUID.randomUUID();
        var port = mock(NotionConnectionPort.class);
        var audit = mock(IntegrationAuditPort.class);
        var active = new ConnectedAccountView(UUID.randomUUID(), "NOTION", "ACTIVE", List.of("read_content"), now, null);
        var revoked = new ConnectedAccountView(active.connectionId(), "NOTION", "REVOKED", List.of(), now, now);
        when(port.complete(userId, "code", now)).thenReturn(active);
        when(port.disconnect(userId, now)).thenReturn(revoked);
        var service = new NotionIntegrationApplicationService(port, audit, Clock.fixed(now, ZoneOffset.UTC));

        assertThat(service.complete(userId, "code")).isEqualTo(active);
        assertThat(service.disconnect(userId)).isEqualTo(revoked);

        verify(audit).record(IntegrationAuditEvent.NOTION_CONNECTED, userId, active.connectionId().toString(), now);
        verify(audit).record(IntegrationAuditEvent.NOTION_DISCONNECTED, userId, active.connectionId().toString(), now);
    }
}
