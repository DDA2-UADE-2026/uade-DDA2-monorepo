# 07. Catálogo de eventos e identificadores

[Índice](README.md) · [Anterior: intercambios](06-integraciones-paso-a-paso.md) · [Siguiente: recorridos completos](08-recorridos-y-estado.md)

Este catálogo describe los contratos implementados. El registro desplegado en Core y sus suscripciones deben contrastarse antes de habilitar el intercambio. La contraparte indicada es funcional; Core realiza el ruteo efectivo.

## 1. Lista completa

| Dirección desde M8 | `eventType` exacto | Contraparte | Momento principal |
| --- | --- | --- | --- |
| Publica | `socialDocumentationRequested` | M1 Ciudadanos | Alta de solicitud cuya edición pide documentos. |
| Publica | `socialBenefitUpdated` | M5 Rentas | Cada cambio de un otorgamiento, no cada cambio de solicitud. |
| Publica | `updateTicketStatus` | M2 Atención Ciudadana | Hecho operativo sobre ticket derivado. |
| Consume | `ticketUpdated` | M2 | Derivación y cambios posteriores. |
| Consume | `socialDocumentationDelivered` | M1 | Estado de los pedidos documentales. |
| Consume | `citizenUpdated` | M1 | Datos, domicilio, bloqueo o fallecimiento. |
| Consume | `debtOverdue` | M5 | Deuda municipal vencida. |
| Consume | `debtSettled` | M5 | Cancelación de deuda. |
| Consume | `exemptionRequested` | M5 | Pedido de exención tributaria. |
| Consume | `updateExemptionStatus` | M5 | Resultado aprobado o rechazado del pedido. |

No hay alias entre `overdueDebt` y `debtOverdue`. M8 consume `ticketUpdated` y publica `updateTicketStatus`; no usa el primero como salida. La versión editorial del documento M2 no cambia automáticamente `eventVersion`.

## 2. Sobre común

| Campo | Regla |
| --- | --- |
| `eventId` | UUID. M8 crea UUID v4 nuevo por hecho; reintentar el mismo envío conserva su ID y contenido. |
| `eventType` | Uno de los nombres exactos de la tabla. |
| `eventVersion` | `"1.0"` para los diez contratos. |
| `occurredAt` | Fecha/hora ISO 8601 con offset. No se publica una fecha sin zona. |
| `sourceModule` | Slug del productor: `desarrollo-social`, `ciudadanos`, `atencion-ciudadana` o `rentas`. Se valida contra el tipo. |
| `data` | Objeto del contrato particular. No se agregan campos de negocio al nivel del sobre. |
| `correlationId` | UUID opcional del hilo de gestión. M8 omite la clave cuando no tiene valor. En entrada se admite ausente o null. |
| `causationId` | UUID opcional del evento que causó este hecho. Misma regla de omisión/null. |

La salida conserva únicamente esas ocho claves posibles. La entrada puede traer metadatos adicionales de Core, como `receivedAt`; no pasan a integrar el hash del sobre lógico.

| Operación | `correlationId` | `causationId` |
| --- | --- | --- |
| Pedido documental | `application.id`. | Ausente: nace de una operación local. |
| Respuesta documental M1 | El mismo `application.id`. | `eventId` del pedido documental propio. |
| Cambio de beneficio | `application.id` del otorgamiento. | Si proviene de otro evento, su `eventId`; por ejemplo, fallecimiento. |
| Respuesta a ticket | Conserva la correlación de la continuación de M2 a la que responde. | `eventId` de `ROUTED`, `INFORMATION_PROVIDED` o `REOPENED` correspondiente. |
| Contexto de Rentas | Se acepta la correlación recibida, también null. | Se acepta la causa recibida, también null. |

Todos los IDs de entidades transmitidos son UUID. Las referencias legibles no sustituyen la correlación ni autorizan a convertir IDs numéricos de M1 en UUID ficticios.

## 3. Publicación: `socialDocumentationRequested`

**Disparador:** el hecho interno `ApplicationSubmitted`, con negocio habilitado y al menos un requisito documental. **Productor:** M8. **Consumidor funcional:** M1.

