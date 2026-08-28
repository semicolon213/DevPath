package com.devpath.integration.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

public record StoredNotionCredential(
    UUID connectionId,
    UUID userId,
    String workspaceId,
    String botId,
    String workspaceName,
    String workspaceIconUrl,
    String accessToken,
    String refreshToken,
    Instant connectedAt
) {}
