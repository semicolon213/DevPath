CREATE TABLE repositories (
    repository_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    external_identity_id UUID NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_repository_id VARCHAR(64) NOT NULL,
    repository_name VARCHAR(100) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    owner_login VARCHAR(255) NOT NULL,
    visibility VARCHAR(16) NOT NULL,
    default_branch VARCHAR(255) NOT NULL,
    provider_archived BOOLEAN NOT NULL,
    lifecycle_status VARCHAR(32) NOT NULL,
    sync_status VARCHAR(32) NOT NULL,
    html_url VARCHAR(2048) NOT NULL,
    discovered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_synced_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT repositories_user_fk
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT repositories_identity_fk
        FOREIGN KEY (external_identity_id) REFERENCES external_identities (external_identity_id) ON DELETE CASCADE,
    CONSTRAINT repositories_provider_check CHECK (provider IN ('GITHUB')),
    CONSTRAINT repositories_visibility_check CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT repositories_lifecycle_check CHECK (lifecycle_status IN ('DISCOVERED', 'ACTIVE', 'ARCHIVED', 'DELETED_EXTERNALLY')),
    CONSTRAINT repositories_sync_check CHECK (sync_status IN ('NOT_SYNCED', 'SYNCHRONIZED', 'FAILED')),
    CONSTRAINT repositories_user_provider_reference_uk UNIQUE (user_id, provider, provider_repository_id)
);

CREATE INDEX repositories_user_discovered_idx
    ON repositories (user_id, discovered_at DESC, repository_id DESC);

CREATE INDEX repositories_user_lifecycle_idx
    ON repositories (user_id, lifecycle_status);
