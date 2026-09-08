# Seguridad, rol activo y preparación para Ciudadanos

Estado al 31 de agosto de 2026: implementado en el backend. No se modificó el frontend, no se agregaron migraciones ni seeds y no se ejecutaron cambios sobre la base de datos del proyecto.

## Alcance confirmado

Se mantiene JWT stateless, sin tablas de sesiones, cookies de autenticación, refresh tokens ni lista de revocación. Ciudadanos todavía no tiene un contrato de autenticación: se prepara únicamente el modelo local. No hay endpoints de login externo, vinculación, desvinculación, aprovisionamiento ni sincronización.

Los tres documentos aportados definen los requisitos conceptuales. Las instrucciones posteriores delimitan lo implementado: roles y permisos se configuran manualmente; el usuario recreará su base de pruebas por su cuenta.

## Diagnóstico y cambios

La implementación previa utilizaba Spring Security, BCrypt y permisos por método con `@PreAuthorize`, pero emitía un JWT con todos los roles y la unión de sus permisos. La selección visual del cliente no modificaba el contexto del backend. Además, `/auth/me` recalculaba permisos desde la base y podía diferir de las autoridades del JWT.

Ahora cada JWT operativo contiene un único `active_role` y exclusivamente sus permisos. El filtro agrega solo la autoridad de ese rol. `/auth/me` devuelve los permisos del JWT utilizado, junto con los datos y roles asignados actuales del usuario.

Se corrigieron también la aceptación inconsistente del prefijo `bearer` en minúsculas y el alcance del manejo de errores del filtro, para no presentar excepciones de negocio como errores de JWT. Se eliminó el secreto JWT predeterminado de ejecución y se valida la configuración al iniciar.

`User` sigue siendo la única identidad del dominio. Las referencias existentes desde programas, ediciones y logs no cambiaron.

## Contrato HTTP

| Operación | Autenticación | Resultado |
| --- | --- | --- |
| `POST /auth/login` | `username` y `password` en el body | Un rol: JWT operativo. Varios roles: JWT de selección sin permisos. Sin roles: 403. |
| `POST /auth/select-role` | `selectionToken` y `role` en el body, sin bearer | Verifica el token temporal, el usuario activo y la asignación actual del rol; emite un JWT operativo. |
| `POST /auth/switch-role` | JWT operativo como bearer; `role` en el body | Emite un JWT nuevo usando los permisos actuales del rol elegido. No invalida el anterior. |
| `GET /auth/me` | JWT operativo como bearer | Perfil actual, roles asignados actuales y contexto de autorización del JWT enviado. |

Los nombres de rol se toman de `user.roles`, sin elegir uno por defecto cuando hay varios. La selección se valida en el servidor. Los roles vacíos o desconocidos no habilitan acceso. El login y el ABM no crean ni asignan automáticamente `CIUDADANO`; el init manual de solicitudes sí crea ese rol y lo asigna a las cuentas de ejemplo.

Ejemplo de login pendiente de selección:

```json
{
  "token": null,
  "expiresIn": null,
  "user": {
    "id": 42,
    "username": "jperez",
    "name": "Juan Pérez",
    "email": "juan@example.com",
    "roles": ["AUDITOR", "CIUDADANO"],
    "activeRole": null,
    "permissions": []
  },
  "requiresRoleSelection": true,
  "selectionToken": "<JWT temporal>",
  "selectionExpiresIn": 300
}
```

Para continuar, enviar a `/auth/select-role`:

```json
{
  "selectionToken": "<JWT temporal recibido>",
  "role": "AUDITOR"
}
```

La respuesta operativa tiene `requiresRoleSelection=false`, `token` y `expiresIn` informados, `user.activeRole` con el rol elegido y sus permisos únicamente en `user.permissions`, sin duplicarlos en la raíz. `selectionToken` y `selectionExpiresIn` quedan en null. El cambio de rol devuelve la misma estructura.

| Error | HTTP | Situación |
| --- | --- | --- |
| `AUTH_INVALID_CREDENTIALS` | 401 | Credenciales incorrectas, cuenta inactiva o sin acceso local. |
| `AUTH_NO_ROLES` | 403 | Usuario sin roles asignados. |
| `AUTH_INVALID_SELECTION_TOKEN` | 401 | Token de selección inválido, vencido o de propósito operativo. |
| `AUTH_ROLE_NOT_ASSIGNED` | 403 | El rol elegido no pertenece actualmente al usuario. |
| `AUTH_INVALID_TOKEN` | 401 | Bearer inválido, vencido, antiguo o de selección. |
| `AUTH_UNAUTHENTICATED` | 401 | Falta un bearer operativo para una ruta protegida. |
| `USER_INCOMPLETE_LOCAL_CREDENTIALS` | 400 | Se intenta habilitar acceso local sin username y contraseña juntos. |

