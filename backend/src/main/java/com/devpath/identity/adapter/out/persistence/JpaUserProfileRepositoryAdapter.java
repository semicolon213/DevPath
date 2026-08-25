package com.devpath.identity.adapter.out.persistence;

import com.devpath.identity.application.UserProfileRepositoryPort;
import com.devpath.identity.domain.UserId;
import com.devpath.identity.domain.UserProfile;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaUserProfileRepositoryAdapter implements UserProfileRepositoryPort {
    private final UserProfileJpaRepository repository;
    JpaUserProfileRepositoryAdapter(UserProfileJpaRepository repository) { this.repository = repository; }
    public Optional<UserProfile> findByUserId(UserId userId) { return repository.findByUserId(userId.value()).map(IdentityPersistenceMapper::toDomain); }
    public UserProfile save(UserProfile profile) { return IdentityPersistenceMapper.toDomain(repository.saveAndFlush(IdentityPersistenceMapper.toEntity(profile))); }
}
