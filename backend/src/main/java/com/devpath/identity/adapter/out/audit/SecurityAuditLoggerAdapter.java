package com.devpath.identity.adapter.out.audit;

import com.devpath.identity.application.AuthenticationAuditEvent;
import com.devpath.identity.application.AuthenticationAuditPort;
import com.devpath.identity.domain.OAuthProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
class SecurityAuditLoggerAdapter implements AuthenticationAuditPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAuditLoggerAdapter.class);

    @Override
    public void record(
        AuthenticationAuditEvent event,
        UUID userId,
        OAuthProvider provider,
        Instant occurredAt
    ) {
        LOGGER.info(
            "security_audit event={} userId={} provider={} occurredAt={}",
            event,
            userId,
            provider,
            occurredAt
        );
    }
}