| Campo de `data` | Origen / significado |
| --- | --- |
| `citizenId` | UUID de `users.external_citizen_id` del titular. |
| `applicationId` | UUID de la solicitud. |
| `applicationNumber` | Referencia opcional; el backend convierte su número local a string. No garantiza el formato ilustrativo `SOL-...`. |
| `programId`, `programEditionId` | Programa y edición de la solicitud. |
| `requiredDocuments[]` | Hasta diez requisitos de la edición. |
| `requiredDocuments[].requirementId` | UUID estable del requisito; M1 lo devuelve al responder. |
| `requiredDocuments[].type`, `.name`, `.required` | Código documental, nombre y obligatoriedad. |
| `requiredDocuments[].description` | Explicación opcional. |
| `notes` | Observaciones opcionales de la convocatoria. |

Se guarda en outbox en la misma transacción que la solicitud. La confirmación del pedido no marca como entregados sus documentos.

## 4. Publicación: `socialBenefitUpdated`

**Disparador:** `BenefitChanged`, originado por asignación, renovación, suspensión, finalización, vencimiento o finalización causal por M1. **Destino funcional:** Rentas.

| Campo de `data` | Significado |
| --- | --- |
| `benefitId` | UUID del otorgamiento; se conserva en todos sus cambios. |
| `citizenId`, `applicationId` | Titular canónico y solicitud de origen. |
| `programId`, `programEditionId`, `programName` | Identificadores y nombre de la edición. |
| `benefitTypeId` | UUID del beneficio del catálogo de esa edición. |
| `type` | `TAX_EXEMPTION`, `HOUSING_SUBSIDY`, `FOOD_ASSISTANCE` o `UTILITY_SUBSIDY`. |
| `description` | Descripción copiada del otorgamiento, nullable. |
| `amount` | Número o null; la clave es obligatoria incluso si no tiene monto. |
| `status` | `APPROVED`, `SUSPENDED` o `FINALIZED`. |
| `reason` | Null para `APPROVED`; texto obligatorio para suspensión/finalización. |
| `startDate`, `endDate` | Vigencia. En finalización manual el fin externo es la fecha efectiva; en vencimiento es el fin pactado; en fallecimiento se usa `effectiveEndDate/deceasedAt`. |
| `updatedAt` | Fecha/hora del cambio guardado; permite a la contraparte reconocer el hecho más reciente. |

Si una solicitud recibe tres prestaciones, se publican tres identidades de beneficio. Renovar mantiene el mismo `benefitId`. El rechazo de la solicitud no publica este evento. La aprobación inicial de la solicitud tampoco lo publica antes de conceder un beneficio.

## 5. Publicación: `updateTicketStatus`

**Disparador:** operación manual de gestión o hecho interno de asignación, visita/reprogramación y resolución. **Destino funcional:** M2.

Campos comunes: `ticketId`, `updateType`, `updatedBy` y `updateOccurredAt`. Los campos de contenido son `publicMessage`, `internalMessage`, `progress` de 0 a 100, `details` y `attachments[]`.

`updatedBy.type` es `SYSTEM` en automatismos y `EXTERNAL_USER` en actuaciones administrativas. El ID se envía null mientras no se use un `actorId` canónico de empleado; nunca se sustituye por `users.id` o un JWT. Los adjuntos llevan `fileName`, `contentType`, `url` y `sizeBytes` opcional, sin binarios.

| `updateType` | Datos mínimos / condición | Uso |
| --- | --- | --- |
| `STARTED` | Solo una vez por derivación `ROUTED`. | Un administrativo toma el ticket. |
| `PROGRESS` | Al menos progreso, texto público, texto interno o adjuntos. | Avance operativo o visita programada/reprogramada. |
| `INFORMATION_REQUIRED` | `details.informationRequest.messageForCitizen`; `requiredBy` opcional. | Pedido de información; solo uno activo. |
| `RETURNED` | `details.returnInfo.reasonCode` y texto público o interno. | Devolver por área/tipo/contexto incorrecto. |
| `RESOLVED` | `details.resolution.type` y `publicMessage`. | Resultado del trámite o resolución manual. |
| `REJECTED` | `details.cancellation.reasonCode` y texto público o interno. | Imposibilidad operativa de gestionar. |

