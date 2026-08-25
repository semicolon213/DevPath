package com.devpath.integration.adapter.in.web;

import com.devpath.identity.config.DevPathSecurityProperties;
import com.devpath.integration.application.GitHubInstallationRequiredException;
import com.devpath.integration.application.GitHubIntegrationApplicationService;
import com.devpath.integration.application.GitHubRepositoryListView;
import com.devpath.integration.application.ConnectedAccountView;
import com.devpath.shared.api.ApiResponse;
import com.devpath.shared.api.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Clock;
import java.util.Base64;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/github")
public class GitHubIntegrationController {
    private static final String STATE = GitHubIntegrationController.class.getName() + ".STATE";
    private static final String USER = GitHubIntegrationController.class.getName() + ".USER";
    private static final String EXPIRES = GitHubIntegrationController.class.getName() + ".EXPIRES";
    private static final long STATE_TTL_SECONDS = 600;
    private final GitHubIntegrationApplicationService service;
    private final DevPathSecurityProperties security;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public GitHubIntegrationController(
        GitHubIntegrationApplicationService service,
        DevPathSecurityProperties security,
        Clock clock
    ) {
        this.service = service;
        this.security = security;
        this.clock = clock;
    }

    @PostMapping("/authorize")
    ApiResponse<OAuthAuthorizationView> authorize(
        Authentication authentication,
        HttpSession session,
        HttpServletRequest request
    ) {
        UUID userId = userId(authentication);
        byte[] stateBytes = new byte[32];
        random.nextBytes(stateBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);
        session.setAttribute(STATE, state);
        session.setAttribute(USER, userId.toString());
        session.setAttribute(EXPIRES, clock.instant().plusSeconds(STATE_TTL_SECONDS).toEpochMilli());
        return ApiResponse.of(new OAuthAuthorizationView(service.authorizationUrl(state)), RequestIds.resolve(request));
    }

    @GetMapping("/callback")
    void callback(
        Authentication authentication,
        HttpSession session,
        @RequestParam String state,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String error,
        HttpServletResponse response
    ) throws IOException {
        try {
            validateAndConsumeState(authentication, session, state);
            if (error != null || code == null || code.isBlank()) {
                redirect(response, "failed");
                return;
            }
            service.complete(userId(authentication), code);
            redirect(response, "success");
        } catch (GitHubInstallationRequiredException exception) {
            redirect(response, "installation-required");
        } catch (RuntimeException exception) {
            redirect(response, "failed");
        }
    }

    @GetMapping("/repositories")
    ApiResponse<GitHubRepositoryListView> repositories(
        Authentication authentication,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.listRepositories(userId(authentication)), RequestIds.resolve(request));
    }

    @DeleteMapping
    ApiResponse<ConnectedAccountView> disconnect(
        Authentication authentication,
        HttpServletRequest request
    ) {
        return ApiResponse.of(service.disconnect(userId(authentication)), RequestIds.resolve(request));
    }

    private void validateAndConsumeState(Authentication authentication, HttpSession session, String received) {
        Object expected = session.getAttribute(STATE);
        Object expectedUser = session.getAttribute(USER);
        Object expires = session.getAttribute(EXPIRES);
        session.removeAttribute(STATE);
        session.removeAttribute(USER);
        session.removeAttribute(EXPIRES);
        boolean validState = expected instanceof String value
            && MessageDigest.isEqual(value.getBytes(java.nio.charset.StandardCharsets.UTF_8), received.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        boolean validUser = expectedUser instanceof String value && value.equals(authentication.getName());
        boolean validExpiry = expires instanceof Long value && value >= clock.instant().toEpochMilli();
        if (!validState || !validUser || !validExpiry) {
            throw new IllegalArgumentException("Invalid or expired GitHub authorization state");
        }
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private void redirect(HttpServletResponse response, String result) throws IOException {
        response.sendRedirect(security.frontendOrigin().replaceAll("/$", "") + "/?githubConnection=" + result);
    }

    public record OAuthAuthorizationView(String authorizationUrl) {}
}
