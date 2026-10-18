ALTER TABLE password_reset_codes
    ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE password_reset_codes
    ADD CONSTRAINT chk_password_reset_failed_attempts CHECK (failed_attempts >= 0);
