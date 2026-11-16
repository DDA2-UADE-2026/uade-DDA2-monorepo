# 04. Beneficios y seguimiento social

[Índice](README.md) · [Anterior](03-evaluacion-visitas-y-resolucion.md) · [Siguiente: salud y actividades](05-salud-y-actividades.md)

## 1. Otorgar una prestación concreta

Una solicitud `APPROVED` habilita el otorgamiento administrativo; todavía no significa que todas las prestaciones de su edición hayan sido concedidas.

| Paso | Operación | Control y resultado | Evento asociado |
| --- | --- | --- | --- |
| 1 | Seleccionar solicitud aprobada. | Permiso `applications:management:benefits:assign`, cuenta/rol vigentes y edición `ACTIVE`. | Sin evento externo. |
| 2 | Seleccionar beneficio de su edición. | Una sola asignación de ese beneficio de catálogo por solicitud; el titular se toma del caso. | Sin evento externo. |
| 3 | Indicar vigencia. | Fechas inclusivas dentro de la edición y `startDate <= hoy <= endDate`. No admite vigencia exclusivamente futura o ya vencida. | Sin evento externo. |
| 4 | Revalidar incompatibilidades. | Otras solicitudes aprobadas del mismo titular en programas incompatibles y con ediciones superpuestas pueden impedir el otorgamiento. | Sin evento externo. |
| 5 | Guardar otorgamiento. | Copia tipo, descripción y monto del catálogo; UUID propio, estado `ACTIVE`, autor y fecha de asignación. | Hecho interno `BenefitChanged` a partir del historial `ASSIGNED`. |
| 6 | Informar a Rentas. | Un mensaje por otorgamiento, manteniendo el mismo `benefitId` durante toda su vida. | Salida `socialBenefitUpdated`, `status=APPROVED`. |
| 7 | Consultar. | Gestión consulta otorgamientos/historial; el titular consulta beneficios propios sin motivos ni responsables internos. | Sin evento externo por lectura. |

La asignación se realiza mediante `POST /api/admin/applications/{id}/benefit-grants`. La solicitud sigue aprobada y conserva su reserva: conceder varios beneficios no ocupa varios cupos. En esta operación no se repite la evaluación ni toda la revisión documental.

El monto puede ser null. Una asignación con importe no ejecuta un pago, transferencia o entrega física. Los cambios posteriores de catálogo no alteran las condiciones copiadas en el otorgamiento.

## 2. Mantener el otorgamiento

Las operaciones manuales requieren `applications:management:benefits:manage`, solicitud aprobada y motivo. Cada actuación agrega historial; el actor no se recibe libremente desde el cuerpo.

| Acción | Estado anterior | Estado local resultante | Reglas principales | Salida `socialBenefitUpdated` |
| --- | --- | --- | --- | --- |
| Renovar | `ACTIVE` o `SUSPENDED` | `ACTIVE` | Nuevo fin estrictamente posterior, dentro de edición y no anterior a hoy; conserva inicio e identidad. Reevalúa requisitos actuales, documentos `VALID`, edición activa e incompatibilidades. | `APPROVED`, motivo externo null. |
| Suspender | `ACTIVE` | `SUSPENDED` | Motivo obligatorio; no extiende la vigencia ni libera cupo. Puede hacerse con edición suspendida/cerrada. | `SUSPENDED`, motivo obligatorio. |
| Finalizar | `ACTIVE` o `SUSPENDED` | `FINALIZED` | Efecto inmediato y motivo obligatorio. Conserva fechas pactadas localmente e historial de la actuación. | `FINALIZED`, con fecha efectiva de fin. |
| Vencer | `ACTIVE` o `SUSPENDED` y `endDate < hoy` | `EXPIRED` | Automático, incluso con edición no activa. El último día pactado sigue incluido. | `FINALIZED`, con la fecha final pactada y motivo de vencimiento. |

Renovar un suspendido lo deja activo; no existe una operación independiente de reanudación que conserve toda la vigencia anterior. Una renovación debe realizarse hasta el último día de vigencia inclusive. `FINALIZED` y `EXPIRED` son terminales.

Cada renovación conserva además una copia de los requisitos comprobados. La vigencia y monto originales no se reemplazan por cambios posteriores del catálogo. Suspender/finalizar no vuelve a exigir la elegibilidad completa que sí exige renovar.

### Equivalencia de estados externos

```text
Local ACTIVE       -> evento APPROVED
Local SUSPENDED    -> evento SUSPENDED
Local FINALIZED    -> evento FINALIZED
Local EXPIRED      -> evento FINALIZED
```

El rechazo de una solicitud no genera un beneficio rechazado: el contrato no incluye `socialBenefitUpdated` con `REJECTED`.

## 3. Vencimiento, cierre y liberación de cupo

1. El coordinador de vencimientos revisa solicitudes con otorgamientos vencidos al iniciar el backend y, por defecto, cada cinco minutos.
2. Convierte en `EXPIRED` los otorgamientos `ACTIVE` o `SUSPENDED` cuyo fin es anterior a hoy en la zona configurada.
3. Agrega historial de sistema y, con negocio Core habilitado, una salida `socialBenefitUpdated/FINALIZED` por otorgamiento vencido.
4. Si queda otro otorgamiento `ACTIVE` o `SUSPENDED`, conserva la solicitud `APPROVED` y su cupo.
5. Al terminar el último pendiente, la solicitud pasa a `CLOSED` y libera exactamente una incorporación, aunque existan beneficios de catálogo nunca concedidos.
6. Se conservan resolución original, responsable y trabajador. El historial, cierre, cupo, auditoría y outbox comparten la transacción del caso.

