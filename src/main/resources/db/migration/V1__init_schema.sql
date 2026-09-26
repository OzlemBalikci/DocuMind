CREATE EXTENSION IF NOT EXISTS vector;


CREATE TABLE users (
                       id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       email           VARCHAR(255) NOT NULL,
                       password_hash   VARCHAR(100) NOT NULL,
                       full_name       VARCHAR(100) NOT NULL,
                       role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
                       created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE UNIQUE INDEX ux_users_email_lower ON users (LOWER(email));


CREATE TABLE documents (
                           id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           user_id            BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                           title              VARCHAR(255) NOT NULL,
                           original_filename  VARCHAR(255) NOT NULL,
                           content_type       VARCHAR(100) NOT NULL,
                           size_bytes         BIGINT       NOT NULL,
                           checksum_sha256    CHAR(64)     NOT NULL,
                           storage_path       VARCHAR(500) NOT NULL,
                           status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                           page_count         INTEGER,
                           chunk_count        INTEGER      NOT NULL DEFAULT 0,
                           embedding_model    VARCHAR(100),
                           error_message      VARCHAR(500),
                           created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
                           updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
                           indexed_at         TIMESTAMPTZ,
                           CONSTRAINT chk_documents_status CHECK (status IN ('PENDING', 'PARSING', 'EMBEDDING', 'READY', 'FAILED')),
                           CONSTRAINT chk_documents_type   CHECK (content_type IN ('application/pdf', 'text/plain', 'text/markdown')),
                           CONSTRAINT chk_documents_size   CHECK (size_bytes > 0),
                           CONSTRAINT ux_documents_user_checksum UNIQUE (user_id, checksum_sha256)
);

CREATE INDEX ix_documents_user_created ON documents (user_id, created_at DESC);
CREATE INDEX ix_documents_status_created ON documents (status, created_at);


CREATE TABLE document_chunks (
                                 id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 document_id  BIGINT      NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
                                 chunk_index  INTEGER     NOT NULL,
                                 content      TEXT        NOT NULL,
                                 token_count  INTEGER,
                                 page_number  INTEGER,
                                 metadata     JSONB       NOT NULL DEFAULT '{}'::jsonb,
                                 embedding    VECTOR(768),
                                 created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 CONSTRAINT ux_chunks_document_index UNIQUE (document_id, chunk_index),
                                 CONSTRAINT chk_chunks_content CHECK (length(content) > 0)
);

CREATE INDEX ix_chunks_document ON document_chunks (document_id);

CREATE INDEX ix_chunks_embedding_hnsw ON document_chunks
    USING hnsw (embedding vector_cosine_ops);


CREATE INDEX ix_chunks_pending_embedding ON document_chunks (document_id)
    WHERE embedding IS NULL;


CREATE TABLE conversations (
                               id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                               title       VARCHAR(200) NOT NULL,
                               created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                               updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_conversations_user_updated ON conversations (user_id, updated_at DESC);


CREATE TABLE messages (
                          id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          conversation_id    BIGINT       NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
                          role               VARCHAR(20)  NOT NULL,
                          content            TEXT,
                          status             VARCHAR(20)  NOT NULL DEFAULT 'DONE',
                          model              VARCHAR(100),
                          prompt_tokens      INTEGER,
                          completion_tokens  INTEGER,
                          latency_ms         INTEGER,
                          error_message      VARCHAR(500),
                          created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
                          updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
                          answered_at        TIMESTAMPTZ,
                          CONSTRAINT chk_messages_role   CHECK (role IN ('USER', 'ASSISTANT')),
                          CONSTRAINT chk_messages_status CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED')),
                          CONSTRAINT chk_messages_user_done CHECK (role <> 'USER' OR (status = 'DONE' AND content IS NOT NULL)),
                          CONSTRAINT chk_messages_done_content CHECK (status <> 'DONE' OR content IS NOT NULL)
);

CREATE INDEX ix_messages_conversation ON messages (conversation_id, created_at);

CREATE TABLE message_citations (
                                   id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   message_id  BIGINT        NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
                                   chunk_id    BIGINT        NOT NULL REFERENCES document_chunks (id) ON DELETE CASCADE,
                                   rank_order  SMALLINT      NOT NULL,
                                   similarity  NUMERIC(6, 5) NOT NULL,
                                   CONSTRAINT ux_citations_message_rank UNIQUE (message_id, rank_order),
                                   CONSTRAINT chk_citations_similarity  CHECK (similarity BETWEEN -1 AND 1),
                                   CONSTRAINT chk_citations_rank        CHECK (rank_order > 0)
);

CREATE INDEX ix_citations_message ON message_citations (message_id);


CREATE TABLE processing_jobs (
                                 id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 document_id    BIGINT       REFERENCES documents (id) ON DELETE CASCADE,
                                 message_id     BIGINT       REFERENCES messages (id) ON DELETE CASCADE,
                                 type           VARCHAR(30)  NOT NULL,
                                 status         VARCHAR(20)  NOT NULL DEFAULT 'QUEUED',
                                 attempts       SMALLINT     NOT NULL DEFAULT 0,
                                 error_message  VARCHAR(500),
                                 created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                 updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                 started_at     TIMESTAMPTZ,
                                 finished_at    TIMESTAMPTZ,
                                 CONSTRAINT chk_jobs_type   CHECK (type IN ('PARSE_DOCUMENT', 'EMBED_CHUNKS', 'ANSWER_QUESTION')),
                                 CONSTRAINT chk_jobs_status CHECK (status IN ('QUEUED', 'RUNNING', 'DONE', 'FAILED')),
                                 CONSTRAINT chk_jobs_target CHECK (
                                     (type IN ('PARSE_DOCUMENT', 'EMBED_CHUNKS') AND document_id IS NOT NULL AND message_id IS NULL)
                                         OR (type = 'ANSWER_QUESTION' AND message_id IS NOT NULL AND document_id IS NULL)
                                     )
);

CREATE INDEX ix_jobs_document ON processing_jobs (document_id);
CREATE INDEX ix_jobs_status_created ON processing_jobs (status, created_at);


CREATE TABLE llm_usage_logs (
                                id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                user_id             BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                document_id         BIGINT       REFERENCES documents (id) ON DELETE SET NULL,
                                message_id          BIGINT       REFERENCES messages (id) ON DELETE SET NULL,
                                job_id              BIGINT       REFERENCES processing_jobs (id) ON DELETE SET NULL,
                                operation           VARCHAR(30)  NOT NULL,
                                model               VARCHAR(100) NOT NULL,
                                prompt_tokens       INTEGER,
                                completion_tokens   INTEGER,
                                embedding_count     INTEGER,
                                latency_ms          INTEGER      NOT NULL,
                                success             BOOLEAN      NOT NULL,
                                error_message       VARCHAR(500),
                                created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                CONSTRAINT chk_usage_operation CHECK (operation IN ('EMBED_DOCUMENT', 'EMBED_QUERY', 'CHAT_COMPLETION'))
);

CREATE INDEX ix_usage_user_created ON llm_usage_logs (user_id, created_at);
