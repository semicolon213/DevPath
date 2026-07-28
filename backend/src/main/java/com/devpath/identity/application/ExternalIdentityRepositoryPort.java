package com.devpath.identity.application;

import com.devpath.identity.domain.ExternalIdentity;
import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.ProviderSubject;

import java.util.Optional;

public interface ExternalIdentityRepositoryPort {
    Optional<ExternalIdentity> findByProviderAndSubject(OAuthProvider provider, ProviderSubject subject);

    ExternalIdentity save(ExternalIdentity externalIdentity);
}
