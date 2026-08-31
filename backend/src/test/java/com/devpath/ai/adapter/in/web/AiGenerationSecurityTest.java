package com.devpath.ai.adapter.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devpath.ai.application.AiGenerationApplicationService;
import com.devpath.ai.application.GenerationJobView;
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

@WebMvcTest(AiGenerationController.class)
@Import({SecurityConfiguration.class, IdentityFoundationConfiguration.class, AbsoluteSessionTimeoutFilter.class,
    NonPersistingOAuth2AuthorizedClientRepository.class})
@TestPropertySource(properties = {"devpath.security.frontend-origin=http://localhost:5173",
    "devpath.security.session-absolute-timeout=12h", "devpath.security.post-login-redirect=/",
    "spring.security.oauth2.client.registration.github.client-id=test",
    "spring.security.oauth2.client.registration.github.client-secret=test"})
class AiGenerationSecurityTest {
    static final UUID USER = UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");
    static final UUID MATRIX = UUID.randomUUID(), JOB = UUID.randomUUID();
    @Autowired MockMvc mockMvc;
    @MockBean AiGenerationApplicationService service;
    @MockBean GitHubOAuth2UserService oAuth2UserService;
    @MockBean com.devpath.identity.application.AuthenticationAuditPort authenticationAuditPort;

    @Test
    void generationCommandsRequireSessionAndCsrf() throws Exception {
        when(service.request(USER, "SKILL_ANALYSIS_EXPLANATION", List.of(MATRIX), "SKILL_EXPLANATION", "key"))
            .thenReturn(job());
        when(service.getJob(USER, JOB)).thenReturn(job());
        String body = "{\"taskType\":\"SKILL_ANALYSIS_EXPLANATION\",\"sourceResourceRefs\":[\"" + MATRIX
            + "\"],\"outputType\":\"SKILL_EXPLANATION\"}";

        mockMvc.perform(post("/api/v1/generation-requests").with(oauth2Login().oauth2User(principal()))
            .contentType(MediaType.APPLICATION_JSON).content(body).header("Idempotency-Key", "key"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/generation-requests").with(oauth2Login().oauth2User(principal())).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body).header("Idempotency-Key", "key"))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.data.jobId").value(JOB.toString()));
        mockMvc.perform(get("/api/v1/generation-jobs/{id}", JOB).with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk());
        verify(service).getJob(USER, JOB);
    }

    private GenerationJobView job() {
        return new GenerationJobView(JOB, "QUEUED", "PENDING", null, null,
            Instant.parse("2026-08-31T00:00:00Z"), null);
    }

    private DevPathOAuth2User principal() {
        return new DevPathOAuth2User(new AuthenticatedUser(USER, "Developer", null, AccountStatus.ACTIVE,
            OAuthProvider.GITHUB, Instant.now()), Map.of("id", "subject", "login", "owner"));
    }
}
