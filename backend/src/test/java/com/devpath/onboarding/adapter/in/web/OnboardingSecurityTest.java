package com.devpath.onboarding.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devpath.identity.adapter.in.security.AbsoluteSessionTimeoutFilter;
import com.devpath.identity.adapter.in.security.DevPathOAuth2User;
import com.devpath.identity.adapter.in.security.GitHubOAuth2UserService;
import com.devpath.identity.adapter.in.security.NonPersistingOAuth2AuthorizedClientRepository;
import com.devpath.identity.application.AuthenticatedUser;
import com.devpath.identity.config.IdentityFoundationConfiguration;
import com.devpath.identity.config.SecurityConfiguration;
import com.devpath.identity.domain.AccountStatus;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.onboarding.application.OnboardingApplicationService;
import com.devpath.onboarding.application.OnboardingProgressView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OnboardingController.class)
@Import({SecurityConfiguration.class, IdentityFoundationConfiguration.class, AbsoluteSessionTimeoutFilter.class,
    NonPersistingOAuth2AuthorizedClientRepository.class})
@TestPropertySource(properties = {
    "devpath.security.frontend-origin=http://localhost:5173",
    "devpath.security.session-absolute-timeout=12h",
    "devpath.security.post-login-redirect=/",
    "spring.security.oauth2.client.registration.github.client-id=test-client",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class OnboardingSecurityTest {
    private static final UUID USER_ID = UUID.fromString("44217ac2-3876-4786-91c0-f45f79988da9");
    private static final Instant NOW = Instant.parse("2026-08-25T06:00:00Z");
    @Autowired MockMvc mockMvc;
    @MockBean OnboardingApplicationService service;
    @MockBean GitHubOAuth2UserService oauth;
    @MockBean com.devpath.identity.application.AuthenticationAuditPort authenticationAuditPort;

    @Test
    void requiresAuthenticationAndReturnsOnlyTheCurrentUsersProjection() throws Exception {
        when(service.getProgress(USER_ID)).thenReturn(new OnboardingProgressView(
            "IN_PROGRESS", 4, 8, "REPOSITORY_IMPORT",
            List.of(new OnboardingProgressView.Step(
                "REPOSITORY_IMPORT", "REQUIRED", "INCOMPLETE", null, "/onboarding#github")), NOW));

        mockMvc.perform(get("/api/v1/users/me/onboarding-progress")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me/onboarding-progress").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nextStep").value("REPOSITORY_IMPORT"))
            .andExpect(jsonPath("$.data.steps[0].resourceId").doesNotExist());
    }

    private DevPathOAuth2User principal() {
        return new DevPathOAuth2User(new AuthenticatedUser(USER_ID, "User", null, AccountStatus.ACTIVE,
            OAuthProvider.GITHUB, NOW), Map.of("id", "1", "login", "owner"));
    }
}
