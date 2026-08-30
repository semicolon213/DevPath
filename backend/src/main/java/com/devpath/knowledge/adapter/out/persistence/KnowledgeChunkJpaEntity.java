package com.devpath.knowledge.adapter.out.persistence;

import com.devpath.knowledge.domain.KnowledgeChunk;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_chunks")
class KnowledgeChunkJpaEntity {
    @Id @Column(name = "knowledge_chunk_id") private UUID id;
    @Column(name = "knowledge_document_version_id", nullable = false, updatable = false) private UUID documentVersionId;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "chunk_position", nullable = false, updatable = false) private int position;
    @Column(name = "heading") private String heading;
    @Column(name = "object_reference", nullable = false, updatable = false) private String objectReference;
    @Column(name = "content_hash", nullable = false, updatable = false) private String contentHash;
    @Column(name = "token_estimate", nullable = false, updatable = false) private int tokenEstimate;
    @Column(name = "index_status", nullable = false) private String status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected KnowledgeChunkJpaEntity() {}
    KnowledgeChunkJpaEntity(UUID id, UUID versionId, UUID userId, int position, String heading,
        String objectReference, String contentHash, int tokenEstimate, Instant createdAt) {
        this.id=id; this.documentVersionId=versionId; this.userId=userId; this.position=position; this.heading=heading;
        this.objectReference=objectReference; this.contentHash=contentHash; this.tokenEstimate=tokenEstimate;
        this.status="INDEXED"; this.createdAt=createdAt;
    }
    void stale() { status="STALE"; }
    KnowledgeChunk toDomain() { return new KnowledgeChunk(id,documentVersionId,userId,position,heading,objectReference,
        contentHash,tokenEstimate,status,createdAt); }
}
