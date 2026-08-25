package com.devpath.analysis.application;

import com.devpath.repository.domain.RepositorySnapshot;
import java.util.UUID;

public interface AnalysisSourcePort {
    RepositorySnapshot resolveOwnedSnapshot(UUID userId, UUID repositoryId, UUID snapshotId);

    default void verifyOwnedRepository(UUID userId, UUID repositoryId) {
        resolveOwnedSnapshot(userId, repositoryId, null);
    }
}
