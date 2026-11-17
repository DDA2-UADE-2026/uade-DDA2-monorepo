# Cuenta de prueba de M1 y verificación del login ciudadano

Estado al 16 de noviembre de 2026: cuenta de prueba de ciudadano obtenida y verificada contra el despliegue real de M1. El [contrato de login](m1-auth-contract.md) registraba la cuenta de prueba como pendiente; este documento la aporta y confirma los campos de identidad del caso de éxito.

Las credenciales de abajo son de una cuenta de prueba de M1, sin datos personales reales. No son credenciales de M8 ni habilitan ningún acceso administrativo.

## Credencial para `POST /auth/session/m1/login`

| Campo | Valor |
| --- | --- |
| `email` | `asd@gmial.com` |
| `password` | `Agus123#` |

El dominio `gmial.com` está escrito así en M1 y no existe. Es el valor cargado como contacto de la cuenta, no una dirección que reciba correo.

Identidad que devuelve M1 para esa cuenta:

| Dato | Valor |
| --- | --- |
| `citizenId`, `uuid`, `actorId` | `4cda1b10-394b-42b8-94f1-1093de89a3fb` |
| `user_id` | `2` |
| `actorType` | `CIUDADANO` |
| Nombre y apellido | `agustin ezequiel`, `Romero` |
| `cuit` | `20234234559` |
| `dni` | `23423455` |

M8 guarda `4cda1b10-394b-42b8-94f1-1093de89a3fb` como `external_citizen_id` y `asd@gmial.com` como `User.email`, este último solo como dato de acceso.

## Por qué el CUIT no sirve para este flujo

El equipo entregó la cuenta identificada por CUIT `20234234559`. M1 expone dos endpoints de login y solo uno es compatible con nuestra integración.

| Endpoint de M1 | Identificador | Sirve para M8 |
| --- | --- | --- |
| `POST /auth/usuario/login` | `gmail` (alias `email`, `mail`, `username`) | Sí. Es el que usa [M1AuthClient](../../src/main/kotlin/com/uade/dda2/server/feature/integration/client/M1AuthClient.kt). |
| `POST /auth/login` | `cuit`, con guiones o sin ellos | No. Su respuesta no trae `auth` ni nombre. |

`POST /auth/login` responde `200` con `{personaId, tipoPersona, rol, uuid, citizenId, actorType, actorId}`. Le faltan el campo `auth` y el nombre del ciudadano, ambos exigidos por nuestro cliente, así que una respuesta de ese endpoint produciría `502 M1_AUTH_INVALID_RESPONSE`. Se usó solo para obtener un token con el que consultar los contactos de la cuenta.

Enviar el CUIT a `POST /auth/usuario/login` tampoco funciona:

- Como campo `cuit`, M1 responde `400` con `Campo no permitido: 'cuit'. Campos aceptados: gmail, password`.
- Como valor de `gmail` o de su alias `username`, M1 responde `401 Credenciales invalidas`, con y sin guiones.

El `401` no indica que la contraseña sea incorrecta. En el código de M1, `loginUsuario` busca primero un empleado por su mail y después un ciudadano que tenga un **contacto** con ese valor; el CUIT no está cargado como contacto, así que M1 nunca llega a comparar la contraseña. El CUIT se usa después, de forma interna, para localizar la credencial del ciudadano ya identificado.

## Cómo se obtuvo el correo

El correo no figura en la respuesta del login por CUIT ni lo entregó el equipo de M1. Se obtuvo con el token de ese login:

1. `POST /auth/login` con `{"cuit":"20234234559","password":"Agus123#"}` devolvió `200` y un `Authorization: Bearer` con vigencia de 900 segundos. Confirma que la contraseña es correcta y que la cuenta está `ACTIVO`: un estado distinto habría dado `403`.
2. `GET /ciudadanos/2/contactos` con ese Bearer devolvió `{"items":[{"contactoId":1,"tipo":"EMAIL","valor":"asd@gmial.com","verificado":false}]}`.
3. `POST /auth/usuario/login` con ese correo devolvió `200` y el cuerpo de identidad completo.

El contacto tiene `verificado: false` y M1 autentica igual: el login acepta cualquier contacto con ese valor, verificado o no. Es un ejemplo concreto de por qué el correo del login no acredita un contacto verificado ni sirve para fusionar identidades en M8.

## Validaciones de M8 que cubre la respuesta real

Las comprobaciones de `M1AuthClient` sobre el caso de éxito real:

| Requisito | Resultado |
| --- | --- |
| HTTP `200` y `Content-Type` JSON | Cumple |
| Encabezado `Authorization: Bearer` presente | Cumple |
| `auth` booleano en `true` | Cumple |
| `actorType` igual a `CIUDADANO` | Cumple |
| `citizenId` UUID canónico | Cumple |
| `uuid` y `actorId` coincidentes con `citizenId` | Los tres coinciden |
| `user_id` entero positivo | `2` |
| Nombre utilizable de hasta 150 caracteres | `agustin ezequiel Romero` |

Queda confirmado que `actorType`, `citizenId` y `actorId` se serializan en la raíz del cuerpo de `/auth/usuario/login`, como indicaba el [ejemplo documentado](../external/m1-auth/login-success.json).

## Pendiente

El recorrido completo por `POST /auth/session/m1/login` de M8, con alta de la cuenta local y emisión de la cookie, todavía no se ejecutó contra el M1 real. Las pruebas existentes de [M1SessionFlowTest](../../src/test/kotlin/com/uade/dda2/server/feature/auth/M1SessionFlowTest.kt) siguen usando un M1 simulado en loopback.

Siguen sin confirmar los puntos de variantes de error que lista el [contrato](m1-auth-contract.md): nulabilidad general de los campos de perfil, estructura de contactos no vacíos y códigos para cuenta inexistente, inactiva o bloqueada.

## Fuentes

- Repositorio de M1: `facugagliardi/Modulo1-Ciudadanos-Grupo8`, con `AuthController`, `AuthService.loginUsuario`, `AuthService.login`, `UsuarioLoginRequest` y `LoginRequest`.
- Llamadas HTTP al despliegue `https://ciudadanos-m1-backend.onrender.com`, realizadas el 16 de noviembre de 2026.
