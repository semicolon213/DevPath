package com.devpath.knowledge.adapter.in.web;

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
import com.devpath.knowledge.application.KnowledgeApplicationService;
import com.devpath.knowledge.application.KnowledgeChunkSummaryListView;
import com.devpath.knowledge.application.KnowledgeDocumentListView;
import com.devpath.knowledge.application.KnowledgeDocumentView;
import com.devpath.knowledge.application.KnowledgeIngestionJobView;
import com.devpath.knowledge.application.KnowledgeSearchApplicationService;
import com.devpath.knowledge.application.KnowledgeSearchFilters;
import com.devpath.knowledge.application.KnowledgeSearchView;
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

@WebMvcTest(KnowledgeController.class)
@Import({SecurityConfiguration.class,IdentityFoundationConfiguration.class,AbsoluteSessionTimeoutFilter.class,
    NonPersistingOAuth2AuthorizedClientRepository.class})
@TestPropertySource(properties={"devpath.security.frontend-origin=http://localhost:5173","devpath.security.session-absolute-timeout=12h",
    "devpath.security.post-login-redirect=/","spring.security.oauth2.client.registration.github.client-id=test",
    "spring.security.oauth2.client.registration.github.client-secret=test"})
class KnowledgeSecurityTest {
    static final UUID USER=UUID.fromString("e046a279-9c82-4bbf-9d8f-0737b222fa97");
    static final UUID CONNECTION=UUID.randomUUID(),JOB=UUID.randomUUID(),DOCUMENT=UUID.randomUUID();
    @Autowired MockMvc mockMvc;
    @MockBean KnowledgeApplicationService service;
    @MockBean KnowledgeSearchApplicationService search;
    @MockBean GitHubOAuth2UserService oAuth2UserService;
    @MockBean com.devpath.identity.application.AuthenticationAuditPort authenticationAuditPort;

    @Test void commandsRequireSessionAndCsrfAndReadsRemainOwnerScoped() throws Exception {
        when(service.importNotion(USER,CONNECTION,"page-1","key")).thenReturn(job());
        when(service.getJob(USER,JOB)).thenReturn(job());
        when(service.list(USER)).thenReturn(new KnowledgeDocumentListView(List.of(document())));
        when(service.get(USER,DOCUMENT)).thenReturn(document());
        when(service.chunks(USER,DOCUMENT)).thenReturn(new KnowledgeChunkSummaryListView(List.of()));
        when(service.archive(USER,DOCUMENT)).thenReturn(document());
        when(service.reindex(USER,DOCUMENT,"reindex-key")).thenReturn(job());
        var filters=new KnowledgeSearchFilters(List.of("NOTION"),List.of(DOCUMENT));
        when(search.search(USER,"testing",filters,5,"USER_SEARCH")).thenReturn(new KnowledgeSearchView(
            UUID.randomUUID(),"SEMANTIC","knowledge-semantic-v1","USER_SEARCH",filters,List.of(),0,2,Instant.now()));
        String body="{\"connectionId\":\""+CONNECTION+"\",\"providerPageId\":\"page-1\"}";
        mockMvc.perform(post("/api/v1/knowledge-documents/imports/notion").contentType(MediaType.APPLICATION_JSON).content(body).header("Idempotency-Key","key")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/knowledge-documents/imports/notion").with(oauth2Login().oauth2User(principal())).contentType(MediaType.APPLICATION_JSON).content(body).header("Idempotency-Key","key")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/knowledge-documents/imports/notion").with(oauth2Login().oauth2User(principal())).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body).header("Idempotency-Key","key"))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.data.jobType").value("KNOWLEDGE_INGESTION"));
        mockMvc.perform(get("/api/v1/knowledge-ingestion-jobs/{id}",JOB).with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.jobId").value(JOB.toString()));
        mockMvc.perform(get("/api/v1/knowledge-documents/{id}",DOCUMENT).with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/knowledge-documents/{id}/archive",DOCUMENT).with(oauth2Login().oauth2User(principal()))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/knowledge-documents/{id}/archive",DOCUMENT).with(oauth2Login().oauth2User(principal())).with(csrf())).andExpect(status().isOk());
        String searchBody="{\"query\":\"testing\",\"filters\":{\"sourceTypes\":[\"NOTION\"],\"documentIds\":[\""+DOCUMENT+"\"]},\"limit\":5,\"contextPurpose\":\"USER_SEARCH\"}";
        mockMvc.perform(post("/api/v1/knowledge-search").with(oauth2Login().oauth2User(principal())).contentType(MediaType.APPLICATION_JSON).content(searchBody)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/knowledge-search").with(oauth2Login().oauth2User(principal())).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(searchBody))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.policyVersion").value("knowledge-semantic-v1"));
        verify(search).search(USER,"testing",filters,5,"USER_SEARCH");
        verify(service).get(USER,DOCUMENT); verify(service).archive(USER,DOCUMENT);
    }
    private KnowledgeIngestionJobView job() { Instant now=Instant.parse("2026-08-30T00:00:00Z"); return new KnowledgeIngestionJobView(JOB,"KNOWLEDGE_INGESTION","queued","QUEUED",0,0,3,now,null,null,"/api/v1/knowledge-ingestion-jobs/"+JOB,null,null,null,true); }
    private KnowledgeDocumentView document() { Instant now=Instant.parse("2026-08-30T00:00:00Z"); return new KnowledgeDocumentView(DOCUMENT,"NOTION","page-1","Page","ACTIVE",UUID.randomUUID(),1,now,now); }
    private DevPathOAuth2User principal() { return new DevPathOAuth2User(new AuthenticatedUser(USER,"Developer",null,AccountStatus.ACTIVE,OAuthProvider.GITHUB,Instant.now()),Map.of("id","subject","login","owner")); }
}
