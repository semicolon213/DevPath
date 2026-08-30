package com.devpath.knowledge.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_document_versions")
class KnowledgeDocumentVersionJpaEntity {
    @Id @Column(name = "knowledge_document_version_id") private UUID id;
    @Column(name = "knowledge_document_id", nullable = false, updatable = false) private UUID documentId;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "version_number", nullable = false, updatable = false) private int versionNumber;
    @Column(name = "content_hash", nullable = false, updatable = false) private String contentHash;
    @Column(name = "object_reference", nullable = false, updatable = false) private String objectReference;
    @Column(name = "source_updated_at", nullable = false, updatable = false) private Instant sourceUpdatedAt;
    @Column(name = "version_status", nullable = false) private String status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected KnowledgeDocumentVersionJpaEntity() {}
    KnowledgeDocumentVersionJpaEntity(UUID id, UUID documentId, UUID userId, int versionNumber, String contentHash,
        String objectReference, Instant sourceUpdatedAt, Instant createdAt) {
        this.id=id; this.documentId=documentId; this.userId=userId; this.versionNumber=versionNumber; this.contentHash=contentHash;
        this.objectReference=objectReference; this.sourceUpdatedAt=sourceUpdatedAt; this.status="INDEXED"; this.createdAt=createdAt;
    }
    void supersede() { status="SUPERSEDED"; }
}
