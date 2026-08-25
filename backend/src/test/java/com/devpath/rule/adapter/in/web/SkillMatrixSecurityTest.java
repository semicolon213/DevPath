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

@WebMvcTest(SkillMatrixController.class)
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
    @Autowired MockMvc mockMvc;
    @MockBean SkillMatrixApplicationService service;
    @MockBean GitHubOAuth2UserService oAuth2UserService;

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

    private SkillMatrixView view() {
        return new SkillMatrixView(MATRIX_ID, UUID.randomUUID(), "skill-matrix-v1", "baseline-v1", "CURRENT",
            List.of(), List.of(), List.of(), Instant.parse("2026-08-11T10:00:00Z"));
    }
    private DevPathOAuth2User principal() {
        var user = new AuthenticatedUser(USER_ID, "DevPath User", null, AccountStatus.ACTIVE,
            OAuthProvider.GITHUB, Instant.parse("2026-07-27T00:00:00Z"));
        return new DevPathOAuth2User(user, Map.of("id", "1849102", "login", "owner"));
    }
}
