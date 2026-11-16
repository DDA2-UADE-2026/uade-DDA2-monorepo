# Contrato de login integrado con M1

Estado al 16 de noviembre de 2026: contrato mínimo registrado a partir de la comunicación de M1 y las capturas de sus ejemplos de éxito y error aportadas por el equipo. El backend M8 actuará como proxy de autenticación: enviará las credenciales a M1 por HTTPS y emitirá su propia sesión con permisos locales. El cliente HTTP y el endpoint de M8 todavía no están implementados.

Este documento cubre el primer paso de la integración. Los ejemplos documentados de HTTP `200` y `401` permiten implementar el cliente y sus pruebas locales. Quedan pendientes de confirmación los casos adicionales de error, la nulabilidad no especificada y la cuenta de prueba privada. Las capturas son documentación de M1; no se ha verificado un login real contra su despliegue.

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
| `Authorization` de respuesta | `Bearer <JWT de M1>`. | Tratarlo como secreto. No validarlo con la clave de M8 ni reenviarlo al navegador como sesión M8. |

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

La guía anterior también documenta `400` por datos faltantes y `403` por cuenta inactiva o bloqueada. Esos casos requieren confirmación actual. El cliente futuro decide por el estado HTTP y los campos estructurados, sin depender del texto de `message` ni interpretar un `status` del cuerpo como un éxito si el HTTP indica error.

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

Esta tabla define la política propuesta de M8. Los códigos internos del backend y los detalles de timeout y rate limiting se concretarán al implementar el cliente y la ruta proxy. Los cuerpos y headers de error de M1 no se reenviarán sin filtrar ni se registrarán con credenciales o tokens.

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
