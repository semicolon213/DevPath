package com.devpath.repository.adapter.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.devpath.repository.application.RepositoryApplicationService;
import com.devpath.repository.application.RepositoryListView;
import com.devpath.repository.application.RepositoryView;
import com.devpath.integration.application.GitHubRateLimitExceededException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RepositoryController.class)
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
class RepositorySecurityTest {
    private static final UUID USER_ID = UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");
    private static final UUID REPOSITORY_ID = UUID.fromString("3fd75d74-17d4-4dc5-bf3b-251f611633f2");

    @Autowired MockMvc mockMvc;
    @MockBean RepositoryApplicationService service;
    @MockBean GitHubOAuth2UserService oAuth2UserService;
    @MockBean com.devpath.identity.application.AuthenticationAuditPort authenticationAuditPort;

    @Test
    void importRequiresTheAuthenticatedOwnerAndCsrf() throws Exception {
        when(service.importGitHub(USER_ID, "42")).thenReturn(repository());

        mockMvc.perform(post("/api/v1/repositories/imports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"providerRepositoryId\":\"42\"}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/repositories/imports")
                .with(oauth2Login().oauth2User(principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"providerRepositoryId\":\"42\"}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/repositories/imports")
                .with(oauth2Login().oauth2User(principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"providerRepositoryId\":\"42\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.repositoryId").value(REPOSITORY_ID.toString()));

        verify(service).importGitHub(USER_ID, "42");
    }

    @Test
    void importSurfacesAProviderRateLimitWithoutTreatingItAsInvalidAccess() throws Exception {
        Instant resetAt = Instant.parse("2026-08-11T01:00:00Z");
        when(service.importGitHub(USER_ID, "42")).thenThrow(
            new GitHubRateLimitExceededException(resetAt, null, new RuntimeException("provider payload"))
        );

        mockMvc.perform(post("/api/v1/repositories/imports")
                .with(oauth2Login().oauth2User(principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"providerRepositoryId\":\"42\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "Tue, 11 Aug 2026 01:00:00 GMT"))
            .andExpect(header().string("X-RateLimit-Reset", Long.toString(resetAt.getEpochSecond())))
            .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void listAndDetailAreScopedToTheAuthenticatedUser() throws Exception {
        when(service.list(USER_ID, 20, null, false)).thenReturn(new RepositoryListView(List.of(repository()), 20, null, 1));
        when(service.get(USER_ID, REPOSITORY_ID)).thenReturn(repository());

        mockMvc.perform(get("/api/v1/repositories")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/repositories").param("limit", "20")
                .with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalCount").value(1));
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}", REPOSITORY_ID)
                .with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fullName").value("owner/devpath"));

        verify(service).get(USER_ID, REPOSITORY_ID);
    }

    @Test
    void archiveAndRestoreRequireTheOwnerSessionAndCsrf() throws Exception {
        when(service.archive(USER_ID, REPOSITORY_ID)).thenReturn(repository("ARCHIVED"));
        when(service.restore(USER_ID, REPOSITORY_ID)).thenReturn(repository("DISCOVERED"));

        mockMvc.perform(post("/api/v1/repositories/{repositoryId}/archive", REPOSITORY_ID)
                .with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/repositories/{repositoryId}/archive", REPOSITORY_ID)
                .with(oauth2Login().oauth2User(principal())).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lifecycle").value("ARCHIVED"));
        mockMvc.perform(post("/api/v1/repositories/{repositoryId}/restore", REPOSITORY_ID)
                .with(oauth2Login().oauth2User(principal())).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lifecycle").value("DISCOVERED"));

        verify(service).archive(USER_ID, REPOSITORY_ID);
        verify(service).restore(USER_ID, REPOSITORY_ID);
    }

    private RepositoryView repository() {
        return repository("DISCOVERED");
    }

    private RepositoryView repository(String lifecycle) {
        return new RepositoryView(
            REPOSITORY_ID, "42", "devpath", "owner/devpath", "owner", "PRIVATE", "main",
            false, lifecycle, "NOT_SYNCED", "https://github.com/owner/devpath",
            Instant.parse("2026-08-11T00:00:00Z"), null, null
        );
    }

    private DevPathOAuth2User principal() {
        var user = new AuthenticatedUser(
            USER_ID, "DevPath User", null, AccountStatus.ACTIVE,
            OAuthProvider.GITHUB, Instant.parse("2026-07-27T00:00:00Z")
        );
        return new DevPathOAuth2User(user, Map.of("id", "1849102", "login", "owner"));
    }
}
