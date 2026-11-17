# Contrato de login integrado con M1

Estado al 16 de noviembre de 2026: contrato mínimo registrado a partir de la comunicación de M1 y las capturas de sus ejemplos de éxito y error aportadas por el equipo. El cliente HTTP y la resolución de cuentas locales por UUID están implementados. El cliente está desactivado por defecto. El endpoint proxy y la emisión de la sesión propia de M8 corresponden al paso siguiente.

Este documento cubre el contrato del primer paso, el cliente HTTP del segundo y el servicio de cuentas del tercero. Los ejemplos documentados de HTTP `200` y `401` son la base de sus pruebas locales. Quedan pendientes de confirmación los casos adicionales de error, la nulabilidad no especificada y la cuenta de prueba privada. Las capturas son documentación de M1; no se ha verificado un login real contra su despliegue.

## Fuentes y precedencia

1. Comunicación reciente de M1 aportada por el equipo en este chat y registrada el 16 de noviembre de 2026: endpoint desplegado, condiciones de éxito, identidad canónica y autorización para emitir una sesión propia de M8.
2. Capturas de la documentación de M1 aportadas posteriormente en este chat: sección 6.2, respuesta ciudadana HTTP `200`, y sección 9.1, error de credenciales HTTP `401`. Los cuerpos están transcritos en [éxito](../external/m1-auth/login-success.json) y [credenciales inválidas](../external/m1-auth/login-invalid-credentials.json). Son ejemplos documentados; no constituyen capturas de una ejecución real.
3. [Catálogo y guía anterior de M1](../external/Catalogo_Eventos_y_Guia_Integracion_Modulo1.docx), versión declarada 2.1: secciones 3 y 6. Conservar como antecedente donde las capturas nuevas no aporten información.
4. [Autenticación actual de M8](cookie-auth.md) y [modelo local de usuario](../../src/main/kotlin/com/uade/dda2/server/feature/auth/entity/User.kt): describen lo implementado en M8, no la respuesta de M1.

Ante diferencias, prevalecen la comunicación reciente de M1 y las capturas nuevas. La etiqueta «Producción / Definitiva» de la guía anterior no acredita que sus ejemplos coincidan con el servicio desplegado.

| Tema | Guía anterior 2.1 | Comunicación reciente, capturas nuevas y decisión para M8 |
| --- | --- | --- |
| Tipo de actor | El ejemplo usa `role: "CIUDADANO"`. | El nuevo ejemplo incluye `actorType` y `role`. Validar `actorType: "CIUDADANO"`; `role` no lo sustituye. |
| Identidad ciudadana | El ejemplo no incluye `citizenId` ni `actorId`; menciona IDs numéricos. | El nuevo ejemplo incluye `uuid`, `citizenId` y `actorId` iguales. Usar `citizenId` como `external_citizen_id`. |
| `user_id` | ID numérico presente en el ejemplo. | Es interno de M1 y no reemplaza el UUID canónico ni el ID local de M8. |
| Confianza en JWT | Propone validación HS256 con secreto compartido. | M1 autentica mediante la llamada de servidor a servidor; M8 emite su sesión. La validación directa del JWT de M1 requiere otro acuerdo. |
| Refresh y vigencia | Menciona `X-Refresh-Token`, `X-Token-Expires-In: 900`, access de 15 minutos y refresh de 7 días. | La nueva captura muestra ambos headers y el valor `900`. No define la vigencia del refresh. La sesión M8 conserva sus propios tiempos y no usa estos tokens para renovarse. |
| Errores y perfil | Incluye estados HTTP, un error JSON, nombres y contactos. | Las nuevas capturas confirman ejemplos de `401`, nombre/apellido y contactos vacíos. Otros estados y variantes siguen pendientes. |

## Solicitud de M8 a M1 confirmada

| Elemento | Valor |
| --- | --- |
| Método | `POST` |
| URL base | `https://ciudadanos-m1-backend.onrender.com` |
| Ruta | `/auth/usuario/login` |
| Content-Type | `application/json` |
| Credenciales | `gmail` y `password`, strings en el cuerpo JSON. |
| Autenticación previa | No se acordó una credencial adicional para este login. |

Ejemplo ilustrativo con valores de reemplazo; no contiene una cuenta utilizable:

```http
POST /auth/usuario/login HTTP/1.1
Host: ciudadanos-m1-backend.onrender.com
Content-Type: application/json

{"gmail":"<correo>","password":"<contraseña>"}
```

