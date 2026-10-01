# Embeddings y busqueda semantica

Se implementan tres endpoints protegidos por el JWT existente:

| Metodo | Ruta | Resultado |
| --- | --- | --- |
| POST | `/api/documents/{documentId}/embeddings?force=false` | documentId, chunksProcessed, embeddingsGenerated |
| POST | `/api/documents/{documentId}/search` | Lista de chunks ordenada por distancia coseno ascendente |
| POST | `/api/cases/{caseId}/search` | Lista entre todos los documentos del expediente |

Body de busqueda: `{"query":"Que potencia optica tiene el Falcon A1 Pro?","topK":5}`.
`query` es obligatoria y no puede estar en blanco; `topK` debe ser de 1 a 20,
con default 5 cuando se omite. Cada resultado incluye chunkId, documentId,
chunkIndex, content completo y distance. Menor distancia significa mayor similitud.
Sin embeddings compatibles, el resultado es `[]`.

## Modelo y configuracion

```properties
gemini.embedding.model=${GEMINI_EMBEDDING_MODEL:gemini-embedding-001}
legalai.embedding.dimension=${LEGALAI_EMBEDDING_DIMENSION:768}
legalai.embedding.batch-size=${LEGALAI_EMBEDDING_BATCH_SIZE:1}
```

Se reutilizan `geminiWebClient`, base URL REST `v1beta`, API key del entorno/local
y timeouts existentes. GeminiEmbeddingServiceImpl usa `embedContent` y
`batchEmbedContents`, sin modificar IAService ni el modelo generativo.
Los documentos usan RETRIEVAL_DOCUMENT; las preguntas, RETRIEVAL_QUERY.
Se solicita explicitamente outputDimensionality=768 y se valida la respuesta;
no se asume que el proveedor siempre devuelve la longitud solicitada.
Los vectores se normalizan y se rechazan valores no numericos, no finitos,
vectores cero y dimensiones incorrectas, incluso si provienen de otro
EmbeddingService. Cada fila registra el modelo para evitar mezclar espacios.

Fuentes verificadas:

- [Google: modelos, dimensiones y normalizacion](https://ai.google.dev/gemini-api/docs/embeddings).
- [Google: contrato REST embedContent y batchEmbedContents](https://ai.google.dev/api/embeddings).
- [pgvector: almacenamiento vector y operador de coseno](https://github.com/pgvector/pgvector).

gemini-embedding-001 permite 128 a 3072 dimensiones; 768 es una dimension
recomendada. La implementacion acepta ese rango pero comprueba que coincida
con vector(N) en PostgreSQL antes de invocar Gemini. El unico modelo habilitado
es gemini-embedding-001: cambiar a otro requiere verificar su contrato de tareas
y regenerar los embeddings; no basta con cambiar el nombre en una variable.

## PostgreSQL y Railway

Antes de modificar la base:

```sql
SELECT name, default_version, installed_version
FROM pg_available_extensions WHERE name = 'vector';
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
```

Ejecutar el archivo completo `database/migrations/20261002_chunk_embeddings.sql`
con `psql -X -v ON_ERROR_STOP=1 -f ...`. El SQL principal es:

```sql
BEGIN;
CREATE EXTENSION IF NOT EXISTS vector;
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS embedding vector(768);
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS embedding_model varchar(120);
COMMIT;
```

El archivo tambien comprueba que una columna preexistente tenga exactamente
vector(768); un conflicto aborta la transaccion. No recrea tablas ni elimina datos.
Para otra dimension, adaptar la migracion y la propiedad conjuntamente ANTES
de ejecutarla. No redimensionar embeddings existentes sin una migracion expresa.

Se mantiene JPA para los campos originales de DocumentChunk y JdbcTemplate para
las columnas embedding/embedding_model. Se usa el driver PostgreSQL existente,
con parametros preparados y CAST(? AS vector); el literal viaja como parametro
JDBC pero se almacena como vector nativo, nunca como TEXT/JSON. No se agrega una
dependencia ORM/vector adicional. La pila comprobada es Spring Boot 4.1.1,
Hibernate 7.4.5.Final, PostgreSQL 18.3, pgvector 0.8.1. El proyecto sigue compilando
con release Java 21; las ejecuciones de esta sesion usaron el JDK local 26.0.1.

Railway: la disponibilidad depende de la imagen de PostgreSQL desplegada.
Ver [Railway PostgreSQL](https://docs.railway.com/databases/postgresql) y
[gestion de extensiones](https://docs.railway.com/databases/database-view).
Ejecutar las consultas anteriores en EL servicio existente. Si pgvector aparece,
aplicar el mismo archivo SQL con el propietario de la base. Si no aparece, el
SQL no instala los binarios y se necesita habilitar pgvector en esa imagen.
No se creo otro PostgreSQL ni se modifico el servicio desplegado. En esta sesion
no se encontro conexion Railway disponible: su activacion queda sin verificar.

## Transacciones, concurrencia y errores

La generacion autentica al usuario y bloquea el documento usando la consulta
existente `findOwnedForProcessing`, filtrada por propietario del expediente.
Comparte bloqueo con extraccion y chunking para evitar persistir vectores de
chunks obsoletos. Mantiene una transaccion por documento (incluidas las llamadas
HTTP): los cambios se confirman juntos. Si falla Gemini o una escritura, TODOS
los cambios se revierten, incluidos los de force=true. Las llamadas ya realizadas
a Gemini no se pueden deshacer y podrian haber consumido cuota.

chunksProcessed cuenta los chunks examinados; embeddingsGenerated cuenta los
nuevos o reemplazados. force=false omite vectores validos del modelo actual;
force=true reemplaza todos. Documento sin chunks: 400. Ajeno/inexistente: 404.
JWT ausente/invalido: 401. Fallo de Gemini, timeout, 429, 5xx o vector invalido:
503 con mensaje controlado e IDs afectados, sin texto privado ni causas externas.
El batch default 1 permite identificar un unico chunk; con lotes mayores se
reportan los IDs del lote afectado. No hay reintentos automaticos ni fallback a
otro modelo. Se puede reintentar la operacion mas tarde.

Los lotes se procesan secuencialmente; batch-size 2..16 usa batchEmbedContents.
El cliente permite solo una llamada simultanea por instancia, incluso con
solicitudes concurrentes. Varias replicas requieren coordinar cuota externamente.
Los documentos muy grandes mantienen el bloqueo/transaccion durante las llamadas;
esta fase es sincrona y el timeout del proxy puede expirar antes de terminar.

La busqueda verifica ownership y la consulta SQL vuelve a filtrar por usuario
y documento/expediente. Filtra vectores del modelo/dimension actuales y ordena
por `<=>`, con chunk_id como desempate. No acepta userId del frontend. Es exacta,
sin HNSW/IVFFlat; considerar un indice vectorial y sus limites de dimensiones
cuando el volumen lo justifique. Regenerar chunks elimina sus embeddings y exige
volver a ejecutar el endpoint de generacion.

## Verificacion realizada (2026-09-30)

- Base local LegalBD: extension vector 0.8.1 activada y migracion confirmada.
- Base legalai_test: misma migracion aplicada; tests usan EmbeddingService mock.
- Suite completa: 127 tests PASS, 0 fallos, 0 errores, 0 omitidos (BUILD SUCCESS).
  Incluye validacion DTO, errores de proveedor, ownership/JWT, filtros SQL,
  orden/topK, generacion selectiva, force y rollback real de transacciones.
- Migracion repetida sobre LegalBD: PASS; conserva los 15 chunks y el embedding.
- Se reparo la instalacion local existente en `C:/Users/Public/pgvector`:
  copia de extension/vector.sql a extension/vector--0.8.1.sql y module_pathname
  apuntando a la DLL existente. Control anterior conservado en
  extension/vector.control.before-legalai. No se descargaron binarios.
- Documento real 5, PDF Falcon, ya extraido y con un chunk: POST embeddings
  devolvio 200, chunksProcessed=1, embeddingsGenerated=1. Repetir devolvio 0.
- Busqueda real: pregunta sobre potencia optica del Falcon A1 Pro; resultado
  chunkId=22, documentId=5, distance=0.22872428215549434, contiene 20W.
- Busqueda por caseId=1: 200 y un resultado del mismo documento.
- Un unico chunk limita la evaluacion de ranking; el orden entre multiples
  vectores, aislamiento de expedientes, topK y rollback se prueban con vectores
  deterministas en PostgreSQL real. No equivale a un benchmark de retrieval.
- No se implementaron respuestas RAG, chat, historial, citaciones ni OCR.

Para repetir: iniciar la app, autenticar en Swagger, elegir un PDF ya chunked,
POST embeddings, POST search con una pregunta cuya respuesta este en el PDF,
y comprobar el content de los primeros resultados. Guardar la pregunta,
informacion esperada y posiciones relevantes; no evaluar una respuesta generativa.

Tests: `mvnw.cmd test`, con TEST_DB_URL/TEST_DB_USERNAME/TEST_DB_PASSWORD apuntando
a una base exclusiva de pruebas preparada con las migraciones. No apuntar los
tests al servicio de produccion: la suite existente crea y elimina sus fixtures.
Los tests de proveedor usan respuestas HTTP simuladas, nunca Gemini real.
