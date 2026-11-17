# Actualización RabbitMQ Java client 5.27.1 → 5.37.0

**Fecha:** 16/11/2026. **Rama:** `feature/security-rabbitmq-client`. **Base:** `48152a03cd8860118219c0210385e389bdd5270a`, con Boot 4.0.9 y BC 1.87 mergeados.

El usuario confirmó **29 dependency risks** después del merge BC. Esta tanda actualiza **el cliente Java `com.rabbitmq:amqp-client`**, no el servidor RabbitMQ. No se ha ejecutado un nuevo análisis Sonar ni se han cambiado estados de riesgos.

## 1. Implementación acotada

- [Build Gradle](<../../../server/build.gradle.kts>): `extra["rabbit-amqp-client.version"] = "5.37.0"`, propiedad oficial del BOM utilizada por el plugin dependency-management existente.
- [Lock](<../../../server/gradle.lockfile>): regeneración selectiva mediante Gradle; una única entrada cambia 5.27.1 → 5.37.0, conservando los siete scopes existentes.
- Se comprobaron `compileClasspath`, `implementationDependenciesMetadata`, `productionRuntimeClasspath`, `runtimeClasspath`, `testCompileClasspath`, `testImplementationDependenciesMetadata` y `testRuntimeClasspath` mediante `dependencyInsight`.
- Boot 4.0.9, Spring AMQP 4.0.6, Kotlin 2.2.21, BC 1.87 y frontend permanecen sin actualizar. No se añadió un `force` global ni se importó otro BOM.

