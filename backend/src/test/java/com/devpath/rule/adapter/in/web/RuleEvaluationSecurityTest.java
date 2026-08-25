package com.devpath.rule.adapter.in.web;

import static org.mockito.Mockito.verify;
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
import com.devpath.rule.application.CompletedRuleEvaluationApplicationService;
import com.devpath.rule.application.RuleEvaluationView;
import com.devpath.rule.application.RuleEvidenceListView;
import com.devpath.rule.application.RuleEvidenceSummaryView;
import com.devpath.rule.application.RuleScoreBreakdownView;
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

@WebMvcTest(RuleEvaluationController.class)
@Import({SecurityConfiguration.class, IdentityFoundationConfiguration.class,
    AbsoluteSessionTimeoutFilter.class, NonPersistingOAuth2AuthorizedClientRepository.class})
@TestPropertySource(properties = {
    "devpath.security.frontend-origin=http://localhost:5173",
    "devpath.security.session-absolute-timeout=12h",
    "devpath.security.post-login-redirect=/",
    "spring.security.oauth2.client.registration.github.client-id=test-client",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class RuleEvaluationSecurityTest {
    private static final UUID USER_ID = UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");
    private static final UUID EVALUATION_ID = UUID.fromString("6a45f02b-801a-4aa1-9358-455764885811");
    private static final UUID SNAPSHOT_ID = UUID.fromString("4583425c-e120-4f7a-91e3-37fac326254f");
    private static final UUID VERSION_ID = UUID.fromString("11000000-0000-0000-0000-000000000001");

    @Autowired MockMvc mockMvc;
    @MockBean CompletedRuleEvaluationApplicationService service;
    @MockBean GitHubOAuth2UserService oAuth2UserService;

    @Test
    void allEvaluationReadsRequireAnAuthenticatedOwnerContext() throws Exception {
        when(service.getEvaluation(USER_ID, EVALUATION_ID)).thenReturn(evaluation());
        when(service.getScoreBreakdown(USER_ID, EVALUATION_ID)).thenReturn(
            new RuleScoreBreakdownView(EVALUATION_ID, new BigDecimal("75.50"), new BigDecimal("100.00"), List.of()));
        when(service.getEvidence(USER_ID, EVALUATION_ID)).thenReturn(new RuleEvidenceListView(EVALUATION_ID, List.of()));

        mockMvc.perform(get("/api/v1/rule-evaluations/{evaluationId}", EVALUATION_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/rule-evaluations/{evaluationId}", EVALUATION_ID).with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.overallScore").value(75.50));
        mockMvc.perform(get("/api/v1/rule-evaluations/{evaluationId}/score-breakdown", EVALUATION_ID)
                .with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/rule-evaluations/{evaluationId}/evidence", EVALUATION_ID)
                .with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk());

        verify(service).getEvaluation(USER_ID, EVALUATION_ID);
        verify(service).getScoreBreakdown(USER_ID, EVALUATION_ID);
        verify(service).getEvidence(USER_ID, EVALUATION_ID);
    }

    private RuleEvaluationView evaluation() {
        return new RuleEvaluationView(EVALUATION_ID, SNAPSHOT_ID, VERSION_ID, "baseline-v1", "formula-v1",
            "engineering-evidence-extractor-v1", new BigDecimal("75.50"), new BigDecimal("100.00"),
            new RuleEvidenceSummaryView(4, 3, 0), List.of(), List.of(), Instant.parse("2026-08-11T10:00:00Z"));
    }

    private DevPathOAuth2User principal() {
        var user = new AuthenticatedUser(USER_ID, "DevPath User", null, AccountStatus.ACTIVE,
            OAuthProvider.GITHUB, Instant.parse("2026-07-27T00:00:00Z"));
        return new DevPathOAuth2User(user, Map.of("id", "1849102", "login", "owner"));
    }
}
