package com.devpath.identity.adapter.out.persistence;

import com.devpath.identity.application.UserRepositoryPort;
import com.devpath.identity.domain.User;
import com.devpath.identity.domain.UserId;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaUserRepositoryAdapter implements UserRepositoryPort {
    private final UserJpaRepository repository;

    JpaUserRepositoryAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return repository.findById(userId.value()).map(IdentityPersistenceMapper::toDomain);
    }

    @Override
    public User save(User user) {
        return IdentityPersistenceMapper.toDomain(repository.saveAndFlush(IdentityPersistenceMapper.toEntity(user)));
    }
}
