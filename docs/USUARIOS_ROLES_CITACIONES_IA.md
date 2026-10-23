# Usuarios, roles y citaciones IA — entrega de implementación

Fecha: 2026-10-23.

## Alcance y requisitos verificables

Se completaron las operaciones explícitamente solicitadas en UsuarioController y RolController, y se documentó el flujo existente de CitacionesIAController. Se mantuvo Controller → Service Interface → Service Implement → Repository → Entity, con DTOs de entrada y salida. No se modificaron entidades, autenticación de registro/login, JWT, Gemini, RAG ni persistencia del chat.

Se inspeccionaron README.md y las referencias disponibles del documento técnico previo. No se encontraron criterios de aceptación formales para HU-003, HU-004, HU-005, HU-006, HU-007, HU-008, HU-009, HU-010, HU-011, HU-051 y HU-052. El enlace externo del README no es una especificación local verificada. No se certifica el cumplimiento exacto de esas HU sin sus criterios. La matriz siguiente distingue la implementación comprobable del número académico.

| Grupo de HU indicado | Cobertura implementada o reutilizada | Límite |
|---|---|---|
| HU-003–006 | Consulta de perfil, listado, detalle, actualización administrativa de nombre y baja lógica; registro existente reutilizado | Falta confirmar criterios, campos editables y semántica de eliminación |
| HU-007–011 | Listado, creación, detalle, actualización y eliminación de roles; duplicados, roles reservados y referencias controlados | Falta confirmar correspondencia exacta de cada número con cada operación |
| HU-051 | Generación de respuesta RAG con fuentes persistidas dentro del envío de mensajes, ya existente | Referencias a fragmentos documentales, no verificación de normas/jurisprudencia |
| HU-052 | Consulta de fuentes del mensaje ASSISTANT con ownership de sesión y expediente | Falta confirmar si los criterios exigen datos adicionales de una cita jurídica |

## Endpoints

| Estado | Método y ruta | Acceso | Respuesta |
|---|---|---|---|
| Conservado | GET /api/users/me | Usuario activo autenticado | 200 UsuarioResponseDTO |
| Nuevo | GET /api/users | ADMIN | 200 lista UsuarioResponseDTO, incluye INACTIVE |
| Nuevo | GET /api/users/{id} | ADMIN | 200 UsuarioResponseDTO |
| Nuevo | PUT /api/users/{id} | ADMIN | 200 UsuarioResponseDTO |
| Nuevo | DELETE /api/users/{id} | ADMIN | 204, baja lógica |
| Conservado | GET /api/roles | ADMIN | 200 lista RolResponseDTO |
| Nuevo | POST /api/roles | ADMIN | 201 RolResponseDTO |
| Nuevo | GET /api/roles/{id} | ADMIN | 200 RolResponseDTO |
| Nuevo | PUT /api/roles/{id} | ADMIN | 200 RolResponseDTO |
| Nuevo | DELETE /api/roles/{id} | ADMIN | 204 |
| Conservado | GET /api/chat/sessions/{sessionId}/messages/{messageId}/citations | Usuario activo dueño de sesión y expediente | 200 lista CitacionIAResponseDTO |

No se agrega POST /api/users: AuthController.register y AuthServiceImpl.register ya realizan registro con rol USER, validación de correo duplicado y BCrypt. No se encontró un contrato que justifique alta administrativa diferente. No se cambia ese endpoint ni se duplica el cifrado.

No se agrega POST de citaciones. Se reutiliza POST /api/chat/sessions/{sessionId}/messages para generar respuestas y fuentes.

Los identificadores no positivos producen 400 en los servicios nuevos y en la consulta de citaciones. Recursos inexistentes producen 404. El acceso ajeno a citaciones también produce 404 sin revelar existencia. JWT ausente/inválido/inactivo produce 401; operaciones administrativas sin ROLE_ADMIN, 403. Conflictos de nombres/correos, cuentas protegidas, roles asignados y restricciones de integridad producen 409 mediante GlobalExceptionHandler existente.

