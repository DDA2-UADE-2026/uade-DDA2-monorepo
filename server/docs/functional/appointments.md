# Appointment — Turnos de salud comunitaria (HU-18, HU-19 y HU-20)

Esta guía explica cómo un ciudadano solicita un turno de salud comunitaria. Está pensada para alguien que no conoce el proyecto y no requiere conocimientos técnicos.

## Quién participa

**Ciudadano.** Ingresa con su cuenta y opera con el rol `CIUDADANO`. Solo puede solicitar turnos para sí mismo; el sistema toma su identidad de la sesión y nunca de lo que envíe en la solicitud.

**Profesional de centro.** Atiende el turno. No aprueba ni rechaza solicitudes; su agenda semanal (HU-17) es la base para generar horarios.

## Recorrido

1. El ciudadano elige un servicio municipal con oferta efectiva (centro activo, servicio activo, profesional asignado y agenda vigente).
2. Elige un centro que ofrece ese servicio y una fecha futura.
3. El sistema muestra únicamente horarios que ese ciudadano puede obtener: bloques completos según la duración del servicio, con profesional identificado, sin horarios pasados, sin horarios ya ocupados por el profesional y sin horarios superpuestos con otros turnos del ciudadano.
4. El ciudadano selecciona un horario y lo confirma. Si el horario sigue disponible, el turno queda **confirmado** de inmediato, sin aprobación manual.
5. El sistema muestra la confirmación: identificador, servicio, centro, dirección, fecha, horario y profesional.

## Reglas principales

| Situación | Comportamiento |
| --- | --- |
| Dos personas eligen el mismo horario | Solo una lo obtiene; la otra recibe un conflicto y la disponibilidad actualizada. |
| El ciudadano ya tiene un turno superpuesto | La solicitud se rechaza, aunque sea otro servicio, centro o profesional. |
| Dos horarios seguidos (por ejemplo 09:00–10:00 y 10:00–11:00) | Se permiten; no se consideran superpuestos. |
| Se reintenta la misma solicitud (doble clic, error de red) | No se duplica: con la misma clave de idempotencia se devuelve el turno original. |
| Cambia la agenda o la duración del servicio después | El turno conserva su fecha y horario; el cambio solo afecta nuevas consultas, salvo reprogramación administrativa posterior (HU-19). |
| Se consulta un turno de otra persona | Se responde como inexistente, sin revelar datos. |

## Qué no incluye HU-18

Solicitar turnos para terceros, listado de "Mis turnos", cancelación, reprogramación, registro de asistencia o resultado de la atención, notificaciones externas y sincronización con calendarios.

## Gestión administrativa posterior (HU-19)

Un `ADMIN` con permisos `appointments:management:view` y `appointments:management:manage` gestiona turnos de cualquier centro. Se considera pendiente de atención un turno `CONFIRMED` cuyo inicio todavía no llegó. Puede buscar turnos por centro y fecha, consultar su detalle, reprogramarlos dentro del mismo centro y servicio a un horario vigente (con otro profesional si corresponde) o cancelarlos.

El turno reprogramado conserva identificador y titular; su horario anterior vuelve a la oferta si sigue siendo válido. Un turno cancelado muestra `CANCELLED` en el detalle propio del ciudadano, pero **retiene** el intervalo del profesional hasta que un administrativo lo habilita manualmente. El turno cancelado no bloquea otros turnos del ciudadano. La habilitación solo quita la retención: la agenda vigente decide si el horario vuelve a ofrecerse.

Contrato administrativo: `GET /api/admin/appointments?centerId={id}&date={YYYY-MM-DD}`, `GET /api/admin/appointments/{id}`, `GET /api/admin/appointments/{id}/slots?date={YYYY-MM-DD}`, `PUT /api/admin/appointments/{id}/schedule`, `PATCH /api/admin/appointments/{id}/cancel`, `PATCH /api/admin/appointments/{id}/release-slot`. La respuesta administrativa incluye titular y `slotRetained`; las consultas ciudadanas siguen limitadas al titular.

En bases PostgreSQL existentes se debe aplicar `server/docs/database/migrations/2026-09-28-hu19-appointment-cancellation.sql` antes de habilitar cancelaciones: `ddl-auto=update` no elimina la unicidad anterior de `(professional_assignment_id, starts_at)`, que impediría reservar un horario liberado, ni corrige un posible check antiguo que solo permita `CONFIRMED`. El script incorpora la columna nullable `slot_released_at`, admite ambos estados y agrega los permisos de gestión al rol `ADMIN` de manera aditiva. En bases nuevas, `server/docs/database/init.sql` carga esos permisos junto con el resto de los datos iniciales de prueba.

## Constancia profesional (HU-20)

El profesional con rol activo `PROFESIONAL_CENTRO` y permisos `appointments:professional:view` / `appointments:professional:manage` consulta sus turnos por fecha en `GET /api/professional/appointments?date={YYYY-MM-DD}` y abre un detalle en `GET /api/professional/appointments/{id}`. El listado incluye turnos históricos y cancelados, pero solo los turnos confirmados cuyo inicio ya llegó admiten un primer registro. Un turno reprogramado corresponde al profesional de la asignación vigente.

`POST /api/professional/appointments/{id}/attention` registra `ATENDIDO` con `attendedOn` no futuro y `description` breve (1–500 caracteres), o `AUSENTE` sin esos campos. `PUT` sobre la misma ruta corrige la constancia existente: exige su `version` actual; si está desactualizada, responde conflicto y requiere refrescar el detalle. Una corrección a `AUSENTE` elimina la fecha y descripción anteriores. Una sola constancia vigente pertenece a cada turno, y los cambios de resultado quedan auditados sin conservar descripciones en el log.

El resultado de atención es independiente de `CONFIRMED` y `CANCELLED`: no libera ni cambia el horario del turno. `GET /api/admin/appointments/{id}` y la búsqueda por centro y fecha incorporan la constancia al detalle administrativo; las respuestas ciudadanas siguen sin datos de asistencia o atención. El texto se limita a una descripción administrativa del servicio, sin campos para diagnósticos ni historia clínica; el backend no clasifica automáticamente contenido médico en texto libre.

Para bases PostgreSQL existentes, aplicar `server/docs/database/migrations/2026-09-29-hu20-community-attention.sql` antes de habilitar la API: crea la tabla si falta y asigna permisos al rol profesional sin quitar permisos previos. En bases nuevas, `server/docs/database/init.sql` incluye los permisos.

## Referencia técnica

- Contrato: `GET /api/citizen/appointment-services`, `GET /api/citizen/appointment-services/{serviceId}/centers`, `GET /api/citizen/appointment-slots`, `POST /api/citizen/appointments`, `GET /api/citizen/appointments/{appointmentId}`.
- Zona horaria municipal configurable, valor inicial `America/Argentina/Buenos_Aires`.
- Permisos: `appointments:own:view`, `appointments:own:create` (rol `CIUDADANO`).
- Auditoría: cada alta registra `APPOINTMENT` con `LogAction.CREATE` en la misma transacción.
