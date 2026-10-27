# Revisión de eliminación de users.active

Preparada el 2026-10-27. **Ejecutada en PostgreSQL local el 2026-10-27, con autorización posterior del usuario.** Única fuente de verdad: `users.status`. Las secciones de planificación siguientes conservan el procedimiento revisado; el resultado efectivo consta a continuación.

## Resultado de ejecución autorizada

- Respaldo completo custom creado y restaurado con éxito en la base local aislada `legalai_restore_active_20261027121459`. Se conserva esa base para recuperación; no fue eliminada.
- Archivo privado: `C:\Users\luisd\AppData\Local\LegalAI\backups\users-active-20261027-121459\before.dump`. Directorio con herencia de permisos deshabilitada y acceso concedido al usuario Windows actual. No está en Git.
- SHA-256 del respaldo: `7F597985B8BE0F36021E104F498859B1D56D36FFD1C8021F486B02CD34C5EA85`.
- SQL aprobado ejecutado con ON_ERROR_STOP y verificaciones adicionales dentro de la misma transacción: comparación de huellas de todas las columnas conservadas por user_id y comparación de definiciones de restricciones, antes de COMMIT. No se imprimieron valores personales ni hashes de contraseñas.
- Resultado: COMMIT exitoso; 7 usuarios conservados, valores de columnas restantes idénticos y 11 restricciones restantes idénticas. Lectura posterior confirma ausencia de active y permanencia de status NOT NULL.
- No se detuvo el proceso backend: el bloqueo ACCESS EXCLUSIVE protegió la ventana de verificación y cambio. El dump es una instantánea anterior; no debe suponerse que incluye escrituras concurrentes posteriores. Una recuperación exacta después de reanudar actividad exige conciliación.
- Maven offline con Java 21: `mvnw.cmd -o -Dtest=AuthServiceImplTest,UsuarioServiceImplTest test`, BUILD SUCCESS; 12 pruebas, 0 fallos, 0 errores, 0 omitidas. Compilación principal al día y compilación de tests correcta.
- No se crearon cuentas ni se invocaron login/registro HTTP en la base de trabajo. La corrección estructural está verificada; queda pendiente la comprobación funcional de registro desde Angular. Las pruebas ejecutadas usan repositorios simulados.
- Sin cambios en Java, DTOs, JWT, frontend o Railway. El respaldo y la restauración aislada contienen datos sensibles: mantenerlos privados.

## Evidencia y alcance

- Búsqueda en código principal, pruebas, SQL y documentación del repositorio: sin referencias funcionales a la columna `users.active`. Las coincidencias `ACTIVE` corresponden al valor de status; `isSynchronizationActive` pertenece a transacciones, no a usuarios.
- `entities/Usuario.java` mapea status y no active. `AuthServiceImpl.register` establece ACTIVE; `UsuarioDetailsService.loadUserByUsername` y `UsuarioServiceImplement.obtenerUsuarioAutenticado` consultan status; `UsuarioServiceImplement.eliminar` establece INACTIVE. DTOs y contratos no exponen active.
- Consultas locales realizadas con `default_transaction_read_only=on`: public.users.active es boolean NOT NULL, sin default; status es varchar(30) NOT NULL, con default ACTIVE. Cero discrepancias entre active y la expresión status = 'ACTIVE'. No se imprimieron registros personales ni hashes.
- pg_depend solo identifica users_active_not_null, la restricción intrínseca de la columna que necesariamente desaparece con ella. No hay dependencia funcional externa conocida. Las demás restricciones deben conservarse.
- Sin vistas/materialized views, funciones/procedimientos de usuario, reglas o políticas con referencia textual a active. Sin triggers de usuario sobre users ni publicaciones lógicas en esta BD. La búsqueda de texto no puede demostrar ausencia de SQL dinámico construido por fragmentos.
- Sesiones observadas: JDBC, pgAdmin y la consulta de diagnóstico. Esto no identifica exhaustivamente consumidores ni demuestra qué consultas ejecutan.
- README y docs describen Angular/API y Gemini, sin consumidor documentado de users.active. No se inspeccionaron repositorios externos, scripts privados, contenido del enlace Drive del README ni Railway. No hay inventario completo de integraciones; la ausencia global de consumidores externos queda pendiente de confirmación del equipo.

No se encontró una dependencia funcional que obligue a detener la preparación. **La ejecución queda condicionada a que responsables de otras versiones, integraciones y scripts confirmen que no leen/escriben active ni dependen de SELECT * o INSERT posicional sobre users.**

## Archivo y comportamiento

`database/migrations/20261027_drop_legacy_users_active.sql` es manual; no se encontró Flyway/Liquibase ni automatización de estos SQL en el repositorio inspeccionado. Prepararlo no cambia la BD.

El SQL toma un bloqueo exclusivo, valida el esquema observado y coherencia de ambos estados, rechaza dependencias catalogadas salvo el NOT NULL propio y elimina únicamente active usando RESTRICT, sin CASCADE. No ejecuta INSERT, UPDATE o DELETE. Conserva filas, status y demás columnas/restricciones. Eliminar active elimina sus valores almacenados: por eso se requiere respaldo, aunque esos valores actualmente coincidan con status.

