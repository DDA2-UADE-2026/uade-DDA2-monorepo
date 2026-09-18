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

Los ciudadanos con los permisos `activities:own:view` y `activities:own:enroll` pueden consultar únicamente actividades `OPEN` e inscribirse sin enviar datos personales en el cuerpo: la identidad se obtiene del JWT.

Cada ciudadano puede inscribirse una sola vez. La inscripción se confirma con HTTP `201`, siempre que la actividad continúe abierta y la cantidad de inscriptos sea menor a su capacidad. La actividad se bloquea durante esta operación para evitar que solicitudes concurrentes superen el cupo.

## Registro de asistencia

Un usuario con rol `PROFESIONAL_CENTRO` puede consultar actividades `OPEN` o `CLOSED`, listar sus inscriptos y marcar cada inscripción como `PRESENT` o `ABSENT`. Las actividades `DRAFT` no admiten registro de asistencia.

La inscripción conserva el profesional autenticado que realizó la última registración y su fecha. Cada cambio también genera un registro de auditoría con los valores anteriores y nuevos. En esta etapa los profesionales no se asignan a centros ni a actividades específicas.

## Autorización

La administración utiliza los permisos `activities:management:view`, `activities:management:create`, `activities:management:edit` y `activities:management:change-status`. La consulta e inscripción propias utilizan `activities:own:view` y `activities:own:enroll`. La asistencia utiliza `activities:attendance:view` y `activities:attendance:manage`. El rol técnico `ADMIN` recibe todos los permisos registrados; `CIUDADANO` y `PROFESIONAL_CENTRO` reciben únicamente los de su función.

## Alcance actual

Los incrementos HU-21, HU-22 y HU-23 implementan el ciclo administrativo, la inscripción ciudadana y el registro profesional de asistencia.
