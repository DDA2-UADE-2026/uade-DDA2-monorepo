# Fase de preparación — ejecutada

**Fecha:** 16/11/2026, America/Buenos_Aires (UTC−03). **Objetivo:** fijar un baseline confiable antes de actualizar dependencias, contrastar scopes y preparar decisiones de revisión. **No se actualizó ninguna dependencia, lock o código de aplicación; no se cambió ningún riesgo o setting Sonar.**

## 1. Punto de partida y revisión analizada

| Dato | Valor observado |
|---|---|
| Rama local | `develop` |
| Commit local probado | `05333b1957c8a842c294e568c4da9a86d49e88d4` |
| Commit local, fecha | 16/11/2026 00:34:58 −03 |
| Análisis Sonar del commit local | `e9c77e24-d0fc-4d93-9e58-16233b57ab62`, 16/11/2026 00:39:25 −03 |
| Último análisis de código observado | `a7ffc2d7-60ca-466d-a2f7-1cf8b448f1d8`, 16/11/2026 01:22:36 −03 |
| Revisión de ese análisis | `05bf96030433a16e1c31802d7aaaeda0563f7a39` |
| SCA | COMPLETED, sin errores; actualizado 16/11/2026 01:24:56.409 −03 |
| Riesgos recapturados | 122; mismos IDs/CVEs/versiones/estados que el snapshot de la revisión inicial |

