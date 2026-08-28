package com.devpath.integration.adapter.out.persistence;

import com.devpath.integration.adapter.out.security.AesGcmCredentialCipher;
import com.devpath.integration.adapter.out.security.EncryptedSecret;
import com.devpath.integration.application.NotionIntegrationUnavailableException;
import com.devpath.integration.config.NotionIntegrationProperties;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EncryptedNotionCredentialStore {
    private static final String PROVIDER = "NOTION";
    private final NotionWorkspaceConnectionJpaRepository repository;
    private final NotionIntegrationProperties properties;

    public EncryptedNotionCredentialStore(NotionWorkspaceConnectionJpaRepository repository, NotionIntegrationProperties properties) {
        this.repository = repository; this.properties = properties;
    }

    @Transactional
    public StoredNotionCredential save(UUID userId, String workspaceId, String botId, String workspaceName,
        String workspaceIconUrl, String accessToken, String refreshToken, Instant now) {
        AesGcmCredentialCipher cipher = cipher();
        EncryptedSecret access = cipher.encrypt(accessToken, context(userId, workspaceId, "access"));
        EncryptedSecret refresh = cipher.encrypt(refreshToken, context(userId, workspaceId, "refresh"));
        var entity = repository.findByUserId(userId).orElse(null);
        if (entity == null) {
            entity = new NotionWorkspaceConnectionJpaEntity(UUID.randomUUID(), userId, workspaceId, botId,
                normalizeName(workspaceName), workspaceIconUrl, access.ciphertext(), access.iv(), refresh.ciphertext(),
                refresh.iv(), access.keyVersion(), now);
        } else {
            entity.rotate(workspaceId, botId, normalizeName(workspaceName), workspaceIconUrl, access.ciphertext(), access.iv(),
                refresh.ciphertext(), refresh.iv(), access.keyVersion(), now);
        }
        return decrypt(repository.saveAndFlush(entity));
    }

    public Optional<StoredNotionCredential> findActive(UUID userId) {
        return repository.findFirstByUserIdAndStatusOrderByConnectedAtAsc(userId, "ACTIVE").map(this::decrypt);
    }

    @Transactional
    public Optional<StoredNotionCredential> revokeActive(UUID userId, Instant now) {
        return transitionActiveWithoutSecrets(userId, now, false);
    }

    @Transactional
    public Optional<StoredNotionCredential> expireActive(UUID userId, Instant now) {
        return transitionActiveWithoutSecrets(userId, now, true);
    }

    public List<com.devpath.integration.application.ConnectedAccountView> findAllViews(UUID userId) {
        return repository.findAllByUserIdOrderByConnectedAtAsc(userId).stream()
            .map(entity -> new com.devpath.integration.application.ConnectedAccountView(
                entity.id(), PROVIDER, entity.status(), "ACTIVE".equals(entity.status())
                    ? List.of("read_content") : List.of(), entity.connectedAt(), null))
            .toList();
    }

    private StoredNotionCredential decrypt(NotionWorkspaceConnectionJpaEntity entity) {
        AesGcmCredentialCipher cipher = cipher();
        String access = cipher.decrypt(new EncryptedSecret(entity.encryptedAccessToken(), entity.accessTokenIv(), entity.keyVersion()),
            context(entity.userId(), entity.workspaceId(), "access"));
        String refresh = cipher.decrypt(new EncryptedSecret(entity.encryptedRefreshToken(), entity.refreshTokenIv(), entity.keyVersion()),
            context(entity.userId(), entity.workspaceId(), "refresh"));
        return new StoredNotionCredential(entity.id(), entity.userId(), entity.workspaceId(), entity.botId(),
            entity.workspaceName(), entity.workspaceIconUrl(), access, refresh, entity.connectedAt());
    }

    private AesGcmCredentialCipher cipher() {
        if (!properties.configured()) throw new NotionIntegrationUnavailableException("Notion integration configuration is incomplete");
        try {
            byte[] key = Base64.getDecoder().decode(properties.credentialKey());
            return new AesGcmCredentialCipher(new SecretKeySpec(key, "AES"), properties.credentialKeyVersion());
        } catch (IllegalArgumentException exception) {
            throw new NotionIntegrationUnavailableException("Provider credential key configuration is invalid", exception);
        }
    }

    private Optional<StoredNotionCredential> transitionActiveWithoutSecrets(UUID userId, Instant now, boolean expired) {
        var found = repository.findFirstByUserIdAndStatusOrderByConnectedAtAsc(userId, "ACTIVE");
        if (found.isEmpty()) return Optional.empty();
        var credential = decrypt(found.get());
        EncryptedSecret discarded = cipher().encrypt(UUID.randomUUID().toString(),
            context(userId, credential.workspaceId(), "access"));
        if (expired) {
            found.get().expire(discarded.ciphertext(), discarded.iv(), discarded.keyVersion(), now);
        } else {
            found.get().revoke(discarded.ciphertext(), discarded.iv(), discarded.keyVersion(), now);
        }
        repository.saveAndFlush(found.get());
        return Optional.of(credential);
    }

    private String context(UUID userId, String workspaceId, String tokenType) { return userId + ":" + PROVIDER + ":" + workspaceId + ":" + tokenType; }
    private String normalizeName(String value) { return value == null || value.isBlank() ? "Notion workspace" : value.trim(); }
}
