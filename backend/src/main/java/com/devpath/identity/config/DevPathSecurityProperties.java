package com.devpath.identity.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties("devpath.security")
public record DevPathSecurityProperties(
    @NotBlank String frontendOrigin,
    @NotNull Duration sessionAbsoluteTimeout,
    @NotBlank String postLoginRedirect
) {
    public DevPathSecurityProperties {
        URI origin = URI.create(frontendOrigin);
        if (origin.getScheme() == null || origin.getHost() == null || origin.getQuery() != null || origin.getFragment() != null) {
            throw new IllegalArgumentException("Frontend origin must be an absolute origin without query or fragment");
        }
        if (sessionAbsoluteTimeout.isZero() || sessionAbsoluteTimeout.isNegative()) {
            throw new IllegalArgumentException("Session absolute timeout must be positive");
        }
        if (!postLoginRedirect.startsWith("/") || postLoginRedirect.startsWith("//")) {
            throw new IllegalArgumentException("Post-login redirect must be an application-relative path");
        }
    }

    public String loginSuccessUrl() {
        return frontendOrigin.replaceAll("/$", "") + postLoginRedirect;
    }

    public String loginFailureUrl() {
        return frontendOrigin.replaceAll("/$", "") + "/?authentication=failed";
    }
}
