# Autenticación: cookies del navegador y Bearer

La API admite ambos transportes para el mismo JWT de acceso y los mismos permisos.
El frontend usa exclusivamente cookies HttpOnly; no guarda JWT en `localStorage`,
`sessionStorage` ni estado React. Al actualizar desde la versión anterior se eliminan
los tokens del almacenamiento y es necesario iniciar sesión otra vez.

## Contratos

Los clientes Bearer conservan `POST /auth/login`, `POST /auth/select-role` y
`POST /auth/switch-role`, sus cuerpos y respuestas con JWT. En los recursos protegidos
se envía `Authorization: Bearer <token>`. Un header Bearer explícito tiene prioridad
sobre la cookie; si es inválido o está vacío devuelve 401, sin recurrir a la cookie.

El navegador usa estos endpoints:

| Método | Ruta | Función |
| --- | --- | --- |
| GET | `/auth/session/csrf` | Obtener `{headerName, token}` antes de escribir. Público, incluso con sesión vencida. |
| POST | `/auth/session/login` | `{username, password}`; emite cookie operativa o de selección. |
| GET | `/auth/session/role-selection` | Recuperar usuario y roles pendientes tras recargar, validando la cookie temporal. |
| POST | `/auth/session/select-role` | `{role}`; valida la cookie temporal y emite cookie operativa. |
| POST | `/auth/session/switch-role` | `{role}`; valida identidad/rol asignado y reemplaza la cookie operativa. |
| GET | `/auth/me` | Mis datos, rol activo y permisos; compartido por cookies y Bearer. |
| POST | `/auth/session/logout` | 204; elimina cookies operativa, de selección y CSRF, incluso vencidas. |

Las respuestas de sesión contienen `user`, `requiresRoleSelection` y `expiresIn`,
sin `token` ni `selectionToken`. La selección pendiente no concede acceso a recursos.
Se mantienen los tiempos configurados de JWT: 8 horas de acceso y 5 minutos de selección
por defecto. No se agrega refresh token ni renovación silenciosa. Al vencer, se vuelve
al login. Cambiar de rol emite otro JWT con la vigencia configurada.

## Cookies y CSRF

- `uade_session`: JWT operativo; `uade_role_selection`: JWT temporal.
- `uade_csrf`: cookie del repositorio CSRF de Spring Security. El token enmascarado
  se obtiene mediante `/auth/session/csrf`, por lo que JavaScript tampoco necesita leer esta cookie.
- Todas usan `HttpOnly`, `Path=/`, `SameSite` configurable y ningún `Domain` (host-only).
  `Max-Age` de las cookies de identidad coincide con la vigencia del JWT.
- Toda escritura de `/auth/session/**`, incluido login/logout, requiere CSRF.
  Los demás recursos también lo requieren cuando se autentican con cookie.
  Bearer puede operar sin CSRF; un Bearer inválido nunca habilita la identidad de la cookie.
- El frontend usa `credentials: 'include'` para toda llamada al cliente generado, consulta
  CSRF antes de cada escritura y agrega `X-XSRF-TOKEN`. Esto también cubre multipart.
  Las consultas CSRF simultáneas se comparten; no se guarda una copia duradera susceptible
  de quedar obsoleta después de un login/logout en otra pestaña.
- Login, selección, cambio de rol y logout limpian el token CSRF anterior.
  Spring Security mantiene las respuestas de autenticación fuera de cachés.
- CORS permite credenciales y `X-XSRF-TOKEN` únicamente para los orígenes explícitos de
  `CORS_ALLOWED_ORIGIN_*`. No usar `*` con credenciales.
- Un error CSRF devuelve 403 con `AUTH_CSRF_INVALID`; una identidad inválida devuelve 401.
  Las fallas de red o 5xx no se interpretan como logout. Logout fallido permite reintentar.

La implementación sigue el patrón de [endpoint CSRF de Spring Security](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html),
con cookies y el manejador de tokens enmascarados predeterminado.

## Desarrollo y despliegue

Al iniciar, `AdminUserInitializer` crea el usuario `admin` con contraseña `admin123`
(guardada con BCrypt) solo si no existe ese username, sin distinguir mayúsculas.
También completa el catálogo de permisos, los roles `ADMIN` y `CIUDADANO`, y agrega
`CIUDADANO` a las cuentas con rol `ADMIN` que aún no lo tengan. Conserva las
contraseñas, estados, datos y demás roles de las cuentas existentes. El permiso
operativo depende exclusivamente del rol activo: al elegir `CIUDADANO`, el JWT no
incluye permisos de administración. Los usuarios nuevos asignados a `ADMIN` reciben
`CIUDADANO` en el mismo alta. El catálogo se mantiene alineado con
`docs/database/init.sql`.
Se puede desactivar la creación con `BOOTSTRAP_ADMIN_ENABLED=false`; el perfil `docs`
la desactiva para generar documentación sin crear usuarios ni necesitar tablas.

