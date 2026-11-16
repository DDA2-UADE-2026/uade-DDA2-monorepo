# Inventario y scopes revisados

Fecha de observación: **16/11/2026**. La clasificación se conserva por ID en [riesgos-122.csv](<evidence/riesgos-122.csv>); la agrupación de 41 versiones está en [dependencias-y-scopes.csv](<evidence/dependencias-y-scopes.csv>).

## 1. Inventario Sonar y contraste con el repositorio

- Proyecto: `DDA2-UADE-2026_uade-DDA2-monorepo`, rama `develop`.
- Sonar inventaría **1.005 dependencias**: 751 npm y 254 Maven.
- Selección OPEN/CONFIRM y VULNERABILITY: **122 registros**, 111 Maven y 11 npm.
- El snapshot recapturado coincide con el anterior en **ID, CVE, versión y estado** de los 122 hallazgos.
- No hay alcanzabilidad analizada para ninguno. `null` / “Not analyzed” **no es “unreachable”**.
- Sonar etiqueta **117 registros** como productivos; sólo cinco como no productivos. La revisión de configuraciones y uso distingue los ámbitos siguientes.

| Ámbito revisado | Registros | Evidencia / alcance |
|---|---:|---|
| Backend runtime | 73 | Presencia en `productionRuntimeClasspath` del lock; no acredita explotación. |
| Frontend runtime | 1 | jsondiffpatch usado mediante `diff()` en auditoría. |
| Tooling Gradle/Kotlin | 36 | 34 BC en `kotlinBouncyCastleConfiguration`; 2 OTel en `swiftExportClasspathResolvable`. |
| Backend sólo tests | 2 | Commons Codec y json-smart, sin `productionRuntimeClasspath`. |
| Tooling frontend/build/CI | 10 | ESLint/AJV, parser de generación OpenAPI, CLI y globs de herramientas. |
| **Total** | **122** | No excluir el riesgo de CI por no ser runtime. |

La diferencia de **43 registros** entre “productivos” de Sonar y la clasificación de runtime revisada es de ámbito: **36 del plugin Kotlin y 7 de tooling npm etiquetado runtime**. Codec/json-smart ya tienen bandera productiva falsa en Sonar, y no forman parte de esa diferencia. **No son 43 falsos positivos de CVE**, ni corresponde confundirlos con los 43 registros Medium.

Referencias del lock:

- [BC:91–94](<../../../server/gradle.lockfile#L91-L94>).
- [OTel:58–60](<../../../server/gradle.lockfile#L58-L60>).
- [Codec:30](<../../../server/gradle.lockfile#L30>).
- [json-smart:77–78](<../../../server/gradle.lockfile#L77-L78>).
- [JAXB y HdrHistogram:97–101](<../../../server/gradle.lockfile#L97-L101>).

## 2. Corrección de la interpretación, sin ocultar dependencias

La API SCA confirma que procesó:

```text
server/gradle-dependencies-q.txt
server/build.gradle.kts
client/package-lock.json
client/package.json
```

El reporte Gradle no está versionado ni se genera explícitamente en el workflow del proyecto. Esto no demuestra un archivo faltante en Git: la [documentación de Sonar SCA](https://docs.sonarsource.com/sonarqube-cloud/advanced-security/analyzing-projects-for-dependencies-sca.md) explica que Maven/Gradle resuelven dependencias y generan información durante el análisis. Por defecto se incluyen todas las configuraciones.

**Hecho comprobado:** el snapshot conserva scopes específicos: 73 registros en `mainSourceElements`, 34 en `kotlinBouncyCastleConfiguration`, 2 en `swiftExportClasspathResolvable` y 2 en `testCompileClasspath`; los 11 npm se reparten en 8 `runtime` y 3 `development`. **La discrepancia no es pérdida del nombre de configuración:** `productionScopeSummary=true` aparece para los 36 hallazgos del plugin, aunque sus scopes correctos son de tooling. Los dos de tests tienen `productionScopeSummary=false`. **Pendiente técnico:** determinar el criterio que genera esa bandera productiva y contrastarlo con el reporte del scanner; no atribuir una causa exacta al parser sin inspeccionarlo.

La corrección aplicada en esta fase es documental y de evidencia:

1. Conservar el scope Sonar original y su bandera `productivo_sonar`, y añadir scope revisado, configuraciones Gradle y línea del lock por riesgo.
2. Contrastar con árboles efectivos por configuración, sin reescribir locks.
3. No modificar `sonar.sca.enabled`, exclusions, estados o severidades para reducir el contador.
4. Ignorar por Git únicamente los directorios locales del audit/checkout/caches, para evitar commits accidentales y análisis duplicados de copias de manifiestos. No se excluyen las dependencias reales del proyecto.

### Próxima verificación del scanner

En una ejecución CI autorizada y sobre la revisión elegida:

- Conservar el reporte del scanner con `sonar.scanner.keepReport=true`; revisar `dependency-files.tar.xz` y el reporte Gradle.
- Revisar las líneas “Gather SCA dependencies” / “Running…” del log; usar debug sólo si hace falta y sanitizar antes de compartir.
- Comparar configuraciones y coordenadas con las seis capturas Gradle del baseline.
- Si el parser no distingue ámbitos, enviar a soporte un caso mínimo con esas evidencias. La clasificación manual debe seguir preservando tooling/tests.
- `sonar.sca.gradleConfigurationPattern` es una opción oficial, pero **no se aplicó un regex runtime-only**: omitiría riesgos del build y no demostraría una reparación. Si se decide separar inventarios, acordar cobertura completa antes.

Fuente: [resolución y parámetros SCA](https://docs.sonarsource.com/sonarqube-cloud/advanced-security/analyzing-projects-for-dependencies-sca.md), [diagnóstico y logs](https://docs.sonarsource.com/sonarqube-cloud/advanced-security/troubleshooting-the-dependency-analysis.md).

## 3. Backend: no basta cambiar la versión del plugin Boot

Punto de partida: Boot **4.0.7**, Kotlin **2.2.21**, Java **17**, dependency-management **1.1.7**, locking en todas las configuraciones.

Hay que preservar estas distinciones:

- Springwolf 2.5.0 introduce una plataforma Boot **4.0.6**, mientras módulos seleccionados son Boot 4.0.7. Que aparezca una plataforma anterior no implica por sí solo que todos los módulos efectivos estén en esa versión.
- Swagger core/model/annotations: **2.2.43 compile vs 2.2.52 runtime**. Es deuda de coherencia de classpath que debe quedar visible al actualizar.
- **Jackson 2 y Jackson 3 coexisten**. JJWT usa Gson, pero eso no elimina Jackson 2 introducido por Springwolf. No excluirlo ni sustituirlo por Jackson 3 sin migrar consumidores.
- Bouncy Castle pertenece a firma/publicación del plugin Kotlin; no añadirlo a `implementation` ni imponerlo globalmente.
- OTel del lock está en una configuración Swift del plugin; no afirmar que instrumenta endpoints HTTP productivos.

Para cada futura actualización se debe comparar `compileClasspath`, `runtimeClasspath`, `productionRuntimeClasspath`, `testRuntimeClasspath` y las configuraciones del plugin, además de los `dependencyInsight` de plataforma Boot/Jackson/Swagger. Un reporte `dependencies` con exit 0 **puede contener `FAILED`**; verificar ambos.

## 4. Frontend: lock, instalación y artefacto distribuido no son lo mismo

La instalación anterior del desarrollador no corresponde al lock actual. El baseline usa una instalación nueva desde lock, no el árbol existente ni una actualización de rangos.

Distinciones importantes:

- `dependencies` / `npm ls --omit=dev` describe clasificación npm, **no demuestra qué código entra al bundle del navegador**.
- shadcn incorpora CLI con dotenvx, fast-glob y ts-morph. Sin embargo [el CSS](<../../../client/src/index.css#L3>) importa `shadcn/tailwind.css`: no eliminar el paquete sin sustituir su uso.
- `@hey-api/client-fetch` incorpora al generador mediante peer; el parser de OpenAPI puede aparecer transitivamente como productivo aunque su función sea codegen.
- jsondiffpatch sí se importa en runtime, pero sólo `diff()` y texto React; no se observó el formatter HTML vulnerable. [Uso](<../../../client/src/components/audit/AuditDiffSheet.tsx#L133-L195>).
- El [contenedor frontend](<../../../client/Dockerfile#L1-L21>) distribuye `dist` en nginx, no un servidor Node con todo `node_modules`. El build y el pipeline siguen siendo superficies relevantes.

### Cadenas relevantes

```text
shadcn → @dotenvx/dotenvx → enquirer
shadcn → fast-glob → micromatch → braces / picomatch 2
shadcn → ts-morph → @ts-morph/common → minimatch 10
typescript-eslint → typescript-estree → minimatch 10
@hey-api/openapi-ts → @hey-api/json-schema-ref-parser → js-yaml 4.2.0
eslint → ajv 6 → uri-js 4.4.1
Vite / Vitest / plugins de router y build → picomatch 4
```

Un upgrade de shadcn no retira las otras raíces. El consumidor del YAML fija 4.2.0; refrescar el lock no alcanza. Cambiar picomatch 2 por 4.0.7 tampoco arregla el CVE observado. Ver decisiones individuales en [excepciones](<excepciones.md>) y el [inventario CSV](<evidence/riesgos-122.csv>).
