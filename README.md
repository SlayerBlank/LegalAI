# LegalAI — backend de gestión legal

## Alcance implementado

Backend tradicional con Java 21, Spring Boot, JPA, Spring Security, JWT, PostgreSQL y Swagger.
Esta fase incluye `Rol`, `Usuario`, `Cliente`, `Expediente`, `Documento` y `AuditLog`.
Los documentos contienen únicamente metadatos: el endpoint no recibe archivos binarios, no descarga
`storageUrl` ni procesa el contenido. Las funcionalidades de IA descritas en la visión al final de
este documento son futuras y no están implementadas.

Se conserva el paquete `pe.edu.upc.legalai` y la estructura existente: `controllers`, `entities`,
`exceptions`, `repositories`, `schemas/dtos/request`, `schemas/dtos/response`, `securities`,
`servicesimplements` y `servicesinterfaces`. Los enums y `ErrorResponse` existentes conservan
su ubicación; no se movieron archivos ni se añadieron dependencias.

## Ejecución local

Requisitos: JDK 21 o posterior, `JAVA_HOME` configurado y la base PostgreSQL `LEGALAI` creada en localhost.
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
| Documento | `GET, DELETE /api/documents/{id}` |

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
No hay endpoint público para consultar auditoría en esta fase.

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
