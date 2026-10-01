# RAG documental sin conversaciones

## Flujo y componentes reutilizados

`RAGController -> RAGServiceImpl -> SemanticSearchService -> EmbeddingService + pgvector`
y, cuando hay contexto seleccionado, `RAGServiceImpl -> IAService -> Gemini`.

Se reutilizan autenticacion JWT, UsuarioService, ownership de SemanticSearchService,
ChunkEmbeddingRepository, GeminiEmbeddingServiceImpl, IAService, geminiWebClient,
timeouts/reintentos/fallback, GlobalExceptionHandler y AuditLogService. No hay otro
retriever ni otro cliente HTTP. No se modifica ModelMapperConfig, configuracion
local, tablas de conversaciones ni el contrato de `/api/ai/test`.

La consulta vectorial existente ahora incluye tambien file_name, char_start y
char_end. Los campos anteriores de `/search` se conservan. No hay migraciones
nuevas de base de datos para RAG.

## API

| Metodo | Endpoint | Alcance |
| --- | --- | --- |
| POST | `/api/documents/{documentId}/ask` | Solo el documento propio |
| POST | `/api/cases/{caseId}/ask` | Documentos del expediente propio |

```json
{
  "question": "¿Qué diferencias hay entre la Falcon A1 y la Falcon A1 Pro?",
  "topK": 5
}
```

JWT obligatorio. question: no vacia, maximo 2000 caracteres. topK: 1..10; al
omitirlo se aplica el default configurado (5). null explicito se rechaza.
Se rechazan campos desconocidos, incluidos userId, ownerId, context y chunkIds.
El controller solo valida/delega; nunca consulta repositorios ni llama a Gemini.

Respuesta: answer, provider, model real que respondio (incluido fallback),
retrievedChunks, sourcesType y sources. sourcesType siempre es
`CONSULTED_FRAGMENTS`: son fragmentos consultados, NO citas verificadas por
afirmacion. retrievedChunks cuenta los fragmentos finalmente enviados al modelo,
despues de filtro, deduplicacion y limite; los candidatos originales y sus
distancias se registran como metadatos de diagnostico.

Cada fuente incluye reference (`F1`, `F2`...), documentId, documentName, chunkId,
chunkIndex, excerpt, distance, charStart y charEnd. excerpt contiene el fragmento
completo enviado. Los offsets provienen de los chunks reales y se refieren al
texto normalizado del chunking. No se inventan paginas. Las menciones `[F1]` en
la respuesta son producidas por Gemini: no se valida semanticamente que cada
afirmacion este sustentada, y no se presentan como citas juridicas verificadas.

Si no se recuperan fragmentos o todos se descartan, se devuelve HTTP 200:

```json
{
  "answer": "No se encontró información documental suficiente para responder.",
  "provider": null,
  "model": null,
  "retrievedChunks": 0,
  "sourcesType": "CONSULTED_FRAGMENTS",
  "sources": []
}
```

En ese caso no se llama al modelo generativo. La busqueda existente puede haber
generado el embedding de la pregunta; nunca regenera embeddings documentales.
Con contexto disponible, Gemini tambien puede reconocer insuficiencia.

## Recuperacion, contexto y seguridad

- El servicio existente comprueba propiedad del documento/expediente ANTES de
  generar el embedding de la pregunta. PostgreSQL vuelve a filtrar por usuario
  y documentId/caseId, embeddings no nulos, modelo y dimension compatibles.
- Se conserva el orden exacto por distancia coseno ascendente (menor es mejor),
  con chunkId como desempate. No se convierte la distancia en confianza.
- El umbral opcional se aplica solo si esta configurado. Sin umbral, estar entre
  los top K no implica relevancia: el prompt exige reconocer contexto insuficiente.
- El contexto es un array JSON con identificadores y contenido de cada fuente.
  El escape JSON mantiene nombres falsificados y marcadores dentro de datos.
