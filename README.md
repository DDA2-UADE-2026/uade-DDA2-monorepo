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

Requisitos: Git para clonar, Docker Desktop (o Docker Engine + Compose v2 con soporte de `--wait`) en ejecución, acceso a Internet para descargar imágenes/dependencias y los puertos 5173, 8080 y 15672 libres. En Windows, usar Docker Desktop con contenedores Linux y Windows PowerShell 5.1 o superior (incluido en Windows moderno). No se necesita WSL ni Git Bash para ejecutar el launcher de Windows. No hace falta instalar Java, Node ni Python en el host.

```bash
git clone https://github.com/DDA2-UADE-2026/uade-DDA2-monorepo.git
cd uade-DDA2-monorepo
```

### Construir y levantar todo con un único comando

Después de clonar, ejecutar **desde la raíz del repositorio**, en una terminal interactiva. Elegir **un solo comando según la terminal**:

**Desde macOS o Linux:**

```bash
sh ./start.sh
```

**Desde Windows — CMD (Símbolo del sistema):**

```bat
start.cmd
```

**Desde Windows — PowerShell:**

```powershell
.\start.cmd
```

En Windows, `start.cmd` invoca internamente `start.ps1` mediante Windows PowerShell. No hace falta abrir otra terminal ni ejecutar el `.ps1` manualmente. La política de ejecución se ajusta únicamente para ese proceso, sin cambiar la configuración permanente del equipo; una política corporativa puede impedir la ejecución y requiere consultar al administrador.

**No hay que copiar ni completar la plantilla antes del primer arranque.** Si no existe `.env`, el comando pide la contraseña que quieras para el usuario `admin` y su confirmación, sin mostrarla mientras se escribe. Debe tener al menos 12 caracteres y ser exclusiva de desarrollo.

El script genera automáticamente `DB_PASSWORD`, `RABBITMQ_PASSWORD`, `JWT_SECRET` y `CORE_MACHINE_SECRET`, cada uno con 32 bytes criptográficamente aleatorios (64 caracteres hexadecimales). En macOS/Linux usa Python dentro de un contenedor Docker; en Windows usa el generador criptográfico de .NET incluido en PowerShell. Guarda las cinco variables en `.env` sin imprimir secretos, con permisos restringidos: `0600` en sistemas POSIX y una ACL privada del usuario actual en Windows. En Windows, ubicar el repositorio en un sistema de archivos compatible con ACL (por ejemplo NTFS); si no puede proteger el archivo, el script falla en lugar de escribir secretos sin protección.

**En los siguientes arranques se usa el mismo comando:** si `.env` ya existe, se conserva sin sobrescribirlo ni volver a pedir contraseñas. Si está incompleto, el arranque falla; no se regenera automáticamente, para evitar desincronizar credenciales de volúmenes existentes. El script descarta las cinco variables de credenciales exportadas en la terminal para que se usen las del archivo.

`.env` está ignorado por Git. No mostrar su contenido en consola, logs, capturas ni informes. Conservarlo mientras existan los volúmenes del entorno: si se pierde, restaurar una copia privada; generar otros secretos no recupera el acceso a los datos existentes. La plantilla `.env.example` queda disponible para configuración manual avanzada; no contiene credenciales.

Tras preparar o reutilizar la configuración, el script ejecuta:

```bash
docker compose up --build --wait --wait-timeout 300
```

Este comando **construye y levanta el sistema completo**: frontend, backend, PostgreSQL, RabbitMQ y Core simulado. También ejecuta la preparación automática de secretos, certificados y del usuario administrador. No es necesario iniciar cada servicio por separado.

El comando termina correctamente cuando todos los servicios están saludables. Después, abrir **http://localhost:5173** e ingresar con el usuario `admin` y la contraseña configurada en `BOOTSTRAP_ADMIN_PASSWORD`.

**Tiempo de ejecución:** la primera vez descarga imágenes y dependencias y compila las aplicaciones; los siguientes arranques aprovechan la caché. El objetivo es completar todo en **15 minutos o menos**. La opción `--wait-timeout 300` limita únicamente la espera de los servicios después del build, **no el tiempo total de ejecución**.

| Servicio | URL | Notas |
|---|---|---|
| Frontend | http://localhost:5173 | React compilado, servido por Nginx |
| API | http://localhost:8080 | Health: `/actuator/health` |
| Swagger UI | http://localhost:8080/swagger-ui | Documentación interactiva |
| RabbitMQ | http://localhost:15672 | Usuario `module_desarrollo-social`; contraseña definida en `RABBITMQ_PASSWORD` |
| PostgreSQL | Solo red interna, `db:5432` | Base `dda2`, sin puerto publicado |
| Core simulado | Solo red interna, `https://core-mock:8443` | API de diagnóstico local |

