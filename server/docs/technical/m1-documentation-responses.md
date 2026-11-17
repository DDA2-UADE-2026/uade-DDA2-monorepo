# Verificación local de respuestas documentales de M1

Estado al 16 de noviembre de 2026: verificadas las dos fases de `socialDocumentationDelivered` con eventos simulados, JPA y una base H2 desechable. Se corrigieron la aceptación de `requestId` duplicados en un sobre y la validación de la identidad y el tipo documental contra el pedido original de M8.

Esta verificación no contacta a M1 ni a Core, no publica mensajes y no modifica el backend desplegado. Los UUID de los casos se generan para la base de pruebas y no representan solicitudes del entorno compartido. No se ejecutó la prueba con el ciudadano sintético del paso 5.

## Las dos respuestas

| Dato | Respuesta inicial | Entrega posterior |
| --- | --- | --- |
| Tipo de evento | `socialDocumentationDelivered`. | El mismo tipo, con un `eventId` propio. |
| `correlationId` | UUID de la solicitud M8, igual a `data.applicationId`. | Conserva la correlación de la solicitud. |
| `causationId` exigido por M8 | `eventId` del `socialDocumentationRequested` guardado en outbox. | El mismo `eventId` del pedido original. |
| `requestId` | UUID de la solicitud documental creada en M1. | Conserva la solicitud documental que se actualiza. |
| `requirementId` y `documentType` | Requisito y código incluidos en el pedido. | Conservan su relación con el requisito solicitado. |
| Estado de entrega | `PENDING`, con `documentId: null` explícito. | `COMPLETED`, con `documentId` UUID. |
| Revisión administrativa | `PENDING`; la entrega no puede revisarse todavía. | Sigue `PENDING` hasta revisión humana como `VALID` u `OBSERVED`. |

El `requestId` de M1, el `requirementId` de M8 y el `eventId` del pedido son identificadores distintos. Un `requestId` debe aparecer una sola vez en todo el sobre, incluso si hay varios expedientes en `files[]`.

M1 confirmó la causa del pedido para la respuesta inicial. Su comunicación no especificó el `causationId` de la entrega posterior: el backend conserva la regla vigente de exigir el pedido original, y esa regla sigue pendiente de confirmar con M1. Usar como causa el evento de la respuesta inicial, o enviar solo correlación, se rechaza. Este trabajo no flexibiliza el contrato ni acredita interoperabilidad remota.

## Casos verificados

| Caso | Resultado esperado y comprobado |
| --- | --- |
| Inicial y entrega posterior válidas. | Dos marcas de inbox y referencias documentales actualizadas, sin publicación de eventos de respuesta. |
| Entrega posterior parcial. | Actualiza los ítems recibidos y conserva las solicitudes documentales no mencionadas. |
| Reentrega del mismo evento. | Devuelve `DUPLICATE`, sin repetir cambios ni auditoría. |
| Mismo `eventId` con otro contenido. | `CORE_EVENT_ID_CONFLICT`; conserva la marca original y el estado previo. |
| Inicial atrasada o documento anterior. | Una fecha `occurredAt` estrictamente anterior no sobrescribe la referencia ni la revisión vigentes. |
| Aviso nuevo del mismo documento y estado. | Conserva la revisión humana existente. |
| Nuevo UUID de documento. | Reinicia la revisión a `PENDING` y elimina observación, revisor y fecha previos. |
| Ciudadano, programa o edición incorrectos. | `M8_DOCUMENT_IDENTITY_MISMATCH`, sin persistir la respuesta en inbox ni sus referencias. |
| Cambio posterior de la vinculación local. | No permite responder por un ciudadano distinto del que figura en el pedido original. |
| Requisito inexistente, no solicitado o con otro código. | Rechazo sin efectos; se contrasta tanto el catálogo vigente como la instantánea del pedido. |
| Cambio posterior del código documental. | No permite sustituir silenciosamente el tipo solicitado originalmente. |
| Causa ausente, desconocida, de otro tipo o de otra solicitud. | Rechazo sin marcar como procesado el evento inválido. |
| Pedido local ilegible. | `M8_DOCUMENT_REQUEST_UNKNOWN`; no aplica la respuesta. |
| `PENDING` con documento o `COMPLETED` sin documento. | `M8_INVALID_PAYLOAD`. |
| `documentId` ausente, numérico o con UUID inválido. | `M8_INVALID_PAYLOAD`; no se convierte un ID numérico a UUID. |
| `requestId` duplicado dentro de un expediente o entre expedientes. | `M8_INVALID_PAYLOAD` antes de modificar referencias. |
| Error en el segundo ítem después de guardar o actualizar el primero. | Rollback de referencias, revisión, auditoría y nueva marca de inbox. |
| Solicitud M8 desconocida. | Inbox confirma el evento sin crear referencias ni generar efectos de negocio. |
| Revisión incoherente. | `OBSERVED` exige observación y `VALID` no la admite; `PENDING` no es un resultado de revisión. |
| Consulta y revisión sin permiso, o permiso retirado. | La API mantiene la autorización por JWT y permisos vigentes en la base. |

`COMPLETED` solo representa entrega. La verificación usa los endpoints administrativos existentes y el estado documental que consulta la evaluación; no descarga archivos de M1 ni trata un UUID como una autorización para ver su contenido.

## Implementación e aislamiento

- [ExternalDocumentationFlowTest](../../src/test/kotlin/com/uade/dda2/server/feature/integration/ExternalDocumentationFlowTest.kt) ejecuta codec, inbox y servicio reales dentro de transacciones locales. Su contexto tiene M1, Core, publicador y consumidor desactivados; una prueba comprueba que no existan clientes M1/Core, conexiones AMQP ni publicador de outbox.
- [ExternalDocumentationService](../../src/main/kotlin/com/uade/dda2/server/feature/integration/service/ExternalDocumentationService.kt) contrasta ciudadano, solicitud, programa, edición y tipo documental con la instantánea persistida del pedido, además de las entidades actuales.
- [M8PayloadValidator](../../src/main/kotlin/com/uade/dda2/server/feature/integration/validator/M8PayloadValidator.kt) controla unicidad de `requestId` en el sobre y coherencia entre estado y `documentId`.

Las regresiones de persistencia y listener utilizan H2 y adaptadores controlados. No se ejecutan `testCoreConnectivity`, pruebas de broker compartido, login M1 ni consultas HTTP remotas. La prueba de commit e inbox no sustituye una comprobación de ack sobre RabbitMQ real.

## Reproducción y resultado

Desde `server`, para verificar únicamente las respuestas documentales:

```powershell
.\gradlew.bat test --tests '*ExternalDocumentationFlowTest' --console=plain --offline
```

La regresión local realizada fue:

```powershell
.\gradlew.bat test --tests '*ExternalDocumentationFlowTest' --tests '*M8BusinessFlowTest' --tests '*M8ContractCodecTest' --tests '*CorePersistenceTest' --tests '*CoreMessageListenerTest' --tests '*AsyncApiDocsGenerationTest' bootJar --console=plain --offline
```

Resultado: **73 casos aprobados, sin fallos ni omisiones**, incluidos 40 casos nuevos de documentación. `bootJar` y AsyncAPI generados correctamente. Los reportes quedan en `build/reports/tests/test/` y `build/test-results/test/` después de ejecutar el comando.

Este paso de verificación no requirió migraciones ni agregó rutas HTTP. La descarga se implementó posteriormente en el [proxy administrativo de archivos](m1-file-proxy.md), desactivado por defecto. La confirmación del contrato de la entrega posterior y el acceso administrativo al archivo por UUID siguen siendo acuerdos externos pendientes.
