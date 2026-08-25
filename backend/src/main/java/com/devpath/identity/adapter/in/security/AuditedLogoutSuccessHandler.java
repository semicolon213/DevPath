package com.devpath.identity.adapter.in.security;

import com.devpath.identity.application.AuthenticationAuditEvent;
import com.devpath.identity.application.AuthenticationAuditPort;
import com.devpath.identity.domain.OAuthProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

public final class AuditedLogoutSuccessHandler implements LogoutSuccessHandler {
    private static final Logger log = LoggerFactory.getLogger(AuditedLogoutSuccessHandler.class);

    private final AuthenticationAuditPort auditPort;
    private final Clock clock;

    public AuditedLogoutSuccessHandler(AuthenticationAuditPort auditPort, Clock clock) {
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public void onLogoutSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        if (authentication != null && authentication.getPrincipal() instanceof DevPathOAuth2User user) {
            try {
                auditPort.record(
                    AuthenticationAuditEvent.LOGOUT_SUCCEEDED,
                    user.userId(),
                    OAuthProvider.GITHUB,
                    clock.instant()
                );
            } catch (RuntimeException exception) {
                log.error("logout_audit_record_failed", exception);
            }
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
