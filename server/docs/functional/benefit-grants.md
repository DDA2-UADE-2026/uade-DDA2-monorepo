# Otorgamiento individual de beneficios

El administrativo con `applications:management:benefits:assign` registra el otorgamiento de un beneficio a una solicitud `APPROVED`. La edición debe seguir `ACTIVE`. El permiso debe estar presente en el JWT y en el rol activo vigente en base de datos, y el usuario debe estar activo.

## Condiciones y vigencia

- Se selecciona un beneficio del catálogo de la edición de la solicitud. No se puede elegir otro ciudadano: el titular se obtiene de la solicitud.
- Se puede otorgar cada beneficio una sola vez por solicitud. Distintos beneficios de una misma edición pueden otorgarse por separado.
- `startDate` y `endDate` son fechas inclusivas. Deben estar dentro de la edición y cumplir `startDate <= hoy <= endDate`, según la zona configurada del módulo.
- Se admiten vigencias de un solo día y fechas de inicio anteriores a la aprobación, siempre que se cumplan las condiciones de vigencia. No se admiten períodos exclusivamente futuros o vencidos.
- Se vuelve a comprobar la regla de incompatibilidades: otras solicitudes `APPROVED` del titular en programas incompatibles con fechas de edición superpuestas, incluyendo extremos. No se repite la evaluación ni la revisión documental.

El otorgamiento copia tipo, descripción y monto del catálogo y queda en `ACTIVE`. El administrativo no puede personalizar esos datos. El monto puede ser nulo cuando el beneficio configurado no tiene importe.

La solicitud conserva `APPROVED`, su motivo, fecha y responsable de resolución y el trabajador asignado. Solo se actualiza `updatedAt`. El cupo ya se ocupó al aprobar; otorgar uno o varios beneficios no incrementa nuevamente `currentEnrollment` ni exige disponibilidad adicional.

## Registro y consulta administrativa

`POST /api/admin/applications/{id}/benefit-grants` devuelve `201` con el otorgamiento registrado:

```json
{
  "programBenefitId": "UUID_DEL_BENEFICIO",
  "startDate": "2026-10-01",
  "endDate": "2026-12-31"
}
```

La respuesta contiene `id`, `applicationId`, `userId`, `programBenefitId`, `benefitType`, `description`, `amount`, `startDate`, `endDate`, `status`, `assignedByUserId`, `assignedByUserName` y `assignedAt`. El responsable se obtiene de la sesión y `assignedAt` es un instante UTC asignado por el servidor. No se aceptan campos adicionales en el cuerpo.

`GET /api/admin/applications/{id}/benefit-grants` exige `applications:management:view` y devuelve los otorgamientos en orden ascendente por `assignedAt` e identificador. Una solicitud existente sin otorgamientos devuelve `[]`. El permiso de consulta es independiente del permiso para otorgar.

| Código HTTP | Situación |
| --- | --- |
| `400` | Cuerpo, UUID, fechas o campos adicionales inválidos; vigencia fuera de la edición o que no incluye hoy. |
| `401` / `403` | Acceso inválido, usuario inactivo o permiso ausente en JWT/rol activo vigente. |
| `404` | Solicitud inexistente, beneficio inexistente o perteneciente a otra edición. |
| `409` | Solicitud no aprobada, edición no activa, beneficio ya otorgado o incompatibilidad. |

La consulta ciudadana de solicitudes conserva su contrato: no incorpora importes, otorgamientos ni responsables. Esta historia agrega consulta solo administrativa.

## Conservación y límites

Los cambios posteriores del catálogo no cambian el tipo, descripción ni monto copiados en otorgamientos anteriores. Sí afectan los otorgamientos posteriores. No se puede eliminar un beneficio con otorgamientos ni al administrativo responsable referenciado. Las fechas de una edición no pueden acortarse si dejan fuera vigencias otorgadas.

Registro, actualización de la solicitud y auditoría se confirman en una transacción; un fallo revierte todo. La auditoría conserva identificadores, estado, responsable, vigencia y fecha, sin copiar descripción ni monto. La unicidad en base y los bloqueos evitan duplicados concurrentes.

No se generan otorgamientos al aprobar ni al migrar datos existentes. `ACTIVE` es el estado inicial registrado. El [vencimiento automático](benefit-grant-expiration.md) cambia a `EXPIRED` los otorgamientos activos o suspendidos cuya fecha final ya pasó; el último día sigue siendo inclusivo. La renovación, suspensión y finalización de otorgamientos existentes se describen en [Ciclo de vida de beneficios](benefit-grant-lifecycle.md). Entregas, pagos, notificaciones e integración por eventos quedan fuera del alcance.
