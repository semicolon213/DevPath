package com.devpath.analysis.adapter.in.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devpath.analysis.application.AnalysisApplicationService;
import com.devpath.analysis.application.AnalysisJobView;
import com.devpath.analysis.application.AnalysisHistoryItemView;
import com.devpath.analysis.application.AnalysisHistoryView;
import com.devpath.analysis.application.AnalysisResultView;
import com.devpath.identity.adapter.in.security.AbsoluteSessionTimeoutFilter;
import com.devpath.identity.adapter.in.security.DevPathOAuth2User;
import com.devpath.identity.adapter.in.security.GitHubOAuth2UserService;
import com.devpath.identity.adapter.in.security.NonPersistingOAuth2AuthorizedClientRepository;
import com.devpath.identity.application.AuthenticatedUser;
import com.devpath.identity.config.IdentityFoundationConfiguration;
import com.devpath.identity.config.SecurityConfiguration;
import com.devpath.identity.domain.AccountStatus;
import com.devpath.identity.domain.OAuthProvider;
import java.time.Instant;
import java.math.BigDecimal;
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

@WebMvcTest(AnalysisController.class)
@Import({SecurityConfiguration.class, IdentityFoundationConfiguration.class,
    AbsoluteSessionTimeoutFilter.class, NonPersistingOAuth2AuthorizedClientRepository.class})
@TestPropertySource(properties = {
    "devpath.security.frontend-origin=http://localhost:5173",
    "devpath.security.session-absolute-timeout=12h",
    "devpath.security.post-login-redirect=/",
    "spring.security.oauth2.client.registration.github.client-id=test-client",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class AnalysisSecurityTest {
    private static final UUID USER_ID = UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");
    private static final UUID REPOSITORY_ID = UUID.fromString("4650f15a-b8aa-47bd-8c15-7579e05f737e");
    private static final UUID JOB_ID = UUID.fromString("f055f15a-b8aa-47bd-8c15-7579e05f737e");
    private static final UUID ANALYSIS_ID = UUID.fromString("a055f15a-b8aa-47bd-8c15-7579e05f737e");
    @Autowired MockMvc mockMvc;
    @MockBean AnalysisApplicationService service;
    @MockBean GitHubOAuth2UserService oAuth2UserService;

    @Test
    void commandsRequireSessionAndCsrfWhileReadsUseAuthenticatedOwner() throws Exception {
        when(service.request(eq(USER_ID), eq(REPOSITORY_ID), eq(null), eq("REPOSITORY_BASELINE"), eq("key")))
            .thenReturn(job());
        when(service.getJob(USER_ID, JOB_ID)).thenReturn(job());
        when(service.getResult(USER_ID, ANALYSIS_ID)).thenReturn(result());
        when(service.listHistory(USER_ID, 10, null)).thenReturn(history());
        when(service.listRepositoryHistory(USER_ID, REPOSITORY_ID, 10, null)).thenReturn(history());
        String body = "{\"repositoryId\":\"" + REPOSITORY_ID + "\",\"analysisScope\":\"REPOSITORY_BASELINE\"}";

        mockMvc.perform(post("/api/v1/analyses").contentType(MediaType.APPLICATION_JSON).content(body)
            .header("Idempotency-Key", "key")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/analyses").with(oauth2Login().oauth2User(principal()))
            .contentType(MediaType.APPLICATION_JSON).content(body).header("Idempotency-Key", "key"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/analyses").with(oauth2Login().oauth2User(principal())).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body).header("Idempotency-Key", "key"))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.data.status").value("queued"));
        mockMvc.perform(get("/api/v1/analysis-jobs/{jobId}", JOB_ID).with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/analyses/{analysisId}", ANALYSIS_ID).with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.analysisId").value(ANALYSIS_ID.toString()))
            .andExpect(jsonPath("$.data.currentForRepository").value(true));
        mockMvc.perform(get("/api/v1/analyses").param("limit", "10").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.analyses[0].overallScore").value(75.5))
            .andExpect(jsonPath("$.data.analyses[0].currentForRepository").value(true));
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/analyses", REPOSITORY_ID).param("limit", "10")
            .with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalCount").value(1));

        verify(service).getJob(USER_ID, JOB_ID);
        verify(service).getResult(USER_ID, ANALYSIS_ID);
    }

    private AnalysisJobView job() {
        return new AnalysisJobView(JOB_ID, "REPOSITORY_ANALYSIS", "queued", "QUEUED", 0, 0, 3,
            Instant.parse("2026-08-11T10:00:00Z"), null, null, "/api/v1/analysis-jobs/" + JOB_ID,
            null, null, null, false);
    }
    private AnalysisResultView result() {
        return new AnalysisResultView(ANALYSIS_ID, REPOSITORY_ID, UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), "REPOSITORY_BASELINE", true, Instant.parse("2026-08-11T10:00:02Z"));
    }
    private AnalysisHistoryView history() {
        return new AnalysisHistoryView(List.of(new AnalysisHistoryItemView(ANALYSIS_ID, REPOSITORY_ID,
            "owner/devpath", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "REPOSITORY_BASELINE",
            new BigDecimal("75.50"), new BigDecimal("88.00"), "baseline-v1", "skill-matrix-v1",
            true, Instant.parse("2026-08-11T10:00:02Z"))), 10, null, 1);
    }
    private DevPathOAuth2User principal() {
        var user = new AuthenticatedUser(USER_ID, "DevPath User", null, AccountStatus.ACTIVE,
            OAuthProvider.GITHUB, Instant.parse("2026-07-27T00:00:00Z"));
        return new DevPathOAuth2User(user, Map.of("id", "1849102", "login", "owner"));
    }
}
