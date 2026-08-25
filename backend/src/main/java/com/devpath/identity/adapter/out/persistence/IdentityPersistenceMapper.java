package com.devpath.identity.adapter.out.persistence;

import com.devpath.identity.domain.ExternalIdentity;
import com.devpath.identity.domain.ExternalIdentityId;
import com.devpath.identity.domain.ProviderSubject;
import com.devpath.identity.domain.User;
import com.devpath.identity.domain.UserId;
import com.devpath.identity.domain.UserPreference;
import com.devpath.identity.domain.UserProfile;

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

    static UserProfileJpaEntity toEntity(UserProfile profile) {
        return new UserProfileJpaEntity(profile.id(), profile.userId().value(), profile.careerStage(), profile.bio(), profile.createdAt(), profile.updatedAt(), profile.version());
    }

    static UserProfile toDomain(UserProfileJpaEntity entity) {
        return UserProfile.rehydrate(entity.id(), new UserId(entity.userId()), entity.careerStage(), entity.bio(), entity.createdAt(), entity.updatedAt(), entity.version());
    }

    static UserPreferenceJpaEntity toEntity(UserPreference preference) {
        return new UserPreferenceJpaEntity(preference.id(), preference.userId().value(), preference.type(), preference.selectedValue(), preference.catalogVersion(), preference.active(), preference.selectedAt(), preference.supersededAt(), preference.version());
    }

    static UserPreference toDomain(UserPreferenceJpaEntity entity) {
        return UserPreference.rehydrate(entity.id(), new UserId(entity.userId()), entity.type(), entity.selectedValue(), entity.catalogVersion(), entity.active(), entity.selectedAt(), entity.supersededAt(), entity.version());
    }
}