M8 usará los nombres confirmados `gmail` y `password`. Los alias `email` y `username` mencionados en la guía no son necesarios para esta integración. La contraseña se transmite sin modificarla y no se guarda en la base local, archivos de ejemplo ni logs.

## Respuesta exitosa y reglas de aceptación

M1 confirmó éxito con HTTP `200`, `auth: true`, `actorType: "CIUDADANO"` y el UUID canónico en `citizenId`. El JWT llega en el encabezado de respuesta `Authorization: Bearer <token>`.

La captura de la sección 6.2 documenta estos headers, con los tokens reemplazados por marcadores:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Authorization: Bearer <accessToken>
X-Refresh-Token: <refreshToken>
X-Token-Expires-In: 900
```

El [cuerpo de éxito transcrito](../external/m1-auth/login-success.json) es un objeto sin envoltorio; los campos están en la raíz. Sus tipos observados son:

| Campos | Tipo en el ejemplo |
| --- | --- |
| `user_id`, `dni`, `cuit` | Números enteros. `cuit` requiere un tipo que admita valores superiores a un entero de 32 bits. |
| `uuid`, `citizenId`, `actorId` | Strings con el mismo UUID. |
| `actorType`, `role`, `nombre`, `apellido` | Strings. |
| `auth` | Booleano `true`. |
| `contactos` | Array vacío `[]`; este ejemplo no define la estructura de sus elementos. |

Un ejemplo de éxito no especifica por sí solo la nulabilidad u obligatoriedad de todos los campos en todas las variantes. M8 exigirá los datos de identidad definidos a continuación y tolerará campos adicionales.

| Campo o dato | Confirmación de M1 | Regla de M8 |
| --- | --- | --- |
| Estado HTTP | `200` en el ingreso exitoso. | Otro estado no crea una sesión. |
| `auth` | `true` en el ingreso exitoso. | Exigir booleano `true`; no convertir strings ni números. |
| `actorType` | `"CIUDADANO"` para el flujo ciudadano. | Exigir ese valor; otros actores no obtienen sesión por este flujo. |
| `citizenId` | UUID canónico del ciudadano. | Exigir string UUID válido y guardarlo en forma canónica como `external_citizen_id`. No deducirlo de otros datos. |
| `actorId` | Coincide con `citizenId` para ciudadanos y aparece en el ejemplo nuevo. | Si viene informado, exigir un UUID equivalente. Su obligatoriedad general debe confirmarse; no usarlo como fallback si falta `citizenId`. |
| `uuid` | Aparece igual a `citizenId` y `actorId` en el ejemplo nuevo. | No usarlo como fallback de `citizenId`. Si viene informado para el ciudadano, exigir un UUID equivalente. |
| `user_id` | ID numérico interno de M1. | No usarlo para vincular al ciudadano ni sobrescribir `users.id` de M8. |
| `Authorization` de respuesta | `Bearer <JWT de M1>`. | Exigir el esquema Bearer y un valor no vacío, sin verificar su firma. Tratarlo como secreto y descartarlo al resolver la identidad. No reenviarlo al navegador como sesión M8. |

El campo `role` también aparece en la raíz con valor `CIUDADANO`. M8 decide el tipo de actor con `actorType` y asigna permisos locales; no deriva privilegios del campo `role` de M1.

El formato de `Authorization` está documentado en la captura. En este flujo la prueba de autenticación es la respuesta a la llamada HTTPS que inicia M8; leer o decodificar un JWT aportado por el navegador no prueba identidad. El login proxy no necesita conservar el access token ni el refresh token de M1 para operar las rutas propias de M8.

Para el aprovisionamiento previsto, `User.name` se obtiene de `nombre` y `apellido`, quitando espacios exteriores y uniendo las partes no vacías. El ejemplo trae ambos strings; si no se puede formar un nombre, M8 debe tratar el perfil como no utilizable y no inventar datos.

El nuevo ejemplo tiene `contactos: []` y no incluye un campo de correo en la raíz. M8 podrá guardar como `User.email` el correo enviado en el login que M1 autenticó, como dato de acceso local. Ese dato no acredita un contacto verificado, no prueba el control de otra cuenta local y no se utiliza para fusionar identidades. La estructura de los contactos y la nulabilidad general de los campos de perfil siguen pendientes; no se inventa un contacto para completar el ejemplo.

## Sesión y vinculación que resolverá M8

Estas son decisiones locales del plan, no campos de la respuesta de M1:

- El frontend enviará sus credenciales a M8; el backend llamará a M1 y usará su resultado para resolver el usuario local por `external_citizen_id`.
- La ruta propuesta es `POST /auth/session/m1/login` con `{email, password}`. M8 transformará `email` en `gmail`. No está implementada ni publicada en OpenAPI.
- La sesión utilizará JWT propio de M8 en cookie `HttpOnly`, CSRF y el contrato `CookieSessionResponse` existente. Sus permisos serán los del rol local `CIUDADANO`.
- El ingreso externo no habilitará selección ni cambio hacia roles administrativos, incluso si la cuenta local los tiene asignados. Esa restricción deberá cubrir también las rutas existentes de cambio de rol.
- Los usuarios locales inactivos no podrán ingresar. El aprovisionamiento no reactivará cuentas ni eliminará restricciones ciudadanas conocidas.
- El primer ingreso podrá crear una cuenta externa sin contraseña local. Vincularla a una cuenta local previa requiere verificar el control de ambas identidades y auditar la operación. Coincidencias de correo, DNI, nombre o teléfono no autorizan una fusión.
- Los roles y permisos enviados por M1 no se copiarán como permisos administrativos de M8. La clave JWT de M8 permanece independiente de cualquier clave de M1.

El ciudadano sintético `4`, confirmado por M1 como `ACTIVO`, tiene UUID `f398dbcb-a2b4-49af-a724-3529c6ddd0f6`. Es una referencia para la prueba controlada de vinculación y eventos; no es una cuenta de login ni permite asumir que el usuario local de M8 tenga ID `4`.

## Errores y comportamiento previsto

La captura nueva de la sección 9.1 documenta un error de credenciales HTTP `401`. Su [cuerpo transcrito](../external/m1-auth/login-invalid-credentials.json) es un objeto en la raíz con `timestamp`, `status`, `error`, `message` y `path`; `status` es el entero `401` y los demás campos son strings. El timestamp del ejemplo es `2026-10-21T09:45:00-03:00`, una fecha ilustrativa de M1 y no la fecha de una prueba nuestra.

La guía anterior también documenta `400` por datos faltantes y `403` por cuenta inactiva o bloqueada. Esos casos requieren confirmación actual. El cliente decide por el estado HTTP; no lee ni conserva el cuerpo de un error remoto y no depende de su texto `message` o `status` interno.

| Situación | Comportamiento previsto en M8 |
| --- | --- |
| Credenciales locales faltantes o JSON inválido | `400`, antes de llamar a M1. |
| M1 rechaza las credenciales con HTTP `401` | `401`, sin crear o vincular un usuario ni emitir sesión. Formato documentado en la captura nueva. |
| M1 responde HTTP `200` con `auth: false` | Rechazar sin sesión; M8 lo tratará como credenciales rechazadas (`401`). Confirmar si M1 utiliza esta variante; no aparece en las capturas. |
| M1 rechaza la cuenta por estado | `403`, sin sesión. Confirmar distinción entre inactivo, bloqueado y fallecido. |
| M1 autentica otro tipo de actor | `403`, sin sesión ciudadana. |
| `200` sin datos obligatorios, UUID inválido o IDs contradictorios | `502`, respuesta de identidad no utilizable; sin sesión. |
| M1 devuelve `5xx` o falla la conexión | `502` o `503` según la causa; error de dependencia, no de contraseña. |
| Se agota el tiempo de espera de M1 | `504`, sin reintentar automáticamente el login. |
| M1 limita intentos | Mantener un error de límite de intentos, si se confirma ese contrato. No reintentar automáticamente. |

Los códigos implementados y su estado HTTP local son:

| Código M8 | HTTP local | Situación |
| --- | --- | --- |
| `M1_AUTH_INVALID_REQUEST` | `400` | Correo o contraseña vacíos; no se llama a M1. |
| `M1_AUTH_INVALID_CREDENTIALS` | `401` | HTTP `401` remoto o `200` con booleano `auth: false`. |
| `M1_AUTH_ACCOUNT_FORBIDDEN` | `403` | HTTP `403` remoto, sin deducir un motivo no informado. |
| `M1_AUTH_ACTOR_NOT_ALLOWED` | `403` | Actor autenticado distinto de `CIUDADANO`. |
| `M1_AUTH_RATE_LIMITED` | `429` | HTTP `429` remoto. |
| `M1_AUTH_INVALID_RESPONSE` | `502` | Protocolo, JSON o identidad inválidos; redirecciones u otro estado exitoso distinto de `200`. |
| `M1_AUTH_UPSTREAM_ERROR` | `502` | Otros rechazos HTTP y fallas remotas, incluido `504` enviado por M1. |
| `M1_AUTH_UNAVAILABLE` | `503` | HTTP `503` remoto o falla de conexión, DNS o TLS. |
| `M1_AUTH_TIMEOUT` | `504` | Se agota el tiempo de espera del cliente M8 al llamar a M1. |

`M1AuthException` extiende `ApiException`, por lo que utiliza el contrato de errores existente de M8 cuando se conecte a una ruta. Conserva únicamente el código local y, si se conoce, el estado HTTP remoto; no conserva la excepción original, URLs, cuerpos ni headers. No se copian mensajes de M1 al frontend. El rate limiting de la futura ruta proxy se implementará en el paso de sesión.

## Cliente HTTP implementado

La [configuración](../../src/main/kotlin/com/uade/dda2/server/config/M1AuthConfiguration.kt) crea un [M1AuthClient](../../src/main/kotlin/com/uade/dda2/server/feature/integration/client/M1AuthClient.kt) independiente del cliente de máquina de Core. Usa [RestClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html) y [JdkClientHttpRequestFactory](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/JdkClientHttpRequestFactory.html), ya disponibles en el stack del proyecto, sin agregar dependencias.

| Variable | Valor predeterminado | Función |
| --- | --- | --- |
| `M1_AUTH_ENABLED` | `false` | Crea el cliente al activar; no realiza una llamada de arranque. |
| `M1_AUTH_BASE_URL` | `https://ciudadanos-m1-backend.onrender.com` | Origen HTTPS sin ruta, credenciales, query ni fragmento. HTTP solo en loopback para pruebas. |
| `M1_AUTH_CONNECT_TIMEOUT` | `5s` | Tiempo máximo de conexión. |
| `M1_AUTH_READ_TIMEOUT` | `15s` | Tiempo de espera de respuesta. |

