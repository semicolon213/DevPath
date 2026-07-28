package com.devpath.identity.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devpath.identity.adapter.in.security.AbsoluteSessionTimeoutFilter;
import com.devpath.identity.adapter.in.security.DevPathOAuth2User;
import com.devpath.identity.adapter.in.security.GitHubOAuth2UserService;
import com.devpath.identity.adapter.in.security.NonPersistingOAuth2AuthorizedClientRepository;
import com.devpath.identity.application.AuthenticatedUser;
import com.devpath.identity.application.FindCurrentUserUseCase;
import com.devpath.identity.config.IdentityFoundationConfiguration;
import com.devpath.identity.config.SecurityConfiguration;
import com.devpath.identity.domain.AccountStatus;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.UserId;
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

@WebMvcTest(controllers = {CurrentUserController.class, CsrfController.class})
@Import({
    SecurityConfiguration.class,
    IdentityFoundationConfiguration.class,
    AbsoluteSessionTimeoutFilter.class
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
    private FindCurrentUserUseCase findCurrentUser;

    @MockBean
    private GitHubOAuth2UserService oAuth2UserService;

    @MockBean
    private NonPersistingOAuth2AuthorizedClientRepository authorizedClientRepository;

    @Test
    void rejectsAnonymousCurrentUserRequests() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
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
            )
            .andExpect(status().isNoContent());
    }

    @Test
    void allowsOnlyTheConfiguredCredentialedCorsOrigin() throws Exception {
        mockMvc.perform(
                options("/api/v1/users/me")
                    .header("Origin", "http://localhost:5173")
                    .header("Access-Control-Request-Method", "GET")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(
                options("/api/v1/users/me")
                    .header("Origin", "https://untrusted.example")
                    .header("Access-Control-Request-Method", "GET")
            )
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
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
