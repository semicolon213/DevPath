package com.devpath.repository.adapter.out.persistence;

import com.devpath.repository.domain.Repository;
import com.devpath.repository.domain.RepositoryLifecycle;
import com.devpath.repository.domain.RepositorySyncStatus;
import com.devpath.repository.domain.RepositoryVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "repositories")
class RepositoryJpaEntity {
    @Id @Column(name = "repository_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "external_identity_id", nullable = false, updatable = false) private UUID externalIdentityId;
    @Column(name = "provider", nullable = false, updatable = false, length = 32) private String provider;
    @Column(name = "provider_repository_id", nullable = false, updatable = false, length = 64) private String providerRepositoryId;
    @Column(name = "repository_name", nullable = false, length = 100) private String name;
    @Column(name = "full_name", nullable = false, length = 255) private String fullName;
    @Column(name = "owner_login", nullable = false, length = 255) private String owner;
    @Column(name = "visibility", nullable = false, length = 16) private String visibility;
    @Column(name = "default_branch", nullable = false, length = 255) private String defaultBranch;
    @Column(name = "provider_archived", nullable = false) private boolean providerArchived;
    @Column(name = "lifecycle_status", nullable = false, length = 32) private String lifecycle;
    @Column(name = "sync_status", nullable = false, length = 32) private String syncStatus;
    @Column(name = "html_url", nullable = false, length = 2048) private String htmlUrl;
    @Column(name = "discovered_at", nullable = false, updatable = false) private Instant discoveredAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "last_synced_at") private Instant lastSyncedAt;
    @Column(name = "current_snapshot_id") private UUID currentSnapshotId;
    @Version @Column(name = "version", nullable = false) private long version;

    protected RepositoryJpaEntity() {
    }

    RepositoryJpaEntity(Repository repository) {
        this.id = repository.id();
        this.userId = repository.userId();
        this.externalIdentityId = repository.externalIdentityId();
        this.provider = repository.provider();
        this.providerRepositoryId = repository.providerRepositoryId();
        this.name = repository.name();
        this.fullName = repository.fullName();
        this.owner = repository.owner();
        this.visibility = repository.visibility().name();
        this.defaultBranch = repository.defaultBranch();
        this.providerArchived = repository.providerArchived();
        this.lifecycle = repository.lifecycle().name();
        this.syncStatus = repository.syncStatus().name();
        this.htmlUrl = repository.htmlUrl();
        this.discoveredAt = repository.discoveredAt();
        this.updatedAt = repository.updatedAt();
        this.lastSyncedAt = repository.lastSyncedAt();
        this.currentSnapshotId = repository.currentSnapshotId();
        this.version = repository.version();
    }

    Repository toDomain() {
        return new Repository(
            id, userId, externalIdentityId, provider, providerRepositoryId, name, fullName, owner,
            RepositoryVisibility.valueOf(visibility), defaultBranch, providerArchived,
            RepositoryLifecycle.valueOf(lifecycle), RepositorySyncStatus.valueOf(syncStatus), htmlUrl,
            discoveredAt, updatedAt, lastSyncedAt, currentSnapshotId, version
        );
    }
}
