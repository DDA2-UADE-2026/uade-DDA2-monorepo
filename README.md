![Header](.github/social/header.png)

# Municipalidad UADE — Módulo Desarrollo Social

Monorepo del módulo **"Desarrollo social, salud comunitaria y beneficios"**, desarrollado para la materia Desarrollo de Aplicaciones II (UADE) en el marco del TPO "Municipalidad UADE — Plataforma distribuida para la gestión integral de una municipalidad".

## Estructura del repo

```
./
├── client/   # Frontend (Vite + React + TypeScript)
├── server/   # Backend
└── notes/    # Notas y documentación de trabajo
```

## Levantar el sistema completo en local (G1)

Requisitos: Git para clonar, Docker Desktop (o Docker Engine + Compose v2) en ejecución, acceso a Internet para descargar imágenes/dependencias y los puertos 5173, 8080 y 15672 libres. No hace falta instalar Java, Node ni Python. Antes del primer arranque hay que preparar el `.env` de la raíz.

```bash
git clone https://github.com/DDA2-UADE-2026/uade-DDA2-monorepo.git
cd uade-DDA2-monorepo
```

### Configuración previa

Copiar la plantilla de la raíz:

```bash
cp .env.example .env
```

En Windows CMD, usar `copy .env.example .env`. Abrir `.env` en un editor y completar **todos** estos valores, sin reutilizar credenciales reales:

| Variable | Qué completar |
|---|---|
| `DB_PASSWORD` | Contraseña local de PostgreSQL |
| `RABBITMQ_PASSWORD` | Contraseña local de la consola y conexión RabbitMQ |
| `JWT_SECRET` | Clave aleatoria de al menos 32 caracteres ASCII para firmar JWT |
| `BOOTSTRAP_ADMIN_PASSWORD` | Contraseña de al menos 12 caracteres para el usuario inicial `admin` |
| `CORE_MACHINE_SECRET` | Secreto compartido del backend y el mock local de Core |

La plantilla está comentada y no contiene credenciales. `.env` está ignorado por Git. No mostrar su contenido con comandos, en logs, capturas ni informes. Usar valores de una sola línea; si contienen `$` o `#`, escribirlos entre comillas simples en el editor. Variables del mismo nombre exportadas en tu terminal tienen prioridad sobre `.env`; evitá esos overrides al seguir este procedimiento.

Si falta `.env` o alguna variable está vacía, Compose corta con un mensaje claro antes de construir o arrancar servicios. El inicializador también rechaza claves JWT o contraseñas de admin demasiado cortas, sin imprimirlas.

Desde la raíz, un único comando construye y levanta el entorno, y termina correctamente cuando los servicios están saludables:

```bash
docker compose up --build --wait --wait-timeout 300
```

La primera ejecución descarga dependencias y compila. El límite de espera de 300 segundos se aplica al arranque después de construir; el requisito G1 es de **15 minutos totales**, incluyendo descarga y build. Las siguientes ejecuciones usan caché.

| Servicio | URL | Notas |
|---|---|---|
| Frontend | http://localhost:5173 | React compilado, servido por Nginx |
| API | http://localhost:8080 | Health: `/actuator/health` |
| Swagger UI | http://localhost:8080/swagger-ui | Documentación interactiva |
| RabbitMQ | http://localhost:15672 | Usuario `module_desarrollo-social`; contraseña definida en `RABBITMQ_PASSWORD` |
| PostgreSQL | Solo red interna, `db:5432` | Base `dda2`, sin puerto publicado |
| Core simulado | Solo red interna, `https://core-mock:8443` | API de diagnóstico local |

Los puertos publicados escuchan únicamente en `127.0.0.1`. Abrir la aplicación usando `localhost`, desde la misma máquina.

### Preparación automática y credenciales

`local-init` recibe las credenciales de `.env` y las entrega en archivos dentro de volúmenes de Docker separados por servicio. Genera únicamente la CA y los certificados locales de Core/RabbitMQ. No muestra contraseñas ni tokens. El contenedor de preparación termina con código 0: es el resultado esperado.

PostgreSQL y RabbitMQ arrancan con esas credenciales. El backend confía en la CA local y conecta a Core por HTTPS y al broker por AMQPS, manteniendo la verificación de certificado y hostname. Hibernate prepara las tablas y el inicializador crea el administrador y sus permisos. No se ejecuta el seed SQL de cuentas con contraseñas fijas.

| Acceso | Usuario | Contraseña |
|---|---|---|
| Aplicación | `admin` | La que completaste en `BOOTSTRAP_ADMIN_PASSWORD` |
| Consola RabbitMQ | `module_desarrollo-social` | La que completaste en `RABBITMQ_PASSWORD` |

Después del login hay que seleccionar ADMIN para entrar a gestión, o CIUDADANO para usar la vista ciudadana. Las credenciales se conservan entre reinicios; el inicializador no cambia la contraseña de una cuenta existente. Si modificás una credencial en `.env` después de inicializar el entorno, el próximo arranque corta para evitar desincronizar los servicios. Para rotar contraseñas de un entorno con datos, se necesita un procedimiento de rotación específico; editar `.env` no las rota.

No se utilizan accesos reales de Core, bases compartidas ni datos de producción.

### Cómo se cargan las variables

