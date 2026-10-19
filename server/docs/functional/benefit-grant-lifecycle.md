# Renovación, suspensión y finalización de beneficios

El administrativo mantiene un otorgamiento existente mediante `applications:management:benefits:manage`, asignado inicialmente a `ADMIN`. Se verifica usuario activo, rol activo y permiso vigente en el JWT y la base de datos. Todas las operaciones exigen solicitud `APPROVED` y motivo recortado de 1 a 1000 caracteres.

## Transiciones y vigencia

| Operación | Estado previo | Estado resultante |
| --- | --- | --- |
| Renovar | `ACTIVE` o `SUSPENDED` | `ACTIVE` |
| Suspender | `ACTIVE` | `SUSPENDED` |
| Finalizar | `ACTIVE` o `SUSPENDED` | `FINALIZED` |

`FINALIZED` es definitivo. No se puede reabrir ni renovar. Suspender nuevamente tampoco se admite. Un conflicto no aplica otra decisión ni modifica datos.

Renovar extiende el mismo otorgamiento, conservando inicio, datos copiados del catálogo y responsable/fecha de asignación original. El nuevo fin debe ser estrictamente posterior al anterior, no anterior a hoy y estar dentro de las fechas de la edición. Las fechas son inclusivas y hoy se calcula en la zona configurada. Puede renovarse un otorgamiento vencido cuyo estado siga siendo `ACTIVE` o `SUSPENDED`; no existe vencimiento automático.

La renovación exige edición `ACTIVE`, todos los requisitos de negocio vigentes cumplidos, documentos obligatorios en `VALID` y ninguna aprobación incompatible. El checklist contiene un resultado booleano por requisito actual, sin duplicados ni requisitos ajenos; `[]` es válido si no hay requisitos. Se conserva una copia de cada requisito evaluado. No se exige convocatoria abierta, nueva evaluación profesional ni disponibilidad adicional de cupo.

Las incompatibilidades se verifican en ambos sentidos: otras solicitudes `APPROVED` del titular, de programas incompatibles y con fechas de edición superpuestas, incluyendo sus extremos.

Suspender o finalizar admite edición suspendida, cerrada o vencida y no repite controles de elegibilidad. Finalizar tiene efecto inmediato, pero conserva las fechas pactadas; la fecha real queda en el historial.

## Solicitud y cupo

Suspender o renovar conserva la reserva. Finalizar uno de varios otorgamientos tampoco libera cupo mientras exista otro `ACTIVE` o `SUSPENDED`.

Al finalizar el último otorgamiento pendiente, la solicitud cambia a `CLOSED` y libera exactamente una reserva de la edición, aunque queden beneficios del catálogo nunca otorgados. Una solicitud cerrada no admite más asignaciones ni cambios de beneficios. Se conservan motivo, responsable y fecha de la resolución original y el trabajador asignado; toda actuación actualiza `updatedAt`.

## API administrativa

Base: `/api/admin/applications/{id}/benefit-grants/{grantId}`.

- `POST /renew`: recibe `reason`, `endDate` y `requirementChecks`.
- `POST /suspend` y `POST /finalize`: reciben exclusivamente `reason`.
- Las tres operaciones devuelven `200` con el otorgamiento actualizado.
- `GET /history`: exige `applications:management:view` y devuelve `200` con el historial ordenado por secuencia.

Ejemplo de renovación:

```json
{
  "reason": "Se mantiene la necesidad de asistencia.",
  "endDate": "2026-12-31",
  "requirementChecks": [
    { "requirementId": "UUID_DEL_REQUISITO", "fulfilled": true }
  ]
}
```

Para suspender o finalizar:

```json
{ "reason": "Finalizó la necesidad del beneficio." }
```

No se admiten campos adicionales. Titular, responsable, estado y fecha los obtiene el servidor.

| Código | Situación |
| --- | --- |
| `400` | Motivo, cuerpo, identificadores, checklist o fechas inválidos. |
| `401` / `403` | Acceso inválido, usuario inactivo o permiso ausente del JWT/rol activo vigente. |
| `404` | Solicitud inexistente u otorgamiento inexistente o ajeno a la solicitud. |
| `409` | Solicitud no aprobada, transición inválida, edición no activa para renovar, requisitos/documentos incumplidos, incompatibilidad o contador inconsistente. |

## Historial y conservación

El historial inmutable comienza con `ASSIGNED` y agrega `RENEWED`, `SUSPENDED` y `FINALIZED`. Cada registro incluye identificadores, secuencia, estado anterior/resultante, fechas anteriores/resultantes, motivo, responsable y fecha UTC. Las renovaciones incluyen identificador, tipo, valor, descripción y resultado de los requisitos vigentes evaluados, aunque el catálogo cambie después.

La asignación inicial no tiene motivo ni estado previo. Para otorgamientos anteriores a esta historia, la migración reconstruye solamente esa asignación conocida con sus datos originales; no inventa cambios ni motivos.

Las consultas administrativas existentes muestran la vigencia y el estado actual. Las respuestas ciudadanas mantienen su estructura y no exponen otorgamientos, motivos ni historial.

## Consistencia y límites

Estado, fechas, historial, controles, solicitud, cupo y auditoría se confirman juntos. Un fallo revierte todo. La auditoría general registra identificadores, transición, responsable, vigencia e instante, sin motivos, descripción ni monto.

No se pueden eliminar responsables referenciados por el historial ni beneficios otorgados. Tampoco se puede acortar una edición dejando vigencias renovadas fuera de sus fechas. El historial no tiene operaciones de modificación o eliminación.

No se agregan reanudación independiente, reapertura de finalizados, vencimiento automático, promoción automática de lista de espera, entregas, pagos, notificaciones ni integración por eventos.
