# Extraccion de texto PDF

Dependencia: Apache PDFBox 3.0.8, `Loader.loadPDF` y `PDFTextStripper`.
Solo se extrae texto seleccionable. No hay OCR, chunks, embeddings ni RAG.

Aplicar `database/migrations/20260930_document_text_processing.sql` antes de
iniciar contra una base existente. `extracted_text` se mapea como PostgreSQL TEXT.

## Prueba en Swagger

1. Iniciar sesion y usar Authorize con el JWT.
2. Subir un PDF con texto mediante `POST /api/cases/{caseId}/documents/upload`
   y categoria `CONTRACT`. Anotar `documentId` (HTTP 201, `UPLOADED`).
3. Ejecutar `POST /api/documents/{documentId}/process`, sin cuerpo ni archivo.
   Devuelve HTTP 200 y los metadatos existentes mas `hasExtractedText` y
   `extractedTextLength`. Nunca incluye el texto completo.
4. Consultar `GET /api/documents/{documentId}/text` para obtener
   `{ "documentId": 2, "text": "..." }`. Antes de extraer, `text` es null.

Ambos endpoints requieren JWT y ownership por el expediente. Documentos ajenos
o inexistentes devuelven 404; una extraccion ya en curso devuelve 409.
Texto vacio o permisos PDF que impiden extraer devuelven 400; archivo ausente,
referencia invalida, PDF corrupto, errores de lectura o BD devuelven 500 con un
mensaje publico y causa en los logs del servidor.

## Storage y transacciones

Se conserva `LEGALAI_STORAGE_PATH`. Se aceptan referencias relativas `UUID.pdf`
y `uploads/UUID.pdf` (tambien el nombre del directorio configurado como prefijo).
Se rechazan rutas absolutas, traversal y enlaces que resuelvan fuera del storage.
El archivo debe estar disponible en el filesystem local o de Railway.

El procesamiento es sincrono. Una transaccion corta bloquea el documento y
confirma `PROCESSING`; PDFBox se ejecuta sin transaccion SQL. Otra transaccion
guarda texto, `PROCESSED` y auditoria `PROCESS_DOCUMENT`, sin texto en audit_logs.
Los fallos posteriores al inicio intentan persistir `ERROR` y limpiar el texto
en una transaccion independiente. Si la BD no permite guardar ERROR, se registra
la causa y la necesidad de revision. Una caida abrupta del proceso puede dejar
`PROCESSING`; esta fase no incluye recuperacion automatica ni trabajos en cola.

`DocumentoProcessingService` separa esta coordinacion del servicio de upload.
Las pruebas usan PDFs generados con PDFBox y storage temporal, y verifican
ownership, estados, errores de lectura/BD/auditoria y rollback en PostgreSQL.
