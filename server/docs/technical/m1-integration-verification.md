# Cierre local de la integración con M1

El paso 8 reúne la verificación reproducible y la documentación de login, identidad local, sesión M8, respuestas documentales y proxy de archivos. Este cierre se limita a pruebas locales: no acredita interoperabilidad con M1/Core ni habilita la integración en un despliegue.

## Ejecutar la verificación

Desde `server`, con JDK 17 y dependencias previamente descargadas:

```powershell
.\gradlew.bat verifyM1Integration --console=plain --offline
```

La tarea ejecuta `testM1Integration` y genera `bootJar`. Selecciona explícitamente 18 clases, excluye `core-smoke` y `testcontainers`, y desactiva los flags de transporte heredados del entorno. No ejecuta `testCoreConnectivity`, no depende de Docker ni utiliza PostgreSQL del despliegue.

Los contextos usan el perfil `docs`, H2 desechable y MockMvc. El cliente de login y el de archivos prueban HTTP únicamente contra servidores efímeros en `127.0.0.1`; la sesión M1 utiliza ese mismo tipo de servidor. Los eventos documentales pasan por codec, inbox y servicios locales. La regresión `M8BusinessFlowTest` habilita negocio sobre adaptadores locales, apunta infraestructura a loopback y mantiene apagados consumidor, publicador y comprobación de arranque.

`--offline` evita descargar dependencias; el aislamiento de las contrapartes proviene de la selección de clases y sus configuraciones locales. Si el código y las entradas no cambiaron, Gradle puede reutilizar el resultado. Para forzar una nueva ejecución:

```powershell
.\gradlew.bat verifyM1Integration --console=plain --offline --rerun-tasks
```

## Cobertura y evidencia

Ejecución local del 16 de noviembre de 2026: **295 casos aprobados en 18 clases, sin fallos, errores ni omisiones**. OpenAPI, AsyncAPI y `bootJar` generados correctamente. No se ejecutaron pruebas contra M1, Core ni el backend desplegado.

| Área | Clases seleccionadas | Qué se comprueba |
| --- | --- | --- |
| Cliente de login | `M1AuthClientTest`, `M1AuthConfigurationTest` | Éxito y 401 documentados, actor incorrecto, tipos/UUID, errores remotos, límites, redirecciones y timeout de encabezados/cuerpo. |
| Cuenta local | `M1CitizenAccountServiceTest` | Buscar por UUID, alta solo con CIUDADANO, usuario inactivo, rol retirado, no fusionar por correo/DNI, rollback y ocho primeros ingresos concurrentes. |
| Sesión integrada | `M1SessionFlowTest`, `M1DisabledSessionFlowTest` | CSRF, cookies HttpOnly, descarte de tokens M1, errores, rate limit y prohibición de cambiar a roles administrativos. |
| Regresión de autenticación | `AuthFlowTest`, `CookieAuthFlowTest`, `CitizenModelTest`, `JwtServiceTest`, `LoginRateLimitFilterTest` | Login local, selección de rol, cookies, modelo de cuenta externa, firma/origen JWT y límite de intentos compartido. |
| Eventos y negocio | `ExternalDocumentationFlowTest`, `M8ContractCodecTest`, `M8BusinessFlowTest` | Ambas respuestas, correlación/causalidad, identidad y requisito contra el pedido original, duplicados, eventos atrasados, rollback y revisión humana. |
| Archivos | `M1DocumentClientTest`, `M1DocumentsConfigurationTest`, `ExternalDocumentFileFlowTest` | UUID persistido, pertenencia, permiso vigente, estados, adjunto sin tokens/caché, límites sobre cuerpos normales/chunked/gzip, timeout y errores de servicio. |
| Documentación | `OpenApiDocsGenerationTest`, `AsyncApiDocsGenerationTest` | Contratos HTTP y de eventos generados localmente. Las nueve peticiones de las colecciones M1 apuntan a rutas/métodos existentes de M8; sin credenciales guardadas y con descarga manual desactivada por defecto. |

Los scripts Postman no se ejecutan por HTTP desde Gradle: se contrastan estáticamente sus peticiones con OpenAPI y sus variables públicas. La funcionalidad se verifica mediante MockMvc, HTTP loopback y JPA, sin necesitar una cuenta privada de M1.

Como comprobación adicional del cierre, se compilaron localmente los 13 scripts de las nueve peticiones y se verificaron ocho condiciones de preparación en una VM JavaScript: omisión sin credenciales, preservación de la contraseña y habilitación explícita con UUID válido para descargar. Esta comprobación no envió solicitudes HTTP y no suma casos al total JUnit.