## Usuarios: contrato deliberadamente limitado

UsuarioUpdateRequestDTO recibe fullName y email obligatorios, con tamaños máximos 150 y validación de correo. Rechaza propiedades adicionales, incluidos roleId, rol, status, password y passwordHash. Solo se modifica fullName, recortando espacios. Se comprueba correo duplicado de otro usuario sin distinguir mayúsculas.

El correo debe coincidir con el actual tras normalización a minúsculas. Cambiarlo produce 400. La razón es que JwtTokenUtil utiliza el correo como sub: cambiar/liberar ese identificador y permitir que otra cuenta lo reutilice podría dar validez a un token antiguo contra otra identidad. No se modificó el formato JWT ni se introdujeron migraciones para resolver un requisito no documentado.

DELETE establece status=INACTIVE; no elimina filas ni históricos. Reserva el correo, conserva referencias y hace que UsuarioDetailsService/JwtRequestFilter rechacen tokens de la cuenta inactiva, incluido un token emitido antes de la baja. Es idempotente para usuarios ya inactivos. Devuelve 409 para administradores o la cuenta autenticada. No se permite reactivar cuentas por este DTO.

Las operaciones de escritura bloquean la fila con PESSIMISTIC_WRITE para evitar que una actualización concurrente de perfil sobrescriba la baja. Los listados se ordenan por userId. Las respuestas reutilizan UsuarioResponseDTO y nunca contienen hash ni contraseña.

Estas restricciones deben contrastarse con los criterios de HU antes de afirmar que cubren una actualización libre de correo, restablecimiento de contraseña o borrado físico. Ninguna de esas operaciones se incorporó.

## Roles

RolRequestDTO recibe name obligatorio (máximo 50; letras, números y guion bajo, empezando por letra) y description opcional (máximo 255). Rechaza propiedades adicionales. El servicio normaliza name a mayúsculas y prohíbe el prefijo ROLE_, porque UsuarioDetailsService ya agrega ese prefijo al construir authorities.

Se consulta duplicidad sin distinguir mayúsculas y se conserva la restricción UNIQUE existente. Los conflictos concurrentes de escritura pueden ser traducidos por el handler de integridad a 409.

ADMIN y USER son fundamentales: no se crean mediante este endpoint, no se renombra un rol existente hacia esos nombres y no se eliminan. Para roles fundamentales se puede modificar description manteniendo name. Los roles asignados a cualquier usuario, incluso INACTIVE, tampoco pueden renombrarse ni eliminarse. Se comprueba Rol.usuarios dentro de una transacción con bloqueo de la fila del rol y se fuerza flush para detectar restricciones de BD.

Crear un rol no lo asigna a usuarios ni concede nuevos permisos en la cadena de seguridad. La asignación administrativa de roles no fue solicitada como contrato separado y no se incorporó al DTO de usuario.

## Citaciones: flujo existente reutilizado

1. ChatController recibe el envío de un mensaje en la sesión.
2. ChatServiceImpl.enviarMensaje verifica ownership y alcance documental y llama RAGService.
3. RAGServiceImpl obtiene fragmentos mediante SemanticSearchService, construye contexto con RAGContextBuilder y solicita la respuesta a GeminiIAServiceImpl cuando hay contexto.
4. ChatStore.guardarAsistente guarda el mensaje ASSISTANT y sus metadatos; llama ICitacionesIAService.registrar dentro de su transacción REQUIRES_NEW.
5. CitacionesIAServiceImplement.registrar exige transacción MANDATORY, verifica propietario de sesión/expediente y que cada documento/chunk existe y está en alcance; elimina repetidos por chunkId y guarda CitacionesIA con mensaje, documento, fragmento y score.
6. El GET existente verifica que messageId pertenece a sessionId y es ASSISTANT, comprueba ownership y devuelve las referencias almacenadas.

La consulta puede devolver una lista vacía. documentId puede ser null si se eliminó el documento conforme a la política existente de retención. chunkId es identificador histórico escalar; la consulta no garantiza que el fragmento siga existiendo tras reprocesar o borrar un documento.

