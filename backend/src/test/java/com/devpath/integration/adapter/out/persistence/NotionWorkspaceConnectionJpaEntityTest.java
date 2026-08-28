package com.devpath.integration.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotionWorkspaceConnectionJpaEntityTest {
    @Test
    void expiryDiscardsBothProviderSecrets() {
        Instant created = Instant.parse("2026-08-27T00:00:00Z");
        var entity = new NotionWorkspaceConnectionJpaEntity(UUID.randomUUID(), UUID.randomUUID(),
            "workspace-1", "bot-1", "DevPath", null, new byte[] {1}, new byte[12],
            new byte[] {2}, new byte[12], "local-v1", created);
        byte[] discarded = new byte[] {9, 8, 7};
        byte[] discardedIv = new byte[12];

        entity.expire(discarded, discardedIv, "local-v2", created.plusSeconds(60));

        assertThat(entity.status()).isEqualTo("EXPIRED");
        assertThat(entity.encryptedAccessToken()).isEqualTo(discarded);
        assertThat(entity.encryptedRefreshToken()).isEqualTo(discarded);
        assertThat(entity.accessTokenIv()).isEqualTo(discardedIv);
        assertThat(entity.refreshTokenIv()).isEqualTo(discardedIv);
        assertThat(entity.keyVersion()).isEqualTo("local-v2");
    }
}
