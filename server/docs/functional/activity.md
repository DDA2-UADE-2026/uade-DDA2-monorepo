# Actividades comunitarias

Esta guía describe la administración disponible para actividades, talleres, jornadas y campañas comunitarias. Todos esos nombres representan el mismo concepto de negocio: una actividad municipal que puede publicarse y, en incrementos posteriores, recibir inscripciones y registrar asistencia.

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

## Autorización

La API utiliza los permisos `activities:management:view`, `activities:management:create`, `activities:management:edit` y `activities:management:change-status`. El rol técnico `ADMIN` recibe todos los permisos registrados mediante el script de datos iniciales.

## Alcance actual

Este incremento implementa el alta, listado, consulta, edición, publicación y cierre. La inscripción ciudadana y el registro de asistencia pertenecen a los incrementos HU-22 y HU-23 y todavía no forman parte de esta funcionalidad.
