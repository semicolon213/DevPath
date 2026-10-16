package com.devpath.knowledge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devpath.integration.application.NotionConnectionNotFoundException;
import com.devpath.integration.application.NotionConnectionPort;
import com.devpath.integration.application.NotionIntegrationUnavailableException;
import com.devpath.integration.application.NotionPageContentView;
import com.devpath.integration.application.NotionRateLimitExceededException;
import com.devpath.knowledge.domain.KnowledgeIngestionJob;
import com.devpath.shared.application.ObjectContentPort;
import com.devpath.shared.infrastructure.WorkerShutdownGate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KnowledgeIngestionWorkerTest {
    private static final Instant NOW = Instant.parse("2026-08-27T00:00:00Z");

    @Test
    void completesIngestion() {
        Fixture fixture = fixture();
        when(fixture.notion.collectPage(fixture.job.userId(), fixture.job.sourceObjectId(), NOW))
            .thenReturn(new NotionPageContentView(fixture.job.connectionId(), fixture.job.sourceObjectId(),
                "Page", NOW, "Knowledge content"));
        when(fixture.objects.put(any(), any(), any(), any(), any())).thenReturn("object://content");
        when(fixture.embeddings.embed("Knowledge content"))
            .thenReturn(new EmbeddingVector("TEST", "test", "v1", 1, List.of(1.0)));

        fixture.worker.processNext();

        var document = ArgumentCaptor.forClass(PreparedKnowledgeDocument.class);
        verify(fixture.service).complete(eq(fixture.job), document.capture(), eq(NOW));
        assertThat(document.getValue().chunks()).hasSize(1);
    }

    @Test
    void mapsRateLimitFailure() {
        assertFailure(new NotionRateLimitExceededException(30L), "RATE_LIMIT_EXCEEDED",
            "Notion request limit was reached; ingestion will retry.");
    }

    @Test
    void mapsMissingConnectionToPermissionChange() {
        assertFailure(new NotionConnectionNotFoundException(), "SOURCE_PERMISSION_CHANGED",
            "Notion page access is no longer available.");
    }

    @Test
    void mapsUnavailableIntegration() {
        assertFailure(new NotionIntegrationUnavailableException("unavailable"), "SOURCE_UNAVAILABLE",
            "Notion content is temporarily unavailable.");
    }

    @Test
    void mapsUnexpectedFailure() {
        assertFailure(new IllegalStateException("unexpected"), "INGESTION_FAILED",
            "Knowledge ingestion failed safely.");
    }

    private static void assertFailure(RuntimeException failure, String code, String message) {
        Fixture fixture = fixture();
        when(fixture.notion.collectPage(fixture.job.userId(), fixture.job.sourceObjectId(), NOW)).thenThrow(failure);

        fixture.worker.processNext();

        verify(fixture.service).fail(fixture.job, code, message, NOW);
    }

    private static Fixture fixture() {
        var service = mock(KnowledgeApplicationService.class);
        var notion = mock(NotionConnectionPort.class);
        var objects = mock(ObjectContentPort.class);
        var embeddings = mock(EmbeddingPort.class);
        var job = KnowledgeIngestionJob.queue(
            UUID.randomUUID(), UUID.randomUUID(), "page-id", UUID.randomUUID(), "key", NOW);
        when(service.claim(NOW)).thenReturn(Optional.of(job));
        var worker = new KnowledgeIngestionWorker(service, notion, objects, embeddings,
            Clock.fixed(NOW, ZoneOffset.UTC), new WorkerShutdownGate());
        return new Fixture(service, notion, objects, embeddings, job, worker);
    }

    private record Fixture(
        KnowledgeApplicationService service,
        NotionConnectionPort notion,
        ObjectContentPort objects,
        EmbeddingPort embeddings,
        KnowledgeIngestionJob job,
        KnowledgeIngestionWorker worker
    ) {}
}
