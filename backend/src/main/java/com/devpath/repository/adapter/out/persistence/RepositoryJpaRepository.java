package com.devpath.repository.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface RepositoryJpaRepository extends JpaRepository<RepositoryJpaEntity, UUID> {
    Optional<RepositoryJpaEntity> findByUserIdAndProviderAndProviderRepositoryId(
        UUID userId, String provider, String providerRepositoryId
    );
    Optional<RepositoryJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    Page<RepositoryJpaEntity> findAllByUserIdOrderByDiscoveredAtDescIdDesc(UUID userId, Pageable pageable);
    Page<RepositoryJpaEntity> findAllByUserIdAndLifecycleNotOrderByDiscoveredAtDescIdDesc(
        UUID userId, String lifecycle, Pageable pageable
    );
    long countByUserId(UUID userId);
    long countByUserIdAndLifecycleNot(UUID userId, String lifecycle);
    long countByUserIdAndSyncStatus(UUID userId, String syncStatus);
    long countByUserIdAndSyncStatusAndLifecycleNot(UUID userId, String syncStatus, String lifecycle);
}