- **Backend:** Compose entrega la configuración pública con `environment`. Spring importa las credenciales de `/run/secrets/` mediante `SPRING_CONFIG_IMPORT=configtree:/run/secrets/`.
- **Frontend:** Compose entrega `VITE_SERVER_URL` y `VITE_CLIENT_URL` como argumentos del build. Vite los incorpora al JavaScript; no contienen secretos. Cambiar esas URLs requiere reconstruir la imagen.
- **Configuración local:** Compose carga el `.env` de la raíz y exige sus cinco variables. Las plantillas de `client/` y `server/` corresponden al desarrollo fuera de Docker. Los archivos `.env` privados no se copian a las imágenes.
- Fuera de Compose, para crear un administrador nuevo el backend requiere `BOOTSTRAP_ADMIN_PASSWORD` de al menos 12 caracteres. Si el administrador ya existe, no se modifica su contraseña.

### Alcance del Core simulado

Acordado para este entorno: Core local simulado y RabbitMQ real. Ciudadanos, Documentación, Atención Ciudadana y Rentas quedan pendientes; no se inventan sus contratos ni mocks SOAP.

El mock sigue las rutas y DTOs de nuestro [contrato técnico de Core](server/docs/roadmaps/00-contrato-core-verificado.md): autenticación de máquina, dashboard, publicaciones, suscripciones y consulta de eventos. Requiere credenciales/token; devuelve listas vacías y 404 para eventos inexistentes. Sus métricas en cero describen el mock, no la actividad del Core real.

La cola `q.desarrollo-social` y el exchange `muni.inbox` se crean en RabbitMQ. Publicador y consumidor de nuestra app quedan desactivados: este mock **no implementa el worker, ruteo de eventos, reintentos ni DLQ de Core**. El arranque y el diagnóstico HTTP/AMQP no acreditan resiliencia ni integración de negocio (eje C).

La interfaz local usa HTTP y cookies sin Secure; HTTPS/AMQPS aquí corresponde a las conexiones técnicas con Core/broker. Este entorno de G1 no acredita por sí solo los controles de seguridad del despliegue (G6/eje D).

### Verificación y operación

Registro de la prueba local y sus límites: [validación de G1](notes/g1-validacion.md).

Con el entorno levantado, ejecutar el smoke test (no necesita Python instalado en el host):

```bash
docker compose run --rm --no-deps verify
```

Verifica frontend, salud del backend, login con cookies y CSRF, selección de ADMIN, conexión HTTPS/AMQPS y las consultas del dashboard de integración. No imprime contraseñas ni tokens. Para el mock aislado:

```bash
docker compose exec -T core-mock python -m unittest -v
```

```bash
docker compose ps -a              # local-init debe terminar con código 0
docker compose logs -f server     # logs del backend
docker compose down              # detener; conserva datos y secretos
```

Para empezar deliberadamente desde cero, **borrando todos los datos y credenciales de este entorno**:

```bash
docker compose down -v
docker compose up --build --wait --wait-timeout 300
```

Si usabas la configuración anterior que generaba contraseñas, después de preparar `.env` detené ese entorno con `docker compose down` **sin `-v`**, para conservar sus datos y liberar los puertos. Luego usá un proyecto nuevo (`docker compose -p dda2-local-nuevo up --build --wait --wait-timeout 300` y `-p dda2-local-nuevo` en los comandos posteriores). Como alternativa, reinicializá el proyecto anterior solo si no necesitás conservar sus datos. No elimines únicamente los secretos dejando la base/broker anteriores.

Para validar la configuración sin imprimir credenciales, ejecutar `docker compose config --quiet`. No compartir la salida de `docker compose config` sin `--quiet`, porque incluye valores resueltos.

Si el arranque falla, revisar `docker compose ps -a` y `docker compose logs local-init db rabbitmq core-mock server client`. Errores de descarga requieren conectividad; errores de bind requieren liberar los puertos. Un contenedor `unhealthy` no se considera G1 cumplido.

## Integración continua

El workflow principal `CI` corre en pull requests y pushes a `develop` y `main`:

- Cambios en `client/`: `npm ci`, lint, tests de Vitest (`npm run test:run`) y build con TypeScript/Vite.
- Cambios en `server/`: `./gradlew build --no-daemon`, que compila, ejecuta la suite `test` de JUnit y genera los artefactos.
- Si cambian ambas aplicaciones, los jobs corren en paralelo. Si cambia `.github/workflows/ci.yml`, se ejecutan ambos. Los cambios solo de documentación omiten las suites.

Un fallo de tests hace fallar `CI`; la publicación de imágenes existente requiere que ese workflow termine correctamente. Los tests frontend ya no se duplican en un workflow separado.

La suite habitual del backend usa H2 y adaptadores locales. Las tareas opt-in de PostgreSQL y `testCoreConnectivity` contra Core remoto no se ejecutan con `build`; requieren preparación y autorización propias. Esta configuración no agrega pruebas E2E ni acredita los demás controles de G3.

## Stack

- **Frontend**: Vite, React 19, TypeScript, TanStack Router/Query, Tailwind CSS, shadcn/ui, Tabler Icons, HeyAPI (cliente OpenAPI)

## Documentación

- [`TPO - Desarollo de Apps II - Gestión de municipalidad.pdf`](./TPO%20-%20Desarolo%20de%20Apps%20II%20-%20Gestión%20de%20municipalidad.pdf) — enunciado original del TP
- [`TPO - Modulo Desarrollo Social.md`](./TPO%20-%20Modulo%20Desarrollo%20Social.md) — resumen del TP y detalle del módulo asignado