No es idempotente deliberadamente: si active ya no existe o el esquema difiere, aborta para revisión. Si falla un chequeo/timeout, la transacción debe cerrarse con ROLLBACK; ejecutar con cliente que detenga errores. No probarlo contra la BD real ni siquiera con ROLLBACK sin autorización: adquiriría bloqueos.

## Respaldo previo a ejecución futura

1. Identificar explícitamente host, puerto y base objetivo sin publicar credenciales. Confirmar versión de backend de todos los consumidores y repetir inspección de dependencias.
2. Pausar escritores/operaciones del backend durante la ventana y mantenerlos pausados hasta terminar verificaciones o reversión.
3. Generar un pg_dump completo en formato custom de la BD objetivo, fuera del repositorio y con permisos restringidos. Contiene datos sensibles y hashes: no compartir ni subir a Git. Usar autenticación local segura, sin contraseña en argumentos.

   Ejemplo orientativo, **no ejecutado**: `pg_dump --host=localhost --port=5432 --username=<usuario> --dbname=<base_confirmada> --format=custom --file=<ruta_privada>/legalai_pre_active.dump`.

4. Verificar salida exitosa, archivo, checksum y listado mediante pg_restore --list. Un listado no basta para probar recuperación: restaurar el backup en una BD aislada autorizada antes de intervenir el entorno objetivo. Documentar versiones, hora, esquema, conteo de filas y restricciones, sin exportar datos al informe.
5. Guardar también el esquema previo para comparar después. Evitar escrituras entre respaldo y migración si se necesita reversión exacta.

## Reversión

- Antes del COMMIT, un fallo deja la transacción abortada; ROLLBACK conserva esquema y datos originales.
- Después del COMMIT, no hay reversión automática ni se ofrece un down.sql ejecutable a ciegas. Detener escritores y evaluar el estado antes de autorizar recuperación.
- Recuperación exacta del punto previo: restaurar el respaldo completo **en una BD separada**, validar y planificar con autorización el cambio de destino. No sobrescribir producción ni borrar tablas automáticamente. Si hubo escrituras posteriores, conciliarlas antes: restaurar una instantánea anterior puede perderlas.
- Reversión limitada del esquema, si se autoriza: recrear active boolean, poblarla desde el respaldo por user_id para el punto previo (o derivarla de status = 'ACTIVE' para el estado actual), verificar y restablecer NOT NULL con el nombre original. Requiere ALTER/UPDATE y autorización independiente. Derivar desde status es equivalente al respaldo solo mientras se mantenga la coherencia comprobada; no reproduce necesariamente la historia después de nuevas escrituras.
- Volver a imponer active NOT NULL sin mapeo/default reproduce el error de registro del backend actual. Una reversión exige coordinar la versión de aplicación y su política de estado; no reanudar tráfico asumiendo que añadir la columna basta.

## Riesgos e impacto en el equipo y Railway

- ACCESS EXCLUSIVE bloquea lecturas y escrituras durante la transacción. lock_timeout de 5 segundos limita la espera y statement_timeout de 30 segundos limita cada sentencia; programar ventana breve. La comprobación de coherencia recorre users y puede agotar el timeout en bases grandes.
- Un consumidor antiguo que use active fallará después del cambio. Revisar ramas, procesos antiguos, reportes, scripts de pgAdmin y accesos SQL directos. No basta con confirmar el frontend actual.
- El catálogo protege dependencias declaradas; RESTRICT no detecta SQL dinámico o consumidores externos. Repetir la revisión antes de ejecutar.
- El esquema local inspeccionado es PostgreSQL 18; otros entornos deben inspeccionarse separadamente. No afirmar compatibilidad o igualdad de esquema en Railway sin comprobarlo.
- Crear estos archivos o ejecutar el SQL exclusivamente contra localhost no modifica una BD independiente de Railway. No se hicieron llamadas, despliegues ni cambios en Railway. Si otro entorno comparte la BD objetivo, sí se verá afectado. Aplicar allí requeriría autorización y respaldo propios.
- Los integrantes con bases locales independientes necesitarán aplicar la migración aprobada en cada base afectada. Los que comparten la base reciben el cambio inmediatamente. Preparar el archivo no corrige sus bases ni el 409 actual.
- spring.jpa.hibernate.ddl-auto=update está presente; un backend antiguo que mapee active podría intentar reintroducir la columna al arrancar. Coordinar versiones; no se cambió esta propiedad.

## Verificación pendiente tras futura autorización

Comparar columnas y restricciones antes/después: solo debe faltar active y su NOT NULL; status, índices únicos, FK e IDs se conservan. Verificar conteos con escritores pausados. Ejecutar compilación y pruebas unitarias pertinentes; comprobar registro/login en una BD aislada autorizada. Registro y login pueden escribir datos/auditoría: no se invocaron. No se ejecutó la migración, un ensayo con rollback, un backup ni una restauración durante esta tarea.