**Los commits no son iguales.** La [comparación GitHub](https://api.github.com/repos/DDA2-UADE-2026/uade-DDA2-monorepo/compare/05333b1957c8a842c294e568c4da9a86d49e88d4...05bf96030433a16e1c31802d7aaaeda0563f7a39) muestra tres commits posteriores y 23 archivos distintos. Cambian tests, dos URLs de documentación frontend, configuración de forwarded headers y exclusiones de coverage/tests de Sonar. **No cambian los seis archivos de dependencias comparados**, cuyos SHA256 se conservaron.

No se hizo `pull`, merge, checkout de otra rama ni modificación de la revisión local para “alinear” el resultado. Por eso:

- El inventario de versiones es comparable; las suites y ciertos comportamientos no son idénticos.
- Este baseline certifica los comandos ejecutados sobre **05333b1**, no el conjunto de tests de 05bf960 ni del próximo PR.
- Antes del upgrade hay que elegir revisión base y repetir baseline si cambia.

[Metadatos, lista de cambios y hashes](<evidence/baseline.json>).

## 2. Aislamiento y seguridad de la ejecución

Se extrajo un **`git archive HEAD`** a un directorio local de trabajo, validando rutas y rechazando enlaces en la extracción inicial. No se copió el working tree del desarrollador:

- Sin dotenv privados, `node_modules`, builds o propiedades locales previas.
- 368 archivos tracked frontend y 646 backend contrastados con el archive del commit. Transformación CRLF de wrapper Windows contemplada según atributos; no es cambio del código.
- Entorno de ejecución con **allowlist**, no herencia completa: sin variables Core/DB/Spring, credenciales, opciones JVM/Node o propiedades Gradle del desarrollador.
- HOME/cache npm y HOME/GRADLE_USER_HOME/TMPDIR backend en directorios de trabajo aislados. La cache Gradle existente se copió mediante clone-copy, no hardlinks; no se copiaron propiedades/init scripts privados.
- Java 17 explícito; Gradle recibe `-Duser.home` para su ejecución aislada. La JVM sigue siendo del host: **esto no es una VM/container ni un sandbox de red absoluto**.
- Frontend lint/tests/build con guardia externa de red: bloquea conexiones/listeners reales de Node, HTTP/HTTPS/TLS y fetch. No se modificaron fuentes ni fixtures para ello. Cero intentos de red observados en los checks Node 26.
- Backend: perfil docs/H2 de tests, MockMvc y fixtures HTTP loopback efímeros. No se arrancó el servidor de la aplicación. Las exclusiones por tags y el preflight evitan Core remoto/containers; se verificó H2 en los reportes. No se atribuye aislamiento de red absoluto al proceso JVM.

Se permitió red para **descargar dependencias bloqueadas** cuando hizo falta. Eso no implica acceder a Core/DB y **no es un upgrade**. El audit de vulnerabilidades de npm se deshabilitó en la instalación: el inventario de esta tarea es el snapshot Sonar, no un contador npm de otra base de avisos.

## 3. Resultados de ejecución

### Frontend: primera corrida reproducible desde lock

macOS arm64, Node **26.1.0**, npm **11.13.0**. Se usó inicialmente el runtime existente, dejando explícita la diferencia con CI Node 24.

| Comando | Exit | Tiempo | Resultado |
|---|---:|---:|---|
| `npm ci --ignore-scripts --no-audit --no-fund` | 0 | 46,407 s | Instalación nueva desde lock. |
| `npm run lint` | 0 | 13,238 s | Sin errores. |
| `npm run test:run` con reporteros default/JSON | 0 | 11,858 s | **119 passed, 6 skipped, 0 failed**, 125 casos / 32 archivos. |
| `npm run build` | 0 | 24,512 s | Typecheck y bundle completados. |
| `npm ls --all --json` | 0 | 0,777 s | Grafo instalado consistente. |
| `npm ls --omit=dev --all --json` | 0 | 0,470 s | Clasificación npm productiva; no confundir con bundle. |
| `npm ls --include=dev --all --json` | 0 | 0,450 s | Árbol completo, incluyendo desarrollo. |

Se capturaron además `npm explain` de tooling/transitivas, incluidos braces, enquirer, js-yaml y uri-js. Los seis skips ya están en los tests del commit: validación/rutas de imágenes de portada y upload POST/PUT; no se añadieron omisiones para lograr verde. Nombres exactos y comandos: [evidencia Node 26](<evidence/frontend-node26.json>).

### Frontend: paridad de major con CI

**Completada y aprobada:** Node **24.22.3** / npm **11.19.1**, macOS arm64. Se utilizó runtime portable oficial sólo dentro del directorio ignorado del audit, sin modificar nvm ni la instalación global. SHA256 verificado contra SHASUMS oficial por HTTPS, con 5.888 entradas/rutas/enlaces validados antes de extraer; no se afirma verificación de firma separada.

| Comando | Exit | Tiempo | Resultado |
|---|---:|---:|---|
| `npm ci --ignore-scripts --no-audit --no-fund` | 0 | 11,679 s | Instalación desde el mismo lock, sin upgrades. |
| `npm run lint` | 0 | 13,414 s | Sin errores. |
| `npm run test:run` con reporteros default/JSON | 0 | 15,446 s | **119 passed, 6 skipped preexistentes, 0 failed**. |
| `npm run build` | 0 | 25,424 s | Typecheck y bundle completados. |
| `npm ls` completo, producción y desarrollo | 0 cada uno | Capturado por comando | Grafo consistente; mismas versiones que en Node 26. |

Cero intentos de red durante lint/tests/build; manifiesto, lock y 368 fuentes tracked intactos. Cero dependencias obligatorias ausentes, versiones discrepantes o extras en el checkout aislado. Todas las restricciones engines comprobadas admiten 24.22.3. Se cubrió el **major de CI**, no la equivalencia de plataforma Ubuntu/Linux ni la revisión remota. [Resultado y verificación Node 24](<evidence/frontend-node24.json>).

### Backend

macOS arm64, **Amazon Corretto 17.0.15**, wrapper **Gradle 9.5.1**.

| Ejecución | Exit | Tiempo | Resultado |
|---|---:|---:|---|
| `./gradlew build --no-daemon --offline --console=plain` | 1 | 36,632 s | Falló antes de tests por cache incompleta: faltaba `testcontainers-rabbitmq:2.0.5` en test compile classpath. Main compile/assemble completados. |
| `./gradlew build --no-daemon --console=plain` | 0 | 149,724 s | Retry con descarga normal del lock: **build PASS**. |
| Seis reportes `dependencies --configuration … --offline` | 0 cada uno | Capturado por comando | Todos resueltos, sin marcadores `FAILED`. |
| Cinco `dependencyInsight --offline` | 0 cada uno | Capturado por comando | Confirman BOM/Jackson/Swagger seleccionados. |

Se pasaron el HOME/JDK aislados en ambos intentos. **El primer fallo fue de cache/entorno, no un test funcional fallido**, y no se ocultó. Excluir la ejecución Testcontainers no elimina sus clases del classpath de compilación; descargar su dependencia no inicia Docker.

JUnit XML verificado: **55 suites, 611 casos: 606 passed, 5 skipped, 0 failures, 0 errors**. Las cinco asunciones requieren PostgreSQL opt-in: BenefitGrantFlowTest (3), ApplicationResolutionFlowTest (1), InterventionPlanFlowTest (1). Las suites excluidas por tag Core/Testcontainers **no se contabilizan como esos cinco skips**.

Evidencia: [resumen, comandos, hashes y suites backend](<evidence/backend-baseline.json>), [log del build exitoso](<evidence/backend-build.txt>).

## 4. Árboles e inclusión real en el artefacto

Se capturaron configuraciones separadas, sin `--write-locks` ni `--refresh-dependencies`:

- [compileClasspath](<evidence/gradle/dependencies-compileClasspath.txt>).
- [runtimeClasspath](<evidence/gradle/dependencies-runtimeClasspath.txt>).
- [productionRuntimeClasspath](<evidence/gradle/dependencies-productionRuntimeClasspath.txt>).
- [testRuntimeClasspath](<evidence/gradle/dependencies-testRuntimeClasspath.txt>).
- [kotlinBouncyCastleConfiguration](<evidence/gradle/dependencies-kotlinBouncyCastleConfiguration.txt>).
- [swiftExportClasspathResolvable](<evidence/gradle/dependencies-swiftExportClasspathResolvable.txt>).

Inspección del BootJar: **130 librerías runtime**, clases de aplicación **major 61 / Java 17**, sin Bouncy Castle, SwiftExport o Testcontainers. La ausencia en el artefacto confirma la distinción de tooling del lock; no demuestra que esos paquetes sean seguros en CI.

Hallazgos efectivos:

- Springwolf introduce plataforma Boot 4.0.6, pero los módulos seleccionados siguen **Boot 4.0.7**.
- Jackson 2 **2.21.4** (annotations 2.21) y Jackson 3 **3.1.4** coexisten.
- Swagger **2.2.43 compile vs 2.2.52 runtime**, confirmado por `dependencyInsight`; los tests OpenAPI/AsyncAPI pasan en el baseline.
- BC pertenece a helpers internos de publicación; OTel está en el worker Swift del plugin, no en el servidor runtime.

Detalle y decisiones: [inventario y scopes](<inventario-y-scopes.md>).

## 5. Drift del entorno npm previo

El árbol instalado original **no representaba el lock**: 65 diferencias de versión por misma ruta, 106 rutas del lock ausentes y 97 rutas extra. Parte de diferencias de rutas es hoisting/duplicación; no equivale a 203 paquetes rotos.

Ejemplos: Vite instalado 8.2.1 frente a lock 8.3.8; ESLint 10.8.0 frente a 10.15.0; shadcn 4.18.0 frente a 4.23.0; undici 7.29.0 frente a 7.30.2.

Tras instalación aislada desde lock: **cero versiones distintas, cero extras y cero dependencias obligatorias ausentes**. Las 73 optional omitidas son 65 de otras OS/CPU y 8 transitivas WASM/WASI bajo bindings wasm32 omitidos. Eso es específico de macOS arm64, no prueba instalación Linux idéntica.

**No se corrigió el `node_modules` original**: se evitó tocar el entorno del desarrollador; las futuras verificaciones deben partir del lock limpio.

## 6. Warnings y fallos preexistentes que no deben confundirse con el upgrade

- Primer intento backend offline: cache incompleta; resuelto con descarga normal, lock intacto.
- Inicio del harness npm: configuración duplicada de `/dev/null` en npm 11; corregido sólo en el ejecutor del audit a configs vacías distintas antes de instalar. No era fallo de la app ni implicó cambios de código.
- Frontend build advierte `%VITE_SERVER_URL%` no definido deliberadamente y chunk >500 kB. El bundle compila pero **no acredita configuración de despliegue**; no se inyectaron URLs privadas.
- Advertencias no fatales de canvas JSDOM/keys React en tests y deprecaciones Jackson/Lombok experimental/CDS backend.
- Se mantienen los seis skips frontend y cinco backend; no eliminar de los informes ni asumir cobertura total.

## 7. Integridad y documentación de las excepciones

- SHA256 antes/después iguales en manifiestos/locks originales y copias. Backend incluye siete ficheros de build/wrapper; frontend manifiesto y lock.
- Los sources archivados permanecen idénticos tras ejecución, salvo outputs generados nuevos fuera del conjunto tracked; no se incorporaron al proyecto.
- Dos candidatos a falso positivo por versión documentados con fuente upstream: Logback core / CVE-2026-10532 y BC provider / CVE-2026-0636.
- Once registros sin parche forward identificado y sus mitigaciones/condiciones de reevaluación.
- La interpretación del inventario queda corregida en evidencia por riesgo, sin silenciar scans ni cambiar estados.

Ver [excepciones](<excepciones.md>) y [checklist posterior](<checklist-actualizaciones.md>).

## 8. Qué queda fuera y qué habilita este resultado

La preparación **no equivale a remediación**. No se ejecutaron Core remoto, bases PostgreSQL compartidas, Testcontainers, migraciones, servidor de aplicación, generación API remota, despliegues, firmas/publicaciones ni exploits. Sí existieron fixtures HTTP locales de tests y red de descarga de paquetes.

Antes de aprobar la primera actualización:

1. Elegir/alinear revisión base y repetir baseline si cambia.
2. Repetir en CI Linux sobre la revisión elegida; Node 24 ya se verificó localmente con los resultados anteriores.
3. Si se quiere corregir cómo Sonar clasifica scopes, capturar el reporte generado del scanner y contrastarlo; esta tarea no alteró su configuración para esconder tooling.
4. Aprobar cada tanda y sus pruebas según el checklist; no actualizar todo junto.

Los caches, runtime portable y logs completos se guardan en directorios locales ignorados del audit. La documentación versionable conserva datos curados y rutas normalizadas como `<workspace>` / `<JDK17>`; no incluye credenciales ni requiere copiar esos caches para leerla.
