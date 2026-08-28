package com.devpath.recommendation.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import com.devpath.learning.adapter.in.web.LearningRoadmapController;
import com.devpath.learning.application.LearningRoadmapView;
import com.devpath.learning.application.LearningRoadmapListView;
import com.devpath.learning.application.RoadmapMilestoneView;
import com.devpath.learning.application.RoadmapStepView;
import com.devpath.recommendation.application.RecommendationApplicationService;
import com.devpath.recommendation.application.RecommendationSetView;
import com.devpath.recommendation.application.RecommendationView;
import com.devpath.recommendation.application.RecommendationSetListView;
import com.devpath.recommendation.application.RecommendationEvidenceListView;
import com.devpath.recommendation.application.RecommendationEvidenceView;
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
import org.springframework.http.MediaType;

@WebMvcTest({RecommendationController.class,LearningRoadmapController.class})
@Import({SecurityConfiguration.class,IdentityFoundationConfiguration.class,AbsoluteSessionTimeoutFilter.class,
    NonPersistingOAuth2AuthorizedClientRepository.class,RecommendationApiExceptionHandler.class})
@TestPropertySource(properties={"devpath.security.frontend-origin=http://localhost:5173","devpath.security.session-absolute-timeout=12h","devpath.security.post-login-redirect=/","spring.security.oauth2.client.registration.github.client-id=test-client","spring.security.oauth2.client.registration.github.client-secret=test-secret"})
class RecommendationSecurityTest {
    private static final UUID USER=UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");
    @Autowired MockMvc mockMvc;@MockBean RecommendationApplicationService service;@MockBean GitHubOAuth2UserService oauth;
    @MockBean com.devpath.identity.application.AuthenticationAuditPort authenticationAuditPort;
    @Test void requiresAuthenticationAndReturnsOwnerScopedStructuredResults() throws Exception{
        UUID setId=UUID.randomUUID();UUID roadmapId=UUID.randomUUID();UUID stepId=UUID.randomUUID();Instant now=Instant.parse("2026-08-25T00:00:00Z");
        UUID recommendationId=UUID.randomUUID();UUID evidenceId=UUID.randomUUID();
        RecommendationView recommendation=new RecommendationView(recommendationId,UUID.randomUUID(),"TESTING","PROJECT","HIGH","CAREER_REQUIRED_GAP","Add testing","Reach 60",List.of("test files"),List.of(evidenceId),16,0,"PROPOSED");
        RecommendationSetView recommendationSet=new RecommendationSetView(setId,UUID.randomUUID(),"recommendation-v1","PUBLISHED",List.of(recommendation),now);
        when(service.getCurrent(USER)).thenReturn(recommendationSet);
        when(service.listRecommendationSets(USER)).thenReturn(new RecommendationSetListView(List.of(recommendationSet)));
        when(service.getRecommendation(USER,recommendationId)).thenReturn(recommendation);
        when(service.getRecommendationEvidence(USER,recommendationId)).thenReturn(new RecommendationEvidenceListView(recommendationId,List.of(new RecommendationEvidenceView(evidenceId,"SNAPSHOT_SIGNAL","README.md","README is present",BigDecimal.valueOf(100),now))));
        LearningRoadmapView roadmap=new LearningRoadmapView(roadmapId,setId,"roadmap-v1","CREATED",BigDecimal.ZERO,List.of(new RoadmapMilestoneView(UUID.randomUUID(),0,"TESTING","Add testing","PLANNED")),List.of(new RoadmapStepView(stepId,UUID.randomUUID(),UUID.randomUUID(),0,"TESTING","Add testing","INTERMEDIATE",16,List.of(),"Reach 60",List.of("test files"),"NOT_STARTED")),now,now);
        when(service.getActiveRoadmap(USER)).thenReturn(roadmap);
        when(service.listRoadmaps(USER)).thenReturn(new LearningRoadmapListView(List.of(roadmap)));
        when(service.archiveRoadmap(USER,roadmapId)).thenReturn(new LearningRoadmapView(roadmapId,setId,"roadmap-v1","ARCHIVED",BigDecimal.ZERO,roadmap.milestones(),roadmap.steps(),now,now));
        mockMvc.perform(get("/api/v1/recommendations/current")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/recommendations/current").with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk()).andExpect(jsonPath("$.data.recommendations[0].priority").value("HIGH"));
        mockMvc.perform(get("/api/v1/recommendations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/recommendations").with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk()).andExpect(jsonPath("$.data.recommendationSets[0].recommendationSetId").value(setId.toString()));
        mockMvc.perform(get("/api/v1/recommendations/{recommendationId}",recommendationId).with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("Add testing"));
        mockMvc.perform(get("/api/v1/recommendations/{recommendationId}/evidence",recommendationId).with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk()).andExpect(jsonPath("$.data.evidence[0].sourceReference").value("README.md"));
        mockMvc.perform(get("/api/v1/learning-roadmaps/active").with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk()).andExpect(jsonPath("$.data.steps[0].category").value("TESTING"));
        mockMvc.perform(get("/api/v1/learning-roadmaps")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/learning-roadmaps").with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk()).andExpect(jsonPath("$.data.roadmaps[0].roadmapId").value(roadmapId.toString()));
        mockMvc.perform(post("/api/v1/learning-roadmaps/{roadmapId}/archive",roadmapId).header("Idempotency-Key","archive-1").contentType(MediaType.APPLICATION_JSON).content("{}").with(oauth2Login().oauth2User(principal()))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/learning-roadmaps/{roadmapId}/archive",roadmapId).header("Idempotency-Key","archive-1").with(csrf()).with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ARCHIVED"));
        mockMvc.perform(post("/api/v1/learning-roadmaps/{roadmapId}/archive",roadmapId).header("Idempotency-Key","archive-2").contentType(MediaType.APPLICATION_JSON).content("{}").with(csrf()).with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk());
        verify(service,org.mockito.Mockito.times(2)).archiveRoadmap(USER,roadmapId);
    }
    private DevPathOAuth2User principal(){return new DevPathOAuth2User(new AuthenticatedUser(USER,"User",null,AccountStatus.ACTIVE,OAuthProvider.GITHUB,Instant.parse("2026-07-27T00:00:00Z")),Map.of("id","1","login","owner"));}
}