El [POM publicado de 5.37.0](https://repo.maven.apache.org/maven2/com/rabbitmq/amqp-client/5.37.0/amqp-client-5.37.0.pom) declara Java 1.8 como target y no exige un salto del toolchain Java 17. Incluye Netty 4.2.18.Final no opcional; esos módulos ya existían en los scopes del proyecto y Boot continúa seleccionando **4.2.19.Final**, sin cambios adicionales en el lock. Publicación no equivale a compatibilidad certificada: se valida el proyecto y se documentan los límites de la prueba.

## 2. Motivo de seguridad y cambios relevantes

Los diez registros originales RabbitMQ quedan por encima de sus mínimos corregidos con 5.37.0:

| CVE | Mínimo corregido contrastado |
|---|---:|
| CVE-2026-69219 / CVE-2026-69220 | 5.33.1 |
| CVE-2026-75516 | 5.34.0 |
| CVE-2026-63337 / CVE-2026-63336 / CVE-2026-61634 | 5.33.0 |
| CVE-2026-63335 | 5.31.0 |
| CVE-2026-106121 | 5.36.1 |
| CVE-2026-106122 | 5.36.0 |
| CVE-2026-106123 | 5.35.0 |

Las referencias por CVE se conservan en el [inventario original](<evidence/riesgos-122.csv>). No se reescribe el snapshot inicial para simular un reanálisis; el cierre real y nuevos avisos se verificarán en Sonar del PR.

Notas oficiales: [5.33.0](https://github.com/rabbitmq/rabbitmq-java-client/releases/tag/v5.33.0) introduce cambios de seguridad en TLS/hostname y JSON-RPC; [5.34.0](https://github.com/rabbitmq/rabbitmq-java-client/releases/tag/v5.34.0) modifica límites inbound/negociación; [5.37.0](https://github.com/rabbitmq/rabbitmq-java-client/releases/tag/v5.37.0) incluye endurecimiento JSONReader y preferencia TLS del helper.

## 3. Preservación del contrato de la aplicación

La [fábrica Core](<../../../server/src/main/kotlin/com/uade/dda2/server/config/CoreAmqpConfiguration.kt#L29-L51>) sigue exigiendo TLS para hosts no loopback, certificado y hostname validados, y utiliza `SSLContext.getDefault()` más `enableHostnameVerification()`. No se cambia a un helper trust-all ni se desactiva TLS para pasar tests.

- Recovery nativo desactivado: Spring AMQP conserva la responsabilidad de reconectar.
- Publisher confirms correlacionados, returns y mandatory se mantienen.
- Listener con ACK manual, sin auto-declaración de topología, prefetch configurado y sin auto-start cuando el consumidor está deshabilitado.
- Inbox/outbox y deduplicación conservan su diseño; confirm perdido puede producir duplicados y no se promete exactly-once.
- Un NACK sin requeue no demuestra envío a DLQ: requiere DLX/política de broker, no declarada automáticamente aquí.

**Límite TLS:** la fábrica manual usa el contexto/truststore JVM; no aplica automáticamente todas las opciones SSL de Boot. La preferencia TLS 1.3 del helper actualizado no prueba que esta ruta con contexto explícito exija exclusivamente TLS 1.3.

## 4. Validación y evidencia

**Build PASS:** ejecución final certificada exit 0 en **108,97 s**, con **617 casos / 57 suites: 612 aprobados, 5 omitidos preexistentes y 0 fallos/errores**. Dos capturas anteriores quedaron interrumpidas sin exit y no se usan como prueba de éxito.

**Regresiones nuevas PASS:** [CoreAmqpConfigurationTest](<../../../server/src/test/kotlin/com/uade/dda2/server/feature/integration/CoreAmqpConfigurationTest.kt>) aporta **7 casos, sin fallos, errores ni skips**, ejecutados offline por separado. Construye beans directamente y verifica TLS remoto obligatorio, rechazo de flags inseguras, timeouts/heartbeat/recovery, confirms/returns, mandatory, listener manual sin arranque y endpoint identification HTTPS en un socket SSL no conectado. No abre conexiones ni realiza handshake TLS. El build de 617 casos y los 7 nuevos se ejecutaron por separado contra el mismo build/lock; no se presenta como una única ejecución combinada de 624 casos.

El BootJar conserva **131 librerías**, incluye sólo `amqp-client-5.37.0.jar` para el cliente RabbitMQ, no incluye BC y mantiene bytecode Java 17 (major 61). Se cotejaron hashes de los inputs probados y del checkout principal.

[Resultados, comandos, hashes y limitaciones](<evidence/rabbitmq-client-5.37.0.json>).

### Integración real pendiente en dev

**No ejecutada localmente:** Docker CLI estaba disponible, pero el socket Unix autorizado no existía y la respuesta de `docker info` indicaba errores del daemon. El preflight frenó antes de iniciar contenedores o ejecutar la suite. No se contabilizan casos de integración como aprobados.

El usuario autorizó expresamente publicar la feature y probar esta integración en el **entorno de dev antes del merge**. No se conecta automáticamente a ese entorno desde esta tarea ni se reutilizan credenciales privadas.

La [suite Testcontainers](<../../../server/src/test/kotlin/com/uade/dda2/server/feature/integration/CorePostgresRabbitTestcontainersTest.kt#L103-L171>) usa PostgreSQL 16 y RabbitMQ 3.13 en contenedores locales exclusivos. Sobrescribe datasource y host/puerto AMQP con los de esos contenedores; no utiliza Core compartido ni bases existentes. Su cobertura incluye rollback outbox, deduplicación DB y publicación persistente confirmada con entrega.

**No demuestra** TLS/CA/hostname mediante handshake real, reconexión tras reinicio, redelivery de un listener real ni enrutamiento a DLQ. El consumidor está deshabilitado y la deduplicación se invoca directamente. Esas pruebas más amplias deben ejecutarse en un entorno de integración autorizado; no se extrapola el resultado de localhost sin TLS al despliegue AMQPS.

## 5. Estado y próximo paso

Entrega en la feature para PR hacia `develop`: el usuario solicitó commit/push y abrirá el PR. No se crea ni mergea un PR ni se despliega desde esta tarea. Antes de integrar: pasar CI Linux y Sonar del commit del PR, ejecutar la integración en dev y validar AMQPS real en ese entorno autorizado. No confundir pruebas locales de configuración con validación de certificados/hostname durante un handshake real; no alterar el broker productivo como prueba rutinaria.

Rollback propuesto: revertir conjuntamente la propiedad del build y la entrada generada del lock; reconstruir y repetir regresiones. No requiere cambiar datos, colas ni versión del broker.