## JWT y límites del esquema stateless

Los JWT operativos usan `token_type=ACCESS`, `sub=users.id`, `active_role`, `permissions`, emisor, emisión y vencimiento. `username` es opcional. Se firman y verifican con HS256. Los JWT de selección usan `token_type=ROLE_SELECTION`, el mismo identificador interno y una vida breve; no llevan permisos operativos.

Un JWT de selección no puede autenticarse en ninguna ruta protegida, incluido el catálogo que solamente exige autenticación. Un JWT operativo tampoco se acepta como prueba temporal en `/auth/select-role`. Se exige propósito, emisor, firma, sujeto válido y marcas de emisión/vencimiento. Los tokens anteriores a este cambio, sin propósito y rol activo, se rechazan: es necesario volver a iniciar sesión.

No guardar sesiones permite este flujo, con estas limitaciones explícitas:

- Cambiar de rol no revoca el JWT anterior. Pueden coexistir JWT válidos del mismo usuario con distintos roles.
- Cerrar sesión en el cliente no invalida una copia del token.
- Retirar roles o permisos, cambiar una contraseña o desactivar al usuario no revoca globalmente los JWT emitidos. Las rutas de negocio siguen tomando sus autoridades del JWT hasta vencer.
- `/auth/me`, la selección y el cambio de rol sí consultan al usuario y rechazan cuentas inactivas. La selección y el cambio de rol también verifican las asignaciones actuales. `/auth/me` conserva los permisos del JWT, aunque los roles listados desde la base hayan cambiado.
- Las rutas `/api/applications` comprueban que el usuario esté activo y conserve el rol activo asignado, y siempre filtran por su identidad interna. Sus permisos son `applications:own:create` y `applications:own:view`; el init los asigna a `CIUDADANO` y conserva a `ADMIN` como superusuario técnico.
- `POST /api/admin/applications` requiere `applications:management:create` en el rol activo. Valida al actor autenticado y permite indicar un titular existente mediante `userId`, sin restricción de jurisdicción ni integración con Ciudadanos. Guarda `registeredByUserId` desde el JWT y audita al administrativo; no admite elegir el registrante ni habilita consultas ajenas. El nuevo permiso no se asigna a `CIUDADANO`. Ver `solicitudes-beneficios.md` para el contrato completo.
- El token temporal puede reutilizarse dentro de su vencimiento; no se promete uso único sin guardar estado. Cada uso vuelve a verificar el usuario y el rol en la base.

Estas limitaciones deben considerarse al elegir tiempos de vida. La revocación de JWT requiere un mecanismo adicional; no se agregó uno por decisión de alcance. Véase [OWASP JSON Web Token Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_Cheat_Sheet.html).

## Modelo preparado para Ciudadanos

`users.username` y `users.password_hash` son opcionales, con una restricción que exige ambos presentes o ambos ausentes. No se generan contraseñas ficticias. El ABM de creación sigue creando cuentas locales con credenciales obligatorias; no aprovisiona identidades externas.

`users.external_citizen_id` es opcional y único. Identifica el vínculo futuro con Ciudadanos, separado de la snapshot. No es una FK a la base externa ni se acepta como campo editable en los DTOs del ABM.

`CitizenSnapshot` es una copia local opcional con PK/FK `user_id`, vinculada 1:1 a `User`. Si se elimina el usuario, su snapshot se elimina por la FK con cascada. El usuario puede existir sin snapshot, con o sin vínculo externo.

| Propiedad | Columna | Tipo |
| --- | --- | --- |
| `userId` | `user_id` | bigint, PK/FK a users |
| `fullName` | `full_name` | varchar(150), opcional |
| `dni` | `dni` | varchar(50), opcional |
| `address` | `address` | varchar(500), opcional |
| `phone` | `phone` | varchar(50), opcional |
| `email` | `email` | varchar(180), opcional |
| `createdAt` | `created_at` | timestamptz, obligatorio |
| `updatedAt` | `updated_at` | timestamptz, obligatorio |

DNI se conserva como texto. Los datos personales admiten una copia parcial, sin inventar información faltante. Las fechas representan la creación y última actualización **local de la snapshot**, no fechas garantizadas por el servicio externo. `updatedAt` comienza igual a `createdAt` y JPA lo actualiza al modificar la copia. El dato maestro continuará perteneciendo a Ciudadanos.

