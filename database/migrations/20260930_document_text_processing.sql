-- Run before starting the application against an existing database.
BEGIN;
ALTER TABLE public.documents ADD COLUMN IF NOT EXISTS extracted_text TEXT;
ALTER TABLE public.documents DROP CONSTRAINT IF EXISTS documents_processing_status_check;
ALTER TABLE public.documents ADD CONSTRAINT documents_processing_status_check
    CHECK (processing_status IN ('UPLOADED', 'PROCESSING', 'PROCESSED', 'ERROR'));
COMMIT;
