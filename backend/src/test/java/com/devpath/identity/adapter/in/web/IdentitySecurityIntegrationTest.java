package com.devpath.identity.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devpath.identity.adapter.in.security.AbsoluteSessionTimeoutFilter;
import com.devpath.identity.adapter.in.security.DevPathOAuth2User;
import com.devpath.identity.adapter.in.security.GitHubOAuth2UserService;
import com.devpath.identity.adapter.in.security.NonPersistingOAuth2AuthorizedClientRepository;
import com.devpath.identity.application.AuthenticatedUser;
import com.devpath.identity.application.AuthenticationAuditEvent;
import com.devpath.identity.application.AuthenticationAuditPort;
import com.devpath.identity.application.CurrentUserApplicationService;
import com.devpath.identity.application.UserProfileApplicationService;
import com.devpath.identity.application.UserProfileView;
import com.devpath.identity.application.UserPreferenceView;
import com.devpath.identity.config.IdentityFoundationConfiguration;
import com.devpath.identity.config.SecurityConfiguration;
import com.devpath.identity.domain.AccountStatus;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.UserId;
import com.devpath.integration.application.ConnectedAccountListView;
import com.devpath.integration.application.ConnectedAccountView;
import com.devpath.integration.application.ProviderConnectionApplicationService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {CurrentUserController.class, CsrfController.class, UserProfileController.class})
@Import({
    SecurityConfiguration.class,
    IdentityFoundationConfiguration.class,
    AbsoluteSessionTimeoutFilter.class,
    NonPersistingOAuth2AuthorizedClientRepository.class
})
@TestPropertySource(properties = {
    "devpath.security.frontend-origin=http://localhost:5173",
    "devpath.security.session-absolute-timeout=12h",
    "devpath.security.post-login-redirect=/",
    "spring.security.oauth2.client.registration.github.client-id=test-client",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class IdentitySecurityIntegrationTest {
    private static final UUID USER_ID = UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrentUserApplicationService findCurrentUser;

    @MockBean
    private GitHubOAuth2UserService oAuth2UserService;

    @MockBean
    private UserProfileApplicationService userProfileService;

    @MockBean
    private ProviderConnectionApplicationService connectedAccounts;

    @MockBean
    private AuthenticationAuditPort authenticationAuditPort;

    @Test
    void rejectsAnonymousCurrentUserRequests() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                .header("X-Request-Id", "mvp-gate-request")
                .header("X-Correlation-Id", "mvp-gate-journey"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("X-Request-Id", "mvp-gate-request"))
            .andExpect(header().string("X-Correlation-Id", "mvp-gate-journey"))
            .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.metadata.requestId").value("mvp-gate-request"));
    }

    @Test
    void returnsTheAuthenticatedInternalUser() throws Exception {
        AuthenticatedUser user = authenticatedUser();
        when(findCurrentUser.find(new UserId(USER_ID))).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/me").with(oauth2Login().oauth2User(principal(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
            .andExpect(jsonPath("$.data.displayName").value("DevPath User"))
            .andExpect(jsonPath("$.data.authenticationProvider").value("GITHUB"))
            .andExpect(jsonPath("$.metadata.apiVersion").value("v1"));
    }

    @Test
    void returnsOnlyTheAuthenticatedUsersProviderConnections() throws Exception {
        AuthenticatedUser user = authenticatedUser();
        var connection = new ConnectedAccountView(
            UUID.randomUUID(),
            "GITHUB",
            "ACTIVE",
            java.util.List.of("repo"),
            Instant.parse("2026-08-11T00:00:00Z"),
            null
        );
        when(connectedAccounts.listFor(USER_ID)).thenReturn(new ConnectedAccountListView(java.util.List.of(connection)));

        mockMvc.perform(get("/api/v1/users/me/connections").with(oauth2Login().oauth2User(principal(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.connections[0].provider").value("GITHUB"))
            .andExpect(jsonPath("$.data.connections[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.connections[0].scopes[0]").value("repo"));
    }

    @Test
    void requiresCsrfForLogoutAndAcceptsTheServerToken() throws Exception {
        AuthenticatedUser user = authenticatedUser();

        mockMvc.perform(post("/api/v1/session/logout").with(oauth2Login().oauth2User(principal(user))))
            .andExpect(status().isForbidden());

        mockMvc.perform(
                post("/api/v1/session/logout")
                    .with(oauth2Login().oauth2User(principal(user)))
                    .header("X-CSRF-TOKEN", "invalid-token")
            )
            .andExpect(status().isForbidden());

        mockMvc.perform(
                post("/api/v1/session/logout")
                    .with(oauth2Login().oauth2User(principal(user)))
                    .with(csrf())
                    .header("X-Request-Id", "logout-request")
                    .header("X-Correlation-Id", "logout-journey")
            )
            .andExpect(status().isNoContent())
            .andExpect(header().string("X-Request-Id", "logout-request"))
            .andExpect(header().string("X-Correlation-Id", "logout-journey"));

        verify(authenticationAuditPort).record(
            org.mockito.ArgumentMatchers.eq(AuthenticationAuditEvent.LOGOUT_SUCCEEDED),
            org.mockito.ArgumentMatchers.eq(USER_ID),
            org.mockito.ArgumentMatchers.eq(OAuthProvider.GITHUB),
            any(Instant.class)
        );
    }

    @Test
    void allowsOnlyTheConfiguredCredentialedCorsOrigin() throws Exception {
        mockMvc.perform(
                options("/api/v1/users/me")
                    .header("Origin", "http://localhost:5173")
                    .header("Access-Control-Request-Method", "GET")
                    .header("Access-Control-Request-Headers", "X-Request-Id,X-Correlation-Id")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
            .andExpect(header().string("Access-Control-Allow-Headers", containsString("X-Request-Id")))
            .andExpect(header().string("Access-Control-Allow-Headers", containsString("X-Correlation-Id")))
            .andExpect(header().string("Access-Control-Expose-Headers", containsString("X-Request-Id")))
            .andExpect(header().string("Access-Control-Expose-Headers", containsString("X-Correlation-Id")));

        mockMvc.perform(
                options("/api/v1/users/me/profile")
                    .header("Origin", "http://localhost:5173")
                    .header("Access-Control-Request-Method", "PATCH")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("PATCH")));

        mockMvc.perform(
                options("/api/v1/repositories/00000000-0000-0000-0000-000000000000/sync")
                    .header("Origin", "http://localhost:5173")
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "x-csrf-token,idempotency-key")
            )
            .andExpect(status().isOk())
            .andExpect(header().string(
                "Access-Control-Allow-Headers",
                org.hamcrest.Matchers.allOf(
                    org.hamcrest.Matchers.containsStringIgnoringCase("x-csrf-token"),
                    org.hamcrest.Matchers.containsStringIgnoringCase("idempotency-key")
                )
            ));

        mockMvc.perform(
                options("/api/v1/users/me")
                    .header("Origin", "https://untrusted.example")
                    .header("Access-Control-Request-Method", "GET")
            )
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void returnsAndUpdatesOnlyTheAuthenticatedUsersProfile() throws Exception {
        var user = authenticatedUser();
        var profile = new UserProfileView(UUID.randomUUID(), "DevPath User", null, null, Instant.parse("2026-07-27T00:00:00Z"));
        when(userProfileService.getProfile(new UserId(USER_ID))).thenReturn(profile);
        when(userProfileService.updateProfile(new UserId(USER_ID), "Updated User", null, "Backend developer")).thenReturn(
            new UserProfileView(profile.profileId(), "Updated User", null, "Backend developer", Instant.parse("2026-07-27T00:01:00Z"))
        );

        mockMvc.perform(get("/api/v1/users/me/profile").with(oauth2Login().oauth2User(principal(user))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.profileId").value(profile.profileId().toString()));

        mockMvc.perform(patch("/api/v1/users/me/profile").with(oauth2Login().oauth2User(principal(user))).with(csrf())
                .contentType("application/json")
                .content("{\"displayName\":\"Updated User\",\"careerStage\":null,\"bio\":\"Backend developer\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.displayName").value("Updated User"));
    }

    @Test
    void requiresCsrfAndRejectsInvalidPreferencePayloads() throws Exception {
        var user = authenticatedUser();
        when(userProfileService.setCareer(new UserId(USER_ID), "backend"))
            .thenReturn(new UserPreferenceView("backend", null, Instant.parse("2026-07-27T00:00:00Z")));

        mockMvc.perform(put("/api/v1/users/me/preferences/career").with(oauth2Login().oauth2User(principal(user)))
                .contentType("application/json").content("{\"careerId\":\"backend\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/users/me/preferences/career").with(oauth2Login().oauth2User(principal(user))).with(csrf())
                .contentType("application/json").content("{\"careerId\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
            USER_ID,
            "DevPath User",
            null,
            AccountStatus.ACTIVE,
            OAuthProvider.GITHUB,
            Instant.parse("2026-07-27T00:00:00Z")
        );
    }

    private DevPathOAuth2User principal(AuthenticatedUser user) {
        return new DevPathOAuth2User(user, Map.of("id", "1849102", "login", "devpath-user"));
    }
}
