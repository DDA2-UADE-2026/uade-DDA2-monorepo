# 03. Evaluación, visitas y resolución

[Índice](README.md) · [Anterior](02-programas-y-solicitudes.md) · [Siguiente: beneficios y seguimiento](04-beneficios-y-seguimiento.md)

## 1. Asignar y evaluar el caso

| Paso | Actor y operación | Condiciones y efecto | Evento externo |
| --- | --- | --- | --- |
| 1 | Personal autorizado consulta la bandeja. | Permiso administrativo; permite localizar solicitud, titular, programa, estado y responsable. | Sin evento externo. |
| 2 | Asigna un trabajador. | Cuenta activa, distinta del titular y con permisos profesionales requeridos. No se reasigna dejando visitas programadas a cargo del trabajador anterior. | Si hay ticket vinculado gestionable, puede salir `updateTicketStatus` de tipo `STARTED`. |
| 3 | El profesional registra evaluación. | Una evaluación por solicitud; no evalúa su propio caso ni otro ya asignado a un trabajador distinto. | Hecho interno `WorkerAssigned`, con el mismo efecto condicional sobre el ticket. |
| 4 | Guarda vulnerabilidad, prioridad y recomendación. | La recomendación es obligatoria. El profesional queda asignado y la solicitud pasa a `IN_EVALUATION`. | No existe un evento externo específico de «evaluación creada». |
| 5 | Consulta evaluación y detalle. | La lectura profesional requiere permiso propio; la consulta ciudadana no expone la recomendación interna. | Sin evento externo. |

La evaluación se admite desde `SUBMITTED`, `IN_VALIDATION`, `PENDING_DOCUMENTATION` o `IN_EVALUATION`, siempre que todavía no exista una. La prioridad y la vulnerabilidad se registran como valoración profesional; este alta no calcula automáticamente ingresos o composición familiar desde M1.

La asignación manual y la evaluación son acciones diferentes. Si nadie estaba asignado, quien evalúa queda asignado. Una desasignación previa a la evaluación puede estar permitida; no se retira el responsable de un caso ya evaluado desde ese flujo.

## 2. Programar una visita domiciliaria o entrevista

| Paso | Operación | Resultado | Evento externo |
| --- | --- | --- | --- |
| 1 | El trabajador asignado decide si necesita actuación. | Debe existir evaluación y estado admitido para visitas. No toda resolución exige una visita. | Sin evento externo. |
| 2 | Informa tipo, inicio con zona, duración y lugar. | Se valida fecha futura, rango y ausencia de superposiciones del titular y profesional. | Sin evento externo antes de confirmar. |
| 3 | Confirma agenda. | Se crea actuación `SCHEDULED`; la solicitud pasa a `IN_VISIT`. | Hecho interno `VisitScheduled`. |
| 4 | Informa avance al ticket vinculado, si corresponde. | La gestión debe seguir siendo de M8, estar abierta y haber sido tomada cuando corresponde. | Salida `updateTicketStatus` con `PROGRESS`, fecha/franja y avance 50. |
| 5 | El ciudadano consulta sus actuaciones. | Ve tipo, fecha, horario, lugar y estado, sin notas profesionales internas. | Sin evento externo. |

Las visitas y los turnos comparten controles de agenda: una superposición puede impedir una reserva de salud o una nueva visita, aunque pertenezcan a funcionalidades distintas. Los intervalos contiguos sí pueden utilizarse.

Un ticket terminal, devuelto o reasignado no recibe avances por estas operaciones. Si hay un pedido de información pendiente de M2, las publicaciones posteriores están restringidas y el caso de uso puede devolver conflicto; no se informa un éxito parcial fuera de la transacción.

## 3. Reprogramación por el responsable

1. El responsable consulta una actuación propia todavía `SCHEDULED`, cuyo inicio aún no llegó, dentro de una solicitud `IN_VISIT`.
2. Indica el nuevo horario, duración y lugar; debe existir un cambio y pasar nuevamente los controles de agenda.
3. Si hay una solicitud ciudadana de reprogramación pendiente, se resuelve por su circuito de revisión; no se pisa silenciosamente con la gestión directa.
4. La actuación conserva identidad y responsable y actualiza su planificación.
5. Se publica el hecho interno `VisitScheduled`; si el ticket vinculado admite avances, se encola otro `updateTicketStatus/PROGRESS` con la nueva franja.

## 4. Pedido de reprogramación del ciudadano

| Paso | Actor | Acción y efecto | Evento externo |
| --- | --- | --- | --- |
| 1 | Titular | Solicita reprogramación con motivo sobre una actuación propia futura y programada. | Sin evento externo; el horario no cambia. |
| 2 | Sistema | Conserva el pedido `PENDING`; evita pedidos pendientes simultáneos para esa actuación. | Sin evento externo. |
| 3 | Responsable | Consulta el motivo y decide. | Sin evento externo. |
| 4a | Responsable | Aprueba con un nuevo horario, duración y lugar válidos. | `VisitScheduled` interno y, si corresponde, salida `updateTicketStatus/PROGRESS`. |
| 4b | Responsable | Rechaza con comentario. El horario anterior se conserva. | Sin evento externo automático. |
| 5 | Titular | Consulta el resultado del pedido y la actuación actualizada. | Sin evento externo. |

La aprobación vuelve a validar estado, responsabilidad, vigencia y agenda. El pedido no constituye una reserva alternativa ni una cancelación de la actuación original.

## 5. Cancelar o registrar el resultado

