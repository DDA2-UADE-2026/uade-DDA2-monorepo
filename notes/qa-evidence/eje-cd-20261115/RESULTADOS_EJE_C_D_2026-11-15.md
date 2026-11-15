# Resultados Ejes C y D — 15/11/2026

## Alcance y versión

- Repositorio local: `uade-DDA2-monorepo`.
- Rama de trabajo: `codex/sonar-org-key`.
- SHA probado: `3478eeccbb2f096ba681220f743a5dfb8806b310`.
- Los cambios de esta sesión están **locales, sin commit ni push**. Por eso no hay un run de GitHub Actions que certifique este SHA.
- No se usó Core remoto, staging, producción ni datos reales. Testcontainers creó PostgreSQL y RabbitMQ descartables.

## Resumen de ejecución

| Suite / verificación | Resultado observado |
|---|---|
| Backend `./gradlew build` | **606 aprobados, 0 fallidos, 0 errores, 5 omitidos**, 611 casos en 55 archivos XML. `BUILD SUCCESSFUL`. |
| Testcontainers `./gradlew testContainers` | **3 aprobados, 0 fallidos, 0 errores, 0 omitidos**; PostgreSQL 16 y RabbitMQ 3.13 reales y descartables. |
| Frontend Vitest | **119 aprobados, 0 fallidos, 6 omitidos**; 30 archivos aprobados y 2 omitidos. |
| Frontend lint | Aprobado, código de salida 0. |
| Frontend build | Aprobado. Avisos: `VITE_SERVER_URL` no definido y chunk mayor a 500 KB. |
| Frontend coverage actual | **65,00% líneas (3541/5448), 50,19% ramas (3193/6362)** en el conjunto frontend medido por LCOV. No es el porcentaje del backend ni de Sonar. |
| k6, evidencia anterior del entorno de desarrollo | 50 VUs durante 5 min; 14.141 solicitudes, 13.941 checks aprobados, 0 fallidos, 0% errores HTTP. p95: programas 126,77 ms; beneficios 125,79 ms; solicitudes 135,35 ms. No corresponde al release final. Estos valores se transcriben de la salida compartida previamente; el JSON original no está en este worktree y falta adjuntarlo al paquete documental. |

Los cinco tests backend omitidos comprueban migraciones PostgreSQL específicas. Se abortan de forma deliberada cuando no están configuradas sus propiedades `*.test.postgres-url`; la ejecución normal de `build` no configura esos URLs. No se quitaron ni se reportan como aprobados. Los casos PostgreSQL nuevos sí se ejecutaron dentro de Testcontainers.

## Eje C — API, integraciones y resiliencia

