# Resolución de solicitudes

El coordinador o administrativo con `applications:management:resolve` puede aprobar, rechazar o enviar una solicitud a lista de espera. No necesita ser el evaluador ni el trabajador asignado. El permiso debe pertenecer al rol activo y conservarse tanto en el JWT como en la base de datos.

## Condiciones previas

La solicitud debe tener una evaluación profesional registrada y ninguna visita o entrevista `SCHEDULED`. Desde `IN_EVALUATION` admite `APPROVED`, `REJECTED` o `WAITLISTED`; desde `WAITLISTED`, únicamente `APPROVED` o `REJECTED`. No se corrigen ni reabren resoluciones aprobadas o rechazadas.

Toda decisión exige un motivo recortado de 1 a 1000 caracteres y una lista con el resultado de **cada requisito vigente de la edición**. La verificación de edad, ingresos, residencia e hijos es responsabilidad del personal: el backend valida que el checklist esté completo y conserva lo declarado. No calcula esos datos ni los obtiene de Ciudadanos. Una edición sin requisitos admite `requirementChecks: []`.

## Resultado de la evaluación

| Decisión | Condiciones adicionales | Cupo |
| --- | --- | --- |
| `APPROVED` | Todos los requisitos cumplidos, documentos obligatorios `VALID`, edición `ACTIVE` no vencida y sin incompatibilidades. | Debe haber disponibilidad; incrementa `currentEnrollment` en uno. |
| `WAITLISTED` | Las mismas condiciones de elegibilidad de aprobación. | Debe estar agotado; no modifica el contador. |
| `REJECTED` | Admite requisitos incumplidos, documentación pendiente, edición no activa, incompatibilidades o falta de cupo. | No modifica el contador. |

La convocatoria puede estar cerrada. La fecha final de la edición es inclusiva. Las ediciones futuras activas pueden resolver solicitudes ya presentadas. Los documentos opcionales no bloquean una resolución; un obligatorio faltante, `PENDING` u `OBSERVED` impide aprobar y esperar.

Se consideran incompatibles las solicitudes `APPROVED` del mismo titular en programas configurados como incompatibles, en cualquiera de los dos sentidos, cuando las fechas de sus ediciones se superponen. Compartir la fecha de inicio o fin también constituye superposición. Otras personas, otros estados o vigencias separadas no bloquean.

Un error deja intactos estado, historial y cupos. Intentar aprobar sin cupo no envía automáticamente a espera.

## API administrativa

`POST /api/admin/applications/{id}/resolutions` devuelve `201` y la resolución registrada:

```json
{
  "decision": "APPROVED",
  "reason": "Cumple los requisitos y dispone de cupo.",
  "requirementChecks": [
    { "requirementId": "UUID_DEL_REQUISITO", "fulfilled": true }
  ]
}
```

El servidor obtiene el responsable del JWT y asigna la fecha UTC. No se admiten estos campos en el cuerpo, ni propiedades adicionales. Se devuelve `400` por datos inválidos o controles incompletos, duplicados o ajenos; `401`/`403` por acceso inválido; `404` por solicitud inexistente y `409` por conflictos de negocio. Swagger documenta el contrato y los errores.

`GET /api/admin/applications/{id}/resolutions` exige `applications:management:view` y devuelve el historial cronológico. Incluye decisión, estado anterior, motivo, responsable, fecha y copias de los requisitos evaluados. Una solicitud sin resoluciones devuelve `[]`. La consulta no exige permiso para resolver.

El detalle administrativo de la solicitud expone el último `resolutionReason`, `resolvedByUserId`, `resolvedByUserName` y `resolvedAt`. El titular sigue viendo su estado; no accede a motivos, responsables ni historial. El trabajador asignado no se reemplaza por quien resuelve.

## Lista de espera e historial

La salida de espera es manual. Al aprobar o rechazar nuevamente se exige un nuevo motivo y el checklist actual; se revalidan las condiciones y se conserva la resolución anterior. Los documentos pueden actualizarse en espera según las reglas existentes y deben volver a estar `VALID` para aprobar.

Cada resolución es inmutable. Guarda identificador, tipo, valor y descripción del requisito al momento de decidir; una modificación o eliminación posterior del catálogo no altera el historial. El motivo no se duplica en los logs generales. Los responsables históricos no pueden eliminarse.

La aprobación ocupa cupo, pero no crea todavía asignaciones ni entregas de beneficios. No hay promoción automática, orden nuevo de prioridad, notificaciones ni publicación de eventos en esta historia.
