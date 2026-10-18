ALTER TABLE documents
    ADD COLUMN original_filename VARCHAR(255),
    ADD COLUMN content_type      VARCHAR(127),
    ADD COLUMN size_bytes        BIGINT;

COMMENT ON COLUMN documents.original_filename IS 'Client original file name';
COMMENT ON COLUMN documents.content_type IS 'MIME type at upload';
COMMENT ON COLUMN documents.size_bytes IS 'File size in bytes';