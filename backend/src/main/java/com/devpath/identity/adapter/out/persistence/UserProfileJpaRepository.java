package com.devpath.identity.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserProfileJpaRepository extends JpaRepository<UserProfileJpaEntity, UUID> {
    Optional<UserProfileJpaEntity> findByUserId(UUID userId);
}