| Acción | Condiciones | Efecto local | Evento externo automático |
| --- | --- | --- | --- |
| Cancelar | Responsable vigente, actuación futura `SCHEDULED`, solicitud `IN_VISIT`, motivo y sin pedido de reprogramación pendiente. | Actuación `CANCELLED`; conserva motivo, actor y fecha. | Ninguno específico. |
| Registrar resultado | Responsable vigente, actuación `SCHEDULED` cuya hora final ya llegó y sin pedido pendiente. | `REALIZED` o `NOT_REALIZED`, con observaciones/conclusiones opcionales y autor/fecha. | Ninguno específico. |
| Volver a evaluar | No quedan otras actuaciones `SCHEDULED` de la solicitud. | La solicitud vuelve a `IN_EVALUATION`. | Ninguno específico. |

Estas operaciones no emiten `VisitaSocialRealizada` ni notificaciones externas con ese nombre: no pertenecen al catálogo actual. El personal puede informar un avance por el circuito manual de tickets cuando las reglas lo permitan.

## 6. Resolver con control completo de requisitos

El circuito de resoluciones con historial usa `POST /api/admin/applications/{id}/resolutions` y permiso `applications:management:resolve`. El actor no necesita coincidir con el trabajador asignado.

1. Se comprueba evaluación registrada, estado `IN_EVALUATION` o `WAITLISTED` y ausencia de visitas `SCHEDULED`.
2. El actor presenta una decisión, motivo obligatorio y control de cada requisito general vigente. Si no hay requisitos, el checklist puede estar vacío.
3. El backend verifica pertenencia, completitud y ausencia de duplicados del checklist. El personal es responsable de la comprobación material de edad, ingresos, residencia e hijos.
4. Para aprobar o esperar se exige elegibilidad: todos los requisitos cumplidos, documentos obligatorios `VALID`, edición activa no vencida y ausencia de incompatibilidades aplicables.
5. Se verifica el cupo y se guarda una resolución inmutable con copias de los requisitos evaluados.
6. Estado, contador, historial, auditoría y cualquier salida a Core se confirman juntos.

| Decisión | Disponibilidad exigida | Resultado de solicitud | Efecto sobre cupo | Evento al ticket gestionable vinculado |
| --- | --- | --- | --- | --- |
| `APPROVED` | Debe haber lugar. | `APPROVED`. | Reserva una incorporación. | `updateTicketStatus/RESOLVED`, `resolution.type=REQUEST_FULFILLED`. |
| `WAITLISTED` | Debe estar agotado, además de cumplir elegibilidad. | `WAITLISTED`. | No reserva. | Sin salida automática de resolución. Puede informarse avance manual permitido. |
| `REJECTED` | No exige cupo ni elegibilidad positiva. | `REJECTED`. | No reserva. | `updateTicketStatus/RESOLVED`, `resolution.type=NO_FURTHER_ACTION_REQUIRED`. |

Desde `WAITLISTED` solo se decide aprobación o rechazo; no se vuelve a poner en espera. La salida es manual y revalida las condiciones actuales. Aprobar sin cupo no convierte automáticamente la operación en espera.

La incompatibilidad considera otras solicitudes aprobadas del mismo titular en programas incompatibles con fechas de edición superpuestas, incluyendo extremos. La convocatoria puede haber cerrado sin impedir resolver casos existentes.

**Aprobar una solicitud todavía no genera `socialBenefitUpdated`.** Ese mensaje se produce al otorgar una prestación concreta, descrita en el capítulo siguiente. Rechazar una solicitud no crea un otorgamiento ni un `socialBenefitUpdated/REJECTED`.

## 7. Ruta anterior de resolución que también existe

El backend conserva `POST /api/admin/applications/{id}/resolution`, singular, con `applications:resolutions:create`. No es equivalente a la ruta plural:

- Exige que resuelva el trabajador asignado y evita resolver la solicitud propia.
- Exige evaluación, estado resoluble y ausencia de visitas programadas.
- Exige motivo para rechazo y espera; para aprobación puede ser opcional.
- Para aprobar comprueba pendientes documentales y capacidad, actualiza la reserva y guarda los datos de resolución en la solicitud.
- No recibe ni conserva el checklist/historial completo de la ruta plural, ni ejecuta todos sus controles de elegibilidad.
- Emite el mismo hecho interno `ApplicationResolved`, por lo que puede producir la respuesta correspondiente al ticket vinculado.

Al describir o consumir la API debe elegirse explícitamente la ruta apropiada. Las dos rutas no deben presentarse como alias con idénticas validaciones.

## 8. Estado, acceso e historial

El titular ve su estado, pero la resolución administrativa conserva además motivo, responsable, fecha y, en la ruta plural, historial/checklist. La aprobación no sustituye al trabajador por quien decidió. Aprobar o rechazar no ofrece una transición genérica de reapertura.

Un ticket resuelto y una solicitud resuelta son objetos diferentes: M2 puede permitir confirmación o reapertura del ticket sin reabrir automáticamente la solicitud aprobada o rechazada de M8.

## 9. Referencias

- [Servicio de evaluación](../../src/main/kotlin/com/uade/dda2/server/feature/application/service/ApplicationEvaluationService.kt).
- [Servicio de visitas](../../src/main/kotlin/com/uade/dda2/server/feature/application/service/ApplicationVisitService.kt).
- [Pedidos de reprogramación](../../src/main/kotlin/com/uade/dda2/server/feature/application/service/ApplicationVisitRescheduleRequestService.kt).
- [Resolución con historial](../../src/main/kotlin/com/uade/dda2/server/feature/application/service/ApplicationResolutionService.kt) y [ruta anterior](../../src/main/kotlin/com/uade/dda2/server/feature/application/service/ApplicationService.kt).
- [Reglas de publicación a M2](../../src/main/kotlin/com/uade/dda2/server/feature/integration/service/SocialTicketEventService.kt).
