package com.devpath.repository.adapter.out.persistence;

import com.devpath.repository.application.RepositoryPersistencePort;
import com.devpath.repository.domain.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

@org.springframework.stereotype.Repository
class JpaRepositoryPersistenceAdapter implements RepositoryPersistencePort {
    private final RepositoryJpaRepository repository;

    JpaRepositoryPersistenceAdapter(RepositoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Repository> findByOwnerAndProviderReference(
        UUID userId, String provider, String providerRepositoryId
    ) {
        return repository.findByUserIdAndProviderAndProviderRepositoryId(userId, provider, providerRepositoryId)
            .map(RepositoryJpaEntity::toDomain);
    }

    @Override
    public Optional<Repository> findByIdAndOwner(UUID repositoryId, UUID userId) {
        return repository.findByIdAndUserId(repositoryId, userId).map(RepositoryJpaEntity::toDomain);
    }

    @Override
    public Repository save(Repository value) {
        return repository.saveAndFlush(new RepositoryJpaEntity(value)).toDomain();
    }

    @Override
    public List<Repository> findPageByOwner(UUID userId, int page, int limit, boolean includeArchived) {
        var result = includeArchived
            ? repository.findAllByUserIdOrderByDiscoveredAtDescIdDesc(userId, PageRequest.of(page, limit))
            : repository.findAllByUserIdAndLifecycleNotOrderByDiscoveredAtDescIdDesc(
                userId, "ARCHIVED", PageRequest.of(page, limit)
            );
        return result
            .stream().map(RepositoryJpaEntity::toDomain).toList();
    }

    @Override
    public long countByOwner(UUID userId, boolean includeArchived) {
        return includeArchived
            ? repository.countByUserId(userId)
            : repository.countByUserIdAndLifecycleNot(userId, "ARCHIVED");
    }

    @Override
    public long countByOwnerAndSyncStatus(UUID userId, String syncStatus, boolean includeArchived) {
        return includeArchived
            ? repository.countByUserIdAndSyncStatus(userId, syncStatus)
            : repository.countByUserIdAndSyncStatusAndLifecycleNot(userId, syncStatus, "ARCHIVED");
    }
}
