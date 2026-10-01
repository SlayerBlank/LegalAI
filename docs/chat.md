# Chat conversacional persistido

`chat_sessions` y `chat_messages` almacenan las conversaciones. Cada mensaje es
una fila de `chat_messages` con `sender_type` en `USER` o `ASSISTANT`, pertenece a
una sesion (`session_id`, FK `ON DELETE CASCADE`) y opcionalmente lleva
`client_message_id` para idempotencia. Cada respuesta se enlaza directamente a
su mensaje de usuario (`reply_to_message_id`), sin inferirlo por orden de llegada.
Las respuestas conservan en `response_metadata` proveedor/modelo y los fragmentos
consultados, para que un reintento con el mismo identificador devuelva el mismo
turno y sus fuentes. `chat_sessions.document_id` es opcional: si es `NULL` la
sesion consulta todos los documentos del expediente solo si
`document_scope_required` es falso. Al borrar un documento acotado, el valor se
mantiene verdadero y los siguientes mensajes se rechazan: nunca se amplia
silenciosamente al expediente.

Si una base ya aplico una version anterior de la migracion y elimino documentos
antes de agregar `document_scope_required`, no es posible reconstruir desde
`document_id = NULL` cuales sesiones habian sido acotadas; revise los registros
o auditorias disponibles antes de activar esa version.

Antes de iniciar en una base existente, ejecutar
`database/migrations/20261003_chat_sessions_documents.sql`. No hay `DROP TABLE`:
solo columnas, indices y cascadas. La FK `chat_messages.session_id` es la que
impide huerfanos; `ai_citations` sigue su mensaje con `ON DELETE CASCADE`.

## Endpoints

| Metodo | Ruta | Respuesta |
| --- | --- | --- |
| POST | `/api/chat/sessions` | `201` con la sesion creada |
| GET | `/api/chat/sessions?caseId=&page=&size=` | `200` con `updatedAt` descendente |
| GET | `/api/chat/sessions/{sessionId}` | `200` con la sesion y `messageCount` |
| POST | `/api/chat/sessions/{sessionId}/messages` | `200` con el turno completo |
| GET | `/api/chat/sessions/{sessionId}/messages?page=&size=` | `200` en orden cronologico; metadatos de fuentes en respuestas |
| PATCH | `/api/chat/sessions/{sessionId}` | `200` solo con `title` |
| DELETE | `/api/chat/sessions/{sessionId}` | `204` |

El propietario se toma siempre del JWT; los cuerpos rechazan campos
desconocidos, por lo que `ownerUserId`, `role` y `senderType` nunca son entrada
confiable. Un recurso ajeno responde `404`, no `403`, para no revelar su
existencia. El alcance documental lo fija la sesion y se revalida en cada
mensaje, de modo que un cambio de propietario del expediente no abre el paso.

## Decision: persistir el turno del usuario antes de llamar al modelo

El mensaje `USER` se guarda en su propia transaccion (`ChatStore`, con
`REQUIRES_NEW`) y solo despues se invoca el RAG. Si Gemini falla, responde con
error, expira o se agota el tiempo, el mensaje del usuario permanece y **no** se
crea una respuesta `ASSISTANT` ficticia: un turno incompleto es honesto y evita
que el historial contenga texto que el modelo jamas genero. El error del
proveedor se propaga tal cual (`503`) y se audita como `result=FAILED` en una
transaccion independiente, para que un fallo de auditoria no oculte el error real.

La respuesta del asistente se guarda en una segunda transaccion junto con
`updated_at` de la sesion. La llamada al proveedor ocurre sin transaccion de
base de datos abierta, de modo que no se retienen bloqueos durante el tiempo de
red.

Con `clientMessageId` el reintento es idempotente: si el turno ya tiene respuesta
se devuelve el mismo par de mensajes y sus metadatos de fuentes sin volver a
llamar al modelo; si el turno existe todavia sin respuesta se reutiliza ese
mensaje del usuario. Reutilizar el mismo identificador con otro contenido se
rechaza con `400`. La FK e indice unico de `reply_to_message_id` evitan asociar
una respuesta a otro turno y guardar dos respuestas para una pregunta si las
solicitudes coinciden. Para respuestas anteriores a la migracion sin metadatos,
los campos de proveedor/fuentes se devuelven como `null` (desconocidos), no como
una lista de fuentes vacia.

## Memoria y reformulacion de consultas

El historial enviado al modelo se limita a los ultimos
`legalai.chat.max-history-messages` mensajes. La pregunta del usuario nunca se
reescribe: lo que cambia es la consulta enviada a pgvector.

```properties
legalai.chat.max-history-messages=${LEGALAI_CHAT_MAX_HISTORY_MESSAGES:10}
legalai.chat.max-message-chars=${LEGALAI_CHAT_MAX_MESSAGE_CHARS:2000}
legalai.chat.default-page-size=${LEGALAI_CHAT_DEFAULT_PAGE_SIZE:20}
legalai.chat.max-page-size=${LEGALAI_CHAT_MAX_PAGE_SIZE:100}
legalai.chat.query-rewrite=${LEGALAI_CHAT_QUERY_REWRITE:heuristic}
```

La reformulacion se aplica solo si la pregunta depende del historial. Se
detecta por referencias explicitas (`anterior`, `mencionado`, `dicho`, `este`,
`su`, ...) o porque la pregunta empieza como seguimiento (`Y ...`, `O ...`).
Una pregunta autonoma se envia a la recuperacion tal cual, sin llamadas extra.

- `none`: no reformula.
- `heuristic` (por defecto): antepone la ultima pregunta del usuario, truncada a
  300 caracteres. No consume una llamada adicional al proveedor.
- `llm`: pide al proveedor una consulta autonoma usando como maximo los ultimos 6
  turnos y 500 caracteres por turno. Si la respuesta viene vacia, multilinea,
  demasiado larga o el proveedor falla, se cae a `heuristic`.

El historial se envia como datos no confiables y el prompt del sistema indica que
no son instrucciones.