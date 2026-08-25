package com.devpath.integration.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderCredentialJpaEntityTest {
    private static final Instant CONNECTED_AT = Instant.parse("2026-08-11T00:00:00Z");

    @Test
    void discardsProviderSecretsAndScopesWhenTheConnectionIsRevoked() {
        var entity = activeCredential();
        byte[] tombstone = {9, 8, 7};
        byte[] tombstoneIv = {6, 5, 4};
        Instant revokedAt = CONNECTED_AT.plusSeconds(60);

        entity.deactivate("REVOKED", tombstone, tombstoneIv, "local-v2", revokedAt);

        assertThat(entity.status()).isEqualTo("REVOKED");
        assertThat(entity.encryptedAccessToken()).containsExactly(tombstone);
        assertThat(entity.accessTokenIv()).containsExactly(tombstoneIv);
        assertThat(entity.encryptedRefreshToken()).isNull();
        assertThat(entity.refreshTokenIv()).isNull();
        assertThat(entity.refreshTokenExpiresAt()).isNull();
        assertThat(entity.scopeSummary()).isEmpty();
        assertThat(entity.expiresAt()).isEqualTo(revokedAt);
        assertThat(entity.keyVersion()).isEqualTo("local-v2");
    }

    @Test
    void rotatingAnInactiveCredentialReactivatesTheSameConnectionRecord() {
        var entity = activeCredential();
        UUID connectionId = entity.id();
        Instant reconnectedAt = CONNECTED_AT.plusSeconds(120);
        entity.deactivate("EXPIRED", new byte[] {9}, new byte[] {8}, "local-v1", CONNECTED_AT.plusSeconds(60));

        entity.rotate(
            new byte[] {3, 4}, new byte[] {5, 6}, reconnectedAt.plusSeconds(3_600),
            new byte[] {7, 8}, new byte[] {9, 10}, reconnectedAt.plusSeconds(7_200),
            "repo read:user", "local-v2", reconnectedAt
        );

        assertThat(entity.id()).isEqualTo(connectionId);
        assertThat(entity.status()).isEqualTo("ACTIVE");
        assertThat(entity.scopeSummary()).isEqualTo("repo read:user");
        assertThat(entity.encryptedRefreshToken()).containsExactly(7, 8);
        assertThat(entity.keyVersion()).isEqualTo("local-v2");
    }

    private ProviderCredentialJpaEntity activeCredential() {
        return new ProviderCredentialJpaEntity(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "GITHUB",
            new byte[] {1, 2}, new byte[] {3, 4}, "repo",
            CONNECTED_AT.plusSeconds(3_600), new byte[] {5, 6}, new byte[] {7, 8},
            CONNECTED_AT.plusSeconds(7_200), "local-v1", "ACTIVE", CONNECTED_AT, CONNECTED_AT, 0
        );
    }
}
