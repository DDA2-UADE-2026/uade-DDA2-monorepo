# Appointment — Turnos de salud comunitaria (HU-18)

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
| Cambia la agenda o la duración del servicio después | El turno confirmado conserva su fecha y horario; el cambio solo afecta nuevas consultas. |
| Se consulta un turno de otra persona | Se responde como inexistente, sin revelar datos. |

## Qué no incluye esta historia

Solicitar turnos para terceros, listado de "Mis turnos", cancelación, reprogramación, registro de asistencia o resultado de la atención, notificaciones externas y sincronización con calendarios.

## Referencia técnica

- Contrato: `GET /api/citizen/appointment-services`, `GET /api/citizen/appointment-services/{serviceId}/centers`, `GET /api/citizen/appointment-slots`, `POST /api/citizen/appointments`, `GET /api/citizen/appointments/{appointmentId}`.
- Zona horaria municipal configurable, valor inicial `America/Argentina/Buenos_Aires`.
- Permisos: `appointments:own:view`, `appointments:own:create` (rol `CIUDADANO`).
- Auditoría: cada alta registra `APPOINTMENT` con `LogAction.CREATE` en la misma transacción.
