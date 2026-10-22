\connect user_db
TRUNCATE TABLE user_profiles;

\connect document_db
TRUNCATE TABLE documents;

\connect chat_db
TRUNCATE TABLE messages, conversations;