HTTP local, frontend `http://localhost:5173` y API `http://localhost:8080`:

```dotenv
# server/.env
AUTH_COOKIE_SECURE=false
AUTH_COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGIN_1=http://localhost:5173

# client/.env
VITE_SERVER_URL=http://localhost:8080
```

Usar el mismo hostname en ambos servicios: no mezclar `localhost` con `127.0.0.1`.
### DEV y PROD en subdominios

Con HTTPS en ambos servicios, estos dominios tienen distinto **origen** pero el mismo
**sitio** (`https://x.com`). `SameSite=Lax` permite enviar las cookies en las llamadas
del frontend a su API. Ver [definición de sitio de MDN](https://developer.mozilla.org/en-US/docs/Glossary/Site).

| Entorno | Frontend | Backend |
| --- | --- | --- |
| DEV | `https://app-muni-uade-dev.x.com` | `https://api-muni-uade-dev.x.com` |
| PROD | `https://app-muni-uade.x.com` | `https://api-muni-uade.x.com` |

Variables del **backend en DEV**:

```dotenv
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGIN_1=https://app-muni-uade-dev.x.com
CORS_ALLOWED_ORIGIN_2=
CORS_ALLOWED_ORIGIN_3=
CORS_ALLOWED_ORIGIN_4=
```

Variables del **backend en PROD**:

```dotenv
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGIN_1=https://app-muni-uade.x.com
CORS_ALLOWED_ORIGIN_2=
CORS_ALLOWED_ORIGIN_3=
CORS_ALLOWED_ORIGIN_4=
```

Los orígenes CORS llevan esquema HTTPS, sin barra final ni rutas. Cada API autoriza
solo al frontend de su entorno. Las cookies conservan **HttpOnly, Secure, SameSite=Lax,
Path=/** y **sin Domain**: pertenecen al host de la API que las emitió, manteniendo
separadas las cookies DEV/PROD. No configurar `Domain=.x.com`.

El workflow `.github/workflows/deploy.yml` ya recibe las URLs del frontend desde estas
variables de GitHub Actions; configurar sus valores así:

| Variable de GitHub | Valor |
| --- | --- |
| `DEVELOP_SERVER_URL` | `https://api-muni-uade-dev.x.com` |
| `DEVELOP_CLIENT_URL` | `https://app-muni-uade-dev.x.com` |
| `PRODUCTION_SERVER_URL` | `https://api-muni-uade.x.com` |
| `PRODUCTION_CLIENT_URL` | `https://app-muni-uade.x.com` |

El build incorpora esos valores como `VITE_SERVER_URL` y `VITE_CLIENT_URL`.
El frontend desplegado llama directamente a la URL de la API: `/proxy` corresponde
al servidor de desarrollo Vite y no está definido en el nginx de la imagen final.
Cambiar las variables Vite requiere reconstruir el frontend. Las variables del backend
se configuran en el entorno que ejecuta su contenedor y requieren reiniciarlo; el workflow
actual publica imágenes, no modifica esas variables del servidor.

### Sitios diferentes y proxy local

Si frontend y API pertenecen a sitios distintos, `AUTH_COOKIE_SAME_SITE=None` exige
`AUTH_COOKIE_SECURE=true`; los navegadores que bloquean cookies de terceros pueden
requerir servir la API mediante un proxy del mismo sitio.

El proxy Vite `/proxy` sigue funcionando con `Path=/` y cookies host-only. Si se usa un
backend remoto que emite cookies Secure, el navegador debe acceder por HTTPS o un
entorno local que admita esas cookies. No desactivar Secure en producción para el proxy.

El logout elimina las credenciales de este navegador. Como en el contrato Bearer existente,
no se agrega revocación en base de datos: una copia previa de un JWT sigue siendo válida
hasta vencer, y cambiar de rol no revoca JWT anteriores. No se requieren migraciones.

## Verificación

Backend: `AuthFlowTest`, `CookieAuthFlowTest`, `JwtServiceTest`: compatibilidad Bearer,
prioridad de credenciales, flags de cookies, selección/cambio de rol, vencimiento,
rechazo de CSRF ausente/incorrecto, CORS y logout.

Frontend: `use-auth.test.tsx`, `auth-route-guards.test.ts`, `LoginForm.test.tsx`:
login/recarga/selección/cambio/logout sin JWT en almacenamiento, CSRF, limpieza de datos
del rol anterior y reintento de errores de red.
