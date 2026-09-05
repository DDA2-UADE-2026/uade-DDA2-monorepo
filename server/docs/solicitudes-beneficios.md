# Solicitudes de beneficios

El backend implementa presentación propia y asistida, listado y detalle propios, catálogo documental por edición y documentos protegidos de solicitudes, mediante JWT con rol activo. La solicitud pertenece a `users.id`: se obtiene del token en la presentación propia y de un `userId` existente en la asistida. `registeredByUserId` identifica por separado a quien la registró y siempre se obtiene del token. No depende de una identidad externa ni de `citizen_snapshot`. No se implementan imágenes públicas de programas, evaluación general, visitas, transiciones del estado de la solicitud, asignaciones ni integración con Ciudadanos.

## Contrato HTTP

| Método y ruta | Permiso del rol activo | Resultado |
| --- | --- | --- |
| `POST /api/applications` | `applications:own:create` | `201 Created` al presentar; `200 OK` al recuperar una presentación mediante idempotencia |
| `POST /api/admin/applications` | `applications:management:create` | Presentación asistida para otro usuario existente; `201` nueva, `200` reintento |
| `GET /api/applications?page=0&size=20` | `applications:own:view` | Página de solicitudes propias, por número descendente |
| `GET /api/applications/{id}` | `applications:own:view` | Detalle propio; una solicitud ajena devuelve `404`, igual que una inexistente |
| `GET /api/applications/{applicationId}/documents` | `applications:own:documents:view` | Metadatos de entregas propias, sin bytes |
| `PUT /api/applications/{applicationId}/documents/{requirementId}` | `applications:own:documents:manage` | Primera carga `201` o reemplazo `200`, mediante `multipart/form-data` |
| `DELETE /api/applications/{applicationId}/documents/{applicationDocumentId}` | `applications:own:documents:manage` | Elimina la entrega y su archivo, `204` |
| `GET /api/applications/{applicationId}/documents/{applicationDocumentId}/content` | `applications:own:documents:view` | Contenido propio protegido con disposición `inline` |
| `GET /api/admin/applications/{applicationId}/documents` | `applications:management:documents:view` | Metadatos de cualquier solicitud |
| `GET /api/admin/applications/{applicationId}/documents/{applicationDocumentId}/content` | `applications:management:documents:view` | Contenido protegido para revisión |
| `PATCH /api/admin/applications/{applicationId}/documents/{applicationDocumentId}/review` | `applications:management:documents:review` | Revisa una entrega `PENDING` como `VALID` u `OBSERVED` |
| CRUD `/api/admin/program-editions/{editionId}/document-requirements` | `programs:management:view\|create\|edit` | Administra el catálogo mientras la edición no tenga solicitudes |

Todos requieren `Authorization: Bearer <accessToken>`. Los JWT de selección de rol no sirven. El actor autenticado debe existir, estar activo y conservar el rol activo asignado. Los permisos se evalúan sobre el JWT, sin mezclar los de otros roles. Como en el resto del modelo JWT, un cambio de permisos no revoca automáticamente tokens anteriores; se renuevan iniciando sesión o cambiando de rol.

La consulta no recibe un identificador de usuario: siempre filtra por el solicitante autenticado. `page` empieza en cero y `size` admite valores entre 1 y 100.

El POST propio acepta únicamente este cuerpo; cualquier campo adicional se rechaza con `400`:

```json
{
  "enrollmentPeriodId": "e14a6f34-b991-476d-8f13-0c6fbe301c51"
}
```

El POST asistido acepta únicamente `userId` y `enrollmentPeriodId`:

```json
{
  "userId": 42,
  "enrollmentPeriodId": "e14a6f34-b991-476d-8f13-0c6fbe301c51"
}
```

Se comprueba la existencia del titular en `users`, sin filtros de jurisdicción, roles, credenciales o vinculación externa. No se necesita el JWT del titular. La autorización para esta operación corresponde al administrativo. Enviar `registeredByUserId`, `registeredBy`, `registradoPor`, `citizenId` o cualquier otro campo adicional devuelve `400`. Para presentar para sí mismo, el actor debe utilizar el POST propio con su permiso correspondiente; la ruta asistida lo rechaza con `403`.

Respuesta de presentación propia:

```http
HTTP/1.1 201 Created
Location: /api/applications/2aa19e54-6f3f-4eef-99b1-8e1af9d8db26
Idempotency-Replayed: false
```

