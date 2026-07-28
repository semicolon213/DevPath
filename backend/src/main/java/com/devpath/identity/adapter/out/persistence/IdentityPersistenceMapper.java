package com.devpath.identity.adapter.out.persistence;

import com.devpath.identity.domain.ExternalIdentity;
import com.devpath.identity.domain.ExternalIdentityId;
import com.devpath.identity.domain.ProviderSubject;
import com.devpath.identity.domain.User;
import com.devpath.identity.domain.UserId;

final class IdentityPersistenceMapper {
    private IdentityPersistenceMapper() {
    }

    static UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
            user.id().value(),
            user.status(),
            user.displayName(),
            user.avatarUrl(),
            user.createdAt(),
            user.updatedAt(),
            user.version()
        );
    }

    static User toDomain(UserJpaEntity entity) {
        return User.rehydrate(
            new UserId(entity.id()),
            entity.status(),
            entity.displayName(),
            entity.avatarUrl(),
            entity.createdAt(),
            entity.updatedAt(),
            entity.version()
        );
    }

    static ExternalIdentityJpaEntity toEntity(ExternalIdentity identity) {
        return new ExternalIdentityJpaEntity(
            identity.id().value(),
            identity.userId().value(),
            identity.provider(),
            identity.providerSubject().value(),
            identity.providerUsername(),
            identity.displayName(),
            identity.avatarUrl(),
            identity.linkedAt(),
            identity.updatedAt(),
            identity.version()
        );
    }

    static ExternalIdentity toDomain(ExternalIdentityJpaEntity entity) {
        return ExternalIdentity.rehydrate(
            new ExternalIdentityId(entity.id()),
            new UserId(entity.userId()),
            entity.provider(),
            new ProviderSubject(entity.providerSubject()),
            entity.providerUsername(),
            entity.displayName(),
            entity.avatarUrl(),
            entity.linkedAt(),
            entity.updatedAt(),
            entity.version()
        );
    }
}
