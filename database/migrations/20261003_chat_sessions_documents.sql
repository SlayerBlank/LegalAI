-- Chat persistente: alcance opcional por documento, mensajes ligados a su sesion,
-- idempotencia de reintentos HTTP y borrado en cascada de conversaciones.
--
-- No recrea tablas, no elimina filas, no usa DROP TABLE y no altera textos juridicos.
-- Es idempotente: puede ejecutarse varias veces sobre la misma base.
-- ddl-auto=update NO alcanza para esto: no crea indices ni convierte claves foraneas
-- existentes en cascada, y no puede decidir de forma segura un NOT NULL sobre datos.
BEGIN;

-- ---------------------------------------------------------------------------
-- 1) Alcance opcional por documento en chat_sessions
-- ---------------------------------------------------------------------------
ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS document_id bigint;
ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS document_scope_required boolean NOT NULL DEFAULT false;
UPDATE chat_sessions SET document_scope_required = true
WHERE document_id IS NOT NULL AND document_scope_required = false;

DO $$
DECLARE
    fk record;
BEGIN
    FOR fk IN SELECT conname FROM pg_constraint
              WHERE conrelid = 'chat_sessions'::regclass
                AND contype = 'f'
                AND pg_get_constraintdef(oid) ILIKE '%documents(document_id)%'
              LOOP
        EXECUTE format('ALTER TABLE chat_sessions DROP CONSTRAINT %I', fk.conname);
    END LOOP;
    -- SET NULL: borrar un documento nunca borra conversaciones ni sus mensajes.
    EXECUTE 'ALTER TABLE chat_sessions ADD CONSTRAINT chat_sessions_document_id_fkey '
         || 'FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE SET NULL';
END $$;

CREATE INDEX IF NOT EXISTS idx_chat_sessions_user_updated
    ON chat_sessions (user_id, updated_at DESC, session_id DESC);
CREATE INDEX IF NOT EXISTS idx_chat_sessions_case_updated
    ON chat_sessions (case_id, updated_at DESC, session_id DESC);

-- ---------------------------------------------------------------------------
-- 2) chat_sessions case_id / user_id en cascada
--    Sin esto, borrar un expediente o un usuario bloquearia por clave foranea.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    fk record;
BEGIN
    FOR fk IN SELECT conname FROM pg_constraint
              WHERE conrelid = 'chat_sessions'::regclass AND contype = 'f'
                AND pg_get_constraintdef(oid) ILIKE '%cases(case_id)%'
              LOOP
        EXECUTE format('ALTER TABLE chat_sessions DROP CONSTRAINT %I', fk.conname);
    END LOOP;
    EXECUTE 'ALTER TABLE chat_sessions ADD CONSTRAINT chat_sessions_case_id_fkey '
         || 'FOREIGN KEY (case_id) REFERENCES cases(case_id) ON DELETE CASCADE';

    FOR fk IN SELECT conname FROM pg_constraint
              WHERE conrelid = 'chat_sessions'::regclass AND contype = 'f'
                AND pg_get_constraintdef(oid) ILIKE '%users(user_id)%'
              LOOP
        EXECUTE format('ALTER TABLE chat_sessions DROP CONSTRAINT %I', fk.conname);
    END LOOP;
    EXECUTE 'ALTER TABLE chat_sessions ADD CONSTRAINT chat_sessions_user_id_fkey '
         || 'FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE';
END $$;

-- ---------------------------------------------------------------------------
-- 3) Mensajes ligados a su sesion
-- ---------------------------------------------------------------------------
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS session_id bigint;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS client_message_id varchar(64);
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS reply_to_message_id bigint;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS response_metadata text;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM chat_messages WHERE session_id IS NULL) THEN
        -- No se borra historial existente: la columna queda NULL y se avisa al operador.
        RAISE NOTICE 'chat_messages tiene filas sin sesion; session_id queda NULL. Reviselas/backfilleelas segun la politica de retencion; la migracion no las elimina.';
    ELSE
        EXECUTE 'ALTER TABLE chat_messages ALTER COLUMN session_id SET NOT NULL';
    END IF;
