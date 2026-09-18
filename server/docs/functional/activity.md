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

## Autorización

La administración utiliza los permisos `activities:management:view`, `activities:management:create`, `activities:management:edit` y `activities:management:change-status`. La consulta e inscripción propias utilizan `activities:own:view` y `activities:own:enroll`. El rol técnico `ADMIN` recibe todos los permisos registrados y `CIUDADANO` recibe los permisos propios mediante el script de datos iniciales.

## Alcance actual

Los incrementos HU-21 y HU-22 implementan el alta, listado, consulta, edición, publicación, cierre e inscripción ciudadana. El registro de asistencia pertenece a HU-23 y todavía no forma parte de esta funcionalidad.
