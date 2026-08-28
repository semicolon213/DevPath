package com.devpath.integration.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notion_workspace_connections")
class NotionWorkspaceConnectionJpaEntity {
    @Id @Column(name = "notion_connection_id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "provider_workspace_id", nullable = false)
    private String workspaceId;
    @Column(name = "provider_bot_id", nullable = false)
    private String botId;
    @Column(name = "workspace_name", nullable = false)
    private String workspaceName;
    @Column(name = "workspace_icon_url", length = 2048)
    private String workspaceIconUrl;
    @Column(name = "encrypted_access_token", nullable = false)
    private byte[] encryptedAccessToken;
    @Column(name = "access_token_iv", nullable = false)
    private byte[] accessTokenIv;
    @Column(name = "encrypted_refresh_token", nullable = false)
    private byte[] encryptedRefreshToken;
    @Column(name = "refresh_token_iv", nullable = false)
    private byte[] refreshTokenIv;
    @Column(name = "key_version", nullable = false)
    private String keyVersion;
    @Column(name = "connection_status", nullable = false)
    private String status;
    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version @Column(name = "version", nullable = false)
    private long version;

    protected NotionWorkspaceConnectionJpaEntity() {}

    NotionWorkspaceConnectionJpaEntity(UUID id, UUID userId, String workspaceId, String botId,
        String workspaceName, String workspaceIconUrl, byte[] access, byte[] accessIv,
        byte[] refresh, byte[] refreshIv, String keyVersion, Instant now) {
        this.id = id; this.userId = userId; this.workspaceId = workspaceId; this.botId = botId;
        this.workspaceName = workspaceName; this.workspaceIconUrl = workspaceIconUrl;
        this.encryptedAccessToken = access.clone(); this.accessTokenIv = accessIv.clone();
        this.encryptedRefreshToken = refresh.clone(); this.refreshTokenIv = refreshIv.clone();
        this.keyVersion = keyVersion; this.status = "ACTIVE"; this.connectedAt = now; this.updatedAt = now;
    }

    void rotate(String workspaceId, String botId, String workspaceName, String workspaceIconUrl, byte[] access, byte[] accessIv,
        byte[] refresh, byte[] refreshIv, String keyVersion, Instant now) {
        this.workspaceId = workspaceId; this.botId = botId; this.workspaceName = workspaceName; this.workspaceIconUrl = workspaceIconUrl;
        this.encryptedAccessToken = access.clone(); this.accessTokenIv = accessIv.clone();
        this.encryptedRefreshToken = refresh.clone(); this.refreshTokenIv = refreshIv.clone();
        this.keyVersion = keyVersion; this.status = "ACTIVE"; this.updatedAt = now;
    }

    void revoke(byte[] discarded, byte[] discardedIv, String keyVersion, Instant now) {
        transitionWithoutSecrets("REVOKED", discarded, discardedIv, keyVersion, now);
    }

    void expire(byte[] discarded, byte[] discardedIv, String keyVersion, Instant now) {
        transitionWithoutSecrets("EXPIRED", discarded, discardedIv, keyVersion, now);
    }

    private void transitionWithoutSecrets(String nextStatus, byte[] discarded, byte[] discardedIv,
        String keyVersion, Instant now) {
        this.encryptedAccessToken = discarded.clone(); this.accessTokenIv = discardedIv.clone();
        this.encryptedRefreshToken = discarded.clone(); this.refreshTokenIv = discardedIv.clone();
        this.keyVersion = keyVersion; this.status = nextStatus; this.updatedAt = now;
    }

    UUID id() { return id; } UUID userId() { return userId; } String workspaceId() { return workspaceId; }
    String botId() { return botId; } String workspaceName() { return workspaceName; }
    String workspaceIconUrl() { return workspaceIconUrl; } byte[] encryptedAccessToken() { return encryptedAccessToken.clone(); }
    byte[] accessTokenIv() { return accessTokenIv.clone(); } byte[] encryptedRefreshToken() { return encryptedRefreshToken.clone(); }
    byte[] refreshTokenIv() { return refreshTokenIv.clone(); } String keyVersion() { return keyVersion; }
    String status() { return status; } Instant connectedAt() { return connectedAt; }
}