Al activar se validan el origen y los tiempos, entre 1 y 2147483647 milisegundos. El perfil `docs` mantiene el cliente desactivado. No requiere compartir claves JWT, guardar una contraseña de prueba ni activar Core.

`login(M1LoginRequest(gmail, password))` envía una solicitud al endpoint acordado. Quita espacios exteriores del correo y conserva la contraseña exactamente. No sigue redirecciones ni agrega reintentos automáticos, caché de credenciales, cookies, tokens previos o llamadas a refresh.

El éxito requiere `Content-Type` JSON, el encabezado Bearer acordado y un cuerpo de hasta 64 KiB. Se validan tipos sin coerción, UUID completos, coincidencia de identificadores auxiliares cuando estén presentes y un nombre utilizable de hasta 150 caracteres. Se ignoran los campos adicionales de M1. El resultado `M1CitizenIdentity` contiene solo `citizenId` UUID, `userId` numérico opcional y `name`; no conserva access token, refresh token, roles remotos ni contactos.

Las [pruebas HTTP](../../src/test/kotlin/com/uade/dda2/server/feature/integration/M1AuthClientTest.kt) utilizan un servidor loopback y los ejemplos versionados. Las [pruebas de configuración](../../src/test/kotlin/com/uade/dda2/server/feature/integration/M1AuthConfigurationTest.kt) comprueban activación, propiedades y valores rechazados. Ejecutar desde `server`:

