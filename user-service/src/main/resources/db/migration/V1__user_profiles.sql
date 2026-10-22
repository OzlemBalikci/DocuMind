CREATE TABLE user_profiles (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100),
    quota_limit INTEGER      NOT NULL DEFAULT 100,
    quota_used  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (quota_used >= 0)
);

CREATE UNIQUE INDEX ux_user_profiles_email ON user_profiles (email);
