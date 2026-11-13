# Seguridad — revisión del 12/11/2026

**Estado general: PARCIAL. No acredita todavía todo el punto 7.**

Base Git: `652b247991ae7df5eeddfefecd90b1a47adad6e3`, árbol modificado con tests preexistentes. Resultados locales, no del commit limpio ni del despliegue. No se realizó push, merge ni tag.

| Control | Alcance/herramienta | Resultado | Estado | Evidencia |
|---|---|---|---|---|
| SCA frontend | npm 12.0.2, árbol completo | Antes 5 críticas/15 altas; después 0/0. Restan 13 moderadas y 2 bajas | PASS del umbral críticas/altas | dependencias-frontend.json y dependencias-frontend-final.json |
| Regresión frontend | Vitest 4.1.12, 2 workers; ESLint, TypeScript, Vite | 124 aprobadas, 1 fallo esperado, 6 omitidas. Tests/lint/build exit 0 | PASS del alcance ejecutado | resumen-ejecuciones.json |
| SCA backend | OSV API v1, runtimeClasspath Maven | Antes 11 paquetes con avisos; después 137 paquetes y cero avisos | PASS del inventario consultado | backend-paquetes-iniciales.json, backend-avisos-iniciales.json, backend-paquetes-finales.json, backend-osv-final.json |
| Secretos | Gitleaks 8.30.1, historial local con --all y redacción 100% | 243 commits, cero hallazgos | PASS del historial disponible | gitleaks-redactado.json y resumen-ejecuciones.json |
| Auth y permisos | MockMvc/H2; 5 suites antes de actualizar dependencias | 61 tests, 0 fallos/errores/omitidos | PASS del alcance ejecutado | auth-permissions-tests.json |
| Regresión backend actualizado | Gradle 9.5.1, Java 17, 66 suites | 872 casos: 867 aprobados, 5 omitidos, 0 fallos/errores; BUILD SUCCESSFUL en 5m 6s | PASS de los casos ejecutados | backend-tests-final.json |

OSV consulta coordenadas y versiones; no cubre sistema operativo, plugins Gradle ni dependencias exclusivas de test. Respuesta validada: 137 resultados, sin páginas pendientes. Gitleaks no acredita ramas remotas ausentes localmente ni secretos fuera de Git. Ninguno de estos análisis garantiza ausencia de vulnerabilidades desconocidas.

## Cambios

- Frontend: actualización compatible del lockfile y override acotado de `@hey-api/json-schema-ref-parser > js-yaml` a 4.3.2. Lectura de YAML OpenAPI sintético verificada. Sin downgrades ni `npm audit fix --force`.
- Backend: Spring Boot 4.0.7 → 4.0.9 y AMQP 5.37.1. BOM resuelve Tomcat 11.0.27, Spring Framework 7.0.10, PostgreSQL 42.7.14, Netty 4.2.19.Final y Jackson 2.21.7/3.1.7.
- Nuevo test HTTP de JWT con firma alterada, alg=none y emisor incorrecto: 401 tanto por Bearer como cookie. Código Bearer inválido: AUTH_INVALID_TOKEN.
- Logs y herramientas temporales `tmp/security-*` excluidos de Git. Publicar solamente evidencia sanitizada.

AMQP 5.36.1 no resolvió en Maven; se sustituyó por la versión publicada 5.37.1. Ese intento fallido no acredita compatibilidad. Las moderadas/bajas npm de ESLint, TypeScript ESLint, shadcn/ts-morph, minimatch y deepmerge siguen abiertas; no se consideran falsos positivos. Evitar entradas de generación no confiables mientras se evalúa su actualización.

## Alcance de permisos y autenticación

- CookieAuthFlowTest: CSRF, cookies HttpOnly/Secure/SameSite configuradas para test, credenciales inválidas, expiración, tokens alterados/sin firma/emisor incorrecto, limpieza de cookies al logout, selección de rol y CORS permitido/rechazado.
- JwtServiceTest y AuthFlowTest: validación de token, roles/permisos y usuario activo.
- AppointmentFlowTest: ciudadano consulta turno ajeno → 404; profesional ajeno consulta/modifica atención → 404; rol sin permisos administra/registra → 403; citizenId inesperado → 400. Se comprueba persistencia en el caso de atención.
- ActivityEnrollmentFlowTest: inscripción propia visible y ajena no expuesta; historial cerrado ajeno → 404, sin permisos → 403 y anónimo → 401.

No acredita automáticamente todos los endpoints. Limpiar cookies al logout no demuestra revocación del JWT copiado previamente.

## Controles no acreditados todavía

| Control | Estado y pendiente |
|---|---|
| ZAP baseline y activo autenticado | NO VERIFICABLE: falta ejecución en entorno aislado descartable; no se escaneó la API pública |
| HTTPS/TLS y headers/cookies desplegados | NO VERIFICABLE: los tests locales no prueban proxy/despliegue |
| Fuerza bruta 429 | NO VERIFICABLE: no se localizó limitador en la búsqueda dirigida; falta prueba dinámica y corrección si se confirma |
| Inyecciones y mass assignment general | PARCIAL: campos inesperados en turnos probados; falta matriz del resto de entradas |
| XXE/SOAP | NO VERIFICABLE: cerrar inventario de integraciones antes de declarar no aplicable |
| Revocación inmediata al logout | NO VERIFICABLE: solo limpieza de cookies acreditada |

## Reproducción

Desde server, con JAVA_HOME disponible:

```powershell
.\gradlew.bat test --rerun --no-configuration-cache --console=plain --max-workers=2
.\gradlew.bat -I qa-security.init.gradle qaSecurityDependencies --no-configuration-cache --console=plain
.\docs\testing\seguridad-entrega4\Scan-Dependencies.ps1 -Inventory .\build\reports\security\runtime-packages.json -Output .\build\reports\security\osv.json
```

Desde client: `npm ci`, `npm audit --json`, `npm run test:run -- --maxWorkers=2`, `npm run lint`, `npm run build`. npm audit puede devolver exit 1 por moderadas/bajas incluso con cero críticas/altas.

Desde la raíz, con Gitleaks instalado:

```powershell
gitleaks git . --redact=100 --report-format json --report-path gitleaks-redactado.json --log-opts="--all" --no-banner
```

## Demostración local y evidencia para Drive

Desde server:

```powershell
.\gradlew.bat test --rerun --tests '*CookieAuthFlowTest' --tests '*JwtServiceTest' --tests '*AuthFlowTest' --tests '*AppointmentFlowTest' --tests '*ActivityEnrollmentFlowTest' --no-configuration-cache --console=plain --max-workers=2
```

Abrir `server/build/reports/tests/test/index.html`. Capturar resumen y nombres de casos JWT, turnos ajenos y permisos. No abrir stdout ni solicitudes con tokens. Adjuntar los JSON sanitizados de esta carpeta con fecha, SHA base y aclaración de cambios locales. El panel de tests no sustituye los reportes SCA o ZAP.

Los 5 omitidos corresponden a pruebas de migración PostgreSQL; no se consideran acreditados por esta ejecución H2. El script qa-security.init.gradle se ejecutó correctamente y reprodujo el inventario. La validación completa del backend actualizado incluye nuevamente las suites de autenticación y permisos.
