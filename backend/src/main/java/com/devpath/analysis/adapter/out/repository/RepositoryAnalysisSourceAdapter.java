package com.devpath.analysis.adapter.out.repository;

import com.devpath.analysis.application.AnalysisNotFoundException;
import com.devpath.analysis.application.AnalysisSourcePort;
import com.devpath.repository.application.RepositoryPersistencePort;
import com.devpath.repository.application.RepositorySynchronizationPersistencePort;
import com.devpath.repository.domain.RepositorySnapshot;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RepositoryAnalysisSourceAdapter implements AnalysisSourcePort {
    private final RepositoryPersistencePort repositories;
    private final RepositorySynchronizationPersistencePort snapshots;

    RepositoryAnalysisSourceAdapter(
        RepositoryPersistencePort repositories,
        RepositorySynchronizationPersistencePort snapshots
    ) {
        this.repositories = repositories;
        this.snapshots = snapshots;
    }

    @Override
    public RepositorySnapshot resolveOwnedSnapshot(UUID userId, UUID repositoryId, UUID snapshotId) {
        var repository = repositories.findByIdAndOwner(repositoryId, userId)
            .orElseThrow(AnalysisNotFoundException::new);
        UUID resolvedSnapshotId = snapshotId == null ? repository.currentSnapshotId() : snapshotId;
        if (resolvedSnapshotId == null) throw new IllegalStateException("Repository has no ready snapshot");
        return snapshots.findSnapshot(userId, repositoryId, resolvedSnapshotId)
            .filter(value -> "READY".equals(value.status()) && "ACTIVE".equals(value.retentionStatus()))
            .orElseThrow(AnalysisNotFoundException::new);
    }

    @Override
    public void verifyOwnedRepository(UUID userId, UUID repositoryId) {
        repositories.findByIdAndOwner(repositoryId, userId).orElseThrow(AnalysisNotFoundException::new);
    }
}
