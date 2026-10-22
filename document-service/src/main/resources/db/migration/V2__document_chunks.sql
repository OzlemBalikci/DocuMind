CREATE TABLE document_chunks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id  UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
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