Los puertos publicados escuchan únicamente en `127.0.0.1`. Abrir la aplicación usando `localhost`, desde la misma máquina. Nginx se ejecuta sin privilegios y escucha en el **8080 interno** del contenedor; Compose lo publica en el **5173 del host**, por lo que la URL del frontend no cambia.

### Preparación automática y credenciales

El launcher de la plataforma (`start.sh` o `start.cmd` → `start.ps1`) prepara `.env` solamente cuando no existe. Luego, `local-init` recibe las credenciales de `.env` y las entrega en archivos dentro de volúmenes de Docker separados por servicio. Genera únicamente la CA y los certificados locales de Core/RabbitMQ. No muestra contraseñas ni tokens. El contenedor de preparación termina con código 0: es el resultado esperado.

PostgreSQL y RabbitMQ arrancan con esas credenciales. El backend confía en la CA local y conecta a Core por HTTPS y al broker por AMQPS, manteniendo la verificación de certificado y hostname. Hibernate prepara las tablas y el inicializador crea el administrador y sus permisos. No se ejecuta el seed SQL de cuentas con contraseñas fijas.

El backend usa UID/GID `10001:10001` y el mock `10002:10002`, sin privilegios de root. `local-init` conserva root para asignar los propietarios de sus volúmenes privados, incluidos los ya existentes, con directorios `0700` y archivos `0600`; los consumidores los montan en modo de solo lectura. No hace falta borrar volúmenes ni regenerar credenciales para adoptar estos permisos. La clave privada de la CA queda separada y no se entrega a los consumidores. El backend crea un truststore privado temporal para importar la CA local, sin modificar el truststore del JDK.

Las justificaciones de los avisos intencionales de Sonar y el cambio de puerto para Dokploy están en la [revisión de seguridad de contenedores](notes/sonar-contenedores.md).

| Acceso | Usuario | Contraseña |
|---|---|---|
| Aplicación | `admin` | La que elegiste al ejecutar el launcher de tu plataforma por primera vez (guardada como `BOOTSTRAP_ADMIN_PASSWORD`) |
| Consola RabbitMQ | `module_desarrollo-social` | Generada automáticamente como `RABBITMQ_PASSWORD`; consultarla únicamente en un editor local privado si necesitás acceder a la consola |

Después del login hay que seleccionar ADMIN para entrar a gestión, o CIUDADANO para usar la vista ciudadana. Las credenciales se conservan entre reinicios; el inicializador no cambia la contraseña de una cuenta existente. Si modificás una credencial en `.env` después de inicializar el entorno, el próximo arranque corta para evitar desincronizar los servicios. Para rotar contraseñas de un entorno con datos, se necesita un procedimiento de rotación específico; editar `.env` no las rota.

No se utilizan accesos reales de Core, bases compartidas ni datos de producción.

### Cómo se cargan las variables

- **Backend:** Compose entrega la configuración pública con `environment`. Spring importa las credenciales de `/run/secrets/` mediante `SPRING_CONFIG_IMPORT=configtree:/run/secrets/`.
- **Frontend:** Compose entrega `VITE_SERVER_URL` y `VITE_CLIENT_URL` como argumentos del build. Vite los incorpora al JavaScript; no contienen secretos. Cambiar esas URLs requiere reconstruir la imagen.
- **Configuración local:** el launcher de tu plataforma crea el `.env` de la raíz en el primer arranque; Compose lo carga y exige sus cinco variables. Si invocás Compose directamente, las variables exportadas en tu terminal tienen prioridad sobre el archivo; el script evita esos overrides para las cinco credenciales. Las plantillas de `client/` y `server/` corresponden al desarrollo fuera de Docker. Los archivos `.env` privados no se copian a las imágenes.
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

- [Seguridad de dependencias: preparación y baseline SonarCloud](docs/security/sonarcloud/README.md) — inventario de 122 riesgos, pruebas reproducibles, scopes revisados y criterios para futuras actualizaciones.

- [`TPO - Desarollo de Apps II - Gestión de municipalidad.pdf`](./TPO%20-%20Desarolo%20de%20Apps%20II%20-%20Gestión%20de%20municipalidad.pdf) — enunciado original del TP
- [`TPO - Modulo Desarrollo Social.md`](./TPO%20-%20Modulo%20Desarrollo%20Social.md) — resumen del TP y detalle del módulo asignado
