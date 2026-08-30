package com.devpath.knowledge.application;

import com.devpath.knowledge.domain.KnowledgeDocument;
import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocumentView(
    UUID documentId, String sourceType, String sourceObjectId, String title, String status,
    UUID currentVersionId, int chunkCount, Instant createdAt, Instant updatedAt
) {
    static KnowledgeDocumentView from(KnowledgeDocument document, int chunkCount) {
        return new KnowledgeDocumentView(document.id(), "NOTION", document.sourceObjectId(), document.title(),
            document.status(), document.currentVersionId(), chunkCount, document.createdAt(), document.updatedAt());
    }
}
