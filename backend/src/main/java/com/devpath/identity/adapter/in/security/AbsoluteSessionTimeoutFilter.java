package com.devpath.identity.adapter.in.security;

import com.devpath.identity.config.DevPathSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

@Component
public class AbsoluteSessionTimeoutFilter extends OncePerRequestFilter {
    private final DevPathSecurityProperties properties;
    private final Clock clock;

    public AbsoluteSessionTimeoutFilter(DevPathSecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
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
                session.invalidate();
            }
        }
        filterChain.doFilter(request, response);
    }
}
