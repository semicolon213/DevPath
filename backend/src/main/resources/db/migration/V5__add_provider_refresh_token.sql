ALTER TABLE provider_credentials
    ADD COLUMN encrypted_refresh_token BYTEA,
    ADD COLUMN refresh_token_iv BYTEA,
    ADD COLUMN refresh_token_expires_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE provider_credentials
    ADD CONSTRAINT provider_credentials_refresh_pair_check CHECK (
        (encrypted_refresh_token IS NULL AND refresh_token_iv IS NULL)
        OR
        (encrypted_refresh_token IS NOT NULL AND refresh_token_iv IS NOT NULL AND octet_length(refresh_token_iv) = 12)
    );