```powershell
.\gradlew.bat test --tests '*M1AuthClientTest' --tests '*M1AuthConfigurationTest' --console=plain
```

Verificación local del 16 de noviembre de 2026: 72 pruebas nuevas de M1 y 24 de regresión de autenticación, cookies y arranque aprobadas, sin omisiones. `bootJar` generado correctamente. Se comprobó el timeout tanto antes de los headers como durante la lectura del cuerpo, el rechazo de redirecciones y la ausencia de reenvíos tras errores. Estas pruebas no contactan al despliegue de M1.

El cliente HTTP no crea usuarios ni modifica roles o sesiones locales. La cuenta local se resuelve con el servicio siguiente. El endpoint `POST /auth/session/m1/login` sigue pendiente de implementación.

## Resolución de cuenta local implementada

[M1CitizenAccountService](../../src/main/kotlin/com/uade/dda2/server/feature/auth/service/M1CitizenAccountService.kt) recibe la identidad aceptada por `M1AuthClient` y el correo utilizado en ese mismo login. Su método interno es `resolve(identity, authenticatedEmail)`. El futuro endpoint debe invocarlo después de que termine la llamada HTTPS, sin mantener una transacción de base abierta durante la autenticación remota. No se admite que el navegador elija un `citizenId` para este flujo.

