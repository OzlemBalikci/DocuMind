\connect auth_db
TRUNCATE TABLE password_reset_codes, refresh_tokens, users RESTART IDENTITY CASCADE;

\connect document_db
TRUNCATE TABLE documents RESTART IDENTITY CASCADE;

\connect chat_db
TRUNCATE TABLE messages, conversations RESTART IDENTITY CASCADE;
