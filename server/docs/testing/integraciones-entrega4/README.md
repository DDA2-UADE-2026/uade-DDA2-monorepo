# API, integraciones y resiliencia — Entrega 4

Estado: **PARCIAL**. Ejecución local del 12/11/2026 sobre el árbol modificado basado en 652b247991ae7df5eeddfefecd90b1a47adad6e3. No hubo push ni uso del Core compartido, base remota o broker real.

## Ejecuciones

- Suite Gradle dirigida: **253 casos, 250 aprobados, 3 omitidos, 0 fallos y errores**, BUILD SUCCESSFUL en 1 min 28 s. Selección: paquete feature.integration, ApplicationVisitFlowTest y BenefitGrantFlowTest. Las omisiones corresponden a migraciones PostgreSQL de beneficios.
- Scripts reales de la colección Postman sobre respuestas sintéticas MockMvc/H2: **20 comprobaciones PASS**, 9 respuestas guardadas. No es Newman ni una ejecución HTTP contra un backend desplegado.
- CoreConnectivitySmokeTest está excluido por el tag core-smoke del test normal y no se ejecutó; no cuenta como integración real ni queda cubierto por el total anterior.

| Control | Verificación y alcance | Resultado |
|---|---|---|
| Endpoints exitosos y errores | Visitas y beneficios mediante MockMvc; JSON/status y scripts de colección | PASS del subconjunto ejecutado |
| Contratos de integración | CoreEnvelopeCodecTest y clientes HTTP con servidor local simulado; campos y errores comprobados por aserciones | PASS de contratos cubiertos; no validación universal contra todos los schemas |
| Duplicados inbox | Mismo evento dos veces genera una fila; mismo UUID con contenido distinto rechazado | PASS con H2 |
| Duplicados outbox (nuevo) | Reencolar mismo evento conserva una fila; conflicto conserva payload original | PASS con H2 |
| Recuperación outbox (nuevo) | Fallo controlado, espera backoff, nueva reserva, rechazo de confirmación vieja y confirmación nueva; misma operación y una fila | PASS con H2, sin envío a broker real |
| Atomicidad | Rollback del llamador revierte outbox; fallo de handler revierte inbox y permite reintento | PASS con H2 |
| Concurrencia | Dos publicadores compiten por una fila, solo uno obtiene la reserva | PASS con H2; no acredita semántica PostgreSQL |
| Confirmaciones y rechazos AMQP | ACK después del servicio; NACK sin requeue en error; fallo de ACK no rechaza un efecto ya confirmado; retorno NO_ROUTE no se acepta como envío correcto | PASS con canal/RabbitTemplate simulados |
| Recuperación HTTP | Renovación de token una vez ante 401, concurrencia, rechazo 403, readiness degradado y códigos de error | PASS contra servidor HTTP local simulado |
| PostgreSQL y RabbitMQ reales | No disponibles localmente: Docker no instalado | NO VERIFICABLE |
| Reinicio del broker, redelivery y DLQ reales | No ejecutados | NO VERIFICABLE |

La clasificación de error/reintento o un basicNack simulado no prueba el traslado de un mensaje a una DLQ real. Tampoco se acredita entrega exactly-once; solo deduplicación y atomicidad de los casos medidos.

## Evidencia y correlación

- resultados-suite.json: nombres de suites/casos, PASS/FAIL/SKIPPED, sin stdout ni credenciales.
- postman-assertions.json: resultado por caso/script de la colección.
- respuestas-sinteticas.json: caseId relaciona la respuesta con el resultado anterior; campos potencialmente sensibles redactados.
- contexto.json: alcance, SHA base y SHA256 de la colección.
- outbox-duplicate.json y outbox-recovery.json: eventId/correlationId sintéticos y resultado persistido; recuperación incluye traceId, intentos, estado final y cantidad de filas. Se generan únicamente después de completar las aserciones.

No se incluyen Authorization, cookies, secretos ni cuerpos de mensajes reales. Los UUID son sintéticos de estas pruebas.

## Reproducir sin Docker

Desde server, con Java configurado:

```powershell
.\gradlew.bat test --rerun --tests 'com.uade.dda2.server.feature.integration.*' --tests '*ApplicationVisitFlowTest' --tests '*BenefitGrantFlowTest' --no-configuration-cache --console=plain --max-workers=2
```

Desde la raíz:

```powershell
node notes/qa-evidence/run-postman-checks.mjs
python scripts/testing/summarize-junit.py server/build/test-results/test server/docs/testing/integraciones-entrega4/resultados-suite.json
```

Los dos casos nuevos están en CorePersistenceTest. Generan correlaciones en server/build/qa-integration. Las respuestas originales sintéticas se generan en server/build/qa-postman. El runner Postman existente depende de client/node_modules y no es un reemplazo de Newman.

## Para terminar con infraestructura real

Instalar Docker Desktop y comprobar que el motor esté iniciado. Luego crear PostgreSQL y RabbitMQ aislados con credenciales sintéticas, puertos locales y nombres propios, sin cargar credenciales remotas de .env. El compose existente del proyecto habilita Core y tiene publicador/consumidor deshabilitados; levantarlo por sí solo no acredita resiliencia.

En el entorno descartable queda ejecutar: persistencia PostgreSQL, duplicado por broker, reinicio/caída y recuperación, confirmaciones, mensajes inválidos a la cola de fallidos y atomicidad. Registrar eventId/correlationId/traceId y conteos antes/después. No ejecutar CoreConnectivitySmokeTest apuntando al Core compartido: la autorización actual es solo local.