El resultado [M1LocalAccount](../../src/main/kotlin/com/uade/dda2/server/feature/auth/dto/M1LocalAccount.kt) contiene `userId` local de M8, `citizenId` UUID y `created`. No contiene roles, permisos, contraseñas ni tokens y no concede una sesión.

| Caso | Resultado |
| --- | --- |
| El UUID ya está vinculado a una cuenta activa con `CIUDADANO`. | Reutiliza su ID local. Conserva nombre, correo, credenciales y todos los roles asignados; no copia el nuevo perfil de M1 sobre ellos. |
| La cuenta local está inactiva. | `403 M1_AUTH_LOCAL_ACCOUNT_INACTIVE`; no la reactiva ni crea otra. |
| A la cuenta vinculada le retiraron `CIUDADANO`. | `403 M1_AUTH_CITIZEN_ROLE_REQUIRED`; no repone el rol automáticamente. |
| El UUID no tiene cuenta local. | Crea un usuario activo con UUID en minúsculas, nombre validado, correo de acceso sin espacios exteriores y solo el rol `CIUDADANO` del catálogo local. `username` y `password_hash` quedan en null. |
| Falta `CIUDADANO` en el catálogo al crear la cuenta. | `503 M1_AUTH_CITIZEN_ROLE_UNAVAILABLE`; no crea roles, permisos ni una cuenta parcial. |
| Coinciden correo, nombre, DNI o el ID numérico de M1 con otra cuenta. | No vincula ni modifica esa cuenta. UUID distintos siguen siendo identidades independientes, aun con el mismo correo. |
| Un vínculo previo contiene letras mayúsculas en el UUID. | Lo encuentra sin distinguir mayúsculas y lo reutiliza sin reescribirlo. |
| Hay varios vínculos legados que difieren solo en mayúsculas. | `409 M1_AUTH_IDENTITY_CONFLICT`; requiere revisión y no elige una cuenta arbitrariamente. |

Los límites locales son nombre de 1 a 150 caracteres y correo de acceso de 1 a 180 después de quitar espacios exteriores. Un nombre no utilizable devuelve `M1_AUTH_INVALID_RESPONSE`; un correo vacío o demasiado largo devuelve `400 M1_AUTH_INVALID_EMAIL`. El ID numérico `user_id` de M1 no se utiliza para buscar, asignar ni sobrescribir `users.id`.

La snapshot ciudadana no se crea ni modifica durante el aprovisionamiento. Se conservan bloqueos, fallecimiento y alertas ya recibidas. Su efecto sobre nuevas solicitudes sigue bajo la política de negocio existente; el servicio no deduce nuevas restricciones de login a partir de esa copia.

El alta y su auditoría `CREATE/USER` se confirman juntas. El registro identifica la fuente `M1`, el UUID y el rol asignado; no guarda correo, contraseña, tokens ni el cuerpo del login. Reutilizar una cuenta no registra otra alta ni un `LOGIN`: el registro de ingreso corresponde a la emisión de sesión del paso siguiente.

Se usa [TransactionTemplate](https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html) con una transacción independiente por intento. La restricción única existente de `external_citizen_id` arbitra los primeros ingresos simultáneos, que siempre insertan el UUID canónico en minúsculas. Si una inserción pierde esa carrera, se revierte por completo y se consulta la cuenta ganadora en una transacción nueva, volviendo a comprobar su estado y rol. No se consulta dentro de la transacción abortada ni se repite el login HTTP. Si no existe una cuenta ganadora, se devuelve `503 M1_AUTH_PROVISIONING_FAILED` sin conservar la causa de base de datos.

El aprovisionamiento queda confirmado de forma independiente de una eventual transacción exterior. El paso de sesión debe volver a consultar el usuario activo y su rol antes de emitir el JWT ciudadano. No se requieren migraciones: se reutilizan las columnas, la restricción de credenciales y la unicidad existentes. La vinculación de una cuenta local previa sin UUID sigue requiriendo un flujo separado que verifique ambas identidades.

