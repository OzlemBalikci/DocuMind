CREATE TABLE documents (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id           UUID         NOT NULL,
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
    CONSTRAINT ux_documents_owner_checksum UNIQUE (owner_id, checksum_sha256)
);

CREATE INDEX ix_documents_owner_created ON documents (owner_id, created_at DESC);
CREATE INDEX ix_documents_status_created ON documents (status, created_at);