```json
{
  "id": "2aa19e54-6f3f-4eef-99b1-8e1af9d8db26",
  "applicationNumber": 15432,
  "userId": 42,
  "registeredByUserId": 42,
  "programEditionId": "2199c5bd-3329-4172-9b28-89d3bbf236f6",
  "enrollmentPeriodId": "e14a6f34-b991-476d-8f13-0c6fbe301c51",
  "status": "SUBMITTED",
  "submittedAt": "2026-08-31T13:30:00",
  "createdAt": "2026-08-31T13:30:00",
  "updatedAt": "2026-08-31T13:30:00",
  "pendingDocuments": [
    {
      "requirementId": "14c850cb-2871-4dc1-b7bc-8ff66ee69380",
      "code": "DNI_FRONT",
      "name": "Frente del DNI",
      "reason": "MISSING",
      "observation": null
    }
  ]
}
```

El listado utiliza `content`, `page`, `size`, `totalElements` y `totalPages`. El detalle y los elementos del listado tienen el mismo contrato que la respuesta del POST. No exponen el hash, la clave de idempotencia ni datos personales del solicitante.

La respuesta asistida tiene el mismo cuerpo, con `userId` del titular y `registeredByUserId` del administrativo que registró originalmente la solicitud. Devuelve `Idempotency-Replayed`; no informa `Location`, ya que no se implementa una consulta administrativa de solicitudes. Registrar para otra persona no concede acceso a su detalle propio. El titular sí recibe la solicitud asistida en su listado y detalle propios.

El campo `status` todavía devuelve el estado interno: la correspondencia completa con los estados públicos simplificados está pendiente de definición, en particular para los cierres sin resultado explícito. No se modifican los estados almacenados ni sus reglas.

## Catálogo y entregas documentales

`ProgramDocumentRequirement` define el catálogo por edición. El `code` se recorta y normaliza a mayúsculas, admite letras, números y guion bajo, y es único junto con `program_edition_id`. El detalle de programas disponibles incorpora `documentRequirements` en cada edición. Crear requiere `programs:management:create`, leer `programs:management:view` y actualizar o eliminar `programs:management:edit`.

Las mutaciones del catálogo bloquean la edición y comprueban que no exista ninguna `Application` asociada. Desde la primera solicitud, cualquier alta, actualización o baja devuelve `409 PROGRAM_DOCUMENT_REQUIREMENTS_LOCKED`. Así se serializa con la presentación, que bloquea la misma edición, y no queda una ventana entre la comprobación y el alta.

`Document` almacena UUID, nombre original saneado, MIME, tamaño, bytes, cargador y fecha. No existe un controller general ni una ruta pública. `ApplicationDocument` vincula solicitud, requisito y archivo con unicidad por `(application_id, requirement_id)` y por `document_id`.

La carga valida, antes de persistir:

- archivo no vacío y tamaño máximo de 10 MiB;
- extensión `pdf`, `jpg`, `jpeg` o `png`;
- MIME declarado exacto `application/pdf`, `image/jpeg` o `image/png`;
- firma real `%PDF-`, JPEG `FF D8 FF` o PNG `89 50 4E 47 0D 0A 1A 0A` coherente con extensión y MIME.

Un exceso detectado por el servicio o por el límite multipart devuelve `413 APPLICATION_DOCUMENT_FILE_TOO_LARGE`. Los demás formatos inválidos devuelven `400`. El límite de petición por defecto es 11 MB para permitir el envoltorio multipart sin aumentar el límite de archivo de 10 MB.

La primera carga crea `Document` y `ApplicationDocument` en `PENDING`. Reemplazar conserva el UUID de `ApplicationDocument`, crea otro `Document`, elimina el anterior y limpia estado, observación, revisor y fecha de revisión. Eliminar borra vínculo y archivo. Las tres operaciones requieren propiedad de la solicitud y se bloquean cuando su estado es `APPROVED`, `REJECTED` o `CLOSED`.

La revisión acepta únicamente una entrega `PENDING`. `OBSERVED` exige una observación no vacía; `VALID` la prohíbe. Cargar, reemplazar, eliminar o revisar cambia `Application.updatedAt`, sin modificar `Application.status`.

`pendingDocuments` se calcula tanto en el listado como en el detalle. Contiene requisitos `required=true` sin entrega (`MISSING`) o con entrega `OBSERVED`. Excluye requisitos opcionales y entregas `PENDING` o `VALID`.

Los listados consultan proyecciones de metadatos y no leen la columna binaria. Solo las rutas `/content` la recuperan y responden los bytes con el MIME almacenado, tamaño, nombre UTF-8 seguro, `Content-Disposition: inline` y `Cache-Control: private, no-store`. Una solicitud o documento ajeno devuelve `404` igual que uno inexistente.

