package com.devpath.identity.application;

import com.devpath.identity.domain.AccountStatus;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.User;

import java.time.Instant;
import java.util.UUID;

public record AuthenticatedUser(
    UUID userId,
    String displayName,
    String avatarUrl,
    AccountStatus status,
    OAuthProvider provider,
    Instant createdAt
) {
    public static AuthenticatedUser from(User user, OAuthProvider provider) {
        return new AuthenticatedUser(
            user.id().value(),
            user.displayName(),
            user.avatarUrl(),
            user.status(),
            provider,
            user.createdAt()
        );
    }
}
