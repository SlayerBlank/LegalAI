# LegalAI — backend de gestión legal

## Alcance implementado

Backend tradicional con Java 21, Spring Boot, JPA, Spring Security, JWT, PostgreSQL y Swagger.
Esta fase incluye `Rol`, `Usuario`, `Cliente`, `Expediente`, `Documento` y `AuditLog`.
Los documentos admiten carga real de PDF y conservan sus metadatos; no hay descarga ni procesamiento
del contenido. `storageUrl` es un identificador interno relativo, no una URL publica. Las funcionalidades de IA descritas en la visión al final de
este documento son futuras y no están implementadas.

Se conserva el paquete `pe.edu.upc.legalai` y sus capas. Esta corrección de línea base no mueve DTOs:
se preservan los cambios de ubicación e imports que ya estaban presentes en el árbol de trabajo.
La auditoría anterior documentó la coexistencia de `DTOs/` y `schemas/dtos/`; al iniciar esta
corrección, los dos DTOs de `SesionChat` ya estaban movidos a `DTOs/`. La normalización de paquetes
y su documentación quedan como deuda técnica, fuera de esta corrección de compilación.

`ClienteController` es el único controller de clientes. Se retiró `ClientController`, que dependía
de tipos inexistentes y duplicaba rutas; su ruta por usuario no tenía servicio implementado.
`CitacionesIA` referencia ahora a `Documento`, dentro del mismo paquete, conservando `document_id`.
La relación de `Mensajes` con `SesionChat` sigue comentada: no bloquea compilación y se completará
en la fase de IA/chat. No se ha añadido conversación funcional, procesamiento documental ni IA.

El POM conserva una sola declaración de Security administrada por Spring Boot y una de Lombok.
Se retiró JJWT porque ninguna clase lo usa; la implementación JWT existente permanece intacta.

## Regla de arquitectura: config/ y ModelMapper

`config/` contiene configuraciones generales del proyecto e incluye `ModelMapperConfig.java`.
`SwaggerConfig`, `WebSecurityConfig` y `CorsConfig` permanecen en `securities/`.

El bean ModelMapper conserva exclusivamente `return new ModelMapper();`, sin TypeMap,
addMappings ni configuraciones que generen proxies de DTOs. Los servicios de clientes,
expedientes y documentos asignan los campos permitidos mediante setters. Las relaciones,
IDs, ownership, estados y fechas se gestionan explicitamente en `ServiceImpl` o por JPA.
ModelMapper queda disponible para conversiones simples donde el mapeo automatico sea seguro.

## Ejecución local

Requisitos: JDK 21, `JAVA_HOME` configurado y la base PostgreSQL `LEGALAI` creada en localhost.
La configuracion anterior de ModelMapper generaba proxies de DTOs y fallaba con JDK 26
(`UnsupportedOperationException` en `JdkClassWriter`); se elimino esa configuracion.
`application.properties` contiene la configuración general y activa e incluye el perfil `local`.
`application-local.properties` contiene únicamente el usuario `postgres` y la contraseña local;
reemplazar `[PASSWORD LOCAL]` por la contraseña de PostgreSQL.
En PowerShell, definir la clave JWT en la terminal que iniciará el backend:

```powershell
$jwtBytes = New-Object byte[] 48
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtBytes)
$jwtRandom.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
.\mvnw.cmd spring-boot:run
```

`JWT_SECRET` se lee directamente del entorno, sin agregar propiedades JWT a los dos archivos.
La clave debe tener al menos 32 bytes UTF-8. Mantenerla estable entre arranques si se desea
conservar la validez de los tokens emitidos. `JWT_EXPIRATION_MS` tiene un valor predeterminado
de 86400000 (24 horas). `CORS_ALLOWED_ORIGINS` admite orígenes separados por comas;
por defecto permite `http://localhost:3000` y `http://localhost:5173`.

## Swagger y endpoints

Abrir `http://localhost:8080/swagger-ui/index.html`. El contrato está en `/v3/api-docs`.
Registrar un usuario o iniciar sesión, copiar `accessToken` y pegarlo en **Authorize → Bearer Token**
sin añadir el prefijo `Bearer`. Registro y login son públicos; los demás endpoints exigen JWT.

| Recurso | Endpoints |
| --- | --- |
| Autenticación | `POST /api/auth/register`, `POST /api/auth/login` |
| Perfil y roles | `GET /api/users/me`, `GET /api/roles` |
| Clientes | `POST, GET /api/clients`; `GET, PUT, DELETE /api/clients/{id}` |
| Expedientes | `POST, GET /api/cases`; `GET, PUT, DELETE /api/cases/{id}` |
| Expedientes de un cliente | `GET /api/clients/{clientId}/cases` |
| Documentos de un expediente | `POST, GET /api/cases/{caseId}/documents` |
| Carga PDF | `POST /api/cases/{caseId}/documents/upload` (`multipart/form-data`) |
| Documento | `GET, DELETE /api/documents/{id}` |
| Fragmentos de documento | `POST, GET /api/documents/{documentId}/chunks` |
| Auditoría (solo lectura) | `GET /api/audit-logs` (paginado, filtros `userId`, `action`, `entityType`, `from`, `to`); `GET /api/audit-logs/{id}`; `GET /api/audit-logs/usuario/{userId}` |

