# Solicitudes de beneficios

El backend implementa presentación, listado y detalle propios mediante JWT con rol activo. La solicitud pertenece a `users.id`, obtenido exclusivamente del token. No depende de una identidad externa ni de `citizen_snapshot`. No se implementan documentación, evaluación, visitas, transiciones de estado, asignaciones ni integración con Ciudadanos.

## Contrato HTTP

| Método y ruta | Permiso del rol activo | Resultado |
| --- | --- | --- |
| `POST /api/applications` | `applications:own:create` | `201 Created` al presentar; `200 OK` al recuperar una presentación mediante idempotencia |
| `GET /api/applications?page=0&size=20` | `applications:own:view` | Página de solicitudes propias, por número descendente |
| `GET /api/applications/{id}` | `applications:own:view` | Detalle propio; una solicitud ajena devuelve `404`, igual que una inexistente |

Todos requieren `Authorization: Bearer <accessToken>`. Los JWT de selección de rol no sirven. El usuario debe existir, estar activo y conservar el rol activo asignado. Los permisos se evalúan sobre el JWT, sin mezclar los de otros roles. Como en el resto del modelo JWT, un cambio de permisos no revoca automáticamente tokens anteriores; se renuevan iniciando sesión o cambiando de rol.

La consulta no recibe un identificador de usuario: siempre filtra por el solicitante autenticado. `page` empieza en cero y `size` admite valores entre 1 y 100.

El POST acepta únicamente este cuerpo; cualquier campo adicional se rechaza con `400`:

```json
{
  "enrollmentPeriodId": "e14a6f34-b991-476d-8f13-0c6fbe301c51"
}
```

Respuesta de presentación:

```http
HTTP/1.1 201 Created
Location: /api/applications/2aa19e54-6f3f-4eef-99b1-8e1af9d8db26
Idempotency-Replayed: false
```

```json
{
  "id": "2aa19e54-6f3f-4eef-99b1-8e1af9d8db26",
  "applicationNumber": 15432,
  "programEditionId": "2199c5bd-3329-4172-9b28-89d3bbf236f6",
  "enrollmentPeriodId": "e14a6f34-b991-476d-8f13-0c6fbe301c51",
  "status": "SUBMITTED",
  "submittedAt": "2026-08-31T13:30:00",
  "createdAt": "2026-08-31T13:30:00",
  "updatedAt": "2026-08-31T13:30:00"
}
```

El listado utiliza `content`, `page`, `size`, `totalElements` y `totalPages`. El detalle y los elementos del listado tienen el mismo contrato que la respuesta del POST. No exponen el hash, la clave de idempotencia ni datos personales del solicitante.

## Reglas de presentación

- La convocatoria existe, está `OPEN` y cumple `openDate <= hoy <= closeDate`.
- La edición se deriva de la convocatoria y debe estar `ACTIVE`.
- Se admite una sola solicitud por usuario y convocatoria, en cualquier estado, incluso `REJECTED` o `CLOSED`.
- En otra convocatoria de la misma edición, todas las solicitudes anteriores deben estar `REJECTED` o `CLOSED`. `DRAFT`, `SUBMITTED`, `IN_VALIDATION`, `PENDING_DOCUMENTATION`, `IN_EVALUATION`, `IN_VISIT`, `APPROVED` y `WAITLISTED` bloquean una nueva.
- Las solicitudes de otras ediciones no bloquean por esta regla. La evaluación de incompatibilidades y asignaciones pertenece a historias futuras.
- El cupo agotado no impide presentar ni se incrementa `currentEnrollment`.
- Se guarda `SUBMITTED` con las fechas asignadas por el servidor. El horario utiliza `ENROLLMENT_PERIOD_TIME_ZONE` (por defecto `America/Argentina/Buenos_Aires`), compartido con la configuración del cierre automático de convocatorias.
- `originTicketId`, resolución y trabajador asignado quedan nulos. Las relaciones con personas utilizan siempre `User`.

## Idempotencia y concurrencia

`Idempotency-Key` es opcional. Admite entre 1 y 128 caracteres ASCII visibles sin espacios y distingue mayúsculas. Se conserva durante la vida de la solicitud, sin vencimiento automático. La clave es un identificador técnico de petición, no una sesión.

La combinación `(user_id, idempotency_key)` es única. Se guarda un SHA-256 del texto canónico `application:v1:<UUID de la convocatoria>`; el orden o formato del JSON no altera su significado.

| Petición | Resultado |
| --- | --- |
| Misma persona, clave y convocatoria | Devuelve la solicitud existente con `200` e `Idempotency-Replayed: true`; no genera otro número ni otra auditoría |
| Misma persona y clave, otra convocatoria | `409 APPLICATION_IDEMPOTENCY_CONFLICT` |
| Otra persona, misma clave | Clave independiente; se aplican las validaciones habituales |
| Sin clave o con una nueva, misma convocatoria ya solicitada | `409 APPLICATION_ALREADY_EXISTS_FOR_PERIOD` si la convocatoria sigue habilitada |
| Fallo de la transacción inicial | No queda solicitud ni reserva de clave; se puede reintentar |

Un reintento válido recupera la solicitud antes de volver a validar la convocatoria: también funciona después de su cierre. Se mantienen las comprobaciones de usuario y autorización. Devuelve los datos actuales de la misma solicitud; no una copia congelada del primer JSON.

Cada presentación bloquea la fila del solicitante hasta que termina la transacción. Esto serializa sus peticiones, incluso para convocatorias diferentes. Después se bloquean edición y convocatoria, en ese orden, compatible con sus operaciones administrativas. La base respalda además las restricciones únicas de usuario/convocatoria y usuario/clave.

