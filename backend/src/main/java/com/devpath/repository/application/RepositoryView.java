package com.devpath.repository.application;

import com.devpath.repository.domain.Repository;
import java.time.Instant;
import java.util.UUID;

public record RepositoryView(
    UUID repositoryId,
    String providerRepositoryId,
    String name,
    String fullName,
    String owner,
    String visibility,
    String defaultBranch,
    boolean providerArchived,
    String lifecycle,
    String syncStatus,
    String htmlUrl,
    Instant discoveredAt,
    Instant lastSyncedAt,
    UUID currentSnapshotId
) {
    static RepositoryView from(Repository repository) {
        return new RepositoryView(
            repository.id(), repository.providerRepositoryId(), repository.name(), repository.fullName(),
            repository.owner(), repository.visibility().name(), repository.defaultBranch(),
            repository.providerArchived(), repository.lifecycle().name(), repository.syncStatus().name(),
            repository.htmlUrl(), repository.discoveredAt(), repository.lastSyncedAt(), repository.currentSnapshotId()
        );
    }
}
