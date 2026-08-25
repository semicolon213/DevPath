package com.devpath.career.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devpath.career.application.CareerCatalogApplicationService;
import com.devpath.career.application.CareerCatalogView;
import com.devpath.career.application.CareerProfileView;
import com.devpath.career.application.CareerSummaryView;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CareerCatalogController.class)
@Import({SecurityConfiguration.class, IdentityFoundationConfiguration.class,
    AbsoluteSessionTimeoutFilter.class, NonPersistingOAuth2AuthorizedClientRepository.class})
@TestPropertySource(properties = {
    "devpath.security.frontend-origin=http://localhost:5173",
    "devpath.security.session-absolute-timeout=12h",
    "devpath.security.post-login-redirect=/",
    "spring.security.oauth2.client.registration.github.client-id=test-client",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class CareerCatalogSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockBean CareerCatalogApplicationService service;
    @MockBean GitHubOAuth2UserService oAuth2UserService;

    @Test
    void requiresAuthenticationAndReturnsOnlyProfileMetadata() throws Exception {
        when(service.list()).thenReturn(new CareerCatalogView("career-v1", List.of(
            new CareerSummaryView("backend", "Backend Engineer", "백엔드 엔지니어", "SUPPORTED", "career-v1", "API 구현")
        )));
        when(service.get("backend")).thenReturn(new CareerProfileView("backend", "Backend Engineer", "백엔드 엔지니어",
            "SUPPORTED", UUID.randomUUID(), "career-v1", "API 구현", List.of("Java"), List.of("테스트"),
            List.of("CI/CD"), List.of("TESTING"), Map.of("TESTING", "HIGH"), List.of("테스트"),
            Instant.parse("2026-08-12T00:00:00Z")));

        mockMvc.perform(get("/api/v1/careers")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/careers").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.careers[0].careerId").value("backend"));
        mockMvc.perform(get("/api/v1/careers/backend").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.requiredCompetencies[0]").value("테스트"));
    }

    private DevPathOAuth2User principal() {
        var user = new AuthenticatedUser(UUID.randomUUID(), "DevPath User", null, AccountStatus.ACTIVE,
            OAuthProvider.GITHUB, Instant.parse("2026-07-27T00:00:00Z"));
        return new DevPathOAuth2User(user, Map.of("id", "1849102", "login", "owner"));
    }
}
