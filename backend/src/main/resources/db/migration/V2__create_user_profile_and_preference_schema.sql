CREATE TABLE user_profiles (
    profile_id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    career_stage VARCHAR(32),
    bio VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT user_profiles_user_fk
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT user_profiles_career_stage_ck
        CHECK (career_stage IS NULL OR career_stage IN ('STUDENT', 'ENTRY_LEVEL', 'JUNIOR', 'MID_LEVEL', 'SENIOR'))
);

INSERT INTO user_profiles (profile_id, user_id, created_at, updated_at, version)
SELECT gen_random_uuid(), user_id, created_at, updated_at, 0
FROM users;

CREATE TABLE user_preferences (
    preference_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    preference_type VARCHAR(16) NOT NULL,
    selected_value VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL,
    selected_at TIMESTAMPTZ NOT NULL,
    superseded_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT user_preferences_user_fk
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT user_preferences_type_ck
        CHECK (preference_type IN ('CAREER', 'COMPANY')),
    CONSTRAINT user_preferences_state_ck
        CHECK ((active AND superseded_at IS NULL) OR (NOT active AND superseded_at IS NOT NULL))
);

CREATE UNIQUE INDEX user_preferences_active_type_uk
    ON user_preferences (user_id, preference_type)
    WHERE active;

CREATE INDEX user_preferences_user_history_ix
    ON user_preferences (user_id, preference_type, selected_at DESC);
