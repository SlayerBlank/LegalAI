# Chunks por caracteres

`DocumentChunk` guarda fragmentos derivados del texto extraido en
`document_chunks`, con FK al documento y UNIQUE `(document_id, chunk_index)`.
No contiene embeddings, vectores ni integraciones de IA.

Antes de iniciar en una base existente, ejecutar
`database/migrations/20261001_document_chunks.sql`. La FK elimina los chunks
cuando se elimina el documento. No se modifican las filas existentes de documents.

## Configuracion

```properties
legalai.rag.chunk-size=${LEGALAI_CHUNK_SIZE:2500}
legalai.rag.chunk-overlap=${LEGALAI_CHUNK_OVERLAP:300}
```

La aplicacion rechaza al arrancar `chunkSize <= 0`, `overlap < 0` y
`overlap >= chunkSize`. El prefijo de configuracion no implica que exista RAG.

## Algoritmo y offsets

Se normalizan CRLF/CR a LF, secuencias de espacios horizontales y cuatro o mas
saltos de linea a tres. Se recortan extremos generales sin reescribir el texto.
El extractedText original no se modifica al generar chunks.

Los offsets son indices UTF-16 sobre el texto normalizado: inicio inclusivo y
fin exclusivo. `content = normalized.substring(charStart, charEnd).trim()`.
La ventana busca hacia atras, por prioridad: parrafo, linea, punto y espacio.
Solo se ajusta si conserva al menos media ventana y avanza mas que el overlap
y que el final anterior. Sin limite adecuado, se utiliza el tamano configurado.
El siguiente inicio es `end - overlap`; no se emiten ventanas vacias ni se genera
otra ventana al alcanzar el final. Los indices se asignan desde cero sin huecos.

## Endpoints y prueba en Swagger

1. Login y Authorize con JWT.
2. Subir y procesar un PDF con suficiente texto para generar varios chunks.
3. `POST /api/documents/{documentId}/chunks`, sin cuerpo: HTTP 200 con
   `documentId`, `chunksCreated`, `chunkSize` y `overlap`.
4. `GET /api/documents/{documentId}/chunks`: HTTP 200, lista por chunkIndex ASC
   con chunkId, documentId, content, charStart y charEnd.
5. Repetir POST: sustituye los fragmentos, sin duplicar indices. Los chunkId
   pueden cambiar; no se garantiza estabilidad de estos IDs entre regeneraciones.

Ambos endpoints validan ownership mediante documento, expediente y usuario
autenticado. Documento ajeno/inexistente: 404; sin JWT: 401; POST con estado
distinto de PROCESSED o sin texto util: 400. Un error de generacion/persistencia
devuelve 500 con mensaje publico y causa real en los logs.

Una transaccion bloquea el documento, hace DELETE de sus chunks, saveAll y
auditoria `GENERATE_DOCUMENT_CHUNKS` (usuario, documento y cantidad, sin texto).
Si falla la persistencia o auditoria, se conservan los chunks anteriores.
Dos regeneraciones simultaneas se serializan por el bloqueo del documento.
El estado PROCESSED se conserva al generar chunks.

Al iniciar una nueva extraccion PDF, se invalidan los chunks anteriores en la
misma transaccion que limpia extractedText. Se deben generar nuevamente al
terminar esa extraccion. Esto evita exponer fragmentos de una version anterior.

```sql
SELECT chunk_id, document_id, chunk_index, content, char_start, char_end, created_at
FROM document_chunks
WHERE document_id = 8
ORDER BY chunk_index;
```

Prueba HTTP local ejecutada: documento 8, texto de 13599 caracteres, 7 chunks,
overlap 300, offsets e indices comprobados y regeneracion sin duplicados.
Swagger UI y OpenAPI comprobados por HTTP; no se ejecuto la interfaz visual.
