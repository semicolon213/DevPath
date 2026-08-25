package com.devpath.identity.adapter.out.persistence;

import com.devpath.identity.application.UserPreferenceRepositoryPort;
import com.devpath.identity.domain.PreferenceType;
import com.devpath.identity.domain.UserId;
import com.devpath.identity.domain.UserPreference;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaUserPreferenceRepositoryAdapter implements UserPreferenceRepositoryPort {
    private final UserPreferenceJpaRepository repository;
    JpaUserPreferenceRepositoryAdapter(UserPreferenceJpaRepository repository) { this.repository = repository; }
    public Optional<UserPreference> findActive(UserId userId, PreferenceType type) {
        return repository.findByUserIdAndTypeAndActiveTrue(userId.value(), type).map(IdentityPersistenceMapper::toDomain);
    }
    public UserPreference save(UserPreference preference) { return IdentityPersistenceMapper.toDomain(repository.saveAndFlush(IdentityPersistenceMapper.toEntity(preference))); }
}