| Requisito | Estado | Resultado y alcance |
|---|---|---|
| Timeout de 5 segundos | **PARCIAL** | El timeout de lectura predeterminado del cliente Core pasó de 10 s a 5 s. Un test con servidor HTTP local lento confirma que se aplica el timeout configurado. El timeout de conexión y el de lectura son presupuestos distintos: no garantizan un límite total de 5 s para toda operación. |
| Fallback ante caída o demora del Core | **PARCIAL** | Se verificó tratamiento de error del cliente HTTP. No hay fallback transparente para todas las solicitudes HTTP entrantes; outbox es resiliencia para eventos salientes y no sustituye ese comportamiento. |
| No perder eventos cuando falla el broker | **PARCIAL** | Testcontainers con PostgreSQL verificó que el rollback elimina el evento encolado junto con la transacción y que un evento pendiente permanece recuperable tras marcar un intento fallido. No se apagó RabbitMQ durante una publicación real para medir recuperación end-to-end. |
| Consumidores idempotentes | **PASS en el caso automatizado** | Con PostgreSQL real, el primer evento se procesa y la segunda entrega con el mismo ID se reconoce como duplicada; queda una sola fila inbox. No cubre todas las clases de evento del sistema. |
| DLQ y recuperación física del broker | **PARCIAL** | Hay mecanismos/pruebas existentes de mensajería y se confirmó publicación/recepción contra RabbitMQ real. No se acreditó un ciclo completo de caída, redelivery y DLQ real con la topología/colas de un entorno autorizado. |
| Rollback y outbox | **PASS en el escenario probado** | Testcontainers comprobó rollback transaccional y retención en PostgreSQL. Evidencia: test `PostgreSQL revierte outbox junto con la transaccion y retiene el evento si el broker no confirma`. |
| `Idempotency-Key` | **PARCIAL** | Está aplicado en flujos concretos existentes, pero no se encontró cobertura uniforme de todos los endpoints de escritura ni una matriz de contratos que lo pruebe en todos ellos. |
| ETag / `If-Match` | **PARCIAL** | Se agregó ETag para consulta de detalle de programa administrativo y `If-Match` para actualizar/eliminar; una versión obsoleta devuelve 412. No está generalizado a todos los recursos mutables. |
| Errores Problem Details | **PASS** | Se añadieron campos compatibles con RFC 9457 y media type `application/problem+json` al manejo global y a errores de autenticación. Hay pruebas para los errores tocados; conviene conservar campos existentes del contrato mientras el frontend los consuma. |
| Carga: 50 usuarios, p95 <500 ms | **PASS histórico, no release final** | La ejecución k6 previa cumplió para tres listados durante cinco minutos. No se encontró el JSON original dentro de este worktree, así que falta incorporarlo como archivo de evidencia. No se volvió a correr contra el SHA de este informe. |

### Testcontainers agregado

Se añadieron dependencias con versión bloqueada, una tarea Gradle `testContainers` separada, tres pruebas de integración, ejecución explícita desde CI y carga de XML/HTML como artefactos por 14 días. Las pruebas usan servicios descartables y no apuntan a Core remoto. La suite completa backend también pasó. La configuración está en `server/build.gradle.kts`, `server/gradle.lockfile`, `server/src/test/kotlin/com/uade/dda2/server/feature/integration/CorePostgresRabbitTestcontainersTest.kt` y `.github/workflows/ci.yml`.

## Eje D — Seguridad

| Requisito | Estado | Resultado y alcance |
|---|---|---|
| Rate limiting / HTTP 429 | **PASS en pruebas locales de autenticación** | Se añadió límite por IP, por proceso, a POST de login/selección de rol: 10 intentos por ventana de 15 minutos; responde 429, `Retry-After` y Problem Details. `AuthFlowTest` verifica el rechazo. En producción con varias réplicas debe aplicarse un limitador compartido en gateway/Redis; memoria local no coordina instancias. |
| BOLA/IDOR y BFLA | **PARCIAL** | La suite incluye comprobaciones de roles y acceso en varios flujos y pasó completa. No existe evidencia de una matriz dinámica exhaustiva para cada operación/recurso (solicitudes, documentos, beneficios, visitas, turnos), cada rol y cada usuario. No se debe afirmar que la prueba en vivo completa pasó. |
| JWT inválidos/expirados | **PASS en la cobertura automatizada existente; alcance parcial en vivo** | Los tests de autenticación incluyen tokens inválidos y expirados para los flujos cubiertos. No se ejecutó un barrido en vivo de todos los endpoints protegidos. |
| Mass assignment e inyecciones | **PARCIAL** | La suite verifica validación/errores en varios flujos; no se ejecutó una batería sistemática de payloads en todos los endpoints contra una app desplegada. No se hicieron pruebas destructivas ni contra datos reales. |
| ZAP sin alertas High | **BLOQUEADO** | No se proporcionó URL ni autorización de staging. No se escaneó producción ni un servicio compartido. Para cerrar: URL de staging autorizada, cuenta de pruebas y ventana de ejecución. |
| Gitleaks | **PARCIAL** | Existe un reporte de escaneo del árbol de trabajo en `gitleaks-current-tree.json`: 1 hallazgo `generic-api-key` en `server/.env.example` (líneas 34–35), que corresponde al marcador `JWT_SECRET=` vacío; inspección previa confirmó que no había valor secreto. El escaneo no acredita historia Git. No pude repetirlo en el cierre: Docker devolvió `permission denied` a `dockerDesktopLinuxEngine`. |
| Dependencias / CVE | **PARCIAL** | Se aplicó `npm audit fix` sin `--force`, solo actualizaciones compatibles, y se ejecutaron tests, lint y build. La última auditoría que sí respondió luego de ese cambio informó 16 vulnerabilidades: 0 críticas, 4 altas, 10 moderadas y 2 bajas. Resolver las restantes requiere revisar actualizaciones mayores de dependencias de tooling. La repetición de `npm audit` durante el cierre no pudo contactar `registry.npmjs.org` (`ENOTFOUND`); el error íntegro está en `npm-audit-network-error.json`. |