Registro de ejemplo:

```json
{"fullName":"Usuario de prueba","email":"usuario@example.com","password":"Ejemplo-2026!"}
```

El rol `USER` se crea automáticamente al registrar el primer usuario. No se aceptan roles,
propietarios ni estados de procesamiento enviados por el cliente como fuente de autorización.
Los recursos ajenos se responden con 404, igual que los inexistentes. Las contraseñas se almacenan
con BCrypt y nunca aparecen en las respuestas. Deben tener al menos ocho caracteres y no superar
72 bytes UTF-8.

Las eliminaciones de clientes con expedientes o de expedientes con documentos devuelven 409;
primero se deben eliminar los registros dependientes. No hay eliminación en cascada.
Al editar un expediente sin estado o fecha de apertura se conservan los valores actuales.
Al cerrar se completa la fecha de cierre si falta; al reabrir se elimina. El cierre no puede
preceder a la apertura. Los errores de la API incluyen `status`, `message`, `timestamp`, `error` y `path`.

La auditoría registra login y las operaciones solicitadas sobre clientes, expedientes y documentos.
Comparte la transacción de la operación: si esta falla, tampoco se confirma el registro de auditoría.
Es de solo lectura vía API: `GET /api/audit-logs` acepta paginación estándar de Spring (`page`, `size`,
`sort`) y filtros opcionales combinables por `userId`, `action`, `entityType` y rango de fechas
(`from`/`to`, ISO-8601). No existe endpoint para crear, editar o eliminar registros de auditoría
manualmente; solo se generan como efecto secundario de otras operaciones.

## Carga de PDF

`POST /api/cases/{caseId}/documents/upload` requiere JWT y acepta `file` (archivo obligatorio)
y `category` (texto opcional, hasta 80 caracteres). Verifica el expediente del usuario autenticado
antes de leer/escribir el archivo desde el servicio. Tanto un expediente ajeno como uno inexistente
devuelven 404. El contenedor puede recibir temporalmente el multipart antes de esa comprobacion.
El endpoint JSON anterior se conserva para registrar metadata externa; no realiza cargas de archivos.

| Variable | Valor predeterminado | Uso |
| --- | --- | --- |
| `LEGALAI_MAX_FILE_SIZE` | `10MB` (10,485,760 bytes) | Limite del servicio y de cada archivo multipart |
| `LEGALAI_MAX_REQUEST_SIZE` | `12MB` | Limite de la solicitud multipart completa |
| `LEGALAI_STORAGE_PATH` | `uploads` | Directorio de almacenamiento; relativo al directorio de ejecucion o absoluto |

Al aumentar el limite del archivo, ajustar tambien el de la solicitud para permitir el overhead multipart.
El servicio exige extension `.pdf`, MIME `application/pdf` y cabecera binaria `%PDF-1.x` o `%PDF-2.x`.
Esta comprobacion basica no certifica la integridad completa del PDF y no usa parser, OCR ni extraccion.
Se rechazan archivos vacios, nombres con rutas o caracteres de control y nombres de mas de 255 caracteres.
Las validaciones del servicio devuelven 400; los limites multipart del contenedor devuelven 413.
Ambos usan `ErrorResponse`, igual que los errores 401, 403, 404 y 500.

Se crea el directorio si falta y se escribe con `CREATE_NEW` bajo un nombre `UUID.pdf`.
La BD guarda solo ese identificador en `storageUrl`; el nombre original se conserva en `fileName`.
El servicio asigna usuario, expediente, MIME, tamano y estado `UPLOADED`; no los toma de campos del formulario.
El directorio `uploads/` esta excluido de Git y no se publica como recurso web.

La escritura precede a la persistencia. Un error de escritura no crea metadata; un error de metadata
o auditoria provoca rollback e intenta borrar el archivo, incluso si el error ocurre al confirmar
la transaccion. Metadata y evento `UPLOAD_DOCUMENT` comparten transaccion. El evento identifica
usuario, documentId, caseId y nombre original; nunca incluye contenido del PDF.
Si falla la limpieza, se registra el identificador para conciliacion. Una caida del proceso entre
escritura y commit puede dejar un archivo huerfano: filesystem y PostgreSQL no forman una transaccion
atomica. Ante resultado de commit desconocido se conserva el archivo y se registra la necesidad de
conciliacion para evitar borrar datos que pudieran haber sido confirmados.
La eliminacion existente sigue eliminando metadata; la gestion de retencion de archivos queda pendiente.

Para Railway, configurar posteriormente `LEGALAI_STORAGE_PATH=/data/uploads` sobre un volumen
persistente montado. Esta tarea no crea volumen ni modifica el despliegue. Sin volumen, los archivos
locales no tienen persistencia garantizada entre despliegues. No se requieren rutas especificas en codigo.

Prueba manual en Swagger:

