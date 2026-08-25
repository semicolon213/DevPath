package com.devpath.identity.adapter.in.security;

import com.devpath.identity.application.AuthenticationAuditEvent;
import com.devpath.identity.application.AuthenticationAuditPort;
import com.devpath.identity.config.DevPathSecurityProperties;
import com.devpath.identity.domain.OAuthProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AbsoluteSessionTimeoutFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AbsoluteSessionTimeoutFilter.class);

    private final DevPathSecurityProperties properties;
    private final Clock clock;
    private final AuthenticationAuditPort auditPort;

    public AbsoluteSessionTimeoutFilter(
        DevPathSecurityProperties properties,
        Clock clock,
        AuthenticationAuditPort auditPort
    ) {
        this.properties = properties;
        this.clock = clock;
        this.auditPort = auditPort;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Instant expiresAt = Instant.ofEpochMilli(session.getCreationTime())
                .plus(properties.sessionAbsoluteTimeout());
            if (!clock.instant().isBefore(expiresAt)) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                session.invalidate();
                SecurityContextHolder.clearContext();
                recordExpiration(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void recordExpiration(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof DevPathOAuth2User user)) {
            return;
        }
        try {
            auditPort.record(
                AuthenticationAuditEvent.SESSION_ABSOLUTE_TIMEOUT,
                user.userId(),
                OAuthProvider.GITHUB,
                clock.instant()
            );
        } catch (RuntimeException exception) {
            log.error("absolute_session_timeout_audit_record_failed", exception);
        }
    }
}
