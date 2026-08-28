package com.devpath.repository.adapter.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.devpath.repository.application.RepositorySnapshotListView;
import com.devpath.repository.application.RepositoryActivityEventView;
import com.devpath.repository.application.RepositoryActivityTimelineView;
import com.devpath.repository.application.RepositoryEvidenceSummaryView;
import com.devpath.repository.application.RepositorySyncJobView;
import com.devpath.repository.application.RepositorySynchronizationApplicationService;
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

@WebMvcTest(RepositorySynchronizationController.class)
@Import({SecurityConfiguration.class, IdentityFoundationConfiguration.class,
    AbsoluteSessionTimeoutFilter.class, NonPersistingOAuth2AuthorizedClientRepository.class})
@TestPropertySource(properties = {
    "devpath.security.frontend-origin=http://localhost:5173",
    "devpath.security.session-absolute-timeout=12h",
    "devpath.security.post-login-redirect=/",
    "spring.security.oauth2.client.registration.github.client-id=test-client",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class RepositorySynchronizationSecurityTest {
    private static final UUID USER_ID = UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");
    private static final UUID REPOSITORY_ID = UUID.fromString("3fd75d74-17d4-4dc5-bf3b-251f611633f2");
    private static final UUID JOB_ID = UUID.fromString("38393675-fd18-410d-9fb8-cff66200fa46");
    private static final UUID SNAPSHOT_ID = UUID.fromString("97fb9cf1-598d-4726-976d-8b20d71609f5");

    @Autowired MockMvc mockMvc;
    @MockBean RepositorySynchronizationApplicationService service;
    @MockBean GitHubOAuth2UserService oAuth2UserService;
    @MockBean com.devpath.identity.application.AuthenticationAuditPort authenticationAuditPort;

    @Test
    void syncRequiresSessionCsrfAndIdempotencyKey() throws Exception {
        when(service.request(USER_ID, REPOSITORY_ID, "sync-1")).thenReturn(job());

        mockMvc.perform(post("/api/v1/repositories/{repositoryId}/sync", REPOSITORY_ID)
                .with(oauth2Login().oauth2User(principal())).header("Idempotency-Key", "sync-1"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/repositories/{repositoryId}/sync", REPOSITORY_ID)
                .with(oauth2Login().oauth2User(principal())).with(csrf()).header("Idempotency-Key", "sync-1"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.data.status").value("queued"));

        verify(service).request(USER_ID, REPOSITORY_ID, "sync-1");
    }

    @Test
    void jobAndSnapshotsRequireTheOwnerSession() throws Exception {
        when(service.getJob(USER_ID, JOB_ID)).thenReturn(job());
        when(service.listSnapshots(USER_ID, REPOSITORY_ID)).thenReturn(new RepositorySnapshotListView(List.of()));

        mockMvc.perform(get("/api/v1/repository-sync-jobs/{jobId}", JOB_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/repository-sync-jobs/{jobId}", JOB_ID)
                .with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/snapshots", REPOSITORY_ID)
                .with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.snapshots").isArray());
    }

    @Test
    void evidenceTimelineRequiresTheOwnerSessionAndReturnsOnlyNormalizedActivity() throws Exception {
        var timeline = new RepositoryActivityTimelineView(
            "repository-activity-timeline-v1", "CURRENT_SNAPSHOT",
            Instant.parse("2026-08-11T00:00:00Z"), Instant.parse("2026-08-10T00:00:00Z"),
            1L, 1, false, List.of(new RepositoryActivityEventView(
                "COMMIT", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", Instant.parse("2026-08-10T00:00:00Z")
            ))
        );
        when(service.getEvidence(USER_ID, REPOSITORY_ID)).thenReturn(new RepositoryEvidenceSummaryView(
            REPOSITORY_ID, SNAPSHOT_ID, "engineering-evidence-extractor-v3", List.of(), timeline
        ));

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evidence", REPOSITORY_ID))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evidence", REPOSITORY_ID)
                .with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.activityTimeline.scope").value("CURRENT_SNAPSHOT"))
            .andExpect(jsonPath("$.data.activityTimeline.daysSinceLatestActivity").value(1))
            .andExpect(jsonPath("$.data.activityTimeline.events[0].eventType").value("COMMIT"))
            .andExpect(jsonPath("$.data.activityTimeline.events[0].sourceReference")
                .value("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));

        verify(service).getEvidence(USER_ID, REPOSITORY_ID);
    }

    private RepositorySyncJobView job() {
        return new RepositorySyncJobView(
            JOB_ID, "REPOSITORY_SYNC", "queued", "QUEUED", 0, 0, 3,
            Instant.parse("2026-08-11T00:00:00Z"), null, null,
            "/api/v1/repository-sync-jobs/" + JOB_ID, null, null, null, false
        );
    }

    private DevPathOAuth2User principal() {
        var user = new AuthenticatedUser(USER_ID, "DevPath User", null, AccountStatus.ACTIVE,
            OAuthProvider.GITHUB, Instant.parse("2026-07-27T00:00:00Z"));
        return new DevPathOAuth2User(user, Map.of("id", "1849102", "login", "owner"));
    }
}
