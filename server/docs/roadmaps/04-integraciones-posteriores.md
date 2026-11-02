# Integraciones de negocio pendientes

Estado: diferidas por indicación del equipo. Este documento conserva los eventos y las preguntas detectadas; no autoriza implementar sus handlers, modificar flujos de negocio ni registrar suscripciones mientras se completa la [fase Core](README.md).

Los nombres siguientes reproducen la lista editada aportada por el equipo. Los ejemplos de los PDFs se usarán para revisar payloads al retomar cada integración, sin reemplazar automáticamente esa lista.

## Catálogo previsto

| Dirección desde Desarrollo Social | Evento | Contraparte |
| --- | --- | --- |
| Publica | `socialDocumentationRequested` | Ciudadanos. |
| Publica | `socialBenefitUpdated` | Rentas y Ciudadanos. |
| Publica | `ticketUpdated` | Atención Ciudadana. |
| Consume | `socialDocumentationDelivered` | Ciudadanos. |
| Consume | `citizenUpdated` | Ciudadanos. |
| Consume | `overdueDebt` | Rentas. |
| Consume | `debtSettled` | Rentas. |
| Consume | `exemptionRequested` | Rentas. |
| Consume | `updateExemptionStatus` | Rentas. |
| Consume | `ticketUpdated` | Atención Ciudadana. |

Estados previstos para `socialBenefitUpdated`: `APPROVED`, `REJECTED`, `SUSPENDED` y `FINALIZED`.

## Diferencias de contratos que deben resolverse

| Tema | Lista editada | PDFs | Acuerdo pendiente |
| --- | --- | --- | --- |
| Publicación de tickets | `ticketUpdated` | Los tres PDFs usan `updateTicketStatus` para la salida. | Confirmar el nombre con M2 y Core. Si se comparte nombre en ambos sentidos, definir esquema, discriminación de payloads y ruteo para evitar procesar la propia salida. |
| Deuda vencida | `overdueDebt` | La primera versión coincide; V2 y V3 usan `debtOverdue`. | Confirmar el nombre que publica Rentas y registra Core. |
| Rechazo de beneficio | Incluye `REJECTED`. | V3 desarrolla la tabla de `APPROVED`, `SUSPENDED` y `FINALIZED`. | Definir campos e identificadores del rechazo cuando aún no existe un beneficio otorgado. |
| Actor de empleados | Sin detalle en la lista. | V3 identifica `updatedBy.id` con `actorId` UUID de M1; permite null mientras no esté desplegado. | Confirmar disponibilidad real y conservar la atribución local del actor. |

No registrar alias ni deducir compatibilidad entre nombres distintos. La versión del archivo PDF tampoco determina la `eventVersion` transmitida.

## Diferencias adicionales del seed de Core

El ZIP `core_desappsii-main.zip` conserva otro estado del catálogo en `backend/app/seeds/board_events.py`. No es evidencia del registro que está desplegado ni reemplaza la lista editada del equipo. El seed agrega registros faltantes y no constituye una migración automática de nombres o una limpieza de contratos anteriores.

| Área | Seed de Core | Lista editada y consecuencia posterior |
| --- | --- | --- |
| Publicación hacia M2 | M8 publica `updateTicketStatus`; M2 consume `updateTicketStatus`. | El equipo indicó `ticketUpdated`. Con el ruteo actual, ese nombre no equivale a `updateTicketStatus` ni se traduce automáticamente; requiere acuerdo antes de habilitarlo. |
| Datos ciudadanos | M8 consume `citizenRegistered` y `addressUpdated`. | La lista utiliza `citizenUpdated`; revisar qué productores y schemas se mantienen activos. |
| Exenciones | M8 consume `exemptionApproved` y `exemptionRejected`. | La lista incluye `exemptionRequested` y `updateExemptionStatus`; preparar un cambio deliberado de registro al retomar Rentas. |
| Deuda vencida | Rentas publica `debtOverdue` y M8 consume `overdueDebt`. | El propio `MISMATCHES` del seed registra la diferencia. Core no implementa un alias que resuelva el ruteo. |
| Coincidencias | `socialDocumentationRequested`, `socialBenefitUpdated`, `socialDocumentationDelivered`, `debtSettled` y consumo de `ticketUpdated`. | Coincidir en el nombre no acredita coincidencia de schema, payload o regla de negocio. |

