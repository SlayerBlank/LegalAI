-- Preserve chat citations when their optional document is deleted.
-- Run after the existing chat migration. No rows, tables or columns are removed.
BEGIN;
DO $$
DECLARE fk record;
BEGIN
    IF to_regclass('ai_citations') IS NULL THEN
        RAISE EXCEPTION 'ai_citations must exist before applying citation retention';
    END IF;
    FOR fk IN SELECT conname FROM pg_constraint
              WHERE conrelid = 'ai_citations'::regclass AND contype = 'f'
                AND confrelid = 'documents'::regclass
                AND conkey = ARRAY[(SELECT attnum FROM pg_attribute
                    WHERE attrelid = 'ai_citations'::regclass AND attname = 'document_id')]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE ai_citations DROP CONSTRAINT %I', fk.conname);
    END LOOP;
    ALTER TABLE ai_citations ADD CONSTRAINT ai_citations_document_id_fkey
        FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE SET NULL;
END $$;
COMMIT;