Las fuentes son fragmentos consultados por RAG; no se afirma que el modelo haya usado todos en cada frase ni que una referencia sea una norma o sentencia auténtica. relevanceScore = 1 - distancia coseno no es porcentaje de confianza. No se añadieron páginas, referencias jurídicas inventadas ni otro generador de respuestas.

## Seguridad y Swagger

WebSecurityConfig conserva JWT/stateless y establece GET /api/users/me como autenticado antes del matcher administrativo /api/users/**. Usuarios y roles administrativos requieren ADMIN en HTTP. @EnableMethodSecurity habilita @PreAuthorize en los métodos administrativos de UsuarioServiceImplement y en RolServiceImplement, para proteger también llamadas a servicios a través del proxy.

Los tags explícitos son usuario-controller, rol-controller y citaciones-ia-controller. Se agregan @Operation, @Parameter, @ApiResponse/@ApiResponses y schemas de DTOs/listas/errores; los cuerpos de entrada usan @Valid. Se reutiliza la configuración global Bearer Token. Se documentan 200/201/204 y 400/401/403/404/409 según operación.

La ruta estática /api/users/me y la variable /api/users/{id} son compatibles: MVC prioriza la coincidencia literal. Los endpoints anteriores no se eliminaron. No se modificaron GlobalExceptionHandler ni SwaggerConfig.

## Archivos

Creados:

- src/main/java/pe/edu/upc/legalai/dtos/request/UsuarioUpdateRequestDTO.java
- src/main/java/pe/edu/upc/legalai/dtos/request/RolRequestDTO.java
- docs/USUARIOS_ROLES_CITACIONES_IA.md

Modificados:

- src/main/java/pe/edu/upc/legalai/controllers/UsuarioController.java
- src/main/java/pe/edu/upc/legalai/controllers/RolController.java
- src/main/java/pe/edu/upc/legalai/controllers/CitacionesIAController.java
- src/main/java/pe/edu/upc/legalai/servicesinterfaces/IUsuarioService.java
- src/main/java/pe/edu/upc/legalai/servicesinterfaces/IRolService.java
- src/main/java/pe/edu/upc/legalai/servicesimplements/UsuarioServiceImplement.java
- src/main/java/pe/edu/upc/legalai/servicesimplements/RolServiceImplement.java
- src/main/java/pe/edu/upc/legalai/servicesimplements/CitacionesIAServiceImplement.java
- src/main/java/pe/edu/upc/legalai/repositories/IUsuarioRepository.java
- src/main/java/pe/edu/upc/legalai/repositories/IRolRepository.java
- src/main/java/pe/edu/upc/legalai/securities/WebSecurityConfig.java

Los constructores existentes de los servicios se conservaron. No se modificaron archivos de pruebas, entidades, POM, application.properties ni application-local.properties.

## Verificación y pendientes

Compilación realizada con Java 21 y el comando autorizado:

```powershell
./mvnw.cmd clean package -Dmaven.test.skip=true
```

Resultado final: BUILD SUCCESS, 161 fuentes principales compiladas. Maven informó Not compiling test sources y Tests are skipped. Advertencias preexistentes: uso de API deprecated en GlobalExceptionHandler y aviso de procesamiento de anotaciones.

No se ejecutaron pruebas, no se arrancó backend, no se consultaron/modificaron datos reales, no se ejecutaron migraciones y no se hicieron commits ni push. La construcción regeneró target, incluidos los artefactos JAR; no equivale a verificar endpoints en una instancia activa.

Pendientes: criterios de aceptación formales de las HU, verificación runtime de Swagger/serialización y controles HTTP cuando esté autorizado ejecutar el backend. application.properties conserva marcadores de conflicto de merge preexistentes; se respetó la prohibición de modificarlo. La compilación no demuestra que esa configuración permita arrancar. La documentación técnica general previa dejó de estar disponible en la raíz durante la inspección; no se recreó ni se sustituyó. Este documento registra el contrato actual de los tres módulos.

