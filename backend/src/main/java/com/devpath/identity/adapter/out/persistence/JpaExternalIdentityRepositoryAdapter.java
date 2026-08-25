package com.devpath.identity.adapter.out.persistence;

import com.devpath.identity.application.DuplicateExternalIdentityException;
import com.devpath.identity.application.ExternalIdentityRepositoryPort;
import com.devpath.identity.domain.ExternalIdentity;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.ProviderSubject;
import com.devpath.identity.domain.UserId;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaExternalIdentityRepositoryAdapter implements ExternalIdentityRepositoryPort {
    private final ExternalIdentityJpaRepository repository;

    JpaExternalIdentityRepositoryAdapter(ExternalIdentityJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ExternalIdentity> findByUserIdAndProvider(UserId userId, OAuthProvider provider) {
        return repository.findByUserIdAndProvider(userId.value(), provider)
            .map(IdentityPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ExternalIdentity> findByProviderAndSubject(
        OAuthProvider provider,
        ProviderSubject subject
    ) {
        return repository
            .findByProviderAndProviderSubject(provider, subject.value())
            .map(IdentityPersistenceMapper::toDomain);
    }

    @Override
    public ExternalIdentity save(ExternalIdentity externalIdentity) {
        try {
            return IdentityPersistenceMapper.toDomain(
                repository.saveAndFlush(IdentityPersistenceMapper.toEntity(externalIdentity))
            );
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateExternalIdentityException(exception);
        }
    }
}