- Se seleccionan fragmentos completos en orden de relevancia. Se omiten los que
  no caben; se consideran los siguientes candidatos. No se trunca texto juridico.
  El limite incluye metadata y caracteres escapados del JSON, no la pregunta ni
  la instruccion del sistema. La pregunta tiene su limite independiente de 2000.
- Se eliminan IDs repetidos, contenido ya incluido y fragmentos del mismo
  documento cuyo rango se solape al menos un 50% con uno ya seleccionado.
  El overlap normal de aproximadamente 12% puede mantenerse. Omitir fragmentos
  por espacio o solapamiento puede perder informacion: la respuesta no debe
  afirmar que se reviso el documento/expediente completo.
- IAContextRequestDTO es interno, no un body HTTP. Distingue instrucciones del
  sistema, contexto y pregunta. El limite de IARequestDTO (10000) sigue aplicando
  a `/api/ai/test`; no se reutiliza ese DTO para saltarse sus validaciones.
- El prompt exige fundamentacion documental, no inventar datos ni citas, indicar
  inferencias, comparar diferencias y caracteristicas compartidas, e ignorar
  instrucciones dentro de PDFs. El contexto se envia en partes de usuario,
  separado del systemInstruction. Esta mitigacion no garantiza inmunidad total
  frente a prompt injection ni elimina la necesidad de revision profesional.

## Configuracion

```properties
legalai.rag.default-top-k=${LEGALAI_RAG_TOP_K:5}
legalai.rag.max-context-chars=${LEGALAI_RAG_MAX_CONTEXT_CHARS:12000}
legalai.rag.max-cosine-distance=${LEGALAI_RAG_MAX_COSINE_DISTANCE:}
```

default-top-k acepta 1..10; max-context-chars, 256..100000. Umbral vacio lo
desactiva; si se configura debe ser finito y estar entre 0 y 2. No se propone un
umbral optimo: hace falta evaluar varias preguntas con respuestas esperadas.
Los modelos generativos y de embeddings y las claves se mantienen sin cambios.

## Errores y auditoria

401 para JWT ausente/invalido; 404 para recurso inexistente o ajeno; 400 para
entrada invalida; 503 para fallos del proveedor/retrieval. Los errores de Gemini
y las respuestas vacias conservan IAServiceException. RAGException sanitiza
fallos de embeddings, PostgreSQL/pgvector u otros fallos operativos.

Se registran scope, ID, cantidades, distancias, clase de error y SQLState cuando
existe, sin preguntas, respuestas, documentos completos, credenciales o causas
externas con datos sensibles. Gemini conserva su diagnostico HTTP existente.

RAGAuditService abre una transaccion REQUIRES_NEW y delega al AuditLogService
existente (MANDATORY); asi un fallo de la transaccion de retrieval no revierte
la auditoria. No se mantiene una transaccion abierta durante la generacion de
la respuesta; retrieval conserva su transaccion de lectura existente.
`RAG_QUERY` guarda usuario, Documento/Expediente, ID, tipo, numero de fragmentos,
SUCCESS/FAILED y created_at. No guarda pregunta ni respuesta. Solicitudes
rechazadas por JWT o validacion MVC antes del servicio no ejecutan una consulta.
Si la propia base de auditoria falla, se registra un diagnostico seguro; una
respuesta exitosa no se entrega si no se pudo registrar su auditoria.

## Verificacion

Suite: `mvnw.cmd clean test`, usando PostgreSQL exclusivo `legalai_test` preparado
con las migraciones anteriores y TEST_DB_PASSWORD desde el entorno. EmbeddingService
e IAService se mockean en integracion; el provider se prueba con HTTP simulado.
No se consume Gemini real durante tests. El proyecto compila para Java 21; en
esta maquina la ejecucion usa el JDK instalado 26.0.1.

