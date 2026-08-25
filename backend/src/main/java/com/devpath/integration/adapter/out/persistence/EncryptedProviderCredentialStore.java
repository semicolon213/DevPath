package com.devpath.integration.adapter.out.persistence;

import com.devpath.integration.adapter.out.security.AesGcmCredentialCipher;
import com.devpath.integration.adapter.out.security.EncryptedSecret;
import com.devpath.integration.application.GitHubIntegrationUnavailableException;
import com.devpath.integration.config.GitHubIntegrationProperties;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EncryptedProviderCredentialStore {
    private static final String PROVIDER = "GITHUB";
    private final ProviderCredentialJpaRepository repository;
    private final GitHubIntegrationProperties properties;

    public EncryptedProviderCredentialStore(
        ProviderCredentialJpaRepository repository,
        GitHubIntegrationProperties properties
    ) {
        this.repository = repository;
        this.properties = properties;
    }

    public StoredProviderCredential save(
        UUID userId,
        UUID externalIdentityId,
        String accessToken,
        Instant expiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        String scopeSummary,
        Instant now
    ) {
        AesGcmCredentialCipher cipher = cipher();
        EncryptedSecret access = cipher.encrypt(accessToken, context(userId, "access"));
        EncryptedSecret refresh = refreshToken == null
            ? null
            : cipher.encrypt(refreshToken, context(userId, "refresh"));
        var existing = repository.findByUserIdAndProviderAndStatus(userId, PROVIDER, "ACTIVE");
        ProviderCredentialJpaEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.rotate(
                access.ciphertext(), access.iv(), expiresAt,
                refresh == null ? null : refresh.ciphertext(),
                refresh == null ? null : refresh.iv(),
                refreshTokenExpiresAt,
                normalizeScopes(scopeSummary), access.keyVersion(), now
            );
        } else {
            entity = new ProviderCredentialJpaEntity(
                UUID.randomUUID(), userId, externalIdentityId, PROVIDER,
                access.ciphertext(), access.iv(), normalizeScopes(scopeSummary), expiresAt,
                refresh == null ? null : refresh.ciphertext(),
                refresh == null ? null : refresh.iv(),
                refreshTokenExpiresAt,
                access.keyVersion(), "ACTIVE", now, now, 0
            );
        }
        return decrypt(repository.saveAndFlush(entity));
    }

    public Optional<StoredProviderCredential> findActive(UUID userId) {
        return repository.findByUserIdAndProviderAndStatus(userId, PROVIDER, "ACTIVE").map(this::decrypt);
    }

    @Transactional
    public Optional<StoredProviderCredential> removeActive(UUID userId) {
        Optional<ProviderCredentialJpaEntity> entity =
            repository.findByUserIdAndProviderAndStatus(userId, PROVIDER, "ACTIVE");
        if (entity.isEmpty()) {
            return Optional.empty();
        }
        StoredProviderCredential credential = decrypt(entity.get());
        repository.delete(entity.get());
        repository.flush();
        return Optional.of(credential);
    }

    private StoredProviderCredential decrypt(ProviderCredentialJpaEntity entity) {
        AesGcmCredentialCipher cipher = cipher();
        String access = cipher.decrypt(
            new EncryptedSecret(entity.encryptedAccessToken(), entity.accessTokenIv(), entity.keyVersion()),
            context(entity.userId(), "access")
        );
        String refresh = entity.encryptedRefreshToken() == null ? null : cipher.decrypt(
            new EncryptedSecret(entity.encryptedRefreshToken(), entity.refreshTokenIv(), entity.keyVersion()),
            context(entity.userId(), "refresh")
        );
        return new StoredProviderCredential(
            entity.id(), entity.userId(), entity.externalIdentityId(), access, entity.expiresAt(), refresh,
            entity.refreshTokenExpiresAt(), entity.scopeSummary(), entity.connectedAt()
        );
    }

    private AesGcmCredentialCipher cipher() {
        if (!properties.configured()) {
            throw new GitHubIntegrationUnavailableException("GitHub integration configuration is incomplete");
        }
        try {
            byte[] key = Base64.getDecoder().decode(properties.credentialKey());
            return new AesGcmCredentialCipher(new SecretKeySpec(key, "AES"), properties.credentialKeyVersion());
        } catch (IllegalArgumentException exception) {
            throw new GitHubIntegrationUnavailableException("Provider credential key configuration is invalid", exception);
        }
    }

    private String context(UUID userId, String tokenType) {
        return userId + ":" + PROVIDER + ":" + tokenType;
    }

    private String normalizeScopes(String value) {
        return value == null ? "" : value.trim().replace(',', ' ');
    }
}
