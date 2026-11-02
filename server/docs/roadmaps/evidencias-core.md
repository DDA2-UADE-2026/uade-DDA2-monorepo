# Evidencias de implementación y conexión con Core

Registro al 2 de noviembre de 2026. Los checklists de los [roadmaps](README.md) distinguen implementación local, conexión y procesamiento de eventos. Un punto marcado se limita a la evidencia indicada aquí.

## E1 Implementación y verificación local

Implementación incorporada en el commit `2475c5a`, integrado en `develop` por `34e836e` (PR #98).

- 22 pruebas técnicas locales, en 8 suites, sin fallos ni errores. Los reportes están en `build/test-results/test/` y `build/reports/tests/test/` después de ejecutar la suite.
- `bootJar` y `generateOpenApiDocs` completados correctamente durante la implementación.
- Pruebas de sobre y hash, HTTP local y token, confirm/return, transacciones y reservas en H2, deduplicación, ack/nack, configuración deshabilitada y comprobación pasiva de arranque.
- Cliente HTTP, AMQPS, outbox, inbox, handler técnico y tarea remota opt-in implementados. Las clases y los comandos reproducibles están en la [guía técnica](../technical/core-integration.md).

Estas pruebas utilizan HTTP local, adaptadores controlados y H2 en modo PostgreSQL. No prueban las transacciones y restricciones sobre PostgreSQL real, ni redelivery, TTL, reintentos o publicación sobre RabbitMQ real. La tarea `testCoreConnectivity` no se ejecutó.

## E2 Conexión AMQPS desde el backend desplegado

Fuente: logs del despliegue aportados por el equipo en este chat. Fecha del resultado: `2026-11-02T13:23:16.173Z` (10:23:16, Argentina).

```text
CORE_AMQP_CONNECTED host=core-uade-2026.duckdns.org port=5671 vhost=core queue=q.desarrollo-social tls=true certificateVerification=true hostnameVerification=true pendingMessages=0 activeConsumers=0
```

| Comprobación | Resultado observado |
| --- | --- |
| Inicio del backend | Spring Boot completó el arranque y Tomcat inició en el puerto 8080. |
| Base del backend | Se estableció conexión con PostgreSQL y se inicializó JPA. Esto no acredita pruebas funcionales de outbox/inbox. |
| DNS y TCP de Core | Se abrió una conexión a `core-uade-2026.duckdns.org:5671`. |
| TLS | Habilitado con verificación de certificado y hostname. |
| Autenticación AMQP | Conexión establecida con `module_desarrollo-social` en el vhost `core`. |
| Cola propia | Consulta pasiva de `q.desarrollo-social` correcta, sin declaraciones activas ni purga. |
| Estado de cola en ese instante | Cero mensajes pendientes y cero consumidores activos. No garantiza que siga vacía en otra ejecución. |

La comprobación de arranque no publica ni consume mensajes. No demuestra el permiso efectivo de publicación, la ejecución de `basic.consume`, el funcionamiento del worker de Core ni el recorrido de un evento. Los logs recibidos no contienen contraseñas, secretos ni tokens.

Core informó habilitada `187.127.41.168/32`. Los logs prueban conectividad desde el proceso desplegado, pero no muestran su IP pública de salida ni si es fija. Tampoco identifican el digest de la imagen o el commit exacto desplegado; corresponde registrarlos antes de la prueba de eventos.

## E3 Dashboard de integración y entrega del secreto HTTP

Core informó la entrega y rotación del secreto HTTP de `desarrollo-social` y la comprobación de token/dashboard desde su servidor. El texto aportado contiene un placeholder; el valor privado no se incorporó al código. La conexión HTTP desde nuestro despliegue todavía no se verificó.

- Siete endpoints de lectura bajo `/api/admin/integrations/core`: estado activo HTTP/AMQPS, dashboard remoto/local, contadores locales, publicaciones, suscripciones, eventos paginados y detalle por UUID.
- Permiso `integrations:core:view`, asignación a ADMIN mediante el inicializador existente y migración manual alternativa. Autorización vigente del usuario, rol y permiso, con pruebas de revocación.
- Credenciales remotas encapsuladas en el servidor; uso del token y renovación ya implementados. Sin registros automáticos, publicación, consumo ni purga desde el dashboard.
- 33 pruebas locales en 9 suites, sin fallos ni errores: las 22 originales más 3 pruebas HTTP y 8 escenarios del dashboard. `bootJar` y `generateOpenApiDocs` verificados; OpenAPI incluye las siete rutas protegidas.
- [Guía de configuración y prueba](../technical/core-dashboard.md): cargar el secreto recibido y ejecutar `/status` con una sesión local autorizada. Se requieren `http.state=UP` y `upstreamStatus=200`; el HTTP local 200 por sí solo no cierra la prueba.

Estas verificaciones siguen siendo locales: HTTP controlado y H2, sin la credencial privada ni llamadas al Core remoto.

## Verificaciones pendientes

- Configurar el secreto HTTP propio recibido y comprobar token, dashboard, registro y diagnóstico desde nuestro despliegue.
- Registrar IP de salida efectiva, estabilidad y versión de la imagen desplegada.
- Acordar el tipo técnico y la ventana, verificar el worker y consultar publicaciones, suscripciones y backlog vigentes.
- Verificar persistencia y fallos de protocolo con PostgreSQL y RabbitMQ desechables.
- Ejecutar el recorrido de publicación y recepción por AMQP, y guardar `eventId`, confirm, estado en Core e inbox/ack.
- Probar recuperación y reintentos reales; incorporar las pruebas técnicas al CI y definir operación y retención.

Los siguientes pasos están en [C1/C3/C4](01-core-accesos-y-conexion.md), [C8](02-core-mensajeria-confiable.md) y [C9–C12](03-core-prueba-y-habilitacion.md). Las integraciones de negocio siguen [diferidas](04-integraciones-posteriores.md).
