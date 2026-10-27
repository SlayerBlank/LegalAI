-- PREPARED ONLY. Execute only after explicit approval, backup and consumer review.
-- Target: verified public.users. status remains the sole state source.
-- Run with a client that stops on errors (psql: -v ON_ERROR_STOP=1).
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

LOCK TABLE public.users IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users'
          AND column_name = 'active' AND data_type = 'boolean'
          AND is_nullable = 'NO' AND column_default IS NULL
    ) THEN
        RAISE EXCEPTION 'Unexpected users.active definition or migration already applied; review schema';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users'
          AND column_name = 'status' AND data_type = 'character varying'
          AND character_maximum_length = 30 AND is_nullable = 'NO'
    ) THEN
        RAISE EXCEPTION 'Expected users.status varchar(30) NOT NULL';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.users
        WHERE status IS NULL OR active IS DISTINCT FROM (status = 'ACTIVE')
    ) THEN
        RAISE EXCEPTION 'active/status inconsistency; migration aborted without changing users';
    END IF;

    -- Reject even automatic dependencies (e.g. indexes), except active's own NOT NULL.
    IF EXISTS (
        SELECT 1 FROM pg_depend d
        WHERE d.refclassid = 'pg_class'::regclass
          AND d.refobjid = 'public.users'::regclass
          AND d.refobjsubid = (
              SELECT attnum FROM pg_attribute
              WHERE attrelid = 'public.users'::regclass AND attname = 'active'
                AND NOT attisdropped
          )
          AND NOT (
              d.classid = 'pg_constraint'::regclass
              AND EXISTS (
                  SELECT 1 FROM pg_constraint c
                  WHERE c.oid = d.objid AND c.conrelid = 'public.users'::regclass
                    AND c.contype = 'n'
              )
          )
    ) THEN
        RAISE EXCEPTION 'Dependency on users.active detected; review before dropping';
    END IF;
END $$;

ALTER TABLE public.users DROP COLUMN active RESTRICT;

COMMIT;
