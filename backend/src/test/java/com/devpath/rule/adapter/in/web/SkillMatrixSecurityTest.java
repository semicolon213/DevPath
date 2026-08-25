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
import com.devpath.rule.application.SkillMatrixApplicationService;
import com.devpath.rule.application.SkillMatrixComparisonView;
import com.devpath.rule.application.SkillDetailView;
import com.devpath.rule.application.SkillEvidenceListView;
import com.devpath.rule.application.SkillEvidenceView;
import com.devpath.rule.application.SkillAssessmentView;
import java.math.BigDecimal;
import com.devpath.rule.application.SkillMatrixView;
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

@WebMvcTest({SkillMatrixController.class, SkillController.class})
@Import({SecurityConfiguration.class, IdentityFoundationConfiguration.class,
    AbsoluteSessionTimeoutFilter.class, NonPersistingOAuth2AuthorizedClientRepository.class})
@TestPropertySource(properties = {
    "devpath.security.frontend-origin=http://localhost:5173",
    "devpath.security.session-absolute-timeout=12h",
    "devpath.security.post-login-redirect=/",
    "spring.security.oauth2.client.registration.github.client-id=test-client",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class SkillMatrixSecurityTest {
    private static final UUID USER_ID = UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");
    private static final UUID MATRIX_ID = UUID.fromString("f823ffb9-88ea-4132-8388-9e5c7981912f");
    private static final UUID OTHER_MATRIX_ID = UUID.fromString("a2c53214-d154-4cb4-a87a-e951ec34953d");
    private static final UUID SKILL_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");
    @Autowired MockMvc mockMvc;
    @MockBean SkillMatrixApplicationService service;
    @MockBean GitHubOAuth2UserService oAuth2UserService;
    @MockBean com.devpath.identity.application.AuthenticationAuditPort authenticationAuditPort;

    @Test
    void currentAndHistoricalMatricesRequireAuthenticatedOwnerContext() throws Exception {
        when(service.getCurrent(USER_ID)).thenReturn(view());
        when(service.get(USER_ID, MATRIX_ID)).thenReturn(view());

        mockMvc.perform(get("/api/v1/skill-matrices/current")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/skill-matrices/current").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CURRENT"));
        mockMvc.perform(get("/api/v1/skill-matrices/{skillMatrixId}", MATRIX_ID)
                .with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.skills").isArray());
        verify(service).getCurrent(USER_ID); verify(service).get(USER_ID, MATRIX_ID);
    }

    @Test
    void comparisonRequiresAuthenticationAndDelegatesBothOwnerScopedIds() throws Exception {
        when(service.compare(USER_ID, List.of(MATRIX_ID, OTHER_MATRIX_ID)))
            .thenReturn(new SkillMatrixComparisonView(List.of(view(), otherView())));

        mockMvc.perform(get("/api/v1/skill-matrices/compare")
                .param("skillMatrixId", MATRIX_ID.toString(), OTHER_MATRIX_ID.toString()))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/skill-matrices/compare")
                .param("skillMatrixId", MATRIX_ID.toString(), OTHER_MATRIX_ID.toString())
                .with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.matrices[0].skillMatrixId").value(MATRIX_ID.toString()))
            .andExpect(jsonPath("$.data.matrices[1].skillMatrixId").value(OTHER_MATRIX_ID.toString()));
        verify(service).compare(USER_ID, List.of(MATRIX_ID, OTHER_MATRIX_ID));
    }

    @Test
    void skillDetailAndEvidenceRequireAuthenticatedOwnerContext() throws Exception {
        when(service.getSkillDetail(USER_ID, SKILL_ID)).thenReturn(detail());
        when(service.getSkillEvidence(USER_ID, SKILL_ID)).thenReturn(evidence());

        mockMvc.perform(get("/api/v1/skills/{skillId}", SKILL_ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/skills/{skillId}", SKILL_ID).with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.skill.skillId").value(SKILL_ID.toString()));
        mockMvc.perform(get("/api/v1/skills/{skillId}/evidence", SKILL_ID)
                .with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.evidence[0].evidenceType").value("REPOSITORY_PATH"));
        verify(service).getSkillDetail(USER_ID, SKILL_ID);
        verify(service).getSkillEvidence(USER_ID, SKILL_ID);
    }

    private SkillMatrixView view() {
        return new SkillMatrixView(MATRIX_ID, UUID.randomUUID(), "skill-matrix-v1", "baseline-v1", "CURRENT",
            List.of(), List.of(), List.of(), Instant.parse("2026-08-11T10:00:00Z"));
    }
    private SkillMatrixView otherView() {
        return new SkillMatrixView(OTHER_MATRIX_ID, UUID.randomUUID(), "skill-matrix-v2", "baseline-v2", "SUPERSEDED",
            List.of(), List.of(), List.of(), Instant.parse("2026-08-10T10:00:00Z"));
    }
    private SkillDetailView detail() {
        return new SkillDetailView(MATRIX_ID, UUID.randomUUID(), "skill-matrix-v2", "baseline-v2", "CURRENT",
            Instant.parse("2026-08-11T10:00:00Z"), new SkillAssessmentView(UUID.randomUUID(), SKILL_ID,
                "testing-discipline", "Testing Discipline", "TESTING", new BigDecimal("70"), "COMPETENT",
                new BigDecimal("90"), false, false, "UNAVAILABLE", "category:TESTING", List.of(UUID.randomUUID()),
                List.of(UUID.randomUUID()), List.of("TEST_FILES"), "baseline-v2"));
    }
    private SkillEvidenceListView evidence() {
        return new SkillEvidenceListView(SKILL_ID, detail().skill().assessmentId(), MATRIX_ID,
            List.of(new SkillEvidenceView(UUID.randomUUID(), UUID.randomUUID(), "REPOSITORY_PATH", "README.md",
                "README evidence", new BigDecimal("100"))));
    }
    private DevPathOAuth2User principal() {
        var user = new AuthenticatedUser(USER_ID, "DevPath User", null, AccountStatus.ACTIVE,
            OAuthProvider.GITHUB, Instant.parse("2026-07-27T00:00:00Z"));
        return new DevPathOAuth2User(user, Map.of("id", "1849102", "login", "owner"));
    }
}
