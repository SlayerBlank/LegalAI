-- Run after document_chunks exists. Default Gemini outputDimensionality = 768.
-- For a different configured dimension, adapt BOTH this migration and the application
-- before the first execution. Never silently resize a populated embedding column.
BEGIN;
CREATE EXTENSION IF NOT EXISTS vector;
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS embedding vector(768);
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS embedding_model varchar(120);
DO $$
BEGIN
    IF (SELECT format_type(atttypid, atttypmod) FROM pg_attribute
        WHERE attrelid = 'document_chunks'::regclass AND attname = 'embedding') <> 'vector(768)' THEN
        RAISE EXCEPTION 'Existing embedding dimension differs from vector(768); migration aborted';
    END IF;
END $$;
COMMIT;