No se agregó un campo de rol activo al usuario ni una tabla de sesiones. El contexto activo pertenece a cada JWT.

La futura vinculación deberá exigir control de ambas cuentas en el mismo flujo, rechazar una identidad ya vinculada a otro usuario, preservar los roles y auditar el cambio. Coincidencias de email, DNI, nombre o teléfono nunca serán prueba de identidad ni causa de vinculación automática. El primer ingreso, detección de posibles cuentas previas y aprovisionamiento se implementarán cuando exista el contrato de Ciudadanos, no en esta entrega.

## Administración de usuarios

`UserManagementResponse` devuelve `permissionsByRole` en lugar de una lista agregada `permissions`, además de `hasLocalCredentials` y `externalCitizenId` de solo lectura. No existe un rol activo universal de un usuario: diferentes JWT pueden tener contextos distintos.

En `PUT /users/{id}`, omitir `username` o enviar null conserva el valor actual. Omitir `password` conserva la contraseña actual. Un futuro usuario sin credenciales puede administrarse sin crearlas; si se habilitan credenciales, se deben enviar username y password juntos y se conservan el mismo `users.id` y vínculo externo. El endpoint no permite quitar credenciales ni vincular/desvincular Ciudadanos.

## Configuración y Postman

Configurar `JWT_SECRET` con un secreto aleatorio propio de al menos 32 bytes. El backend falla al iniciar si falta, es demasiado corto o conserva el antiguo ejemplo `change-me...`. El perfil `docs` tiene una clave exclusiva para pruebas/documentación con H2 y no debe utilizarse en despliegues reales.

- `JWT_EXPIRATION_SECONDS`: 28800 por defecto.
- `JWT_ROLE_SELECTION_EXPIRATION_SECONDS`: 300 por defecto.
- Ambos tiempos deben ser positivos.

En `program-feature.postman_collection.json`, completar `username` y `password`; para usuarios con varios roles, completar `selectedRole` con un nombre de `availableRoles`. `targetRole` es opcional para probar un cambio posterior.

La colección guarda `accessToken`, `selectionToken`, `activeRole` y permisos en variables de colección. Ya no usa el antiguo `pm.globals.token`. Cada intento de login limpia las credenciales anteriores. Las requests incluyen comprobación de rechazo del JWT temporal como bearer, selección, perfil, cambio y errores de autenticación. El cambio se omite en Runner si no se configuró `targetRole`; la selección se omite si el login ya entregó un token operativo.

Los endpoints administrativos siguen necesitando sus permisos específicos. Los nombres de ejemplo no crean roles ni cuentas. `api-entities.dbml` refleja el modelo JPA, sin ejecutar SQL. La recreación de la base de pruebas queda a cargo del usuario.

El frontend no fue modificado. Su selector y consumo de respuestas deberán adaptarse después: el nuevo login multirrol no entrega un token operativo inmediato y las respuestas administrativas cambian a permisos por rol.

## Verificación

Resultado final: **36 pruebas aprobadas**, generación de OpenAPI correcta y **48 requests de Postman** contrastadas con rutas/métodos del contrato. Se verificaron también la sintaxis de scripts, los ejemplos y el manejo de tokens del login y selección. El DBML fue procesado correctamente por el compilador oficial a SQL PostgreSQL; ese archivo temporal de validación quedó en `build`, no se ejecutó ni se agregó como migración.

Se verifican con pruebas de JWT, MockMvc y JPA sobre H2: aislamiento de permisos, separación de propósitos, vencimiento/firma/emisor, selección con cambios de roles o estado, ausencia de roles/credenciales, cambio sin revocación, administración de credenciales sobre el mismo User, vínculo único y snapshot opcional con eliminación en cascada.

El runner original fallaba al cargar clases bajo la ruta con `año`. Se resolvió ejecutando Gradle con la codificación nativa de Windows, sin cambiar la versión Java 17 del proyecto ni mover archivos. Comando utilizado:

```powershell
.\gradlew.bat test generateOpenApiDocs --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
```

También se corrigió un matcher nullable de Mockito en una prueba preexistente de cierre de períodos, que interrumpía la verificación y contaminaba las pruebas siguientes. Las pruebas de arranque usan explícitamente el perfil aislado `docs`.

No se verificó un despliegue real ni su TLS, proxy, secretos efectivos, límites de tráfico o base PostgreSQL. No se realizó una auditoría de vulnerabilidades de dependencias. La aplicación aún no implementa limitación de intentos de login: debe resolverse en una etapa posterior o verificarse en la infraestructura. Véase [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).
