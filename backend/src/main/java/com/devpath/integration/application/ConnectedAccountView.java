package com.devpath.integration.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConnectedAccountView(
    UUID connectionId,
    String provider,
    String status,
    List<String> scopes,
    Instant connectedAt,
    Instant expiresAt
) {
    public ConnectedAccountView {
        scopes = List.copyOf(scopes);
    }
}