Valores de resolución: `ACTION_COMPLETED`, `REQUEST_FULFILLED`, `INQUIRY_ANSWERED`, `ACKNOWLEDGED`, `NO_FURTHER_ACTION_REQUIRED`.

Motivos de devolución: `INVALID_INFORMATION`, `REQUEST_TYPE_MISMATCH`, `INSUFFICIENT_CONTEXT`, `OTHER`.

Motivos de rechazo operativo: `OUT_OF_SCOPE`, `DOES_NOT_APPLY`, `INVALID_REQUEST_TYPE`, `INVALID_DATA`, `REJECTED_BY_AREA`, `OTHER`.

La salida admite únicamente el bloque de `details` correspondiente a su variante. Con información pendiente se impide continuar publicando avances hasta la continuación de M2. Una gestión cerrada, devuelta, resuelta o de otra área no admite nuevas respuestas normales.

## 6. Consumo: `ticketUpdated`

**Productor:** `atencion-ciudadana`. Es broadcast; la suscripción no implica que cada ticket corresponda a M8.

| Campo / bloque | Uso |
| --- | --- |
| `ticketId`, `publicId` | Identidad UUID y referencia legible. |
| `citizenId`, `isAnonymous` | Ciudadano canónico o null si es anónimo. |
| `responsibleAreaId` | Código `M8` para gestión propia, no el slug del sobre. |
| `updateType`, `currentStatus`, `currentPriority` | Hecho comunicado y contexto vigente de M2. |
| `updatedAt` | Orden temporal por ticket. Un evento anterior al cursor no modifica gestión. |
| `details.routing` | En `ROUTED`: requestType, category, subcategory, ticketType, summary, description y reasignación. |
| Opcionales de routing | `formData` JSON, `location`, `resolutionDueAt` y `escalation`, según la adaptación M2 v1.79 de esta rama. |
| `details.informationRequest` / `.informationResponse` | Pedido/respuesta y `purpose`; la reclasificación se ignora después de atender un cambio de área. |
| `details.reopen` | Motivo de reapertura. |
| `details.escalation` | En cambios de escalamiento: active, reasonCode y escalatedAt cuando está activo. |
| `attachments[]` | Adjuntos referenciados del mensaje; la respuesta de información usa los adjuntos de `data`. |

Tipos con tratamiento de gestión: `ROUTED`, `INFORMATION_PROVIDED`, `CANCELLED`, `REOPENED`, `PRIORITY_CHANGED`, `ESCALATION_CHANGED`, `DUPLICATE_LINKED` y `CLOSED`. Las variantes restantes reconocidas por el flujo no generan un eco de salida.

La recepción de `ROUTED` guarda una gestión, no una solicitud automática. Una nueva derivación con `reassignment=true` reinicia el ciclo. Reapertura/respuesta cambian la causa para continuar y no habilitan otro `STARTED` por sí mismas.

## 7. Consumo: `socialDocumentationDelivered`

**Productor:** `ciudadanos`. Exige correlación con la solicitud y causa del pedido documental propio.

| Campo | Uso |
| --- | --- |
| `citizenId`, `applicationId`, `programId`, `programEditionId` | Control de identidad y pertenencia del expediente al caso. |
| `files[].fileId`, `.number`, `.initiatingArea`, `.status` | Referencias/contexto de expedientes de M1. M8 conserva las referencias necesarias, no replica todo su expediente. |
| `files[].documentationRequests[].requestId` | Identidad externa del pedido documental. |
| `.requirementId`, `.documentType` | Cruce con UUID y código de requisito de la edición y del pedido original. |
| `.status` | `PENDING` o `COMPLETED`. |
| `.documentId` | Clave obligatoria-nullable: null si está pendiente; UUID si está completo. |

`COMPLETED` indica entrega. La revisión de M8 se conserva por separado como `PENDING`, `VALID` u `OBSERVED`. Una solicitud desconocida se confirma e ignora; identificadores contradictorios de un caso conocido provocan rechazo y rollback.

## 8. Consumo: `citizenUpdated`

**Productor:** `ciudadanos`. Campos comunes: `citizenId`, `updateType`, `details` y `updatedBy`. Solo se actúa sobre usuarios ya vinculados.

