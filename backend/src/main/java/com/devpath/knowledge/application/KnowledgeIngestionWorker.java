package com.devpath.knowledge.application;

import com.devpath.integration.application.NotionConnectionNotFoundException;
import com.devpath.integration.application.NotionConnectionPort;
import com.devpath.integration.application.NotionIntegrationUnavailableException;
import com.devpath.integration.application.NotionRateLimitExceededException;
import com.devpath.knowledge.domain.KnowledgeIngestionJob;
import com.devpath.shared.infrastructure.WorkerShutdownGate;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "devpath.runtime.worker-enabled", havingValue = "true")
class KnowledgeIngestionWorker {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionWorker.class);
    private final KnowledgeApplicationService service;
    private final NotionConnectionPort notion;
    private final ObjectContentPort objects;
    private final EmbeddingPort embeddings;
    private final Clock clock;
    private final WorkerShutdownGate shutdownGate;
    private final KnowledgeChunker chunker = new KnowledgeChunker();

    KnowledgeIngestionWorker(KnowledgeApplicationService service, NotionConnectionPort notion,
        ObjectContentPort objects, EmbeddingPort embeddings, Clock clock, WorkerShutdownGate shutdownGate) {
        this.service = service; this.notion = notion; this.objects = objects; this.embeddings = embeddings;
        this.clock = clock; this.shutdownGate = shutdownGate;
    }

    @Scheduled(fixedDelayString = "${devpath.jobs.knowledge-ingestion.poll-interval:1000}",
        initialDelayString = "${devpath.jobs.knowledge-ingestion.initial-delay:2000}",
        scheduler = "workerTaskScheduler")
    void processNext() {
        if (!shutdownGate.acceptingClaims()) return;
        service.claim(clock.instant()).ifPresent(this::process);
    }

    private void process(KnowledgeIngestionJob job) {
        try {
            var source = notion.collectPage(job.userId(), job.sourceObjectId(), clock.instant());
            if (!source.connectionId().equals(job.connectionId())) throw new NotionConnectionNotFoundException();
            String contentHash = KnowledgeChunker.hash(source.normalizedContent());
            UUID versionId = deterministicId(job.id() + ":" + contentHash);
            var drafts = chunker.chunk(source.normalizedContent());
            String sourceReference = objects.put(job.userId(), job.documentId(), versionId, "source.md", source.normalizedContent());
            var preparedChunks = new ArrayList<PreparedKnowledgeChunk>();
            for (var draft : drafts) {
                UUID chunkId = deterministicId(versionId + ":" + draft.position() + ":" + draft.contentHash());
                String reference = objects.put(job.userId(), job.documentId(), versionId,
                    "chunks/" + draft.position() + ".md", draft.content());
                EmbeddingVector embedding = embeddings.embed(draft.content());
                preparedChunks.add(new PreparedKnowledgeChunk(chunkId, draft.position(), draft.heading(), reference,
                    draft.contentHash(), draft.tokenEstimate(), embedding));
            }
            var document = new PreparedKnowledgeDocument(job.documentId(), versionId, source.title(),
                source.providerPageId(), source.sourceUpdatedAt(), contentHash, sourceReference, preparedChunks);
            service.complete(job, document, clock.instant());
            log.info("Knowledge ingestion completed job_id={} document_id={} chunk_count={}",
                job.id(), job.documentId(), preparedChunks.size());
        } catch (NotionRateLimitExceededException exception) {
            service.fail(job, "RATE_LIMIT_EXCEEDED", "Notion request limit was reached; ingestion will retry.", clock.instant());
        } catch (NotionConnectionNotFoundException exception) {
            service.fail(job, "SOURCE_PERMISSION_CHANGED", "Notion page access is no longer available.", clock.instant());
        } catch (NotionIntegrationUnavailableException exception) {
            service.fail(job, "SOURCE_UNAVAILABLE", "Notion content is temporarily unavailable.", clock.instant());
        } catch (RuntimeException exception) {
            service.fail(job, "INGESTION_FAILED", "Knowledge ingestion failed safely.", clock.instant());
            log.warn("Knowledge ingestion attempt failed job_id={} error_type={}", job.id(), exception.getClass().getSimpleName());
        }
    }

    private static UUID deterministicId(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }
}