Cada mutación bloquea la fila de la solicitud. Documento, vínculo, `updatedAt` y log `application_document` se confirman en una transacción; un fallo revierte todo. La auditoría contiene identificadores, nombre, MIME, tamaño, estado y revisión, nunca `content`. Las bajas de usuarios se bloquean también si el usuario cargó o revisó documentos.

## Reglas de presentación

- La convocatoria existe, está `OPEN` y cumple `openDate <= hoy <= closeDate`.
- La edición se deriva de la convocatoria y debe estar `ACTIVE`.
- Se admite una sola solicitud por usuario y convocatoria, en cualquier estado, incluso `REJECTED` o `CLOSED`.
- En otra convocatoria de la misma edición, todas las solicitudes anteriores deben estar `REJECTED` o `CLOSED`. `DRAFT`, `SUBMITTED`, `IN_VALIDATION`, `PENDING_DOCUMENTATION`, `IN_EVALUATION`, `IN_VISIT`, `APPROVED` y `WAITLISTED` bloquean una nueva.
- Las solicitudes de otras ediciones no bloquean por esta regla. La evaluación de incompatibilidades y asignaciones pertenece a historias futuras.
- El cupo agotado no impide presentar ni se incrementa `currentEnrollment`.
- Se guarda `SUBMITTED` con las fechas asignadas por el servidor. El horario utiliza `ENROLLMENT_PERIOD_TIME_ZONE` (por defecto `America/Argentina/Buenos_Aires`), compartido con la configuración del cierre automático de convocatorias.
- `originTicketId`, resolución y trabajador asignado quedan nulos. Las relaciones con personas utilizan siempre `User`.
- `registeredBy` es obligatorio e inmutable: en la presentación propia coincide con el titular; en la asistida referencia al administrativo autenticado. No reemplaza a `assignedWorker`.
- Ambas presentaciones comparten las reglas anteriores, sin cambiar duplicados, cupo ni incompatibilidades. Los documentos se gestionan después sobre la solicitud ya creada.

## Idempotencia y concurrencia

`Idempotency-Key` es opcional. Admite entre 1 y 128 caracteres ASCII visibles sin espacios y distingue mayúsculas. Se conserva durante la vida de la solicitud, sin vencimiento automático. La clave es un identificador técnico de petición, no una sesión.

La combinación `(user_id, idempotency_key)` es única. `user_id` siempre corresponde al titular, incluso en asistencia. Se guarda un SHA-256 del texto canónico `application:v1:<UUID de la convocatoria>`; el orden o formato del JSON no altera su significado. El titular forma parte de la clave de búsqueda; cambiar de titular utiliza otra clave independiente, aunque el administrativo sea el mismo.

| Petición | Resultado |
| --- | --- |
| Misma persona, clave y convocatoria | Devuelve la solicitud existente con `200` e `Idempotency-Replayed: true`; no genera otro número ni otra auditoría |
| Misma persona y clave, otra convocatoria | `409 APPLICATION_IDEMPOTENCY_CONFLICT` |
| Otra persona, misma clave | Clave independiente; se aplican las validaciones habituales |
| Sin clave o con una nueva, misma convocatoria ya solicitada | `409 APPLICATION_ALREADY_EXISTS_FOR_PERIOD` si la convocatoria sigue habilitada |
| Fallo de la transacción inicial | No queda solicitud ni reserva de clave; se puede reintentar |

Un reintento válido recupera la solicitud antes de volver a validar la convocatoria: también funciona después de su cierre. Se mantienen las comprobaciones de usuario y autorización. Devuelve los datos actuales de la misma solicitud; no una copia congelada del primer JSON.

La clave se comparte entre los dos flujos. Un reintento autorizado para el mismo titular, clave y convocatoria devuelve la original, aunque lo haga otro administrativo o el titular desde el flujo propio. Siempre conserva `registeredByUserId` y la auditoría original. El actor nunca forma parte del hash ni reemplaza al titular para comprobar duplicados.

Cada presentación bloquea la fila del solicitante hasta que termina la transacción. Esto serializa sus peticiones, incluso para convocatorias diferentes. Después se bloquean edición y convocatoria, en ese orden, compatible con sus operaciones administrativas. La base respalda además las restricciones únicas de usuario/convocatoria y usuario/clave.

