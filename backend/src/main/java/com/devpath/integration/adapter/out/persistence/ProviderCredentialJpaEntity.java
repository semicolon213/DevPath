package com.devpath.integration.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_credentials")
class ProviderCredentialJpaEntity {
    @Id
    @Column(name = "provider_credential_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "external_identity_id", nullable = false, updatable = false)
    private UUID externalIdentityId;

    @Column(name = "provider", nullable = false, updatable = false, length = 32)
    private String provider;

    @Column(name = "encrypted_access_token", nullable = false, length = 8192)
    private byte[] encryptedAccessToken;

    @Column(name = "access_token_iv", nullable = false, length = 12)
    private byte[] accessTokenIv;

    @Column(name = "scope_summary", nullable = false, length = 2048)
    private String scopeSummary;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "encrypted_refresh_token", length = 8192)
    private byte[] encryptedRefreshToken;

    @Column(name = "refresh_token_iv", length = 12)
    private byte[] refreshTokenIv;

    @Column(name = "refresh_token_expires_at")
    private Instant refreshTokenExpiresAt;

    @Column(name = "key_version", nullable = false, length = 64)
    private String keyVersion;

    @Column(name = "credential_status", nullable = false, length = 32)
    private String status;

    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProviderCredentialJpaEntity() {
    }

    ProviderCredentialJpaEntity(
        UUID id,
        UUID userId,
        UUID externalIdentityId,
        String provider,
        byte[] encryptedAccessToken,
        byte[] accessTokenIv,
        String scopeSummary,
        Instant expiresAt,
        byte[] encryptedRefreshToken,
        byte[] refreshTokenIv,
        Instant refreshTokenExpiresAt,
        String keyVersion,
        String status,
        Instant connectedAt,
        Instant updatedAt,
        long version
    ) {
        this.id = id;
        this.userId = userId;
        this.externalIdentityId = externalIdentityId;
        this.provider = provider;
        this.encryptedAccessToken = encryptedAccessToken.clone();
        this.accessTokenIv = accessTokenIv.clone();
        this.scopeSummary = scopeSummary;
        this.expiresAt = expiresAt;
        this.encryptedRefreshToken = copy(encryptedRefreshToken);
        this.refreshTokenIv = copy(refreshTokenIv);
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.keyVersion = keyVersion;
        this.status = status;
        this.connectedAt = connectedAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    UUID id() { return id; }
    UUID userId() { return userId; }
    String provider() { return provider; }
    String scopeSummary() { return scopeSummary; }
    Instant expiresAt() { return expiresAt; }
    String status() { return status; }
    Instant connectedAt() { return connectedAt; }
    UUID externalIdentityId() { return externalIdentityId; }
    byte[] encryptedAccessToken() { return encryptedAccessToken.clone(); }
    byte[] accessTokenIv() { return accessTokenIv.clone(); }
    byte[] encryptedRefreshToken() { return copy(encryptedRefreshToken); }
    byte[] refreshTokenIv() { return copy(refreshTokenIv); }
    Instant refreshTokenExpiresAt() { return refreshTokenExpiresAt; }
    String keyVersion() { return keyVersion; }

    void rotate(
        byte[] encryptedAccessToken,
        byte[] accessTokenIv,
        Instant expiresAt,
        byte[] encryptedRefreshToken,
        byte[] refreshTokenIv,
        Instant refreshTokenExpiresAt,
        String scopeSummary,
        String keyVersion,
        Instant now
    ) {
        this.encryptedAccessToken = encryptedAccessToken.clone();
        this.accessTokenIv = accessTokenIv.clone();
        this.expiresAt = expiresAt;
        this.encryptedRefreshToken = copy(encryptedRefreshToken);
        this.refreshTokenIv = copy(refreshTokenIv);
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.scopeSummary = scopeSummary;
        this.keyVersion = keyVersion;
        this.status = "ACTIVE";
        this.updatedAt = now;
    }

    private static byte[] copy(byte[] value) {
        return value == null ? null : value.clone();
    }
}
