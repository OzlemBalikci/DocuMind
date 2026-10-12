CREATE TABLE refresh_tokens (
                                id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                family_id       UUID         NOT NULL,
                                token_hash      CHAR(64)     NOT NULL,
                                expires_at      TIMESTAMPTZ  NOT NULL,
                                revoked_at      TIMESTAMPTZ,
                                replaced_by_id  BIGINT       REFERENCES refresh_tokens (id),
                                user_agent      VARCHAR(255),
                                ip_address      INET,
                                created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                CONSTRAINT ux_refresh_tokens_hash UNIQUE (token_hash),
                                CONSTRAINT ck_refresh_tokens_expiry CHECK (expires_at > created_at)
);

CREATE INDEX ix_refresh_tokens_user    ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family  ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_expires ON refresh_tokens (expires_at);
