package com.devpath.identity.application;

import com.devpath.identity.domain.OAuthProvider;
import com.devpath.identity.domain.ProviderSubject;

import java.util.Objects;

public record OAuthLoginCommand(
    OAuthProvider provider,
    ProviderSubject providerSubject,
    String providerUsername,
    String displayName,
    String avatarUrl
) {
    public OAuthLoginCommand {
        Objects.requireNonNull(provider, "OAuth provider is required");
        Objects.requireNonNull(providerSubject, "Provider subject is required");
        if (displayName == null || displayName.isBlank()) {
            displayName = providerUsername == null || providerUsername.isBlank()
                ? provider + " user " + providerSubject.value()
                : providerUsername;
        }
    }
}
