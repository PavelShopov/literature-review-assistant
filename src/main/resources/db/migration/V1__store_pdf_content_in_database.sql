ALTER TABLE document
    ADD COLUMN IF NOT EXISTS pdf_content BYTEA,
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS original_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS checksum_sha256 VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_document_checksum_sha256
    ON document (checksum_sha256);
