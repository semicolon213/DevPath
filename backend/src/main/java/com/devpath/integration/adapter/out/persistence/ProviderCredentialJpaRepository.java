package com.devpath.integration.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProviderCredentialJpaRepository extends JpaRepository<ProviderCredentialJpaEntity, UUID> {
    List<ProviderCredentialJpaEntity> findAllByUserIdOrderByConnectedAtAsc(UUID userId);
    Optional<ProviderCredentialJpaEntity> findByUserIdAndProvider(UUID userId, String provider);
    Optional<ProviderCredentialJpaEntity> findByUserIdAndProviderAndStatus(UUID userId, String provider, String status);
}
