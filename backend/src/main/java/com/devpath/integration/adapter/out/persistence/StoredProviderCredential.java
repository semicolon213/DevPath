package com.devpath.integration.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

public record StoredProviderCredential(
    UUID connectionId,
    UUID userId,
    UUID externalIdentityId,
    String accessToken,
    Instant expiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt,
    String scopeSummary,
    Instant connectedAt
) {
}
