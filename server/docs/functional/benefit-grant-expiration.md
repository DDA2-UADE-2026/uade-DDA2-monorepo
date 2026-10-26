# Vencimiento automático de beneficios otorgados

Cada otorgamiento tiene su propia `endDate`, indicada por el administrativo al asignarlo. Esa fecha es inclusiva: un beneficio con fin el 31 de octubre puede vencer desde el 1 de noviembre, según la zona horaria configurada del módulo.

El sistema revisa otorgamientos al iniciar el servidor y cada cinco minutos. Los que continúan en `ACTIVE` o `SUSPENDED` con `endDate < hoy` pasan a `EXPIRED`, incluso si la edición está suspendida, cerrada o vencida. Una parada del servidor no pierde vencimientos: el siguiente barrido recupera los atrasados. El estado persistido puede demorar hasta ese barrido; la demora no extiende la vigencia.

`EXPIRED` es definitivo. No admite renovación, suspensión, finalización ni reapertura. Las renovaciones deben registrarse hasta el último día de vigencia inclusive: una renovación tardía devuelve `409 BENEFIT_GRANT_INVALID_TRANSITION`, aunque el barrido aún no haya cambiado el estado; si la solicitud ya se cerró aplica el conflicto de solicitud no aprobada.

## Historial y reserva

El vencimiento conserva identidad, fechas pactadas, tipo, descripción, monto, responsable y fecha originales del otorgamiento. Agrega una actuación `EXPIRED` con motivo `Vencimiento automático de vigencia.`, responsable ID `null`, nombre `Sistema` e instante UTC real del procesamiento con precisión de microsegundos. No se atribuye la actuación a un usuario humano ni se inventa una fecha histórica de procesamiento. La fecha final pactada permite identificar cuándo terminó la vigencia.

Si quedan otros otorgamientos `ACTIVE` o `SUSPENDED`, la solicitud sigue `APPROVED` y conserva su reserva. Cuando termina el último, por vencimiento o finalización manual, pasa a `CLOSED` y libera exactamente una reserva. Se conservan la resolución original y el trabajador asignado; se actualiza `updatedAt`. Los planes de intervención y sus seguimientos continúan disponibles.

El proceso se confirma por solicitud: vencimientos, historial, eventual cierre, contador y auditoría se guardan juntos. Si falla alguno, se revierte ese caso completo, se continúa con los demás y se reintenta en el próximo barrido. Un contador inconsistente impide liberar la reserva y provoca rollback.

## Consulta y operación

El vencimiento no agrega rutas para disparar el proceso. Los GET administrativos de otorgamientos e historial muestran `EXPIRED` y la actuación de sistema con los permisos existentes. El [listado propio de beneficios](citizen-benefit-grants.md) muestra `EXPIRED` sin motivos, responsables ni historial profesional. Las respuestas existentes de solicitudes mantienen su estructura.

La configuración predeterminada es:

| Variable | Valor | Uso |
| --- | --- | --- |
| `BENEFIT_GRANT_EXPIRATION_ENABLED` | `true` | Habilita tanto el barrido inicial como el programado. |
| `BENEFIT_GRANT_EXPIRATION_CRON` | `0 */5 * * * *` | Ejecuta cada cinco minutos. `-` desactiva solo el cron; el arranque sigue habilitado. |
| `BENEFIT_GRANT_EXPIRATION_BATCH_SIZE` | `100` | Solicitudes por lote; debe ser positivo. |
| `ENROLLMENT_PERIOD_TIME_ZONE` | `America/Argentina/Buenos_Aires` | Misma zona ya usada por asignaciones y renovaciones. |

Antes de desplegar, ejecutar `docs/database/migrations/2026-10-26-benefit-grant-expiration.sql`, después de las migraciones de otorgamiento y ciclo de vida. La migración es repetible, actualiza restricciones e índices y conserva los datos. Los otorgamientos anteriores ya vencidos se procesarán al iniciar la nueva versión, con su auditoría correspondiente.

El perfil `docs` deshabilita el scheduler para pruebas y generación de OpenAPI. Las pruebas específicas invocan el servicio y el coordinador con un reloj controlado. No se agregan endpoints para disparar vencimientos, notificaciones, pagos, entregas ni promoción de lista de espera.