| Variante | Información de `details` exigida por el contrato tipado | Efecto |
| --- | --- | --- |
| `REGISTERED` | dni, cuil, name, lastname, birthdate, state, nationality y address. | Copia nombre/DNI y domicilio al snapshot; no crea usuario. |
| `ADDRESS_UPDATED` | previousAddress y newAddress. | Cambia domicilio con reloj independiente. |
| `BLOCKED` | reason y reviewRequired. | Restricción para nuevas solicitudes y alerta administrativa; sin suspensión automática. |
| `DECEASED` | deceasedAt y source. | Cierre causal de casos abiertos y finalización de prestaciones activas/suspendidas. |

Una dirección contiene `propertyId`, `street` y `number`; puede agregar principal y fechas de efectividad. El modelo local no guarda automáticamente cada campo que acepta el contrato.

## 9. Consumos de Rentas

Todos tienen `sourceModule=rentas`. Los IDs externos se conservan tal como llegan. El contexto de deuda y exenciones no modifica por sí mismo elegibilidad ni beneficios sociales.

| Evento | Campos principales de `data` | Regla de aplicación |
| --- | --- | --- |
| `debtOverdue` | debtId, citizenId, conceptCode, outstandingAmount, dueDate. | Registra deuda; descarta actualizaciones anteriores o un overdue de una deuda conocida ya saldada. |
| `debtSettled` | debtId, citizenId nullable, conceptCode, originType, originId, settledAt, remainingBalance, outstandingBalance; infractionId opcional ligado a tránsito. | Solo `MUNICIPAL_TAX`, ciudadano no null y deuda conocida. Otros casos se confirman sin efectos. |
| `exemptionRequested` | requestId, citizenId, conceptCode, reason, requestedPercentage, requestedFrom, requestedUntil. | Guarda solicitud tributaria; asocia un TAX_EXEMPTION activo si la coincidencia es única. |
| `updateExemptionStatus` aprobado | requestId, citizenId, status=APPROVED, conceptCode, exemptionId, percentage, validFrom, validUntil. | Guarda la resolución y vigencia de Rentas. |
| `updateExemptionStatus` rechazado | requestId, citizenId, status=REJECTED, conceptCode, reason. | Guarda rechazo; no exige los campos exclusivos de aprobación. |

Las resoluciones de exención que llegan antes del pedido se conservan para completar el contexto posteriormente. Si hay varios otorgamientos candidatos, queda una selección administrativa en vez de elegir arbitrariamente.

## 10. Qué valida y qué confirma el receptor

1. El codec comprueba tipos, UUID, fechas, claves obligatorias-nullables y contrato/origen/versión. Evita tratar un número expresado como string como importe válido.
2. El validador aplica reglas condicionales de cada variante.
3. La inbox controla identidad, repetición y hash, y el servicio comprueba pertenencia y condiciones del caso.
4. Un evento aceptado pero no aplicable puede terminar sin efecto de negocio y con ack. Un contrato inválido o tipo sin handler provoca rechazo, no aceptación silenciosa.
5. Los efectos y salidas causales quedan confirmados antes del ack; los reintentos y DLQ requieren además la configuración operativa real de Core.

## 11. Referencias de contrato

- [Registro de tipos](../../src/main/kotlin/com/uade/dda2/server/feature/integration/contract/M8EventType.kt).
- [Envelope y estructuras comunes](../../src/main/kotlin/com/uade/dda2/server/feature/integration/contract/M8Payload.kt).
- [Publicaciones](../../src/main/kotlin/com/uade/dda2/server/feature/integration/contract/PublishedPayloads.kt), [tickets de entrada](../../src/main/kotlin/com/uade/dda2/server/feature/integration/contract/TicketPayloads.kt), [Ciudadanos](../../src/main/kotlin/com/uade/dda2/server/feature/integration/contract/CitizenPayloads.kt) y [Rentas](../../src/main/kotlin/com/uade/dda2/server/feature/integration/contract/RevenuePayloads.kt).
- [Codec tipado](../../src/main/kotlin/com/uade/dda2/server/feature/integration/service/M8ContractCodec.kt) y [validación condicional](../../src/main/kotlin/com/uade/dda2/server/feature/integration/validator/M8PayloadValidator.kt).
