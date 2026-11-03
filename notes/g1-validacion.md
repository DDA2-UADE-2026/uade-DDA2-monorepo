# Validación local de G1

Fecha: 2 de noviembre de 2026 (Argentina).

## Alcance acordado

Frontend, backend, PostgreSQL, RabbitMQ real y Core HTTP simulado. Las otras áreas municipales y sus contratos SOAP/REST quedan pendientes por decisión del equipo. El mock cubre las rutas de autenticación y lectura usadas por el dashboard de integración; no sustituye el worker, el ruteo, los reintentos ni la DLQ del Core real. Esta validación no acredita el eje C ni G6/eje D.

## Actualización: credenciales elegidas en `.env`

La aclaración posterior del profesor permite completar `.env` antes del arranque y prohíbe mostrar credenciales por consola. La configuración actual usa la plantilla comentada de la raíz, exige sus cinco valores y no incluye comandos para imprimirlos. La CA y los certificados locales siguen generándose automáticamente.

Validación de esta actualización, sin nuevas dependencias de aplicación:

- Tres tests de configuración aprobados: cada variable ausente/vacía (diez subcasos), ausencia del archivo aun con variables exportadas y valores literales con `$`/`#`.
- Arranque y smoke completos aprobados en `dda2-g1-env-validation`, con `.env` privado temporal, volúmenes nuevos y sin publicar puertos. Se reutilizaron imágenes de frontend/backend/mock; se reconstruyó el inicializador. No se repitió la medición de build sin caché del código de aplicación, que no cambió.
- Las cinco credenciales entregadas al backend se compararon con los valores de `.env`, sin mostrarlos.
- Se rechazaron JWT y contraseña de administrador demasiado cortos, y un cambio incompatible de contraseña de base. Se conservaron las credenciales anteriores y el smoke volvió a pasar.
- Los logs capturados del entorno aislado no contenían ninguna de las cinco credenciales de prueba.
- El entorno anterior del usuario no se detuvo ni se modificó. La migración a `.env` y a un proyecto nuevo está documentada en el README.

```bash
python3 -m unittest discover -s infra/local -p 'test_configuration.py' -v
```

[Resultados de la actualización](evidencias/g1-env-validacion.json).

## Validación histórica anterior a la configuración con `.env`

Los tiempos y resultados siguientes corresponden a la versión anterior con credenciales generadas. Se conservan como evidencia histórica del build; no describen la preparación actual de credenciales.

### Procedimiento y tiempo

1. Clon local independiente del repositorio, con los cambios candidatos de G1 superpuestos. Sin `.env` privados, `node_modules` ni directorios de build del host. Las fuentes de esa copia se compararon byte por byte con los archivos candidatos.
2. Builder nuevo `dda2-g1-cold`, driver `docker-container`, sin caché previa de compilación. `BUILDX_BUILDER=dda2-g1-cold` y `COMPOSE_BAKE=true` seleccionaron ese builder para el comando de Compose.
3. Proyecto `dda2-g1-validation` con volúmenes nuevos. Docker 28.0.4, Compose 2.34.0-desktop.1, Linux arm64 sobre Docker Desktop.
4. Comando ejecutado:

```bash
docker compose -p dda2-g1-validation up --build --wait --wait-timeout 300
```

**Resultado: exit 0, 202,84 segundos (3 min 23 s), por debajo de 15 minutos.** Incluye la construcción de las imágenes propias y espera de todos los servicios. El builder descargó sus bases y dependencias sin caché. Las imágenes de ejecución de PostgreSQL/RabbitMQ ya se habían descargado durante el diagnóstico inicial; el tiempo no mide esa descarga previa ni la instalación de Docker/Git. La velocidad de Internet y los recursos de otra máquina pueden variar.

No fue un clon del remoto que ya contuviera estos cambios: es la validación del estado candidato. Antes de que el docente clone, hay que integrar y publicar todos los archivos de G1; esta evidencia no acredita por sí sola su disponibilidad en GitHub ni un tag de entrega.

### Resultados funcionales

- `local-init` terminó con código 0; generó secretos y certificados en volúmenes privados.
- Base, broker, mock, backend y frontend quedaron saludables.
- Backend compilado con Kotlin/Java 17; frontend compilado con TypeScript y Vite.
- Smoke test: frontend HTTP 200, backend UP, login de admin con cookies/CSRF, selección de ADMIN, consulta de identidad, HTTP autenticado con Core mock y conexión AMQPS con RabbitMQ.
- Dashboard, publicaciones, suscripciones, eventos y métricas locales consultados mediante la API protegida; evento inexistente devolvió 404.
- TLS de Core y broker con validación de certificado y hostname; sin modificar las restricciones de transporte del backend.
- Interfaz pública abierta y renderizada en navegador en `http://localhost:5173`.
- Tras `down` sin `-v`, nuevo arranque y smoke aprobados. Contraseña conservada; no se mostró en los logs. Reinicio + smoke: 48,94 segundos.

### Checks adicionales

```bash
docker compose config --quiet
git diff --check
sh -n infra/local/init/init.sh server/local-entrypoint.sh
python3 -m unittest discover -s infra/local/core-mock -v
```

Todos aprobados. Pruebas del mock repetidas dentro de su contenedor, también aprobadas.

Suites del backend ejecutadas en un contenedor efímero de la etapa `build`, contra H2 (sin acceder a bases compartidas):

```bash
./gradlew test --tests '*AdminUserInitializerTest' \
  --tests '*CoreDisabledConfigurationTest' \
  --tests '*CoreDashboardFlowTest' --no-daemon
```

Resultado: `BUILD SUCCESSFUL`. Comprueban contraseña configurada y hasheada, rechazo de contraseña insuficiente al crear admin, preservación de cuentas existentes, roles y permisos, Core deshabilitado y dashboard protegido. No se ejecutó la suite completa del proyecto.

### Evidencia

[Extracto de construcción, salud y tiempos](evidencias/g1-arranque.txt). No incluye contraseñas, certificados privados ni tokens. El procedimiento vigente para configurar `.env`, arrancar, ingresar y ejecutar el smoke está en el [README](../README.md).

## Seguridad y límites operativos

La inicialización local no usa el seed SQL con contraseñas fijas. Fuera de Compose, crear un admin nuevo requiere `BOOTSTRAP_ADMIN_PASSWORD` de al menos 12 caracteres; una cuenta existente no se resetea. Las conexiones del navegador local siguen siendo HTTP, con cookies sin Secure; los controles HTTPS/headers del despliegue se validan por separado.

El build informó vulnerabilidades en dependencias npm. No se modificaron dependencias para resolverlas como parte de G1; deben analizarse para el eje D. Tampoco se escaneó el historial de Git. No interpretar esta validación de arranque como aprobación de seguridad.
