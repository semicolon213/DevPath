package com.devpath.integration.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.devpath.integration.application.ConnectedAccountView;
import com.devpath.integration.application.NotionIntegrationApplicationService;
import com.devpath.integration.application.NotionWorkspaceListView;
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

@WebMvcTest(NotionIntegrationController.class)
@Import({SecurityConfiguration.class, IdentityFoundationConfiguration.class, AbsoluteSessionTimeoutFilter.class,
    NonPersistingOAuth2AuthorizedClientRepository.class})
@TestPropertySource(properties = {
    "devpath.security.frontend-origin=http://localhost:5173", "devpath.security.session-absolute-timeout=12h",
    "devpath.security.post-login-redirect=/", "spring.security.oauth2.client.registration.github.client-id=test-client",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class NotionIntegrationSecurityTest {
    private static final UUID USER_ID = UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");
    private static final UUID OTHER_USER_ID = UUID.fromString("ad4c199f-8795-47be-937a-8fd7830bc994");
    @Autowired MockMvc mockMvc;
    @MockBean NotionIntegrationApplicationService service;
    @MockBean GitHubOAuth2UserService oAuth2UserService;
    @MockBean com.devpath.identity.application.AuthenticationAuditPort authenticationAuditPort;

    @Test
    void authorizationAndDisconnectRequireCsrfWhileDiscoveryRequiresTheOwnerSession() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/notion/authorize").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isForbidden());
        when(service.authorizationUrl(anyString())).thenReturn("https://api.notion.com/v1/oauth/authorize");
        mockMvc.perform(post("/api/v1/integrations/notion/authorize").with(oauth2Login().oauth2User(principal())).with(csrf()))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/integrations/notion/workspaces")).andExpect(status().isUnauthorized());
        when(service.discover(USER_ID)).thenReturn(new NotionWorkspaceListView(List.of()));
        mockMvc.perform(get("/api/v1/integrations/notion/workspaces").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.workspaces").isArray());
        mockMvc.perform(delete("/api/v1/integrations/notion").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isForbidden());
    }

    @Test
    void callbackConsumesSessionBoundStateAndReturnsOnlyASafeFrontendResult() throws Exception {
        when(service.authorizationUrl(anyString())).thenReturn("https://api.notion.com/v1/oauth/authorize");
        var state = ArgumentCaptor.forClass(String.class);
        var initiated = mockMvc.perform(post("/api/v1/integrations/notion/authorize")
            .with(oauth2Login().oauth2User(principal())).with(csrf())).andReturn();
        verify(service).authorizationUrl(state.capture());
        MockHttpSession session = (MockHttpSession) initiated.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/integrations/notion/callback").session(session)
            .with(oauth2Login().oauth2User(principal())).param("state", state.getValue()).param("code", "temporary-code"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "http://localhost:5173/settings/integrations?notionConnection=success"));
        verify(service).complete(USER_ID, "temporary-code");
    }

    @Test
    void callbackRejectsStateCreatedByAnotherUser() throws Exception {
        when(service.authorizationUrl(anyString())).thenReturn("https://api.notion.com/v1/oauth/authorize");
        var state = ArgumentCaptor.forClass(String.class);
        var initiated = mockMvc.perform(post("/api/v1/integrations/notion/authorize")
            .with(oauth2Login().oauth2User(principal(USER_ID))).with(csrf())).andReturn();
        verify(service).authorizationUrl(state.capture());
        MockHttpSession session = (MockHttpSession) initiated.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/integrations/notion/callback").session(session)
            .with(oauth2Login().oauth2User(principal(OTHER_USER_ID))).param("state", state.getValue())
            .param("code", "temporary-code"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "http://localhost:5173/settings/integrations?notionConnection=failed"));
        verify(service, never()).complete(any(UUID.class), anyString());
    }

    private DevPathOAuth2User principal() {
        return principal(USER_ID);
    }

    private DevPathOAuth2User principal(UUID userId) {
        var user = new AuthenticatedUser(userId, "DevPath User", null, AccountStatus.ACTIVE,
            OAuthProvider.GITHUB, Instant.parse("2026-07-27T00:00:00Z"));
        return new DevPathOAuth2User(user, Map.of("id", "1849102", "login", "devpath-user"));
    }
}
