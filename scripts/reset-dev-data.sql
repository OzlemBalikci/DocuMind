-- Dev only: delete all application data, keep schema + Flyway history.
-- Usage (from project root, Postgres container running):
--   docker exec -i documind-db psql -U documind_app -d documind_db -f - < scripts/reset-dev-data.sql
-- Or paste into pgAdmin / DBeaver.

TRUNCATE TABLE
	message_citations,
	messages,
	conversations,
	document_chunks,
	documents,
	processing_jobs,
	llm_usage_logs,
	refresh_tokens,
	users
RESTART IDENTITY CASCADE;
