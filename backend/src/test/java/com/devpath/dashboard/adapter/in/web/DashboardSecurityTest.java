package com.devpath.dashboard.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devpath.dashboard.application.DashboardApplicationService;
import com.devpath.dashboard.application.DashboardSummaryView;
import com.devpath.dashboard.application.DashboardSummaryView.SourceStatus;
import com.devpath.identity.adapter.in.security.AbsoluteSessionTimeoutFilter;
import com.devpath.identity.adapter.in.security.DevPathOAuth2User;
import com.devpath.identity.adapter.in.security.GitHubOAuth2UserService;
import com.devpath.identity.adapter.in.security.NonPersistingOAuth2AuthorizedClientRepository;
import com.devpath.identity.application.AuthenticatedUser;
import com.devpath.identity.config.IdentityFoundationConfiguration;
import com.devpath.identity.config.SecurityConfiguration;
import com.devpath.identity.domain.AccountStatus;
import com.devpath.identity.domain.OAuthProvider;
import java.math.BigDecimal;
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

@WebMvcTest(DashboardController.class)
@Import({SecurityConfiguration.class, IdentityFoundationConfiguration.class, AbsoluteSessionTimeoutFilter.class,
    NonPersistingOAuth2AuthorizedClientRepository.class})
@TestPropertySource(properties = {
    "devpath.security.frontend-origin=http://localhost:5173",
    "devpath.security.session-absolute-timeout=12h",
    "devpath.security.post-login-redirect=/",
    "spring.security.oauth2.client.registration.github.client-id=test-client",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class DashboardSecurityTest {
    private static final UUID USER_ID = UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");
    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");
    @Autowired MockMvc mockMvc;
    @MockBean DashboardApplicationService service;
    @MockBean GitHubOAuth2UserService oauth;
    @MockBean com.devpath.identity.application.AuthenticationAuditPort authenticationAuditPort;

    @Test
    void requiresAuthenticationAndReturnsTheOwnerScopedSummary() throws Exception {
        when(service.getSummary(USER_ID)).thenReturn(summary());

        mockMvc.perform(get("/api/v1/dashboard/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/summary").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.repositories.totalCount").value(2))
            .andExpect(jsonPath("$.data.analyses.latest.overallScore").value(82.5))
            .andExpect(jsonPath("$.data.recommendations.items[0].priority").value("HIGH"));
    }

    private DashboardSummaryView summary() {
        UUID repositoryId = UUID.randomUUID();
        return new DashboardSummaryView(NOW,
            new DashboardSummaryView.TargetSummary(SourceStatus.AVAILABLE,
                new DashboardSummaryView.Target("backend", "백엔드 개발자", "career-v2"), null),
            new DashboardSummaryView.RepositorySummary(SourceStatus.AVAILABLE, 2, 1, List.of()),
            new DashboardSummaryView.AnalysisSummary(SourceStatus.AVAILABLE, 1,
                new DashboardSummaryView.AnalysisItem(UUID.randomUUID(), repositoryId, "owner/devpath",
                    new BigDecimal("82.50"), new BigDecimal("90.00"), true, NOW), List.of()),
            new DashboardSummaryView.SkillSummary(SourceStatus.AVAILABLE, UUID.randomUUID(), 5, 2, 1,
                "skills-v1", "rules-v1", NOW),
            new DashboardSummaryView.ReadinessSummary(SourceStatus.AVAILABLE, UUID.randomUUID(), "COMPLETED",
                new BigDecimal("76.50"), "COMPETENT", new BigDecimal("88.00"), List.of(), NOW),
            new DashboardSummaryView.RecommendationSummary(SourceStatus.AVAILABLE, UUID.randomUUID(),
                "recommendation-v1", List.of(new DashboardSummaryView.RecommendationItem(UUID.randomUUID(),
                    "TESTING", "PROJECT", "HIGH", "테스트 보강", 16, 0, "PROPOSED")), NOW),
            new DashboardSummaryView.RoadmapSummary(SourceStatus.AVAILABLE, UUID.randomUUID(), "roadmap-v1",
                "CREATED", BigDecimal.ZERO, 1, 1, NOW),
            new DashboardSummaryView.JobSummary(SourceStatus.EMPTY, List.of()));
    }

    private DevPathOAuth2User principal() {
        return new DevPathOAuth2User(new AuthenticatedUser(USER_ID, "User", null, AccountStatus.ACTIVE,
            OAuthProvider.GITHUB, NOW), Map.of("id", "1", "login", "owner"));
    }
}