Las [pruebas de cuentas](../../src/test/kotlin/com/uade/dda2/server/feature/auth/M1CitizenAccountServiceTest.kt) usan JPA y una base H2 descartable en modo PostgreSQL. Cubren la reutilización, restricciones locales, ausencia de fusiones por datos personales, auditoría y rollback. La carrera fuerza que ocho llamadas desde dos instancias lean inicialmente que no existe la cuenta: las ocho deben resolver al mismo ID, con una sola asignación de rol y una sola alta auditada. Ejecutar desde `server`:

```powershell
.\gradlew.bat test --tests '*M1CitizenAccountServiceTest' --console=plain
```

Verificación local del paso 3, 16 de noviembre de 2026: 18 pruebas nuevas de cuentas y 119 de regresión aprobadas, sin omisiones, incluyendo cliente M1, autenticación local, cookies, modelo ciudadano, eventos M8 y arranque. `bootJar` generado correctamente. La prueba de concurrencia se ejecutó sobre H2; no se verificó esta etapa sobre PostgreSQL ni contra el login remoto de M1.

El endpoint proxy y la emisión de cookies/JWT todavía no están implementados. No se agregan rutas ni cambios de estructura al contrato OpenAPI actual.

## Pendientes y criterio de cierre del primer paso

- [x] Registrar URL, método, cuerpo de solicitud y condición mínima de éxito confirmados por M1.
- [x] Separar UUID ciudadano, ID interno de M1 e ID local de M8.
- [x] Documentar la precedencia frente a la guía 2.1 y la decisión de sesión propia de M8.
- [x] Preparar las validaciones de identidad, el tratamiento de errores y la solicitud de confirmación.
- [x] Recibir y transcribir el ejemplo documentado de HTTP `200`, con campos en la raíz y tokens reemplazados en los headers.
- [x] Registrar los tipos observados de `uuid`, `citizenId`, `actorId`, `user_id` y perfil, incluido `contactos: []`.
- [x] Recibir y transcribir el ejemplo documentado de HTTP `401` por credenciales inválidas.
- [x] Definir nombre local desde `nombre`/`apellido` y correo de acceso desde la solicitud autenticada, sin atribuir verificación de contacto ni fusionar cuentas.
- [ ] Confirmar nulabilidad y obligatoriedad general de campos de perfil e identificadores auxiliares, y estructura de contactos no vacíos.
- [ ] Confirmar variantes para cuenta inexistente, inactiva o bloqueada, actor no ciudadano y campos faltantes; aclarar si existe `200` con `auth: false` y si hay `429`.
- [ ] Coordinar una cuenta de prueba por privado y la forma de acceso del equipo, sin versionar sus credenciales.

El contrato mínimo de éxito y credenciales inválidas queda definido para avanzar con el cliente HTTP y las pruebas locales. Los pendientes anteriores se conservan para completar las variantes del contrato y preparar la prueba real. La prueba de login real corresponde a la implementación posterior del cliente y la sesión; el acceso a archivos por UUID y la autorización documental se acuerdan por separado.

## Mensaje preparado para M1

Borrador para enviar por el canal habitual; no se ha enviado desde este chat:

> Buenas, equipo. Vamos a usar el backend de M8 como proxy: recibimos el correo y contraseña, llamamos por HTTPS a `POST /auth/usuario/login` con `gmail` y `password` y, si responde `200`, `auth: true`, `actorType: "CIUDADANO"` y `citizenId` UUID, emitimos nuestra sesión con permisos de Desarrollo Social. Usaremos `citizenId` como `external_citizen_id`; `user_id` queda como ID interno de M1.
>
> Ya tenemos los ejemplos de las secciones 6.2 y 9.1: éxito con `uuid`, `citizenId` y `actorId` iguales, nombre/apellido, `contactos: []` y error `401` de credenciales. Nos permiten avanzar con el cliente. Para completar las variantes, ¿nos confirman qué campos pueden faltar o ser null, cómo vienen los contactos no vacíos y los códigos para usuario inexistente, inactivo/bloqueado, actor no ciudadano y campos faltantes? También si hay casos `200` con `auth: false` y límites `429`.
>
> Usaremos su mensaje reciente y estos ejemplos como referencia por encima de la guía 2.1 anterior. Como el ejemplo no devuelve correo, conservaremos el utilizado en el login autenticado como dato de acceso de M8, sin considerarlo contacto verificado ni usarlo para fusionar cuentas. No necesitamos compartir la clave JWT ni validar su token directamente para este flujo. Coordinamos por privado la cuenta de prueba y no guardaremos credenciales ni tokens en el repositorio. El acceso administrativo a archivos por UUID lo cerraremos aparte.
