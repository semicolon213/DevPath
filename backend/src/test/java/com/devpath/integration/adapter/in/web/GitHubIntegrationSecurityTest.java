package com.devpath.integration.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.devpath.integration.application.GitHubIntegrationApplicationService;
import com.devpath.integration.application.GitHubRepositoryListView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GitHubIntegrationController.class)
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
class GitHubIntegrationSecurityTest {
    private static final UUID USER_ID = UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");

    @Autowired MockMvc mockMvc;
    @MockBean GitHubIntegrationApplicationService service;
    @MockBean GitHubOAuth2UserService oAuth2UserService;

    @Test
    void authorizationRequiresAuthenticationAndCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/github/authorize"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/integrations/github/authorize").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isForbidden());

        when(service.authorizationUrl(anyString())).thenReturn("https://github.com/login/oauth/authorize?client_id=test");
        mockMvc.perform(post("/api/v1/integrations/github/authorize")
                .with(oauth2Login().oauth2User(principal())).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.authorizationUrl").value("https://github.com/login/oauth/authorize?client_id=test"));
    }

    @Test
    void callbackConsumesTheSessionBoundStateAndRedirectsSafely() throws Exception {
        when(service.authorizationUrl(anyString())).thenReturn("https://github.com/login/oauth/authorize");
        var state = ArgumentCaptor.forClass(String.class);
        var initiated = mockMvc.perform(post("/api/v1/integrations/github/authorize")
                .with(oauth2Login().oauth2User(principal())).with(csrf()))
            .andExpect(status().isOk()).andReturn();
        verify(service).authorizationUrl(state.capture());
        MockHttpSession session = (MockHttpSession) initiated.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/integrations/github/callback")
                .session(session)
                .with(oauth2Login().oauth2User(principal()))
                .param("state", state.getValue())
                .param("code", "temporary-code"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "http://localhost:5173/?githubConnection=success"));
        verify(service).complete(USER_ID, "temporary-code");
    }

    @Test
    void repositoryListingRequiresTheAuthenticatedOwner() throws Exception {
        when(service.listRepositories(USER_ID)).thenReturn(new GitHubRepositoryListView(List.of()));
        mockMvc.perform(get("/api/v1/integrations/github/repositories"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/integrations/github/repositories")
                .with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.repositories").isArray());
    }

    @Test
    void disconnectRequiresAuthenticationAndCsrfAndReturnsRevokedState() throws Exception {
        var disconnected = new com.devpath.integration.application.ConnectedAccountView(
            UUID.randomUUID(), "GITHUB", "REVOKED", List.of(),
            Instant.parse("2026-08-11T00:00:00Z"), Instant.parse("2026-08-11T01:00:00Z")
        );
        when(service.disconnect(USER_ID)).thenReturn(disconnected);

        mockMvc.perform(delete("/api/v1/integrations/github"))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/integrations/github")
                .with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/integrations/github")
                .with(oauth2Login().oauth2User(principal())).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REVOKED"));

        verify(service).disconnect(USER_ID);
    }

    private DevPathOAuth2User principal() {
        var user = new AuthenticatedUser(
            USER_ID, "DevPath User", null, AccountStatus.ACTIVE,
            OAuthProvider.GITHUB, Instant.parse("2026-07-27T00:00:00Z")
        );
        return new DevPathOAuth2User(user, Map.of("id", "1849102", "login", "devpath-user"));
    }
}