Presentación y log `CREATE` de tipo `application` se confirman en una única transacción. Si falla la auditoría o la persistencia, no se devuelve confirmación y se revierte la solicitud. Las claves y hashes no se incluyen en el log.

## Numeración y esquema

`id` sigue siendo UUID. `applicationNumber` se genera con la secuencia `application_number_seq`, global, desde 1, con incremento 1 y sin reinicio anual. Hibernate registra y crea esta secuencia con el esquema; no hay una migración ni un script automático de inserción de datos. Tampoco se calcula `MAX + 1`.

La secuencia garantiza unicidad y crecimiento de los valores asignados. Los números pueden tener saltos y las transacciones concurrentes pueden confirmarse en distinto orden. `SOL-000015432` es un formato de presentación, no el valor almacenado.

Al recrear la base de pruebas, iniciar el backend con la configuración habitual de generación del esquema y luego ejecutar manualmente `docs/init.sql`. No es necesario crear la secuencia a mano. El esquema documentado está en `docs/api-entities.dbml`.

Las relaciones no borran solicitudes en cascada. El ABM de usuarios rechaza eliminar a un solicitante o trabajador vinculado; las FK protegen también las referencias a ediciones y convocatorias.

## Permisos y pruebas manuales

El init incluye `CIUDADANO` con `applications:own:create` y `applications:own:view`. Los roles administrativos normales no reciben estos permisos automáticamente. `ADMIN` conserva la excepción anterior de superusuario técnico con todos los permisos.

Las cuentas de ejemplo `admin` y `viewer` también reciben `CIUDADANO` al ejecutar el init. En una base nueva, ambas deben seleccionar rol después del login. La gestión de roles del ABM conserva la asignación explícita existente; no se cambió para agregar roles automáticamente.

La colección `docs/program-feature.postman_collection.json` incorpora la carpeta **Solicitudes propias**. Configurar un JWT con los permisos correspondientes y `enrollmentPeriodId`; ejecutar presentación, reintento, listado y detalle. La presentación guarda `applicationId`, `applicationNumber` y `applicationIdempotencyKey`. Borrar la clave antes de iniciar una presentación diferente, y conservarla para reintentar la anterior. Las peticiones negativas indican sus precondiciones.

CORS admite `Idempotency-Key` y expone `Location` e `Idempotency-Replayed` a los orígenes ya configurados. No se modificó el frontend.

## Errores funcionales

| HTTP | Código | Causa |
| --- | --- | --- |
| 400 | `INVALID_REQUEST_BODY` | JSON inválido, UUID inválido, campo obligatorio ausente o campo extra |
| 400 | `APPLICATION_INVALID_IDEMPOTENCY_KEY` | Formato de clave inválido |
| 401 | `AUTH_UNAUTHENTICATED` / `AUTH_INVALID_TOKEN` | Falta de autenticación, token inválido o usuario inexistente/inactivo |
| 403 | `AUTH_FORBIDDEN` | Rol activo sin permiso o ya no asignado |
| 404 | `APPLICATION_NOT_FOUND` | Solicitud inexistente o ajena |
| 404 | `APPLICATION_ENROLLMENT_PERIOD_NOT_FOUND` | Convocatoria inexistente |
| 409 | `APPLICATION_ENROLLMENT_PERIOD_NOT_OPEN` | Convocatoria no abierta |
| 409 | `APPLICATION_OUTSIDE_ENROLLMENT_PERIOD` | Fecha fuera de la convocatoria |
| 409 | `APPLICATION_PROGRAM_EDITION_NOT_ACTIVE` | Edición no activa |
| 409 | `APPLICATION_ALREADY_EXISTS_FOR_PERIOD` | Ya existe una solicitud en esa convocatoria |
| 409 | `APPLICATION_ALREADY_EXISTS_FOR_EDITION` | Solicitud anterior de la edición aún bloqueante |
| 409 | `APPLICATION_IDEMPOTENCY_CONFLICT` | Clave utilizada con otro payload |
| 409 | `USER_HAS_APPLICATION_REFERENCES` | Intento de eliminar un usuario vinculado a solicitudes |

## Verificación automatizada

`ApplicationFlowTest` utiliza HTTP con JWT reales y persistencia real en una base aislada. Cubre permisos y propiedad, validación de fechas/estados, cupo, paginación, campos internos rechazados, unicidad, reintentos concurrentes, convocatorias concurrentes, números globales concurrentes, restricciones de base y rollback de auditoría.

Verificación de esta entrega: 77 pruebas exitosas en H2 (41 de solicitudes), OpenAPI generado, 57 peticiones de Postman contrastadas con sus rutas y métodos, y DBML compilado para PostgreSQL. No se ejecutó la suite en PostgreSQL: Docker Desktop no pudo iniciar su motor por un error local al acceder a un socket de su servicio de telemetría. No se modificó ni se ejecutó SQL sobre la base de la aplicación.

```powershell
.\gradlew.bat test generateOpenApiDocs --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
```

El parámetro de codificación permite ejecutar el worker de Java 17 en la ruta local que contiene `año`. No cambia la codificación de la API. El perfil de pruebas no usa las credenciales ni la base de la aplicación.

Para repetir la misma suite en PostgreSQL, la tarea `testApplicationsPostgres` usa exclusivamente `127.0.0.1:55439/application_test`, con usuario `application_test` y contraseña de `APPLICATION_TEST_POSTGRES_PASSWORD`. Debe ser una base desechable: la tarea crea y elimina su esquema. Nunca apunta a `DB_URL`.

```powershell
.\gradlew.bat testApplicationsPostgres --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
```
