# Cinco recorridos E2E integrados

Playwright abre Chromium contra Vite en `http://localhost:15173` y el backend real en `http://localhost:18080`. PostgreSQL es un contenedor exclusivo en `127.0.0.1:55440/uade_e2e`. No se simulan respuestas HTTP del sistema.

Los fixtures crean datos sintéticos por API. Las acciones que cada caso evalúa se realizan desde la interfaz y se vuelven a consultar para comprobar persistencia.

| ID | Acción principal de navegador | Comprobación |
|---|---|---|
| E2E-01 | Login ciudadano y navegación por rol | Portal accesible, API administrativa devuelve 403 y ruta de gestión redirige al portal |
| E2E-02 | Crear programa como ADMIN | Nombre y objetivo persisten después de recargar |
| E2E-03 | Elegir un programa y presentar solicitud | Número de solicitud, detalle y aparición en listado propio |
| E2E-04 | Subir PNG y validarlo en otra sesión ADMIN | Estado pendiente, revisión y estado válido visible para el ciudadano |
| E2E-05 | Elegir servicio, centro, fecha y horario; reservar | Detalle y listado de turnos conservan la reserva |

## Ejecución local

Desde la raíz del repositorio:

```powershell
docker compose -f compose.e2e.yml up -d --wait
cd server
.\gradlew.bat bootJar --no-configuration-cache
cd ..\client
npm ci
npx playwright install chromium
npm run test:e2e
npm run test:e2e:report
```

Si Java no está en PATH, definir `E2E_JAVA` con la ruta a `java.exe` antes de ejecutar las pruebas. No es necesario iniciar manualmente el backend ni Vite: Playwright los inicia y cierra. Se rechaza reutilizar servidores existentes en los puertos de E2E.

Al finalizar, desde la raíz:

```powershell
docker compose -f compose.e2e.yml down -v
```

El script del backend fuerza la base descartable y `create-drop`; nunca debe reutilizarse como comando de arranque del entorno compartido. Las contraseñas incluidas corresponden únicamente a estas cuentas sintéticas locales. El Core remoto está deshabilitado en estos ensayos.

## CI y evidencia

`.github/workflows/e2e.yml` prepara PostgreSQL, construye el backend, instala Chromium y ejecuta los cinco casos con un worker y sin reintentos automáticos. Conserva el reporte HTML, JUnit, capturas finales y, ante fallos, video y trace. El artefacto incluye el SHA y número de intento.

Los resultados remotos solo quedan acreditados después de que el workflow publicado termine. Una ejecución local aprobada no acredita por sí sola la ejecución en GitHub Actions.

Documentación utilizada: https://playwright.dev/docs/ci
