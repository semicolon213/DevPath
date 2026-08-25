package com.devpath.identity.adapter.out.persistence;

import com.devpath.identity.domain.PreferenceType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserPreferenceJpaRepository extends JpaRepository<UserPreferenceJpaEntity, UUID> {
    Optional<UserPreferenceJpaEntity> findByUserIdAndTypeAndActiveTrue(UUID userId, PreferenceType type);
}
