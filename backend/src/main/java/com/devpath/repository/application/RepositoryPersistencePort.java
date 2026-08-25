package com.devpath.repository.application;

import com.devpath.repository.domain.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryPersistencePort {
    Optional<Repository> findByOwnerAndProviderReference(UUID userId, String provider, String providerRepositoryId);
    Optional<Repository> findByIdAndOwner(UUID repositoryId, UUID userId);
    Repository save(Repository repository);
    List<Repository> findPageByOwner(UUID userId, int page, int limit, boolean includeArchived);
    long countByOwner(UUID userId, boolean includeArchived);
}
