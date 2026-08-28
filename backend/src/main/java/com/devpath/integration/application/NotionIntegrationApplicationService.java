package com.devpath.integration.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotionIntegrationApplicationService {
    private final NotionConnectionPort notion;
    private final IntegrationAuditPort audit;
    private final Clock clock;

    public NotionIntegrationApplicationService(NotionConnectionPort notion, IntegrationAuditPort audit, Clock clock) {
        this.notion = notion;
        this.audit = audit;
        this.clock = clock;
    }

    public String authorizationUrl(String state) { return notion.authorizationUrl(state); }

    public ConnectedAccountView complete(UUID userId, String code) {
        Instant now = clock.instant();
        ConnectedAccountView connection = notion.complete(userId, code, now);
        audit.record(IntegrationAuditEvent.NOTION_CONNECTED, userId, connection.connectionId().toString(), now);
        return connection;
    }

    public NotionWorkspaceListView discover(UUID userId) {
        return notion.discover(userId, clock.instant());
    }

    public ConnectedAccountView disconnect(UUID userId) {
        Instant now = clock.instant();
        ConnectedAccountView connection = notion.disconnect(userId, now);
        audit.record(IntegrationAuditEvent.NOTION_DISCONNECTED, userId, connection.connectionId().toString(), now);
        return connection;
    }
}
