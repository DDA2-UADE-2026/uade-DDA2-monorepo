# 05. Centros, turnos y actividades comunitarias

[Índice](README.md) · [Anterior](04-beneficios-y-seguimiento.md) · [Siguiente: integraciones](06-integraciones-paso-a-paso.md)

Estos circuitos pertenecen a M8, pero no requieren crear una solicitud de beneficio social. **En el contrato implementado no hay eventos externos de centros, turnos, atención o actividades.** Las acciones de este capítulo se resuelven en nuestra API/base y auditoría.

## 1. Preparar un centro y su oferta

| Paso | Trabajo administrativo | Validaciones y resultado | Evento externo |
| --- | --- | --- | --- |
| 1 | Registrar centro municipal. | Nombre normalizado único, dirección y contactos; condición activa/inactiva. | Ninguno. |
| 2 | Registrar servicio municipal. | Nombre normalizado único, descripción y duración positiva de atención. | Ninguno. |
| 3 | Asociar servicio con centro. | Ambas dependencias deben estar activas; no se duplica una relación ya activa. | Ninguno. |
| 4 | Cargar horarios de apertura. | Día semanal y franjas válidas sin solapamientos; deben permitir las disponibilidades efectivas. | Ninguno. |
| 5 | Asignar profesional al servicio del centro. | Usuario activo con rol `PROFESIONAL_CENTRO`, dependencias vigentes y relación válida. | Ninguno. |
| 6 | Cargar disponibilidad del profesional. | Franjas contenidas completamente en apertura; inicio anterior a fin y sin superposición del profesional en otras asignaciones efectivas. | Ninguno. |
| 7 | Mantener oferta y activaciones. | Desactivar retira oferta efectiva para nuevas reservas. Reactivar vuelve a validar las disponibilidades dependientes. | Ninguno. |

Centro, servicio, relación centro-servicio, asignación profesional y disponibilidad son niveles diferentes. Tener un centro activo no alcanza para ofrecer turnos si falta alguno de los otros componentes.

Cambiar duración o disponibilidad no mueve los turnos ya confirmados. Un cambio administrativo de datos y la reprogramación de un turno son operaciones distintas.

## 2. Reservar un turno como ciudadano

| Paso | Actor | Acción y efecto | Evento externo |
| --- | --- | --- | --- |
| 1 | Ciudadano autenticado | Consulta servicios con oferta efectiva en `GET /api/citizen/appointment-services`. | Ninguno. |
| 2 | Ciudadano | Consulta centros de un servicio y elige fecha. | Ninguno. |
| 3 | Backend | Genera horarios con duración del servicio, apertura y disponibilidad profesional. Descarta pasados, ocupados, retenidos y superpuestos. | Ninguno. |
| 4 | Ciudadano | Confirma en `POST /api/citizen/appointments`, con clave de idempotencia. El titular se obtiene de la sesión. | Ninguno. |
| 5 | Backend | Revalida disponibilidad concurrente y guarda un turno `CONFIRMED`; no requiere aprobación municipal posterior. | Ninguno. |
| 6 | Ciudadano | Consulta sus turnos y el detalle propio en `GET /api/citizen/appointments` y `/{appointmentId}`. | Ninguno. |

La reserva requiere rol activo `CIUDADANO` y permisos `appointments:own:view`/`appointments:own:create` según acción. No permite reservar a nombre de otro usuario. La misma clave y contenido devuelve el turno original, sin duplicar la reserva.

La agenda considera turnos del titular y del profesional, y visitas/entrevistas sociales superpuestas de ambos. Dos intervalos contiguos no se superponen. Si otra persona obtiene el horario antes de confirmar, se devuelve conflicto y debe consultarse la disponibilidad nuevamente.

## 3. Reprogramar, cancelar y liberar horarios

La gestión administrativa de turnos requiere rol `ADMIN` y los permisos `appointments:management:view` o `appointments:management:manage`. Incluye búsqueda por centro/fecha, detalle, resumen mensual y disponibilidad diaria.

| Operación | Condiciones | Efecto | Evento externo |
| --- | --- | --- | --- |
| Reprogramar | Turno `CONFIRMED` cuyo inicio no llegó, con nuevo horario válido. | Conserva ID y titular; mismo centro/servicio y puede cambiar el profesional. El intervalo anterior queda disponible si la agenda aún lo ofrece. | Ninguno. |
| Cancelar | Turno futuro gestionable. | Pasa a `CANCELLED`; su intervalo profesional queda retenido. Ya no bloquea la agenda personal del ciudadano. | Ninguno. |
| Liberar intervalo cancelado | Cancelación con retención pendiente. | Quita la retención administrativa; la oferta efectiva decide si el horario puede volver a reservarse. | Ninguno. |

La cancelación no implica liberación inmediata del profesional. La API diferencia `PATCH /api/admin/appointments/{id}/cancel` de `PATCH /api/admin/appointments/{id}/release-slot`.

