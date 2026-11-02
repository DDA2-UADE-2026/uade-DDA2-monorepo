# Prueba coordinada y habilitación de Core

Tercer roadmap de la [secuencia de integración](README.md). Verifica el intercambio real después de cerrar [accesos y conexión](01-core-accesos-y-conexion.md) y [mensajería confiable](02-core-mensajeria-confiable.md).

La guía informa un único ambiente compartido. Antes de publicar o consumir se necesita acordar la ventana y el tipo técnico con Core. El [código revisado](00-contrato-core-verificado.md) permite que Desarrollo Social publique y se suscriba a su propio tipo: no hace falta una contraparte de negocio ni un servicio de eco.

## Contrato de la prueba técnica

Nombre propuesto para acordar con Core: `socialCoreConnectivityProbe`. Versión: `1.0`. Dueño, origen y único consumidor previsto: `desarrollo-social`. Es un contrato técnico nuevo propuesto por este roadmap, no un evento ya autorizado o presente en el seed.

El handler valida origen, nombre, versión y `runId` esperado; su resultado es la recepción persistida en inbox. No modifica solicitudes, ciudadanos o beneficios y no publica una respuesta: de ese modo la autorrecepción no crea un bucle.

Schema propuesto para `data`, a declarar únicamente sobre ese tipo propio:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "probe": { "const": "core-connectivity" },
    "runId": { "type": "string", "minLength": 1 }
  },
  "required": ["probe", "runId"],
  "additionalProperties": false
}
```

Cada ejecución deliberada genera `runId` UUID, `eventId` UUID y `occurredAt` con zona. Usa `correlationId=runId`, `sourceModule=desarrollo-social` y el payload `probe=core-connectivity`, `runId=<UUID generado>`. `causationId` se omite porque no responde a otro hecho.

El driver implementado es `CoreConnectivitySmokeTest`, etiquetado `core-smoke` y ejecutado únicamente por la tarea Gradle opt-in `testCoreConnectivity`. Exige habilitación explícita, configuración del probe, UUID de ejecución y evento, y una base técnica PostgreSQL llamada `core_smoke_<nombre>`, distinta de la base del backend. Los requisitos y variables están en la [guía de ejecución](../technical/core-integration.md). La prueba remota todavía no se ejecutó.

La suite habitual, el CI y la generación de OpenAPI excluirán esa etiqueta y seguirán sin acceso a AWS. La prueba publica una sola ejecución mediante la outbox, verifica Core e inbox con esperas acotadas y luego cierra su contexto y recursos locales. No agrega un endpoint público ni emite probes al arrancar normalmente la aplicación. Las retransmisiones de la misma ejecución conservan sobre y UUID; probar ack manual e idempotencia no requiere handlers de negocio.

## C9 Preparar la prueba remota

- [ ] Confirmar nuevamente dominio, vhost, cola, credenciales vigentes e IP de salida del backend desplegado.
- [ ] Registrar participantes, horario, tipo exacto, versión, payload ficticio y política de reintentos de la prueba.
- [ ] Acordar el recorrido M8 → `muni.inbox` → worker Core → `muni.events` → cola propia → inbox M8 y comprobar que solo M8 tenga una suscripción activa al tipo técnico.
- [ ] Confirmar actividad del worker de Core, externo o embebido; una readiness con checks positivos no lo prueba.
- [ ] Consultar las publicaciones y suscripciones propias antes de modificar el registro.
- [ ] Crear solo las declaraciones faltantes que correspondan a la prueba, o actualizar deliberadamente las existentes con Core.
- [ ] Declarar en orden el tipo propio con su schema, la publicación propia y la suscripción propia con `maxAttempts=4`. Si ya existen, contrastar dueño, schema y estado antes de decidir cualquier modificación.
- [ ] Comprobar pasivamente la cola después del registro. Un `201` del registro no basta para habilitar el consumidor.
- [ ] Confirmar las suscripciones activas y resolver con Core cualquier backlog de mensajes cuyo contrato todavía no tenga handler. No iniciar un consumidor genérico que los confirme y descarte.
- [ ] Coordinar con Core la pausa deliberada de las suscripciones de negocio propias si corresponde y el tratamiento de entregas/reintentos anteriores. La pausa no retira mensajes encolados ni cancela retries existentes, y no se purga la cola.
- [ ] Mantener separados los interruptores de publicación y consumo, y cargar los secretos en el runtime privado del backend.

## C10 Verificar publicación y recepción

- [ ] Habilitar el consumidor técnico acordado sobre la cola propia, sin purgar mensajes ni declarar topología.
- [ ] Generar un evento nuevo para la prueba de salida y persistirlo en la outbox.
- [ ] Registrar `eventId`, fecha con zona y resultado de publicación, incluidas posibles devoluciones.
- [ ] Solicitar a Core la comprobación del registro, validación y ruteo del evento.
- [ ] Consultar `GET /api/v1/events/{eventId}` con espera acotada hasta obtener evidencia de ingesta. Un 404 inmediato tras el confirm no implica por sí solo pérdida: el worker puede no haberlo procesado todavía.
- [ ] Verificar el sobre, `ingestionChannel=AMQP`, estado y entrega hacia M8. Si figura `NO_SUBSCRIBERS`, resolver el registro con Core antes de otra prueba; reenviar ese mismo UUID después de suscribirse no lo vuelve a rutear.
- [ ] Recibir el sobre directo en la cola propia, comprobar headers de Core, ejecutar el handler técnico y verificar el registro en inbox antes del ack.
- [ ] Registrar separadamente confirm del broker, estado de Core y éxito de inbox/ack. Core asigna `DELIVERED` antes de que M8 procese; ese estado solo no cierra la prueba.
- [ ] Correlacionar la evidencia de nuestra aplicación con la de Core y la contraparte.

La contraparte receptora en este recorrido es nuestra propia inbox. Como diagnóstico opcional, `POST /api/v1/events` puede ejercitar el mismo hub desde HTTP con un evento de otra ejecución, pero no demuestra que funcione la publicación AMQPS ni reemplaza ese criterio de cierre. No se configura como fallback del publicador.

Los IDs de los documentos son ilustrativos y no se reutilizan. Una prueba nueva genera otro `eventId`; la retransmisión deliberada del mismo hecho conserva el original.

## C11 Verificar recuperación y operación

- [ ] Coordinar aparte la retransmisión del mismo evento al ingreso de Core: debe conservar un solo evento y no producir otra entrega. Esto verifica la deduplicación de Core, no la de M8.
- [ ] Verificar nuestra inbox mediante el reinicio tras commit y antes de ack en aislamiento, o mediante reentrega del mismo UUID por intervención administrativa de Core. Confirmar un solo resultado local y una nueva entrega confirmada sin repetirlo.
- [ ] Verificar un fallo controlado del handler y la entrega posterior programada por Core.
- [ ] Confirmar el `maxAttempts` efectivo. El ejemplo `4` significa cuatro intentos totales, incluido el inicial, no cuatro reintentos adicionales.
- [ ] Con los defaults del snapshot y cuatro intentos, observar la entrega inicial y los reintentos tras 5 s, 30 s y 120 s. No esperar además el escalón de 10 min; ese requiere más intentos. La configuración desplegada puede cambiar esos valores.
- [ ] Si se acuerda una prueba de agotamiento, obtener de Core la evidencia de entrega fallida y dead letter. No consumir ni republicar por cuenta propia en la DLQ de infraestructura.
- [ ] Usar `GET /api/v1/dlq` y `GET /api/v1/deliveries` para observación propia y pedir a Core cualquier `retry`, descarte o aplicación de topología: esos POST requieren administrador en el snapshot.
- [ ] Probar una interrupción controlada de conexión y confirmar recuperación de pendientes sin detener operaciones locales independientes.
- [ ] Incorporar diagnóstico operativo separado para configuración, conexión, pendientes de outbox y errores de procesamiento, sin exponer secretos ni payloads completos por defecto.
- [ ] Mantener separado el estado de la integración de la salud del proceso para que una caída remota no provoque reinicios innecesarios del backend.
- [ ] Definir límites operativos y retención de registros con el equipo, usando la evidencia de la prueba; no fijar volúmenes o plazos sin información.
- [ ] Documentar que Core no recibe un ack de negocio por HTTP. Una entrega puede quedar `RETRYING` aun si el reintento se procesó correctamente; nuestra evidencia de commit y ack sigue siendo necesaria.
- [ ] Al terminar, cerrar el contexto de la prueba, deshabilitar el flag de probe y acordar la pausa o baja de sus registros técnicos propios. Mantener el consumidor remoto deshabilitado si las restantes suscripciones todavía no tienen handlers; no reactivar contratos diferidos por un reinicio.

## C12 Incorporar verificación en CI y documentar operación

- [ ] Ejecutar compilación y las pruebas relevantes con el Gradle Wrapper del repositorio.
- [ ] Agregar al CI las pruebas de la integración técnica, sin secretos ni acceso al Core compartido. El job actual del backend ejecuta `assemble`.
- [ ] Medir la cobertura y documentar su resultado frente al objetivo del TPO de 85 % para backend y frontend por separado. Esta fase no acredita por sí sola la cobertura de todo el proyecto.
- [ ] Verificar que el perfil `docs` y la generación de OpenAPI funcionan con la integración desactivada.
- [ ] Documentar configuración, arranque, desactivación, recuperación de pendientes y diagnóstico de fallos.
- [ ] Registrar qué operador coordina con Core la inspección de eventos fallidos y su eventual reentrega.

### Evidencia de cada prueba remota

| Campo | Dato que debe conservarse |
| --- | --- |
| Fecha | Fecha y hora con zona. |
| Versión local | Commit o imagen del backend probado. |
| Participantes | Módulo emisor, receptor y referente de Core. |
| Contrato | `eventType`, `eventVersion` y referencia al payload ficticio acordado. |
| Identificadores | `eventId` y correlación cuando corresponda. |
| Publicación | Confirm, devolución o error técnico redactado. |
| Core | Evidencia de registro, validación y ruteo disponible. |
| Identidad de la entrega | `message_id`, headers `x-core-*` relevantes y flag de redelivery. |
| Recepción | Registro local de procesamiento, commit y ack. |
| Resultado | Correcto, fallido o pendiente, con incidencia y responsable del siguiente paso. |

Esta tabla es una plantilla de evidencia; no contiene resultados de pruebas ejecutadas.

## Criterios de cierre de la fase Core

- [ ] HTTP autenticado y AMQPS verificados desde el entorno real del backend.
- [ ] Publicación y consumo técnicos comprobados con participantes y contratos acordados.
- [ ] El recorrido es AMQP y de autorrecepción técnica, sin depender de Ciudadanos, Atención Ciudadana o Rentas.
- [ ] Outbox e inbox verificadas ante duplicados, reinicios e indisponibilidad.
- [ ] Confirmaciones, validación de Core y procesamiento remoto registrados como hechos distintos.
- [ ] Política de rechazo y reintentos compatible con Core; tratamiento de fallos documentado.
- [ ] CI y generación de documentación independientes del ambiente compartido.
- [ ] Consumo limitado a contratos implementados; ninguna integración de negocio diferida queda habilitada accidentalmente.
- [ ] Evidencias y pendientes actualizados en estos roadmaps.

El resultado es una conexión técnica operativa. La siguiente fase se planifica desde [integraciones de negocio pendientes](04-integraciones-posteriores.md), cuando el equipo decida retomarla.
