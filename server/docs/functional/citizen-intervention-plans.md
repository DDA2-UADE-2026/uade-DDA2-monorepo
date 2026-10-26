# Consulta ciudadana de planes de intervención

El ciudadano puede consultar todos los planes de sus solicitudes, incluidas las registradas por un administrativo y los casos rechazados o cerrados. El servidor determina al titular por su autenticación. Los planes existentes también están disponibles; no se crean planes nuevos durante la consulta.

## Objetivos, acciones y progreso

| Método y ruta | Contenido |
| --- | --- |
| `GET /api/intervention-plans` | Planes propios, solicitud de origen, programa, edición y estado calculado. |
| `GET /api/intervention-plans/{planId}` | La misma cabecera y objetivos/acciones en su orden original. |
| `GET /api/intervention-plans/{planId}/actions/{actionId}/follow-ups` | Seguimientos con publicación ciudadana vigente. |

La cabecera contiene `id`, `applicationId`, `applicationNumber`, `programId`, `programName`, `programEditionId`, `programEditionName`, `createdAt` UTC y `status`. Cada objetivo muestra identificador, descripción y acciones. Cada acción muestra identificador, descripción, `startDate`, `dueDate` y estado.

El plan figura `PENDING` cuando todas sus acciones están pendientes; `COMPLETED` cuando todas terminaron; `IN_PROGRESS` en cualquier otro caso, incluida una combinación de acciones terminadas y pendientes. Es un cálculo de lectura, sin un ciclo independiente. Completar acciones no cierra la solicitud ni modifica beneficios o cupos.

Las fechas de las acciones son las acordadas en la planificación. No representan reservas de agenda. Los seguimientos muestran su fecha real `occurredOn`.

## Paginación y mensajes

Los listados aceptan `page` desde cero y `size` entre 1 y 100; por defecto, `page=0&size=20`. Devuelven `content`, `page`, `size`, `totalElements`, `totalPages` y `message`. Los planes se ordenan por creación e identificador descendentes. Los seguimientos públicos mantienen el orden original del registro profesional, sin publicar la secuencia ni contar entradas privadas o retiradas.

Si el total es cero, el mensaje es **“No tenés planes de intervención registrados.”** o **“No hay seguimientos habilitados para tu visualización.”**, respectivamente. Con resultados, `message` es null. Una página fuera de rango conserva los totales y devuelve `content: []`, con mensaje null cuando existen resultados.

Cada seguimiento público contiene exclusivamente `id`, `actionId`, `occurredOn`, `status`, `summary` y `publishedAt` UTC. No muestra notas, resultados profesionales, autores, responsables, motivos ni fechas de gestión internas. El identificador corresponde al seguimiento original, de modo que una corrección actualiza el mismo elemento visible.

## Publicación profesional separada

Los seguimientos comienzan privados, también los anteriores a esta funcionalidad. El trabajador actualmente asignado y responsable del plan puede redactar un resumen específico para el ciudadano, incluso sobre acciones completadas y casos rechazados o cerrados. Nunca se copian automáticamente las notas ni el resultado profesional.

Base: `/api/applications/{id}/intervention-plan/actions/{actionId}/follow-ups/{followUpId}/citizen-publication`.

| Operación | Resultado |
| --- | --- |
| `PUT` con `{ "summary": "Se acordó continuar el acompañamiento." }` | Primera publicación: `201`. Corrección o republicación: `200`. |
| `PUT` con el mismo texto recortado ya visible | `200`, sin cambiar fechas ni repetir auditoría. |
| `DELETE` | `204`, retira la visibilidad y conserva el resumen profesional. Repetirlo o retirar un seguimiento válido nunca publicado no cambia datos. |

El resumen es obligatorio, recortado, de 1 a 2000 caracteres. No se admiten campos adicionales. Cada publicación o corrección fija `publishedAt` al instante actual; el retiro conserva esa fecha y actualiza la fecha de gestión. Republicar vuelve a mostrar el resumen elegido y actualiza ambas fechas.

El historial profesional agrega `citizenPublication`: null si nunca se publicó o un objeto con `id`, `summary`, `visible`, `publishedAt`, `updatedAt`, `updatedByUserId` y `updatedByUserName`. Las versiones retiradas siguen disponibles allí. Solo se conserva la versión actual de la publicación; el seguimiento original permanece inmutable.

## Permisos y errores

La lectura requiere `applications:own:interventions:view`, inicialmente para `CIUDADANO` y `ADMIN`. Publicar o retirar requiere `applications:interventions:follow-ups:publish`, inicialmente para `ADMIN`, además de ser el trabajador asignado y responsable. Ambos permisos se pueden configurar en otros roles. Se comprueba usuario activo, pertenencia al rol activo y permiso vigente tanto en JWT como en base de datos. Los tokens anteriores deben renovarse mediante login o selección de rol para incorporar los permisos nuevos.

Las consultas devuelven `200`, también sin resultados. Identificadores, paginación o resumen inválidos producen `400`; acceso inválido, `401`/`403`. Recursos inexistentes, ajenos o mal asociados a la ruta producen `404`. La gestión de publicación por otro trabajador produce `409`.

La publicación, la actualización de fecha del caso y la auditoría se confirman en una transacción. Cualquier fallo revierte toda la actuación. No se modifican notas, resultados, estados de acciones/solicitud, resolución, beneficios ni cupos. Los logs guardan identificadores, actor, operación, visibilidad y fechas, sin textos. Las consultas son de solo lectura y no generan auditoría.

Aplicar [la migración PostgreSQL](../database/migrations/2026-10-26-citizen-intervention-plans.sql) después de la migración de planes y antes del despliegue. Crea la estructura vacía y permisos de forma idempotente, sin publicar seguimientos anteriores. No se incorpora agenda, edición de estructura, reasignación, historial público de versiones, notificaciones ni cambios de frontend.
