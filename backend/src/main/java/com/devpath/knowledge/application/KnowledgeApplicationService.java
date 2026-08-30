package com.devpath.knowledge.application;

import com.devpath.integration.application.NotionConnectionPort;
import com.devpath.knowledge.domain.KnowledgeDocument;
import com.devpath.knowledge.domain.KnowledgeIngestionJob;
import com.devpath.knowledge.domain.KnowledgeIngestionStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeApplicationService {
    private final KnowledgePersistencePort persistence;
    private final NotionConnectionPort notion;
    private final KnowledgeAuditPort audit;
    private final Clock clock;

    public KnowledgeApplicationService(KnowledgePersistencePort persistence, NotionConnectionPort notion,
        KnowledgeAuditPort audit, Clock clock) {
        this.persistence = persistence; this.notion = notion; this.audit = audit; this.clock = clock;
    }

    @Transactional
    public KnowledgeIngestionJobView importNotion(UUID userId, UUID connectionId, String providerPageId, String key) {
        validateKey(key);
        Optional<KnowledgeIngestionJob> repeated = persistence.findJobByOwnerAndKey(userId, key);
        if (repeated.isPresent()) {
            KnowledgeIngestionJob job = repeated.get();
            if (!job.connectionId().equals(connectionId) || !job.sourceObjectId().equals(providerPageId)) {
                throw new IllegalArgumentException("Idempotency key belongs to another knowledge import");
            }
            return KnowledgeIngestionJobView.from(job);
        }
        notion.verifyPageAccess(userId, connectionId, providerPageId);
        Optional<KnowledgeIngestionJob> active = persistence.findActiveJob(userId, providerPageId);
        if (active.isPresent()) return KnowledgeIngestionJobView.from(active.get());
        UUID documentId = persistence.findDocumentByOwnerAndSource(userId, providerPageId)
            .map(KnowledgeDocument::id).orElseGet(UUID::randomUUID);
        KnowledgeIngestionJob job = persistence.saveJob(KnowledgeIngestionJob.queue(
            userId, connectionId, providerPageId, documentId, key, clock.instant()));
        audit.record(KnowledgeAuditEvent.INGESTION_REQUESTED, userId, job.id(), clock.instant());
        return KnowledgeIngestionJobView.from(job);
    }

    @Transactional(readOnly = true)
    public KnowledgeIngestionJobView getJob(UUID userId, UUID jobId) {
        return persistence.findJobByIdAndOwner(jobId, userId).map(KnowledgeIngestionJobView::from)
            .orElseThrow(KnowledgeNotFoundException::new);
    }

    @Transactional
    public KnowledgeDocumentListView list(UUID userId) {
        var documents = persistence.findDocumentsByOwner(userId).stream()
            .map(document -> KnowledgeDocumentView.from(document, persistence.countCurrentChunks(document.id(), userId)))
            .toList();
        return new KnowledgeDocumentListView(documents);
    }

    @Transactional
    public KnowledgeDocumentView get(UUID userId, UUID documentId) {
        KnowledgeDocument document = owned(userId, documentId);
        audit.record(KnowledgeAuditEvent.DOCUMENT_VIEWED, userId, documentId, clock.instant());
        return KnowledgeDocumentView.from(document, persistence.countCurrentChunks(documentId, userId));
    }

    @Transactional(readOnly = true)
    public KnowledgeChunkSummaryListView chunks(UUID userId, UUID documentId) {
        owned(userId, documentId);
        return new KnowledgeChunkSummaryListView(persistence.findCurrentChunks(documentId, userId).stream()
            .map(KnowledgeChunkSummaryView::from).toList());
    }

    @Transactional
    public KnowledgeDocumentView archive(UUID userId, UUID documentId) {
        KnowledgeDocument archived = persistence.archive(owned(userId, documentId), clock.instant());
        audit.record(KnowledgeAuditEvent.DOCUMENT_ARCHIVED, userId, documentId, clock.instant());
        return KnowledgeDocumentView.from(archived, persistence.countCurrentChunks(documentId, userId));
    }

    @Transactional
    public KnowledgeIngestionJobView reindex(UUID userId, UUID documentId, String key) {
        KnowledgeDocument document = owned(userId, documentId);
        KnowledgeIngestionJobView job = importNotion(userId, document.connectionId(), document.sourceObjectId(), key);
        audit.record(KnowledgeAuditEvent.DOCUMENT_REINDEX_REQUESTED, userId, documentId, clock.instant());
        return job;
    }

    @Transactional
    Optional<KnowledgeIngestionJob> claim(Instant now) {
        Optional<KnowledgeIngestionJob> candidate = persistence.findNextClaimable(now);
        if (candidate.isEmpty()) return Optional.empty();
        KnowledgeIngestionJob claimed = persistence.saveJob(candidate.get().claim(now));
        if (claimed.status() == KnowledgeIngestionStatus.FAILED) {
            audit.record(KnowledgeAuditEvent.INGESTION_FAILED, claimed.userId(), claimed.id(), now);
            return Optional.empty();
        }
        return Optional.of(claimed);
    }

    @Transactional
    void complete(KnowledgeIngestionJob claimed, PreparedKnowledgeDocument document, Instant now) {
        KnowledgeIngestionJob current = persistence.findJobByIdAndOwner(claimed.id(), claimed.userId())
            .orElseThrow(KnowledgeNotFoundException::new);
        if (current.status() != KnowledgeIngestionStatus.RUNNING) return;
        persistence.complete(current, document, now);
        persistence.saveJob(current.succeed(now));
        audit.record(KnowledgeAuditEvent.INGESTION_COMPLETED, current.userId(), current.id(), now);
    }

    @Transactional
    void fail(KnowledgeIngestionJob claimed, String code, String safeMessage, Instant now) {
        KnowledgeIngestionJob current = persistence.findJobByIdAndOwner(claimed.id(), claimed.userId())
            .orElseThrow(KnowledgeNotFoundException::new);
        KnowledgeIngestionJob failed = persistence.saveJob(current.failOrRetry(code, safeMessage, now));
        if (failed.status() == KnowledgeIngestionStatus.FAILED) {
            audit.record(KnowledgeAuditEvent.INGESTION_FAILED, failed.userId(), failed.id(), now);
        }
    }

    private KnowledgeDocument owned(UUID userId, UUID documentId) {
        return persistence.findDocumentByIdAndOwner(documentId, userId).orElseThrow(KnowledgeNotFoundException::new);
    }

    private static void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128) throw new IllegalArgumentException("Idempotency key is invalid");
    }
}
