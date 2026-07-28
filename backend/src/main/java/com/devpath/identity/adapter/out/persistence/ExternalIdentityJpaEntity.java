package com.devpath.identity.adapter.out.persistence;

import com.devpath.identity.domain.OAuthProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "external_identities",
    uniqueConstraints = @UniqueConstraint(
        name = "external_identities_provider_subject_uk",
        columnNames = {"provider", "provider_subject"}
    )
)
class ExternalIdentityJpaEntity {
    @Id
    @Column(name = "external_identity_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, updatable = false, length = 32)
    private OAuthProvider provider;

    @Column(name = "provider_subject", nullable = false, updatable = false, length = 255)
    private String providerSubject;

    @Column(name = "provider_username", length = 255)
    private String providerUsername;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ExternalIdentityJpaEntity() {
    }

    ExternalIdentityJpaEntity(
        UUID id,
        UUID userId,
        OAuthProvider provider,
        String providerSubject,
        String providerUsername,
        String displayName,
        String avatarUrl,
        Instant linkedAt,
        Instant updatedAt,
        long version
    ) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.providerUsername = providerUsername;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.linkedAt = linkedAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    UUID id() {
        return id;
    }

    UUID userId() {
        return userId;
    }

    OAuthProvider provider() {
        return provider;
    }

    String providerSubject() {
        return providerSubject;
    }

    String providerUsername() {
        return providerUsername;
    }

    String displayName() {
        return displayName;
    }

    String avatarUrl() {
        return avatarUrl;
    }

    Instant linkedAt() {
        return linkedAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }

    long version() {
        return version;
    }
}
