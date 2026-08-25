package com.devpath.identity.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.devpath.identity.application.AuthenticatedUser;
import com.devpath.identity.application.AuthenticationAuditEvent;
import com.devpath.identity.application.AuthenticationAuditPort;
import com.devpath.identity.config.DevPathSecurityProperties;
import com.devpath.identity.domain.AccountStatus;
import com.devpath.identity.domain.OAuthProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AbsoluteSessionTimeoutFilterTest {
    private static final UUID USER_ID = UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");
    private static final Duration TIMEOUT = Duration.ofMinutes(30);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void keepsTheSessionImmediatelyBeforeTheAbsoluteDeadline() throws Exception {
        MockHttpSession session = new MockHttpSession();
        AuthenticationAuditPort audit = org.mockito.Mockito.mock(AuthenticationAuditPort.class);
        var filter = filterAt(session, TIMEOUT.minusMillis(1), audit);
        var request = requestWith(session);
        var chainCalled = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(),
            (currentRequest, currentResponse) -> chainCalled.set(true));

        assertThat(session.isInvalid()).isFalse();
        assertThat(chainCalled).isTrue();
        verify(audit, never()).record(any(), any(), any(), any());
    }

    @Test
    void invalidatesAtTheExactDeadlineClearsAuthenticationAndRecordsTheEvent() throws Exception {
        MockHttpSession session = new MockHttpSession();
        AuthenticationAuditPort audit = org.mockito.Mockito.mock(AuthenticationAuditPort.class);
        var filter = filterAt(session, TIMEOUT, audit);
        var request = requestWith(session);
        var principal = principal();
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, "n/a", principal.getAuthorities())
        );

        filter.doFilter(request, new MockHttpServletResponse(), (currentRequest, currentResponse) -> {});

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(audit).record(
            eq(AuthenticationAuditEvent.SESSION_ABSOLUTE_TIMEOUT),
            eq(USER_ID),
            eq(OAuthProvider.GITHUB),
            any(Instant.class)
        );
    }

    private AbsoluteSessionTimeoutFilter filterAt(
        MockHttpSession session,
        Duration elapsed,
        AuthenticationAuditPort audit
    ) {
        Instant now = Instant.ofEpochMilli(session.getCreationTime()).plus(elapsed);
        var properties = new DevPathSecurityProperties("http://localhost:5173", TIMEOUT, "/");
        return new AbsoluteSessionTimeoutFilter(properties, Clock.fixed(now, ZoneOffset.UTC), audit);
    }

    private MockHttpServletRequest requestWith(MockHttpSession session) {
        var request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    private DevPathOAuth2User principal() {
        var user = new AuthenticatedUser(
            USER_ID,
            "DevPath User",
            null,
            AccountStatus.ACTIVE,
            OAuthProvider.GITHUB,
            Instant.parse("2026-08-25T00:00:00Z")
        );
        return new DevPathOAuth2User(user, Map.of("id", "1849102", "login", "owner"));
    }
}
