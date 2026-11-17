# Actualización Frontend acotada: `jsondiffpatch` 0.7.7 y `js-yaml` 4.3.2

**Fecha:** 16/11/2026. **Rama:** `feature/security-frontend-patches`. **Base:** `87da65300fc062ca7af0f44a1067bfeb5a1f9e2b` (`feature/security-rabbitmq-client`, con Spring Boot 4.0.9 y Bouncy Castle 1.87 mergeados en `develop`).

Esta tanda aborda los **3 registros** del grupo frontend compatible en el inventario de SonarCloud: **1 Low** en `jsondiffpatch` y **2 Medium** en `js-yaml`, sin actualizar otras dependencias ni ejecutar `npm audit fix --force`. No se reejecutó SonarCloud localmente ni se cambiaron estados en la plataforma.

## 1. Cambios implementados

| Paquete | Antes | Después | Mecanismo | Ámbito real |
|---|---:|---:|---|---|
| `jsondiffpatch` | `0.7.6` | `0.7.7` | Dependencia directa `^0.7.7` en `package.json` | Runtime frontend (`AuditDiffSheet`) |
| `js-yaml` | `4.2.0` | `4.3.2` | `overrides` acotado a `@hey-api/json-schema-ref-parser` | Tooling codegen (`gen:api` / `@hey-api/openapi-ts`) |

- [Manifiesto](<../../../client/package.json>): sube `jsondiffpatch` a `^0.7.7` y agrega `overrides` acotado exclusivamente al consumidor `@hey-api/json-schema-ref-parser`.
- [Lockfile](<../../../client/package-lock.json>): regenerado con `npm install --package-lock-only --ignore-scripts --no-audit --no-fund` (npm `11.19.1`). El diff programático confirma **759 entradas**, **0 agregadas**, **0 eliminadas**, y cambios únicamente en la especificación raíz, `node_modules/jsondiffpatch` (`0.7.6 → 0.7.7`) y `node_modules/js-yaml` (`4.2.0 → 4.3.2`).
- [Tests nuevos](<../../../client/src/components/audit/AuditDiffSheet.test.tsx>): 5 tests de regresión para el visor de auditoría.

### Por qué se usa un `overrides` acotado para `js-yaml`

Se verificó en el registro npm que `@hey-api/openapi-ts@0.99.0` y `@hey-api/json-schema-ref-parser@1.4.4` siguen siendo las últimas versiones publicadas, y `@hey-api/json-schema-ref-parser@1.4.4` fija de forma exacta `"js-yaml": "4.2.0"` (`@hey-api/shared@0.5.0` sólo lo declara en `devDependencies`).

- Un simple refresh del lock no resuelve `4.2.0`.
- Saltar a `js-yaml` 5.x no corresponde por cambio de línea mayor frente a un consumidor 4.x.
- Se eligió `4.3.2` (y no `4.3.0`) porque `4.3.0` deja abierto `CVE-2026-84375`.
- `npm explain js-yaml --json` confirma `"version": "4.3.2"`, `"rawSpec": "4.2.0"` y `"overridden": true` únicamente bajo `@hey-api/json-schema-ref-parser@1.4.4`.

**Deuda de mantenimiento explícita:** revisar y retirar el override cuando `@hey-api/json-schema-ref-parser` actualice su dependencia directa de `js-yaml` a `>=4.3.2`.

## 2. Motivo de seguridad y alcance verificado

