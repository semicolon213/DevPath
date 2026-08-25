package com.devpath.recommendation.adapter.in.web;

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
import com.devpath.learning.adapter.in.web.LearningRoadmapController;
import com.devpath.learning.application.LearningRoadmapView;
import com.devpath.learning.application.RoadmapMilestoneView;
import com.devpath.learning.application.RoadmapStepView;
import com.devpath.recommendation.application.RecommendationApplicationService;
import com.devpath.recommendation.application.RecommendationSetView;
import com.devpath.recommendation.application.RecommendationView;
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

@WebMvcTest({RecommendationController.class,LearningRoadmapController.class})
@Import({SecurityConfiguration.class,IdentityFoundationConfiguration.class,AbsoluteSessionTimeoutFilter.class,
    NonPersistingOAuth2AuthorizedClientRepository.class,RecommendationApiExceptionHandler.class})
@TestPropertySource(properties={"devpath.security.frontend-origin=http://localhost:5173","devpath.security.session-absolute-timeout=12h","devpath.security.post-login-redirect=/","spring.security.oauth2.client.registration.github.client-id=test-client","spring.security.oauth2.client.registration.github.client-secret=test-secret"})
class RecommendationSecurityTest {
    private static final UUID USER=UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");
    @Autowired MockMvc mockMvc;@MockBean RecommendationApplicationService service;@MockBean GitHubOAuth2UserService oauth;
    @Test void requiresAuthenticationAndReturnsOwnerScopedStructuredResults() throws Exception{
        UUID setId=UUID.randomUUID();UUID roadmapId=UUID.randomUUID();UUID stepId=UUID.randomUUID();Instant now=Instant.parse("2026-08-25T00:00:00Z");
        when(service.getCurrent(USER)).thenReturn(new RecommendationSetView(setId,UUID.randomUUID(),"recommendation-v1","PUBLISHED",List.of(new RecommendationView(UUID.randomUUID(),UUID.randomUUID(),"TESTING","PROJECT","HIGH","CAREER_REQUIRED_GAP","Add testing","Reach 60",List.of("test files"),List.of(UUID.randomUUID()),16,0,"PROPOSED")),now));
        when(service.getActiveRoadmap(USER)).thenReturn(new LearningRoadmapView(roadmapId,setId,"roadmap-v1","CREATED",BigDecimal.ZERO,List.of(new RoadmapMilestoneView(UUID.randomUUID(),0,"TESTING","Add testing","PLANNED")),List.of(new RoadmapStepView(stepId,UUID.randomUUID(),UUID.randomUUID(),0,"TESTING","Add testing","INTERMEDIATE",16,List.of(),"Reach 60",List.of("test files"),"NOT_STARTED")),now,now));
        mockMvc.perform(get("/api/v1/recommendations/current")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/recommendations/current").with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk()).andExpect(jsonPath("$.data.recommendations[0].priority").value("HIGH"));
        mockMvc.perform(get("/api/v1/learning-roadmaps/active").with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk()).andExpect(jsonPath("$.data.steps[0].category").value("TESTING"));
    }
    private DevPathOAuth2User principal(){return new DevPathOAuth2User(new AuthenticatedUser(USER,"User",null,AccountStatus.ACTIVE,OAuthProvider.GITHUB,Instant.parse("2026-07-27T00:00:00Z")),Map.of("id","1","login","owner"));}
}
