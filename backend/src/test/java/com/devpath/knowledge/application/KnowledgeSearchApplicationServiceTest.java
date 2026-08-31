package com.devpath.knowledge.application;

import com.devpath.shared.application.ObjectContentPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeSearchApplicationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-30T00:00:00Z");
    @Mock EmbeddingPort embeddings;
    @Mock ObjectContentPort objects;
    @Mock KnowledgeSearchRecordService records;

    @Test
    void returnsBoundedAuthorizedExcerptsAndRetainsOnlyTheQueryHash() {
        UUID userId=UUID.randomUUID(),documentId=UUID.randomUUID(),chunkId=UUID.randomUUID();
        var vector=new EmbeddingVector("OLLAMA","model","v1",3,List.of(0.1d,0.2d,0.3d));
        var filters=new KnowledgeSearchFilters(List.of("NOTION"),List.of(documentId));
        when(embeddings.embed("testing strategy")).thenReturn(vector);
        var candidate=new KnowledgeSearchCandidate(
            chunkId,documentId,"Testing notes","page-1","https://www.notion.so/page-1","Tests",
            "object://chunk",120,0.9d,NOW);
        when(records.retrieve(userId,vector,filters,3,0.25)).thenReturn(List.of(candidate));
        when(records.retainAuthorized(userId,List.of(candidate))).thenReturn(List.of(candidate));
        when(objects.read(userId,"object://chunk")).thenReturn("x".repeat(700));
        var service=new KnowledgeSearchApplicationService(embeddings,objects,records,
            Clock.fixed(NOW,ZoneOffset.UTC),0.25);

        KnowledgeSearchView result=service.search(userId,"  testing   strategy ",filters,3,"USER_SEARCH");

        assertThat(result.results()).singleElement().satisfies(item -> {
            assertThat(item.excerpt()).hasSize(500);
            assertThat(item.relevance()).isEqualTo(0.9d);
            assertThat(item.sourceObjectId()).isEqualTo("page-1");
        });
        ArgumentCaptor<KnowledgeRetrievalRecord> record=ArgumentCaptor.forClass(KnowledgeRetrievalRecord.class);
        verify(records).record(record.capture());
        assertThat(record.getValue().queryHash()).hasSize(64).doesNotContain("testing");
        assertThat(record.getValue().items()).singleElement().satisfies(item -> assertThat(item.chunkId()).isEqualTo(chunkId));
    }

    @Test
    void normalizesProviderFailureWithoutExposingProviderDetails() {
        UUID userId=UUID.randomUUID();
        when(embeddings.embed("query")).thenThrow(new IllegalStateException("provider raw error"));
        var service=new KnowledgeSearchApplicationService(embeddings,objects,records,
            Clock.fixed(NOW,ZoneOffset.UTC),0.25);

        assertThatThrownBy(() -> service.search(userId,"query",null,null,null))
            .isInstanceOf(KnowledgeRetrievalUnavailableException.class)
            .hasMessageNotContaining("provider raw error");
    }
}