Cobertura: ambas modalidades, JWT, recurso inexistente/ajeno, aislamiento SQL
entre usuarios/documentos/expedientes, sin chunks/embeddings/resultados, filtro,
topK, default configurable, contexto acotado, overlap, metadata real, campos
prohibidos, separacion del prompt, fallo/vacio de Gemini, errores sanitizados,
auditoria exitosa/fallida y conservacion de APIs existentes.

Prueba manual solo con clave local configurada: buscar el PDF por nombre en la
base o Swagger (no fijar su ID), comprobar PDF almacenado, texto, chunks y
embeddings; iniciar `mvnw.cmd spring-boot:run` y consultar ambos endpoints.
Preguntas: comparacion Falcon A1/A1 Pro y precio en 2035. Comprobar 10W/20W,
305x381/268x358 mm, 600 mm/s compartidos, y ausencia de precio inventado.
Swagger UI y `/v3/api-docs` deben mostrar ambos POST /ask.

Resultados ejecutados el 2026-09-30:

- `mvnw.cmd clean test`: PASS, 153 tests, 0 fallos, 0 errores, 0 omitidos.
- `mvnw.cmd spring-boot:run` (puerto de verificacion 18080): PASS.
- Swagger UI: HTTP 200; OpenAPI contiene ambos POST /ask y las rutas anteriores.
- PDF Falcon localizado por nombre: documentId=5, caseId=1, chunkId=23; archivo
  presente, estado PROCESSED, texto extraido y un embedding existente.
- Comparacion final por documento y por expediente: HTTP 200, ambas incluyen
  10W/20W, 305 x 381 / 268 x 358 mm y hasta 600 mm/s compartidos.
- Modelo real que respondio: `gemini-3.5-flash-lite`; fuente F1 del documento 5,
  chunk 23; distancia recuperada 0.24246085682298746 en ambas comparaciones.
- Pregunta del precio en 2035: HTTP 200, reconoce informacion insuficiente sin
  inventar una cifra; distancia 0.38918933256243726. Esto ilustra por que distancia
  y confianza no son equivalentes.
- Auditoria local RAG_QUERY verificada con tipo, ID, cantidad, SUCCESS y fecha,
  sin preguntas/respuestas. Los FAILED se verificaron en integracion con mocks.
- Sin cambios en ModelMapperConfig ni en application-local.properties.

La prueba real tiene un solo chunk: demuestra el recorrido completo y los datos
del PDF, no una evaluacion estadistica de retrieval. Se observo variacion inicial
en la exhaustividad de las comparaciones; el prompt final exige dos secciones
(diferencias y caracteristicas compartidas). No se garantiza que futuras
respuestas sean identicas o exhaustivas. El aislamiento con multiples documentos
y usuarios se comprueba con PostgreSQL real y proveedores mockeados.

## Archivos de esta fase

Creados (bajo `src/main/java/pe/edu/upc/legalai` salvo indicacion):

- DTOs/request: RAGRequestDTO, IAContextRequestDTO.
- DTOs/response: RAGResponseDTO, RAGSourceDTO.
- config/RAGSettings; controllers/RAGController; exceptions/RAGException.
- servicesinterfaces/RAGService.
- servicesimplements/RAGServiceImpl, RAGContextBuilder, RAGAuditService.
- Tests RAGServiceTest y RAGControllerTest; `docs/rag.md`.

Modificados en esta fase:

- DTOs/response/SemanticSearchResultDTO; repositories/ChunkEmbeddingRepository.
- servicesinterfaces/IAService; servicesimplements/GeminiIAServiceImpl.
- exceptions/GlobalExceptionHandler; resources/application.properties.
- Tests GeminiIAServiceImplTest y LegalAiApplicationTests.

Los cambios de embeddings de la fase anterior ya estaban en el workspace y
se conservaron. No se crea historial ni se escribe en sesiones, mensajes o
citaciones persistentes. No hay commit ni push automatico.
