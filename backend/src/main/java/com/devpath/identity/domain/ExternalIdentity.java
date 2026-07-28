package com.devpath.identity.domain;

import java.time.Instant;
import java.util.Objects;

public final class ExternalIdentity {
    private final ExternalIdentityId id;
    private final UserId userId;
    private final OAuthProvider provider;
    private final ProviderSubject providerSubject;
    private final Instant linkedAt;
    private final String providerUsername;
    private final String displayName;
    private final String avatarUrl;
    private final Instant updatedAt;
    private final long version;

    private ExternalIdentity(
        ExternalIdentityId id,
        UserId userId,
        OAuthProvider provider,
        ProviderSubject providerSubject,
        String providerUsername,
        String displayName,
        String avatarUrl,
        Instant linkedAt,
        Instant updatedAt,
        long version
    ) {
        this.id = Objects.requireNonNull(id, "External identity ID is required");
        this.userId = Objects.requireNonNull(userId, "User ID is required");
        this.provider = Objects.requireNonNull(provider, "OAuth provider is required");
        this.providerSubject = Objects.requireNonNull(providerSubject, "Provider subject is required");
        this.providerUsername = normalizeOptional(providerUsername);
        this.displayName = normalizeOptional(displayName);
        this.avatarUrl = normalizeOptional(avatarUrl);
        this.linkedAt = Objects.requireNonNull(linkedAt, "Link time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Update time is required");
        this.version = version;
    }

    public static ExternalIdentity link(
        UserId userId,
        OAuthProvider provider,
        ProviderSubject providerSubject,
        String providerUsername,
        String displayName,
        String avatarUrl,
        Instant now
    ) {
        return new ExternalIdentity(
            ExternalIdentityId.newId(),
            userId,
            provider,
            providerSubject,
            providerUsername,
            displayName,
            avatarUrl,
            now,
            now,
            0
        );
    }

    public static ExternalIdentity rehydrate(
        ExternalIdentityId id,
        UserId userId,
        OAuthProvider provider,
        ProviderSubject providerSubject,
        String providerUsername,
        String displayName,
        String avatarUrl,
        Instant linkedAt,
        Instant updatedAt,
        long version
    ) {
        return new ExternalIdentity(
            id,
            userId,
            provider,
            providerSubject,
            providerUsername,
            displayName,
            avatarUrl,
            linkedAt,
            updatedAt,
            version
        );
    }

    public ExternalIdentityId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public OAuthProvider provider() {
        return provider;
    }

    public ProviderSubject providerSubject() {
        return providerSubject;
    }

    public String providerUsername() {
        return providerUsername;
    }

    public String displayName() {
        return displayName;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public Instant linkedAt() {
        return linkedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
