package com.devpath.integration.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notion_page_metadata")
class NotionPageMetadataJpaEntity {
    @Id @Column(name = "notion_page_metadata_id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "notion_connection_id", nullable = false, updatable = false)
    private UUID connectionId;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "provider_page_id", nullable = false, updatable = false)
    private String providerPageId;
    @Column(name = "object_type", nullable = false, updatable = false)
    private String objectType;
    @Column(name = "title", nullable = false, length = 512)
    private String title;
    @Column(name = "provider_url", length = 2048)
    private String url;
    @Column(name = "last_edited_at", nullable = false)
    private Instant lastEditedAt;
    @Column(name = "in_trash", nullable = false)
    private boolean inTrash;
    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;

    protected NotionPageMetadataJpaEntity() {}

    NotionPageMetadataJpaEntity(UUID id, UUID connectionId, UUID userId, String providerPageId,
        String objectType, String title, String url, Instant lastEditedAt, boolean inTrash, Instant discoveredAt) {
        this.id = id; this.connectionId = connectionId; this.userId = userId; this.providerPageId = providerPageId;
        this.objectType = objectType; this.title = title; this.url = url; this.lastEditedAt = lastEditedAt;
        this.inTrash = inTrash; this.discoveredAt = discoveredAt;
    }
}
