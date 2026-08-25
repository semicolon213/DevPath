package com.devpath.identity.application;

import com.devpath.identity.domain.ExternalIdentity;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.ProviderSubject;
import com.devpath.identity.domain.UserId;

import java.util.Optional;

public interface ExternalIdentityRepositoryPort {
    Optional<ExternalIdentity> findByProviderAndSubject(OAuthProvider provider, ProviderSubject subject);

    Optional<ExternalIdentity> findByUserIdAndProvider(UserId userId, OAuthProvider provider);

    ExternalIdentity save(ExternalIdentity externalIdentity);
}
