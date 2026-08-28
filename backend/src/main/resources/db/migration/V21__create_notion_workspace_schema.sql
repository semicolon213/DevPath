CREATE TABLE notion_workspace_connections (
    notion_connection_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider_workspace_id VARCHAR(255) NOT NULL,
    provider_bot_id VARCHAR(255) NOT NULL,
    workspace_name VARCHAR(255) NOT NULL,
    workspace_icon_url VARCHAR(2048),
    encrypted_access_token BYTEA NOT NULL,
    access_token_iv BYTEA NOT NULL,
    encrypted_refresh_token BYTEA NOT NULL,
    refresh_token_iv BYTEA NOT NULL,
    key_version VARCHAR(64) NOT NULL,
    connection_status VARCHAR(32) NOT NULL,
    connected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT notion_workspace_connections_user_fk
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT notion_workspace_connections_user_uk UNIQUE (user_id),
    CONSTRAINT notion_workspace_connections_status_check
        CHECK (connection_status IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
    CONSTRAINT notion_workspace_connections_access_iv_check
        CHECK (octet_length(access_token_iv) = 12),
    CONSTRAINT notion_workspace_connections_refresh_iv_check
        CHECK (octet_length(refresh_token_iv) = 12)
);

CREATE INDEX notion_workspace_connections_user_status_idx
    ON notion_workspace_connections (user_id, connection_status);

CREATE TABLE notion_page_metadata (
    notion_page_metadata_id UUID PRIMARY KEY,
    notion_connection_id UUID NOT NULL,
    user_id UUID NOT NULL,
    provider_page_id VARCHAR(255) NOT NULL,
    object_type VARCHAR(32) NOT NULL,
    title VARCHAR(512) NOT NULL,
    provider_url VARCHAR(2048),
    last_edited_at TIMESTAMP WITH TIME ZONE NOT NULL,
    in_trash BOOLEAN NOT NULL,
    discovered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT notion_page_metadata_connection_fk
        FOREIGN KEY (notion_connection_id) REFERENCES notion_workspace_connections (notion_connection_id) ON DELETE CASCADE,
    CONSTRAINT notion_page_metadata_user_fk
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT notion_page_metadata_connection_page_uk
        UNIQUE (notion_connection_id, provider_page_id),
    CONSTRAINT notion_page_metadata_object_type_check
        CHECK (object_type IN ('PAGE', 'DATA_SOURCE'))
);

CREATE INDEX notion_page_metadata_user_connection_idx
    ON notion_page_metadata (user_id, notion_connection_id, last_edited_at DESC);
