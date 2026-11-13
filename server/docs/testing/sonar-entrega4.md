# Sonar — Entrega 4 (preparación del 12/11/2026)

Estado: NO VERIFICABLE para Sonar remoto. La aplicación GitHub aparece instalada para el repositorio, pero falta importar/crear el proyecto y confirmar la organización, project key, URL y permisos. No hay análisis remoto ni Quality Gate acreditados.

## Preparación local

- Backend: `server/qa-coverage.init.gradle`, reutilizado del script de cobertura existente. JaCoCo 0.8.14 mide todas las clases de main, sin exclusiones de lógica. Produce `server/build/reports/qa-coverage/coverage.xml` y HTML.
- Frontend: `npm run test:coverage`, Vitest con proveedor V8 4.1.12. Produce `client/coverage/lcov.info`, HTML y coverage-summary.json. Incluye todos los TS/TSX de src, incluso los no ejecutados. Excluye solamente pruebas, declaraciones de tipos, soporte de pruebas, cliente API generado y árbol de rutas generado.
- Estas rutas aún NO se han importado en Sonar. No se configuraron claves inventadas ni se incluyeron tokens.
- No se publicó ningún commit ni se creó el tag de entrega.

## Requisitos pendientes de medición/auditoría

| Criterio | Exigido | Resultado Sonar | Estado |
|---|---|---|---|
| Bugs Blocker/Critical | 0 | Sin análisis | NO VERIFICABLE |
| Vulnerabilidades | 0 | Sin análisis | NO VERIFICABLE |
| Duplicación Overall Code | <=3% | Sin análisis | NO VERIFICABLE |
| Reliability y Security | A | Sin análisis | NO VERIFICABLE |
| Hotspots revisados | 100% | Sin análisis/revisión | NO VERIFICABLE |
| Cobertura backend/frontend | >=85% cada uno | Importación pendiente | NO VERIFICABLE |
| Ramas dominio backend | >=70% | Definir y documentar alcance de dominio | NO VERIFICABLE |
| Quality Gate contra todos los criterios del TPO | Aprobado | Sin gate verificado | NO VERIFICABLE |
| CI del tag v2.0-entrega4 en main | Verde | Sin ejecución acreditada | NO VERIFICABLE |

El verde del gate predeterminado no basta para acreditar el TPO. Separar Overall Code de New Code y cobertura de líneas de ramas; auditar backend y frontend por separado.

## Siguiente paso de la cuenta

Importar DDA2-UADE-2026 desde GitHub en SonarQube Cloud y luego uade-DDA2-monorepo. Si la organización ya fue importada por otro miembro, solicitar incorporación y permisos de proyecto allí. Compartir solo el enlace del proyecto. El token, cuando se requiera, se guarda como secreto SONAR_TOKEN en GitHub; nunca en chat ni código.

La cobertura requiere análisis basado en CI, con generación previa de XML/LCOV. No duplicar análisis automático y análisis desde CI.

Referencias oficiales:
- https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/test-coverage/javascript-typescript-test-coverage
- https://www.sonarsource.com/blog/how-sonarqube-coverage-reporting-works/
- https://vitest.dev/config/coverage.html

## Mediciones locales obtenidas (no importadas en Sonar)

- Backend JaCoCo: 89,79% de líneas y 70,95% de ramas globales, sin excluir lógica de main. Medido antes de agregar los dos casos nuevos de CorePersistenceTest. XML y HTML en build/reports/qa-coverage. Estos porcentajes no equivalen automáticamente a la métrica combinada Coverage de Sonar ni al alcance del dominio. Desglose de paquetes feature/validator y contadores: sonar-cobertura-local.json.
- Frontend V8: 30,03% de líneas, 27,55% de sentencias, 22,84% de ramas y 25,97% de funciones. **NO CUMPLE el 85%**. Datos: sonar-cobertura-frontend.json; LCOV y HTML en client/coverage.
- Tests con cobertura frontend: 124 aprobados, 1 fallo esperado, 6 omitidos. La primera ejecución simultánea con Gradle tuvo 3 timeouts a 5 s; la ejecución aislada con límite de 15 s para instrumentación pasó. El comando normal de tests conserva sus aserciones y timeout; no se acredita estabilidad de tres corridas a partir de este reintento.
- El comando test:coverage registra ese timeout de 15 s para reproducibilidad. El proveedor 4.1.12 coincide con Vitest instalado.
- No hay proyecto/URL ni análisis remoto, importación XML/LCOV, Quality Gate o CI de Sonar verificados. La preparación de reportes no acredita el punto 6 completo.