Consultar el estado actual por HTTP antes de editarlo. No ejecutar el seed de Core desde M8 para actualizar suscripciones ni registrar en esta fase todos los contratos futuros. El [contrato técnico verificado](00-contrato-core-verificado.md) explica el alcance real de esas consultas y modificaciones.

Si se conserva `ticketUpdated` en ambos sentidos, Core puede devolver a M8 su propia publicación porque no filtra por origen. Además, el schema de `data` es único por tipo, sin selección por versión u origen. El acuerdo posterior debe resolver ambos puntos, no solo renombrar un DTO.

## Ciudadanos

Al retomar esta integración, definir:

- Contrato de login externo, validación de identidad y vinculación con cuentas locales. El token de máquina de Core no cumple esta función.
- Aprovisionamiento cuando llega `citizenUpdated` para un ciudadano sin usuario local. El modelo actual tiene `externalCitizenId` y `CitizenSnapshot`, pero la snapshot depende de `users` y el ejemplo de registro no incluye email.
- Qué datos se conservarán y cómo se aplicarán actualizaciones parciales, tardías o anteriores al alta local.
- Efecto de los cambios `BLOCKED` y `DECEASED` sobre acceso y trámites, sin deducir cancelaciones automáticas.
- Disponibilidad del `actorId` de empleados, preservando `users.id` en las relaciones internas.

## Documentación

Al retomar esta integración, definir:

- Cuándo emitir `socialDocumentationRequested`, cómo reenviar pedidos y cómo mapear `requiredDocuments.type` con el `code` de nuestros requisitos.
- Formato externo de `applicationNumber`: el ejemplo usa `SOL-2026-000456`, mientras que el modelo local tiene un número `Long`.
- API y autorización para consultar o descargar el contenido referenciado por `socialDocumentationDelivered`. El evento contiene referencias documentales; nuestro modelo actual exige archivo y contenido local.
- Relación entre solicitud, edición, requisito, expediente y documento externo.
- Diferencia entre documento entregado, solicitud documental `COMPLETED` y revisión local `VALID`.

## Atención Ciudadana

Al retomar esta integración, definir:

- Gestión de la derivación y vínculo posterior con una solicitud. El payload `ROUTED` de ejemplo no determina convocatoria y el alta local requiere titular, convocatoria y registrante.
- Persistencia de `ticketId`, evento causal, correlación, último `data.updatedAt` y ciclo de reasignación.
- Aplicación de las reglas de V3: área M8, deduplicación, orden por `data.updatedAt`, reasignación, cancelación, reapertura y filtros de reclasificación.
- Emisión de `STARTED` una vez por derivación; luego avances y resultados, con la semántica acordada para `RESOLVED`, `REJECTED` y `RETURNED`.
- Efecto de cancelaciones y reaperturas sobre una solicitud o un beneficio ya existente. Cerrar el ticket no implica por sí solo revertirlos.

## Beneficios y Rentas

Al retomar estas integraciones, definir:

- Momento de emisión de `socialBenefitUpdated`: aprobación de solicitud y otorgamiento son operaciones distintas en el repositorio.
- Correspondencia de `benefitId` y `benefitTypeId` con otorgamiento y catálogo local, y mapeo de `ACTIVE` a `APPROVED` si se acuerda.
- Payload de `REJECTED`, tratamiento del vencimiento local `EXPIRED` y representación de renovaciones.
- Datos de deuda que se conservarán, reglas de orden y efecto de deuda vencida o cancelada sobre la evaluación social.
- Circuito de `exemptionRequested`: responsable, evaluación, respuesta y vínculo de `requestId` con nuestra actuación y con `socialBenefitUpdated`, si esa es la respuesta acordada.
- Aplicación de `updateExemptionStatus` sin modificar directamente datos propiedad de Rentas.

## Condición para iniciar otra fase

Retomar el módulo que el equipo priorice después del cierre técnico de Core. Antes de implementar, preparar su roadmap específico con contrato confirmado, cambios de modelo, reglas de negocio, permisos, pantallas y pruebas de extremo a extremo. Este backlog no fija un orden entre los módulos posteriores.