| Artefacto generado | Ubicación |
| --- | --- |
| Reporte HTML de la verificación | `build/reports/tests/testM1Integration/index.html` |
| Resultados JUnit por clase | `build/test-results/testM1Integration/TEST-*.xml` |
| OpenAPI | `build/openapi/openapi.json` |
| AsyncAPI | `build/asyncapi/asyncapi.json` |
| Ejecutable | `build/libs/server-0.0.1-SNAPSHOT.jar` |

Los resultados de esta tarea se guardan separados de `test`, para conservar la evidencia de la selección local. Los archivos de `build/` se generan y no se versionan.

## Estado de los ocho pasos

| Paso | Estado y referencia |
| --- | --- |
| 1. Contrato de login | [Contrato y ejemplos de M1](m1-auth-contract.md) registrados. Casos adicionales de error/nulabilidad y cuenta privada pendientes de acuerdo. |
| 2. Cliente HTTP | Implementado, desactivado por defecto y probado con loopback. |
| 3. Resolver/crear usuario | Implementado por `citizenId`; concurrencia probada con JPA/H2. La vinculación explícita de cuentas anteriores sin UUID queda como trabajo separado; nunca se fusionan por correo o DNI. |
| 4. Endpoint y sesión M8 | `/auth/session/m1/login` implementado con CSRF y sesión limitada a CIUDADANO. Prueba de login real pendiente. |
| 5. Ciudadano sintético | Prueba detenida por instrucción del equipo. No se modificaron datos ni se publicaron eventos; no se retoma con esta tarea. |
| 6. Dos respuestas documentales | [Verificadas en aislamiento](m1-documentation-responses.md). Falta confirmar con M1 el `causationId` del pedido original para la entrega posterior. |
| 7. Proxy de archivos | [Implementado y probado localmente](m1-file-proxy.md), desactivado hasta acordar ruta por UUID y autorización de servicio. La ruta numérica actual de M1 no se usa con UUID. |
| 8. Pruebas y documentación | Comando local consolidado, contratos generados, colecciones preparadas y límites externos documentados. |

## Configuración y colecciones

La configuración de ejemplo está en [`.env.example`](../../.env.example). `M1_AUTH_ENABLED`, `M1_DOCUMENTS_ENABLED` y `CORE_ENABLED` permanecen apagados por defecto. Las claves JWT de M8, las credenciales Core y la futura credencial documental de servicio M1 son independientes.

- [Login ciudadano](../postman/m1-session.postman_collection.json): siete peticiones con cookies/CSRF; correo y contraseña solo en el entorno privado de Postman.
- [Consulta documental administrativa](../postman/m1-documents.postman_collection.json): dos GET a M8. Usa un token administrativo M8 privado con `applications:management:documents:view`. `applicationId` y `requestId` deben elegirse explícitamente; no crea solicitudes ni revisa entregas. La descarga se omite mientras `m1FileProxyReadEnabled` no sea `true`.

Preparar estas colecciones no autoriza ni ejecuta una prueba contra los despliegues. El acceso al archivo debe acordarse con M1 antes de configurar o habilitar una descarga real. El adaptador disponible espera un Bearer de servicio: otro mecanismo acordado requiere adaptar el cliente.

## IDs de una futura prueba coordinada

No hay IDs intercambiados como evidencia remota en este cierre. El ciudadano sintético confirmado por M1 es `4`, UUID `f398dbcb-a2b4-49af-a724-3529c6ddd0f6`; no se usó para crear datos ni publicar mensajes.

Cuando se autorice expresamente una prueba, registrar por separado:

| Campo | Relación esperada |
| --- | --- |
| `applicationId` | Solicitud M8 de la prueba autorizada. |
| `correlationId` | El mismo `applicationId`, conservado en ambas respuestas. |
| `eventId` del pedido | ID de `socialDocumentationRequested` persistido en outbox. |
| `eventId` de respuesta inicial | ID informado por M1 y procesado por inbox; causa igual al pedido. |
| `eventId` de entrega posterior | Otro evento M1, misma correlación; causa del pedido pendiente de confirmación con M1. |
| `requestId` y `documentId` | Solicitud documental de M1 y UUID del archivo, distintos de los IDs de eventos. |

Los UUID generados por las pruebas H2 son datos de aislamiento y no se presentan como IDs reales ni como confirmaciones de procesamiento remoto. No se requieren migraciones para este cierre.
