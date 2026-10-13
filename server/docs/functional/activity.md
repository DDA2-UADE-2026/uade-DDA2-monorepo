# Actividades comunitarias

Esta guía describe la administración e inscripción disponibles para actividades, talleres, jornadas y campañas comunitarias. Todos esos nombres representan el mismo concepto de negocio: una actividad municipal que puede publicarse y recibir inscripciones ciudadanas.

## Datos de una actividad

Cada actividad define nombre, descripción, lugar, fecha inicial, fecha final y capacidad. La capacidad debe ser mayor a cero y la fecha final no puede ser anterior a la inicial.

La persona que realiza el alta y las fechas de creación y actualización se obtienen automáticamente; no se aceptan desde el cuerpo de la solicitud.

## Estados

| Estado | Significado | Operaciones disponibles |
| --- | --- | --- |
| `DRAFT` | La actividad está siendo preparada y todavía no está publicada. | Consultar, editar y publicar. |
| `OPEN` | La actividad está publicada. | Consultar y cerrar. |
| `CLOSED` | La actividad finalizó administrativamente. | Consultar. |

Las transiciones permitidas son únicamente `DRAFT -> OPEN -> CLOSED`. Una actividad publicada o cerrada no puede volver a editarse, y el cierre es terminal.

## Inscripción ciudadana

Los ciudadanos con el permiso `activities:own:view` consultan las actividades `OPEN`. El permiso `activities:own:enroll` permite inscribirse sin enviar datos personales en el cuerpo: la identidad se obtiene del JWT.

Cada ciudadano puede inscribirse una sola vez. La inscripción se confirma con HTTP `201`, siempre que la actividad continúe abierta y la cantidad de inscriptos sea menor a su capacidad. La actividad se bloquea durante esta operación para evitar que solicitudes concurrentes superen el cupo.

El catálogo y el detalle incluyen el estado de la actividad y `myEnrollment`: la inscripción del usuario autenticado, su fecha y su asistencia, o `null` si no está inscripto. No incluyen inscripciones ni datos personales de otros participantes. Este estado se recupera del servidor y se conserva al recargar la pantalla.

La vista **Mis inscripciones** incluye actividades abiertas y cerradas. Después del cierre, el titular puede seguir abriendo el detalle y consultar su asistencia; otro ciudadano no inscripto recibe `404` al intentar abrir esa actividad cerrada. Los borradores nunca aparecen en el catálogo ni en el historial ciudadano.

## Registro de asistencia

Un usuario con permiso `activities:attendance:view` puede consultar actividades `OPEN` o `CLOSED` y listar sus inscriptos. Con `activities:attendance:manage`, puede marcar cada inscripción como `PRESENT` o `ABSENT`. Estos permisos corresponden al rol `PROFESIONAL_CENTRO` y también están disponibles para `ADMIN`. Las actividades `DRAFT` no admiten registro de asistencia.

La inscripción conserva el usuario autenticado que realizó la última registración y su fecha. Cada cambio también genera un registro de auditoría con los valores anteriores y nuevos. La asistencia puede corregirse y se muestra el último valor guardado. En actividades no se exige una asignación previa del profesional al centro ni a la actividad.

## Recorrido en el frontend

1. En `/gestion/actividades`, el personal con permisos de gestión crea la actividad, edita el borrador y lo publica. Las acciones de alta, edición y cambio de estado se muestran según los permisos activos.
2. En `/portal/actividades`, el ciudadano consulta **Actividades abiertas**, abre el detalle y confirma su inscripción. Si se agota el cupo o ya existe una inscripción, se informa el conflicto y se actualiza la oferta.
3. **Mis inscripciones**, en la misma pantalla, permite recuperar las inscripciones guardadas, incluso después del cierre, con su estado de asistencia.
4. En `/gestion/actividades`, el profesional ve el listado para asistencia. Desde allí entra a `/gestion/actividades/{id}/asistencia`, consulta los inscriptos paginados y marca presentes o ausentes. Un usuario con permiso de consulta únicamente no ve botones de modificación.
5. El administrador también puede entrar a asistencia desde el listado o detalle de una actividad publicada o cerrada.
6. El cierre impide nuevas inscripciones. El profesional conserva el acceso para registrar o corregir asistencia y el ciudadano inscripto conserva su historial.

Los listados, detalles y acciones incluyen estados de carga, vacíos y errores recuperables. Las operaciones refrescan las consultas afectadas; no se muestra un guardado exitoso si el servidor rechaza el cambio.

## Contratos de consulta

| Operación | Endpoint | Permiso |
| --- | --- | --- |
| Catálogo abierto con inscripción propia | `GET /api/activities?page=0&size=10` | `activities:own:view` |
| Mis inscripciones, abiertas y cerradas | `GET /api/activities?enrolledOnly=true&page=0&size=10` | `activities:own:view` |
| Detalle ciudadano | `GET /api/activities/{id}` | `activities:own:view` |
| Inscripción | `POST /api/activities/{id}/enrollments` | `activities:own:enroll` |
| Actividades para asistencia | `GET /api/professional/activities` | `activities:attendance:view` |
| Detalle para asistencia | `GET /api/professional/activities/{activityId}` | `activities:attendance:view` |
| Inscriptos paginados | `GET /api/professional/activities/{activityId}/enrollments` | `activities:attendance:view` |
| Registrar o corregir asistencia | `PATCH /api/professional/activities/{activityId}/enrollments/{enrollmentId}/attendance` | `activities:attendance:manage` |

El último endpoint recibe `{"attendance":"PRESENT"}` o `{"attendance":"ABSENT"}`. La identidad del autor se obtiene del JWT. La inscripción debe pertenecer a la actividad indicada.

## Verificación

- Backend: `./gradlew test --tests 'com.uade.dda2.server.feature.activity.*'`. Incluye el recorrido completo desde alta hasta cierre, aislamiento entre ciudadanos, permisos, historial, paginación, corrección de asistencia y competencia por el último cupo. Estas pruebas usan H2 con modo PostgreSQL; no sustituyen la verificación de concurrencia contra PostgreSQL.
- Frontend: `npm run test:run -- src/components/activities/ActivityFlow.test.tsx`. Usa los clientes generados con respuestas HTTP controladas para comprobar inscripción persistente, cupo agotado, consulta profesional, registro/corrección, errores y permisos de edición.
- Los tipos y clientes de API se regeneran desde el OpenAPI del backend; las adiciones de consulta no requieren cambios en las tablas.

## Autorización

La administración utiliza los permisos `activities:management:view`, `activities:management:create`, `activities:management:edit` y `activities:management:change-status`. La consulta e inscripción propias utilizan `activities:own:view` y `activities:own:enroll`. La asistencia utiliza `activities:attendance:view` y `activities:attendance:manage`. El rol técnico `ADMIN` recibe todos los permisos registrados; `CIUDADANO` y `PROFESIONAL_CENTRO` reciben únicamente los de su función.

## Alcance actual

Los incrementos HU-21, HU-22 y HU-23 implementan el ciclo administrativo, la inscripción ciudadana y el registro profesional de asistencia.