END $$;

-- A response is linked to its exact user message; message ID ordering alone is
-- insufficient when requests for the same session overlap.
DO $$
DECLARE
    fk record;
BEGIN
    FOR fk IN SELECT conname FROM pg_constraint
              WHERE conrelid = 'chat_messages'::regclass
                AND contype = 'f'
                AND pg_get_constraintdef(oid) ILIKE '%reply_to_message_id%'
                AND pg_get_constraintdef(oid) ILIKE '%chat_messages(message_id)%'
              LOOP
        EXECUTE format('ALTER TABLE chat_messages DROP CONSTRAINT %I', fk.conname);
    END LOOP;
    EXECUTE 'ALTER TABLE chat_messages ADD CONSTRAINT chat_messages_reply_to_message_id_fkey '
         || 'FOREIGN KEY (reply_to_message_id) REFERENCES chat_messages(message_id) ON DELETE CASCADE';
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_messages_reply
    ON chat_messages (reply_to_message_id)
    WHERE reply_to_message_id IS NOT NULL;

DO $$
DECLARE
    fk record;
BEGIN
    FOR fk IN SELECT conname FROM pg_constraint
              WHERE conrelid = 'chat_messages'::regclass AND contype = 'f'
                AND pg_get_constraintdef(oid) ILIKE '%chat_sessions(session_id)%'
              LOOP
        EXECUTE format('ALTER TABLE chat_messages DROP CONSTRAINT %I', fk.conname);
    END LOOP;
    EXECUTE 'ALTER TABLE chat_messages ADD CONSTRAINT chat_messages_session_id_fkey '
         || 'FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE';
END $$;

-- Orden cronologico estable: created_at y luego message_id como desempate.
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_order
    ON chat_messages (session_id, created_at, message_id);

-- Reintentos HTTP con el mismo clientMessageId no duplican mensajes.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM chat_messages
               WHERE client_message_id IS NOT NULL
               GROUP BY session_id, client_message_id
               HAVING COUNT(*) > 1) THEN
        RAISE NOTICE 'Hay client_message_id duplicados; no se crea el indice unico. Revise duplicados antes de reintentar la migracion.';
    ELSE
        CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_messages_client_message
            ON chat_messages (session_id, client_message_id)
            WHERE client_message_id IS NOT NULL;
    END IF;
END $$;

-- sender_type solo admite los roles del modelo conversacional.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conrelid = 'chat_messages'::regclass
                     AND conname = 'chat_messages_sender_type_check') THEN
        IF EXISTS (SELECT 1 FROM chat_messages
                   WHERE sender_type IS NULL OR sender_type NOT IN ('USER', 'ASSISTANT')) THEN
            RAISE NOTICE 'chat_messages.sender_type tiene valores no compatibles; el CHECK USER/ASSISTANT no se aplica.';
        ELSE
            EXECUTE 'ALTER TABLE chat_messages ADD CONSTRAINT chat_messages_sender_type_check '
                 || 'CHECK (sender_type IN (''USER'', ''ASSISTANT''))';
        END IF;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 4) ai_citations en cascada con el mensaje
--    Sin esto, borrar una sesion con citas fallaria por clave foranea.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    fk record;
BEGIN
    IF to_regclass('ai_citations') IS NULL THEN
        RETURN;
    END IF;
    FOR fk IN SELECT conname FROM pg_constraint
              WHERE conrelid = 'ai_citations'::regclass AND contype = 'f'
                AND pg_get_constraintdef(oid) ILIKE '%chat_messages(message_id)%'
              LOOP
        EXECUTE format('ALTER TABLE ai_citations DROP CONSTRAINT %I', fk.conname);
    END LOOP;
    EXECUTE 'ALTER TABLE ai_citations ADD CONSTRAINT ai_citations_message_id_fkey '
         || 'FOREIGN KEY (message_id) REFERENCES chat_messages(message_id) ON DELETE CASCADE';
END $$;

COMMIT;