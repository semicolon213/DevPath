package com.devpath.knowledge.adapter.out.persistence;

import com.devpath.knowledge.domain.KnowledgeDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_documents")
class KnowledgeDocumentJpaEntity {
    @Id @Column(name = "knowledge_document_id") private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "source_type", nullable = false, updatable = false) private String sourceType;
    @Column(name = "source_connection_id", nullable = false, updatable = false) private UUID connectionId;
    @Column(name = "source_object_id", nullable = false, updatable = false) private String sourceObjectId;
    @Column(name = "title", nullable = false) private String title;
    @Column(name = "lifecycle_status", nullable = false) private String status;
    @Column(name = "current_version_id") private UUID currentVersionId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(name = "version", nullable = false) private long version;

    protected KnowledgeDocumentJpaEntity() {}
    KnowledgeDocumentJpaEntity(UUID id, UUID userId, UUID connectionId, String sourceObjectId,
        String title, Instant now) {
        this.id=id; this.userId=userId; this.sourceType="NOTION"; this.connectionId=connectionId;
        this.sourceObjectId=sourceObjectId; this.title=title; this.status="ACTIVE"; this.createdAt=now; this.updatedAt=now;
    }
    void activate(String newTitle, UUID versionId, Instant now) { title=newTitle; currentVersionId=versionId; status="ACTIVE"; updatedAt=now; }
    void archive(Instant now) { status="ARCHIVED"; updatedAt=now; }
    UUID id() { return id; }
    UUID currentVersionId() { return currentVersionId; }
    KnowledgeDocument toDomain() { return new KnowledgeDocument(id,userId,connectionId,sourceObjectId,title,status,
        currentVersionId,createdAt,updatedAt,version); }
}
