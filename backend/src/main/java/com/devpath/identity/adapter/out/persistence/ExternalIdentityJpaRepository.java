package com.devpath.identity.adapter.out.persistence;

import com.devpath.identity.domain.OAuthProvider;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ExternalIdentityJpaRepository extends JpaRepository<ExternalIdentityJpaEntity, UUID> {
    Optional<ExternalIdentityJpaEntity> findByProviderAndProviderSubject(
        OAuthProvider provider,
        String providerSubject
    );

    Optional<ExternalIdentityJpaEntity> findByUserIdAndProvider(UUID userId, OAuthProvider provider);
}
