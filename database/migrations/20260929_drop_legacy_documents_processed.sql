-- Documento uses processing_status (UPLOADED / ERROR), not processed.
-- Preserve documents, processing_status and legacy extracted_text.
BEGIN;

ALTER TABLE public.documents DROP COLUMN IF EXISTS processed;

COMMIT;