1. Ejecutar `mvnw.cmd clean test` con Java 21 y la base exclusiva de pruebas configurada.
2. Ejecutar `mvnw.cmd spring-boot:run` y abrir `http://localhost:8080/swagger-ui/index.html`.
3. Autenticarse con **Authorize**, crear o elegir un expediente propio y abrir el endpoint de carga.
4. Usar **Try it out**, completar `caseId`, seleccionar un PDF real con `file` y, opcionalmente, `category`.
5. Ejecutar y comprobar 201, `processingStatus=UPLOADED`, `sizeBytes` igual al tamano real y `storageUrl=UUID.pdf`.
6. Consultar el documento y verificar el archivo en `<LEGALAI_STORAGE_PATH>/<storageUrl>`.

## Pruebas

Las pruebas de integración levantan el servidor HTTP en un puerto aleatorio y requieren una base
PostgreSQL exclusiva para pruebas, creada previamente. No utilizan la base local `LEGALAI`.
Por defecto buscan `legalai_test` en `localhost:5432`; configurar la conexión así:

```powershell
$env:TEST_DB_URL = 'jdbc:postgresql://localhost:5432/legalai_test'
$env:TEST_DB_USERNAME = '<usuario de pruebas>'
$env:TEST_DB_PASSWORD = '<contraseña de pruebas>'
.\mvnw.cmd verify
```

El perfil `test` está en `src/test/resources/application-test.properties` y su clave JWT es exclusiva
de pruebas. Las pruebas eliminan los usuarios y recursos que crean. Comprueban arranque, persistencia,
registro concurrente, JWT inválidos y vencidos, usuarios inactivos, CORS, aislamiento entre usuarios,
CRUD, validaciones, fechas, auditoría, rollback y el contrato OpenAPI de los 19 endpoints.
El JAR ejecutable se genera en `target/LegalAI-0.0.1-SNAPSHOT.jar`.

## Visión futura del proyecto

El texto siguiente describe la visión original, fuera del alcance de la fase implementada.

# ⚖️ Plataforma Jurídica Inteligente con IA

Este proyecto propone el desarrollo de una **plataforma web inteligente orientada a profesionales del derecho**, diseñada para facilitar la **gestión, consulta y análisis de documentos jurídicos mediante tecnologías de inteligencia artificial**.

La solución busca centralizar en un mismo entorno digital documentos como **expedientes, contratos, informes legales y otros archivos jurídicos**, permitiendo que el usuario pueda acceder rápidamente a la información relevante contenida en grandes volúmenes documentales.

## 🎯 Objetivo del proyecto

Reducir el tiempo que los profesionales del derecho destinan a tareas repetitivas relacionadas con la revisión y búsqueda de información documental, proporcionando herramientas que apoyen su trabajo sin sustituir su criterio profesional.

## 🤖 Funcionalidades principales Futuros

La plataforma permitirá:

* 📁 Gestionar y organizar documentos jurídicos.
* 🔎 Realizar búsquedas dentro de documentos y expedientes.
* 📝 Generar resúmenes automáticos.
* 📌 Identificar cláusulas y fragmentos relevantes.
* 💬 Consultar información mediante preguntas sobre los documentos almacenados.
* 📄 Generar borradores jurídicos para su posterior revisión por el profesional.
* 🗂️ Centralizar información relacionada con casos, contratos y expedientes.

> **La inteligencia artificial funciona como una herramienta de apoyo. Las decisiones, interpretaciones y documentos finales deben ser revisados y validados por un profesional del derecho.**

## 🚧 Estado actual del proyecto

Actualmente se encuentra implementada la primera etapa del backend.

### Módulos implementados

- Roles
- Usuarios
- Clientes
- Documentos

### Arquitectura

El backend utiliza una arquitectura por capas:

- Controllers
- DTOs
- Entities
- Repositories
- Services
- Security
- Configuration

### Tecnologías

- Java
- Spring Boot
- Spring Data JPA
- Hibernate
- Maven
- Swagger / OpenAPI
- Base de datos relacional

## 📚 Swagger

Después de iniciar la aplicación, la documentación de la API se encuentra en:

```text
http://localhost:8080/swagger-ui/index.html

## 🏗️ Arquitectura general

La solución se plantea como una aplicación web compuesta principalmente por:

**Frontend**
Interfaz encargada de la interacción entre el usuario y la plataforma.

**Backend – Spring Boot**
Responsable de la lógica de negocio, gestión de usuarios, documentos, expedientes y comunicación con otros servicios.

**Servicios de Inteligencia Artificial**
Encargados del procesamiento, análisis, búsqueda semántica, generación de resúmenes y asistencia sobre documentos jurídicos.

## 💡 Enfoque

El proyecto se centra en tres pilares principales:

**Gestión documental + Inteligencia Artificial + Apoyo al profesional del derecho**

El objetivo no es reemplazar al abogado, sino ofrecer una herramienta tecnológica que permita **trabajar con mayor rapidez, organización y acceso a la información jurídica relevante**.

---

### Proyecto académico

Desarrollado como una propuesta de solución tecnológica orientada a la aplicación de **Ingeniería de Software e Inteligencia Artificial en el sector legal (LegalTech)**.
