package com.devpath.identity.application;

import com.devpath.identity.domain.OAuthProvider;

import java.time.Instant;
import java.util.UUID;

public interface AuthenticationAuditPort {
    void record(
        AuthenticationAuditEvent event,
        UUID userId,
        OAuthProvider provider,
        Instant occurredAt
    );
}
