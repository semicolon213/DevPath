ALTER TABLE user_preferences
    ADD COLUMN catalog_version VARCHAR(64) NOT NULL DEFAULT 'v1';

ALTER TABLE user_preferences
    ALTER COLUMN catalog_version DROP DEFAULT;
