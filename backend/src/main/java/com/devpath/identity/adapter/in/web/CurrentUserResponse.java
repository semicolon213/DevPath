package com.devpath.identity.adapter.in.web;

import com.devpath.identity.application.AuthenticatedUser;

import java.time.Instant;
import java.util.UUID;

public record CurrentUserResponse(
    UUID userId,
    String displayName,
    String avatarUrl,
    String status,
    String authenticationProvider,
    Instant createdAt
) {
    static CurrentUserResponse from(AuthenticatedUser user) {
        return new CurrentUserResponse(
            user.userId(),
            user.displayName(),
            user.avatarUrl(),
            user.status().name(),
            user.provider().name(),
            user.createdAt()
        );
    }
}