Un contador inconsistente revierte el caso y debe corregirse; no se resta por debajo de cero. Una parada del servidor no pierde vencimientos: el siguiente barrido retoma los pendientes. Finalizar manualmente el último otorgamiento aplica la misma regla de cierre/cupo.

## 4. Restricción o fallecimiento informado por M1

| Entrada | Trabajo de M8 | Salida |
| --- | --- | --- |
| `citizenUpdated/BLOCKED` | Marca restricción en el snapshot y la expone en alertas. Impide nuevas solicitudes con negocio habilitado. No suspende automáticamente prestaciones existentes. | Sin evento de beneficio automático. |
| `citizenUpdated/DECEASED` | Para el usuario vinculado, finaliza otorgamientos activos y suspendidos de los casos abiertos, cancela actuaciones programadas y cierra solicitudes abiertas. Libera reservas de las aprobadas. | Un `socialBenefitUpdated/FINALIZED` por otorgamiento afectado. |

El motivo de finalización por fallecimiento es `Fallecimiento del titular`. Se conserva la fecha efectiva `deceasedAt` y se usa como `causationId` el `eventId` recibido. La fecha efectiva puede ser anterior a la carga del evento; las fechas pactadas originales se preservan en el modelo/historial.

La inbox, los cierres, cancelaciones, contador y publicaciones causales participan en la misma transacción. Reprocesar el mismo `eventId` no duplica prestaciones finalizadas ni mensajes. Una persona desconocida no se aprovisiona por recibir `citizenUpdated`.

## 5. Crear un plan de intervención

El seguimiento social tiene un ciclo propio y puede continuar aunque el trámite haya sido rechazado o cerrado.

| Paso | Acción | Reglas y efecto | Evento externo |
| --- | --- | --- | --- |
| 1 | El trabajador asignado abre el plan de su caso. | Debe existir evaluación; una solicitud admite un solo plan. Se permiten `IN_EVALUATION`, `IN_VISIT`, `WAITLISTED`, `APPROVED`, `REJECTED` y `CLOSED`. | Sin evento externo. |
| 2 | Define objetivos. | Al menos uno, con descripción y orden. | Sin evento externo. |
| 3 | Define acciones por objetivo. | Al menos una, con inicio y fin previsto coherentes; comienzan `PENDING` y quedan a cargo del responsable. | Sin evento externo. |
| 4 | Guarda la estructura inicial. | Fechas y estructura quedan fijas en este circuito. No hay edición, borrado ni reasignación del plan. | Sin evento externo. |
| 5 | Consulta el plan. | Personal con permiso profesional puede leerlo; el titular tiene una vista propia separada. | Sin evento externo. |

Las fechas de acciones representan planificación y no reservas de la agenda de turnos o visitas. Crear el plan actualiza la modificación del caso, pero no cambia su resolución, cupo ni beneficios.

## 6. Registrar seguimientos y resultados

1. El responsable elige una acción y registra `occurredOn`, notas obligatorias, estado resultante y resultado cuando corresponde.
2. La fecha real debe estar entre el inicio de la acción y hoy; no puede retroceder frente al último seguimiento de esa acción. Se admiten registros históricos y posteriores al fin previsto.
3. Desde `PENDING` se puede avanzar a `IN_PROGRESS` o `COMPLETED`. Desde `IN_PROGRESS` se permite otro avance o completar. `COMPLETED` no admite más seguimientos ni reapertura.
4. Completar exige resultado. Cada entrada conserva secuencia, autor, instante y estados anterior/resultante y no se modifica ni elimina.
5. Si todas las acciones terminan, el estado calculado del plan es `COMPLETED`. Esto no cierra automáticamente la solicitud ni termina sus beneficios.

La vista ciudadana calcula `PENDING` cuando todas las acciones están pendientes, `COMPLETED` cuando todas terminaron e `IN_PROGRESS` en cualquier otro caso, incluso si combina acciones terminadas y pendientes. No existe un cambio de estado independiente del plan que reemplace ese cálculo.

No hay publicación externa de un seguimiento en el catálogo actual.

## 7. Publicar un resumen para el ciudadano

| Acción | Resultado | Evento externo |
| --- | --- | --- |
| Crear primera publicación | El responsable redacta un resumen específico; no se copian automáticamente notas ni resultados privados. | Sin evento externo. |
| Corregir o republicar | Actualiza el texto visible y sus fechas; el seguimiento profesional original no cambia. | Sin evento externo. |
| Retirar | Oculta el resumen al ciudadano y conserva los datos administrativos. | Sin evento externo. |
| Consultar como titular | Ve únicamente seguimientos con publicación vigente, además de objetivos, acciones y progreso propios. | Sin evento externo. |

Este uso de la palabra «publicar» se refiere a visibilidad dentro del portal y no a publicar un evento en RabbitMQ. Un resumen idéntico ya visible o un retiro repetido no agrega cambios innecesarios.

## 8. Referencias

- [Otorgamiento](../../src/main/kotlin/com/uade/dda2/server/feature/benefit/service/BenefitGrantService.kt), [ciclo de vida](../../src/main/kotlin/com/uade/dda2/server/feature/benefit/service/BenefitGrantLifecycleService.kt) y [vencimientos](../../src/main/kotlin/com/uade/dda2/server/feature/benefit/service/BenefitGrantExpirationService.kt).
- [Aplicación de eventos de Ciudadanos](../../src/main/kotlin/com/uade/dda2/server/feature/integration/service/CitizenIncomingService.kt).
- [Planes profesionales](../functional/intervention-plans.md) y [consulta/publicación ciudadana](../functional/citizen-intervention-plans.md).
- [Reglas exactas del evento de beneficios](07-catalogo-de-eventos.md).
