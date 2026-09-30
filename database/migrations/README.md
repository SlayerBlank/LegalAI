# Correccion del esquema de documents

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