## Cambios locales realizados

- Límite de intentos de autenticación con respuesta 429 y cabecera `Retry-After`.
- ETag/`If-Match` para detalle y mutaciones administrativas de programas.
- Formato Problem Details compatible en las respuestas de error.
- Timeout de lectura Core por defecto de 5 segundos y prueba de timeout.
- Integración real descartable PostgreSQL/RabbitMQ con Testcontainers.
- Workflow CI backend para ejecutar Testcontainers y conservar sus reportes.
- Actualización segura de dependencias frontend dentro de rangos compatibles.

## Evidencias en esta carpeta

- `backend-junit/`: XML de los 611 casos backend de la última ejecución `build`.
- `testcontainers-junit/`: XML de los tres casos PostgreSQL/RabbitMQ.
- `frontend-lcov.info`: LCOV generado por `npm run test:coverage`.
- `gitleaks-current-tree.json`: resultado previo redacted del árbol de trabajo, con el falso positivo descrito.
- `npm-audit-network-error.json`: error de red al intentar refrescar el audit en este cierre; no es un reporte de vulnerabilidades.

Los reportes de tests Testcontainers se regeneraron después de la suite general. Comandos ejecutados: `server/gradlew.bat build --offline --no-daemon --no-configuration-cache --max-workers=2 --console=plain` y `server/gradlew.bat testContainers` con las mismas opciones; frontend `npm run test:coverage`, `npm run lint` y `npm run build`.

## Para cerrar antes de declararlo completo

1. Ejecutar Gitleaks sobre el árbol actual y sobre historia Git con el runner habilitado; confirmar/excluir el marcador vacío de `.env.example` sin exponer valores.
2. Repetir `npm audit` cuando el registry esté disponible; revisar con el equipo las 4 altas, 10 moderadas y 2 bajas restantes, evitando upgrades mayores automáticos.
3. Ejecutar ZAP en staging autorizado y guardar el reporte de severidad.
4. Preparar y ejecutar matriz BOLA/BFLA, JWT, mass assignment e inyecciones contra staging de prueba, cubriendo cada recurso/rol y conservando request/response.
5. Probar caída/recuperación de RabbitMQ y DLQ con la topología autorizada y confirmar la política de reintentos/retención.
6. Acordar el alcance obligatorio de `Idempotency-Key`, ETag/`If-Match` y fallback; hoy la implementación no cubre todos los endpoints.
7. Commit/push de los cambios y repetir CI/E2E sobre el SHA publicado. El CI modificado publica reportes con retención configurada de 14 días.
8. Adjuntar el JSON original de k6 y repetir la carga si se van a presentar estos resultados como evidencia del release; capturar también JaCoCo/PIT del mismo SHA final.

## Dictamen

La validación local funcional de backend y los tres escenarios nuevos de Testcontainers pasaron. Se incorporaron los controles de 429, Problem Details, ETag/`If-Match` acotado y timeout. **Ejes C y D no quedan completamente cerrados**: siguen parciales o bloqueados la amplitud de fallback/Idempotency-Key/ETag, caída y DLQ end-to-end, matriz dinámica de seguridad, ZAP, escaneo final de dependencias/secretos y verificación CI del SHA publicado.

