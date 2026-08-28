package com.devpath.integration.application;

import java.time.Instant;
import java.util.UUID;

public interface NotionConnectionPort {
    String authorizationUrl(String state);
    ConnectedAccountView complete(UUID userId, String code, Instant now);
    NotionWorkspaceListView discover(UUID userId, Instant now);
    ConnectedAccountView disconnect(UUID userId, Instant now);
}