Presentación y log `CREATE` de tipo `application` se confirman en una única transacción. Si falla la auditoría o la persistencia, no se devuelve confirmación y se revierte la solicitud. El actor del log es quien registra; el snapshot incluye `userId` y `registeredByUserId` por separado. Las claves y hashes no se incluyen en el log.

## Numeración y esquema

`id` sigue siendo UUID. `applicationNumber` se genera con la secuencia `application_number_seq`, global, desde 1, con incremento 1 y sin reinicio anual. Hibernate registra y crea esta secuencia con el esquema; no hay una migración ni un script automático de inserción de datos. Tampoco se calcula `MAX + 1`.

La secuencia garantiza unicidad y crecimiento de los valores asignados. Los números pueden tener saltos y las transacciones concurrentes pueden confirmarse en distinto orden. `SOL-000015432` es un formato de presentación, no el valor almacenado.

Al recrear la base de pruebas, iniciar el backend con la configuración habitual de generación del esquema y luego ejecutar manualmente `docs/init.sql`. No es necesario crear la secuencia a mano. El esquema documentado está en `docs/api-entities.dbml`.

Las relaciones no borran solicitudes en cascada. El ABM de usuarios rechaza eliminar a un solicitante, registrante, trabajador, cargador o revisor vinculado; las FK protegen también las referencias a ediciones, convocatorias, requisitos y archivos. Las tablas nuevas se crean con Hibernate al recrear el esquema; no se agrega una migración ni se completa información histórica automáticamente.

## Permisos y pruebas manuales

El init incluye `CIUDADANO` con `applications:own:create`, `applications:own:view`, `applications:own:documents:view` y `applications:own:documents:manage`. Los roles administrativos normales no reciben estos permisos automáticamente. `ADMIN` conserva la excepción anterior de superusuario técnico con todos los permisos.

El init incorpora `applications:management:create`, `applications:management:documents:view` y `applications:management:documents:review` y los incluye en `ADMIN`. No los concede a `CIUDADANO` ni crea nuevos roles: se deben asignar explícitamente a los roles administrativos que correspondan mediante la gestión existente. Después de cambiar permisos, renovar el JWT para que los contenga.

Las cuentas de ejemplo `admin` y `viewer` también reciben `CIUDADANO` al ejecutar el init. En una base nueva, ambas deben seleccionar rol después del login. La gestión de roles del ABM conserva la asignación explícita existente; no se cambió para agregar roles automáticamente.

La colección `docs/program-feature.postman_collection.json` incorpora la carpeta **Solicitudes propias**. Configurar un JWT con los permisos correspondientes y `enrollmentPeriodId`; ejecutar presentación, reintento, listado y detalle. La presentación guarda `applicationId`, `applicationNumber` y `applicationIdempotencyKey`. Borrar la clave antes de iniciar una presentación diferente, y conservarla para reintentar la anterior. Las peticiones negativas indican sus precondiciones.

La carpeta **Solicitudes asistidas** usa `assistedAccessToken` con permiso administrativo, `assistedApplicantUserId` y `enrollmentPeriodId`. Conserva `assistedApplicationId`, `assistedApplicationNumber`, `assistedRegisteredByUserId` y `assistedIdempotencyKey` para comprobar reintentos. Para comprobar el detalle propio del titular, configurar `assistedApplicantAccessToken`. No reutilizar la clave para otra convocatoria del mismo titular.

Las carpetas **Requisitos documentales**, **Documentos de solicitudes propias** y **Revisión documental administrativa** cubren el nuevo contrato. Configurar `documentRequirementId`, `applicationId`, `applicationDocumentId` y `applicationDocumentFile`; cambiar el rol activo antes de alternar entre operaciones ciudadanas y administrativas.

CORS admite `Idempotency-Key` y expone `Location` e `Idempotency-Replayed` a los orígenes ya configurados. No se modificó el frontend.

## Errores funcionales