## 4. Registrar constancia de atención

1. El profesional consulta sus turnos por fecha en `/api/professional/appointments` con `appointments:professional:view`.
2. Abre el detalle de un turno que tiene asignado. Un reprogramado pertenece al profesional de la asignación vigente.
3. Una vez que comenzó un turno confirmado, registra una constancia con `appointments:professional:manage`.
4. El resultado `ATENDIDO` requiere fecha de atención no futura y descripción administrativa breve. `AUSENTE` no lleva esos campos.
5. Una corrección usa la versión vigente de la constancia; una versión desactualizada devuelve conflicto. Cambiar a ausente elimina fecha/descripción previas.
6. La constancia queda visible en la consulta profesional/administrativa y auditada. No cambia por sí sola `CONFIRMED`/`CANCELLED` ni libera el intervalo.

La constancia no agrega diagnósticos ni historia clínica estructurada. Las respuestas ciudadanas de turnos no exponen la descripción profesional de atención. Ninguno de estos pasos publica `AtencionComunitariaRegistrada` en el contrato actual.

## 5. Crear una campaña, taller o actividad

En el backend estas propuestas se representan mediante la misma entidad de actividad, independiente de la convocatoria de un programa.

| Paso | Acción | Reglas y resultado | Evento externo |
| --- | --- | --- | --- |
| 1 | Personal autorizado crea actividad. | Nombre, descripción, lugar, inicio/fin y capacidad positiva; nace `DRAFT`. Autor y fechas de registro los fija el servidor. | Ninguno. |
| 2 | Edita borrador. | La edición se permite mientras permanezca `DRAFT`. | Ninguno. |
| 3 | Publica. | `DRAFT -> OPEN`; se ofrece a ciudadanos. Publicar aquí significa habilitar oferta local. | Ninguno. |
| 4 | Ciudadano consulta catálogo. | Ve actividades abiertas y su inscripción propia; no datos de otros participantes. | Ninguno. |
| 5 | Se inscribe. | Identidad desde sesión, actividad abierta, lugar disponible y una única inscripción por ciudadano. Se bloquea el recurso para controlar el último cupo. | Ninguno. |
| 6 | Consulta «mis inscripciones». | Mantiene acceso a sus actividades abiertas y cerradas y al último registro de asistencia. | Ninguno. |
| 7 | Personal cierra actividad. | `OPEN -> CLOSED`, terminal; impide nuevas inscripciones y conserva historial. | Ninguno. |

Una actividad abierta/cerrada no vuelve a borrador y no admite la edición prevista para borradores. El permiso de administración de actividades no se confunde con el de inscripción propia.

## 6. Registrar asistencia a actividades

| Paso | Trabajo del profesional o personal autorizado | Resultado | Evento externo |
| --- | --- | --- | --- |
| 1 | Consulta actividades para asistencia. | Solo `OPEN` o `CLOSED`; un borrador no admite asistencia. | Ninguno. |
| 2 | Lista inscripciones de la actividad. | Acceso con `activities:attendance:view`, paginado y separado del catálogo ciudadano. | Ninguno. |
| 3 | Marca `PRESENT` o `ABSENT`. | Exige `activities:attendance:manage`; conserva actor y fecha. | Ninguno. |
| 4 | Corrige una marca. | Guarda el último valor y audita el cambio anterior/nuevo. Se puede corregir después del cierre. | Ninguno. |
| 5 | El titular consulta su inscripción. | Ve su asistencia vigente, sin registros ajenos. | Ninguno. |

En actividades no se exige una asignación profesional previa a esa actividad o centro para registrar asistencia: aplica el permiso. Esta regla difiere de la atención de un turno, que sí exige que esté asignado al profesional.

## 7. Relación con otros circuitos

- Las visitas y los turnos se controlan conjuntamente para evitar superposiciones.
- Turnos y actividades se agregan al tablero de indicadores, pero no consumen el cupo de una edición social.
- Un beneficio otorgado y una inscripción a actividad son registros distintos; no hay un evento que cree automáticamente uno desde el otro.
- No está implementada una notificación externa de reserva, cancelación, atención o asistencia dentro de estos diez contratos.

## 8. Referencias

- [Controladores de centros y agendas](../../src/main/kotlin/com/uade/dda2/server/feature/center/controller).
- [Validación de disponibilidad](../../src/main/kotlin/com/uade/dda2/server/feature/center/validator/ProfessionalAvailabilityValidator.kt).
- [Turnos ciudadanos](../../src/main/kotlin/com/uade/dda2/server/feature/appointment/controller/CitizenAppointmentController.kt), [gestión administrativa](../../src/main/kotlin/com/uade/dda2/server/feature/appointment/controller/AdminAppointmentController.kt) y [atención profesional](../../src/main/kotlin/com/uade/dda2/server/feature/appointment/controller/ProfessionalAppointmentController.kt).
- [Guía de actividades](../functional/activity.md).
