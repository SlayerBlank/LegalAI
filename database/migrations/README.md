# Correccion del esquema de documents

## Embeddings pgvector

Despues de crear `document_chunks`, ejecutar `20261002_chunk_embeddings.sql`.
Agrega `embedding vector(768)` y `embedding_model` sin borrar filas. Requiere
pgvector instalado en el servidor y permisos para `CREATE EXTENSION`.
Hibernate no administra estas dos columnas; no basta con `ddl-auto=update`.
Ver `docs/semantic-search.md` para Railway, configuracion y pruebas.

Ejecutar manualmente `20260929_drop_legacy_documents_processed.sql` en la base
PostgreSQL configurada para la aplicacion, con un usuario propietario de la tabla:

```powershell
psql -h localhost -U postgres -d LegalBD -X -v ON_ERROR_STOP=1 -f database/migrations/20260929_drop_legacy_documents_processed.sql
```

La migracion es transaccional e idempotente. Elimina solo la columna legacy
`processed`, ausente en `Documento`; su restriccion NOT NULL sin default impedia
el INSERT del upload. El estado actual se almacena en `processing_status` con
valores `UPLOADED` o `ERROR`. No recrea la tabla ni elimina filas.

`extracted_text` tambien esta ausente en la entidad actual, pero se conserva
para no descartar informacion potencialmente util. No se agrega procesamiento
de PDF ni extraccion de texto.

Hibernate `ddl-auto=update` no elimina esta columna antigua. Este proyecto no
configura un ejecutor automatico de migraciones; aplicar el script una vez en
cada base antigua afectada, antes de volver a probar el upload con `CONTRACT`.

## Extraccion de texto PDF

Antes de iniciar la version con PDFBox, ejecutar tambien
`20260930_document_text_processing.sql` en cada base existente (incluida Railway).
Conserva el contenido de `extracted_text`, crea la columna TEXT si falta y amplia
la restriccion `documents_processing_status_check` a `UPLOADED`, `PROCESSING`,
`PROCESSED` y `ERROR`. No confiar en `ddl-auto=update` para migrar constraints.

```powershell
psql -h localhost -U postgres -d LegalBD -X -v ON_ERROR_STOP=1 -f database/migrations/20260930_document_text_processing.sql
```

## Chunks de documentos

Ejecutar `20261001_document_chunks.sql` antes de iniciar la version con chunks,
tanto localmente como en Railway. Crea `document_chunks` con contenido TEXT,
FK a documents, UNIQUE por documento e indice y validaciones de rango.
La FK usa ON DELETE CASCADE para no bloquear la eliminacion de documentos.

```powershell
psql -h localhost -U postgres -d LegalBD -X -v ON_ERROR_STOP=1 -f database/migrations/20261001_document_chunks.sql
```

## Sesiones y mensajes de chat

Ejecutar `20261003_chat_sessions_documents.sql` antes de usar `/api/chat/**` en
cualquier base existente (local y Railway). La entidad `Mensajes` ya existia, pero
la relacion con la sesion estaba comentada y `chat_messages` no tenia `session_id`.

```powershell
psql -h localhost -U postgres -d LegalBD -X -v ON_ERROR_STOP=1 -f database/migrations/20261003_chat_sessions_documents.sql
```

Que hace, sin recrear tablas ni borrar filas:

- Anade `chat_sessions.document_id` (bigint, NULL) con FK `ON DELETE SET NULL`:
  una sesion puede acotarse a un documento sin que borrar el documento borre la
  conversacion. `document_scope_required` conserva el hecho de que esa sesion
  era documentalmente acotada, para que borrar su documento nunca la amplie al
  expediente entero. La migracion marca los documentos todavia asociados; si
  una ejecucion anterior ya puso `document_id` en NULL al borrar un documento,
  esa asociacion perdida no puede inferirse automaticamente.
- Anade `chat_messages.session_id` y `chat_messages.client_message_id`, y convierte
  `session_id` en NOT NULL solo si no hay mensajes huerfanos; si los hay emite un
  `RAISE NOTICE` y deja la columna NULL en lugar de borrar historial.
- `chat_messages.session_id` con `ON DELETE CASCADE`, igual que `ai_citations.message_id`.
- `chat_sessions.case_id` y `chat_sessions.user_id` con `ON DELETE CASCADE` para no
  bloquear el borrado de expedientes o usuarios.
- Indices de orden `(session_id, created_at, message_id)` y de listado por usuario,
  mas un indice unico parcial `(session_id, client_message_id)` que deduplica
  reintentos HTTP con el mismo identificador. Si encuentra claves existentes
  duplicadas, la migracion avisa y omite solo ese indice para no descartar filas;
  limpie/regularice los duplicados segun la politica de retencion y vuelva a ejecutarla.
- `chat_messages.reply_to_message_id` asocia cada respuesta a su pregunta exacta,
  y un indice unico evita guardar dos respuestas al mismo mensaje incluso si las
  peticiones coinciden. `response_metadata` conserva el proveedor/modelo y los
  fragmentos realmente consultados para que un reintento devuelva las mismas
  fuentes, sin presentarlas como citas verificadas.
- `CHECK sender_type IN ('USER','ASSISTANT')`, aplicado solo si no hay valores
  heredados incompatibles.

`ddl-auto=update` no alcanza: no crea indices, no convierte claves foraneas en cascada
y no puede decidir un NOT NULL sobre datos existentes. Aplicar el script una vez en
cada base antes de arrancar la version con chat.