| HTTP | Código | Causa |
| --- | --- | --- |
| 400 | `INVALID_REQUEST_BODY` | JSON inválido, UUID inválido, campo obligatorio ausente o campo extra |
| 400 | `APPLICATION_INVALID_IDEMPOTENCY_KEY` | Formato de clave inválido |
| 401 | `AUTH_UNAUTHENTICATED` / `AUTH_INVALID_TOKEN` | Falta de autenticación, token inválido o usuario inexistente/inactivo |
| 403 | `AUTH_FORBIDDEN` | Rol activo sin permiso o ya no asignado |
| 403 | `APPLICATION_ASSISTED_SELF_NOT_ALLOWED` | Se intentó usar la asistencia para uno mismo, en lugar de la presentación propia |
| 404 | `APPLICATION_NOT_FOUND` | Solicitud inexistente o ajena |
| 404 | `APPLICATION_USER_NOT_FOUND` | El titular indicado en la presentación asistida no existe |
| 404 | `APPLICATION_ENROLLMENT_PERIOD_NOT_FOUND` | Convocatoria inexistente |
| 409 | `APPLICATION_ENROLLMENT_PERIOD_NOT_OPEN` | Convocatoria no abierta |
| 409 | `APPLICATION_OUTSIDE_ENROLLMENT_PERIOD` | Fecha fuera de la convocatoria |
| 409 | `APPLICATION_PROGRAM_EDITION_NOT_ACTIVE` | Edición no activa |
| 409 | `APPLICATION_ALREADY_EXISTS_FOR_PERIOD` | Ya existe una solicitud en esa convocatoria |
| 409 | `APPLICATION_ALREADY_EXISTS_FOR_EDITION` | Solicitud anterior de la edición aún bloqueante |
| 409 | `APPLICATION_IDEMPOTENCY_CONFLICT` | Clave utilizada con otro payload |
| 409 | `USER_HAS_APPLICATION_REFERENCES` | Intento de eliminar un usuario vinculado a solicitudes |
| 400 | `APPLICATION_DOCUMENT_EMPTY_FILE` / `APPLICATION_DOCUMENT_INVALID_FILE_NAME` / `APPLICATION_DOCUMENT_INVALID_FILE_TYPE` | Archivo vacío o inconsistente |
| 413 | `APPLICATION_DOCUMENT_FILE_TOO_LARGE` | Archivo mayor a 10 MB |
| 404 | `APPLICATION_DOCUMENT_NOT_FOUND` / `PROGRAM_DOCUMENT_REQUIREMENT_NOT_FOUND` | Entrega o requisito inexistente, ajeno o de otra edición |
| 409 | `APPLICATION_DOCUMENTS_FINALIZED` | Intento de modificar documentos de una solicitud aprobada, rechazada o cerrada |
| 409 | `APPLICATION_DOCUMENT_INVALID_REVIEW_STATUS` | Estado destino inválido o entrega que ya no está pendiente |
| 409 | `APPLICATION_DOCUMENT_OBSERVATION_REQUIRED` / `APPLICATION_DOCUMENT_OBSERVATION_NOT_ALLOWED` | Observación inconsistente con el resultado |
| 409 | `PROGRAM_DOCUMENT_REQUIREMENTS_LOCKED` | Intento de cambiar el catálogo después de la primera solicitud |
| 409 | `PROGRAM_DOCUMENT_REQUIREMENT_CODE_EXISTS` | Código documental repetido dentro de la edición |

## Verificación automatizada

`ApplicationFlowTest` utiliza HTTP con JWT reales y persistencia real en una base aislada. Cubre permisos y propiedad, validación de fechas/estados, cupo, paginación, campos internos rechazados, unicidad, reintentos concurrentes, convocatorias concurrentes, números globales concurrentes, restricciones de base y rollback de auditoría. `ApplicationDocumentFlowTest` cubre catálogo, pendientes, propiedad, permisos, formatos, firmas, límite, reemplazo, eliminación, descarga inline, revisión, estados finales, concurrencia, auditoría, rollback y ausencia de archivos huérfanos.

Verificación del 5 de septiembre de 2026: 104 pruebas exitosas en H2, incluidas 13 del flujo documental; OpenAPI generado; 77 peticiones de Postman contrastadas con sus rutas, métodos, scripts y ejemplos; y DBML compilado para PostgreSQL. No se ejecutó SQL sobre la base de la aplicación. La equivalencia de estados públicos sigue pendiente de definición y no forma parte de esta entrega.

```powershell
.\gradlew.bat test generateOpenApiDocs --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
```

El parámetro de codificación permite ejecutar el worker de Java 17 en la ruta local que contiene `año`. No cambia la codificación de la API. El perfil de pruebas no usa las credenciales ni la base de la aplicación.

Para repetir la misma suite en PostgreSQL, la tarea `testApplicationsPostgres` usa exclusivamente `127.0.0.1:55439/application_test`, con usuario `application_test` y contraseña de `APPLICATION_TEST_POSTGRES_PASSWORD`. Debe ser una base desechable: la tarea crea y elimina su esquema. Nunca apunta a `DB_URL`.

```powershell
.\gradlew.bat testApplicationsPostgres --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
```
