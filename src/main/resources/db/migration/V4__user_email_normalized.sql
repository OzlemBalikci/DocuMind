UPDATE users
SET email = lower(btrim(email))
WHERE email <> lower(btrim(email));

ALTER TABLE users
    ADD CONSTRAINT chk_users_email_normalized
        CHECK (email = lower(email) AND email = btrim(email));

DROP INDEX IF EXISTS ux_users_email_lower;

CREATE UNIQUE INDEX ux_users_email ON users (email);