| Registro Sonar | CVE | Paquete | CVSS / Severidad Sonar | Contraste en el repositorio |
|---|---|---|---|---|
| `aff4ad97-90a6-45b8-ba73-cc4a4477e32a` | `CVE-2026-122234` | `jsondiffpatch` | 4.7 / Low (`CWE-79`) | [AuditDiffSheet](<../../../client/src/components/audit/AuditDiffSheet.tsx#L3-L197>) importa sólo `diff()` y renderiza texto plano en React (`<pre>`); no usa `formatters.html`, `formatters.annotated` ni `dangerouslySetInnerHTML`. Se parchea a `0.7.7` por higiene preventiva. |
| `9e320e0a-8d48-49be-a0be-16e2f9cc0176` | `CVE-2026-59869` | `js-yaml` | 7.5 / Medium (`CWE-407`) | Usado sólo en tiempo de generación OpenAPI (`npm run gen:api`) vía `@hey-api/json-schema-ref-parser`; no se incluye en el bundle `dist` servido por nginx. |
| `034948d0-933b-4f3b-aec0-b98ed38138bf` | `CVE-2026-84375` | `js-yaml` | 7.5 / Medium (`CWE-400; CWE-407`) | Mismo ámbito de generación OpenAPI; en `4.2.0` los merges con mappings vacíos evaden `maxTotalMergeKeys`, corregido en `4.3.2`. |

## 3. Validación y evidencia

Toda la validación se ejecutó en copias limpias extraídas por `git archive` del commit `87da653`, usando el runtime portable de CI **Node `v24.22.3` / npm `11.19.1`** (macOS `arm64`), con instalación limpia (`npm ci --ignore-scripts --no-audit --no-fund`) y `frontend-network-guard.cjs` activo durante `lint`, `test:run`, `build` y el probe de OpenAPI/YAML (**0 intentos de red bloqueados**).

| Ejecución (Node `v24.22.3`) | `lint` | `test:run` | `build` (`tsc -b && vite build`) | Tests |
|---|---:|---:|---:|---|
| Baseline (`87da653`, sin cambios) | exit 0 | exit 0 | exit 0 | **186** (`180` aprobados, `6` omitidos preexistentes, `0` fallos / `47` suites) |
| Candidato (`0.7.7` / `4.3.2`, mismas fuentes) | exit 0 | exit 0 | exit 0 | **186** (`180` aprobados, `6` omitidos preexistentes, `0` fallos / `47` suites) |
| Candidato + `AuditDiffSheet.test.tsx` | exit 0 | exit 0 | exit 0 | **191** (`185` aprobados, `6` omitidos preexistentes, `0` fallos / `48` suites) |

Los 6 casos omitidos preexistentes pertenecen a `src/lib/program-images.test.ts` (3) y `src/components/programs/ProgramImageUploadDialog.test.tsx` (3); no se añadieron skips nuevos.

### Cobertura agregada en `AuditDiffSheet.test.tsx` (5 tests)

1. Objetos simples y anidados con claves desordenadas: líneas sin cambios, agregadas, eliminadas y modificadas.
2. Arrays con movimientos, inserciones y eliminaciones mostrados como reemplazo estructural completo (`line-through` vs `bg-emerald-500/15`).
3. Objetos equivalentes con distinto orden de claves y valores vacíos/undefined (`"No hay diferencias entre los valores registrados."`).
4. Strings no JSON válidos (fallback de `parseJsonValues`) y strings largos sin errores.
5. Payloads HTML/XSS (`<script>`, `<img onerror>`, `<svg onload>`, `<b onclick>`) renderizados como texto literal dentro de `<pre>`, sin nodos HTML inyectados y verificando ausencia de `formatters/html`, `formatters/annotated` y `dangerouslySetInnerHTML`.

*(Nota técnica: `tsconfig.app.json` define `"types": ["vite/client"]` sin tipos Node, por lo que la verificación estática del fuente en el test usa el import `?raw` nativo de Vite en lugar de `node:fs`).*

### Prueba offline de `js-yaml 4.3.2` y `@hey-api/openapi-ts 0.99.0`

- `js-yaml@4.3.2` y `$RefParser` de `@hey-api/json-schema-ref-parser@1.4.4`: parsing correcto de anchors y merge keys (`<<: *base`), rechazo controlado de YAML inválido (`YAMLException` / `ParserError`), y resolución acotada ante patrones asociados a `CVE-2026-59869` (80 merges encadenados en **1,652 ms**) y `CVE-2026-84375` (120 objetivos × 50 aliases a mapping vacío en **2,522 ms**).
- `@hey-api/openapi-ts@0.99.0` con los cuatro plugins de [openapi-ts.config.ts](<../../../client/openapi-ts.config.ts>) ejecutado en un directorio temporal ignorado sobre una copia local de `server/build/openapi/openapi.json` (167 paths, 246 schemas), tanto en JSON como en YAML: generó los 6 archivos (`client.gen.ts`, `index.ts`, `sdk.gen.ts`, `types.gen.ts`, `zod.gen.ts`, `@tanstack/react-query.gen.ts`) con salida idéntica byte a byte y sin tocar `client/src/generated`.

### Comparación de bundle e integridad

- `dist/assets`: **306 archivos** tanto en baseline (`5.581.546` bytes) como en candidato (`5.581.683` bytes, `+137` bytes). El chunk de auditoría pasa de `31.138` a `31.242` bytes (`+104` bytes). `js-yaml` no aparece en ningún JS de `dist/assets`.
- `npm ls --all`, `npm ls --omit=dev --all` y `npm ls --include=dev --all`: exit 0 sin problemas (`problems: null`).
- Los SHA256 de `client/package.json`, `client/package-lock.json` y `client/src/components/audit/AuditDiffSheet.test.tsx` en el checkout principal son idénticos a los del candidato probado.

[Resumen completo, comandos, hashes y resultados](<evidence/frontend-jsondiffpatch-js-yaml.json>).

## 4. Límites y próximo paso

- Validación ejecutada localmente en macOS `arm64` con Node `v24.22.3`; la paridad exacta con Ubuntu en GitHub Actions y el reanálisis de SonarCloud deben validarse en el PR.
- No se modificó `client/node_modules` del checkout local ni `client/src/generated`.
- No se ejecutó `gen:api` contra un servidor remoto ni se tocaron dependencias fuera de esta tanda (`minimatch`, `braces`, `enquirer`, `picomatch`, `uri-js`).

**Rollback propuesto:** revertir conjuntamente los cambios de `client/package.json`, `client/package-lock.json` y (si corresponde) `client/src/components/audit/AuditDiffSheet.test.tsx`, reinstalar con `npm ci --ignore-scripts` y reejecutar `lint`, `test:run` y `build`.
