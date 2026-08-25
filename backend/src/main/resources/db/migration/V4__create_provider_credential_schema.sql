CREATE TABLE provider_credentials (
    provider_credential_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    external_identity_id UUID NOT NULL,
    provider VARCHAR(32) NOT NULL,
    encrypted_access_token BYTEA NOT NULL,
    access_token_iv BYTEA NOT NULL,
    scope_summary VARCHAR(2048) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    key_version VARCHAR(64) NOT NULL,
    credential_status VARCHAR(32) NOT NULL,
    connected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT provider_credentials_user_fk
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT provider_credentials_identity_fk
        FOREIGN KEY (external_identity_id) REFERENCES external_identities (external_identity_id) ON DELETE CASCADE,
    CONSTRAINT provider_credentials_identity_uk UNIQUE (external_identity_id),
    CONSTRAINT provider_credentials_provider_check CHECK (provider IN ('GITHUB')),
    CONSTRAINT provider_credentials_status_check CHECK (credential_status IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
    CONSTRAINT provider_credentials_iv_length_check CHECK (octet_length(access_token_iv) = 12)
);

CREATE INDEX provider_credentials_user_status_idx
    ON provider_credentials (user_id, credential_status);
