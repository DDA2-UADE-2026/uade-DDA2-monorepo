# Actualización OpenTelemetry 1.55.0 → 1.67.0 — tooling SwiftExport

**Fecha:** 16/11/2026. **Rama:** `feature/security-opentelemetry-1-67-0`. **Base:** `cf2bda16ff71ded4bfcb7162da9b3cdab6385366` (`origin/develop`, con Spring Boot 4.0.9, Bouncy Castle 1.87, RabbitMQ Java client 5.37.0 y frontend `jsondiffpatch`/`js-yaml` mergeados).

Esta tanda cierra el último riesgo **Medium** del inventario inicial de SonarCloud (`CVE-2026-45292`, confirmado en SonarCloud) junto con el riesgo **Low** de la misma librería (`CVE-2026-118259`), elevando `io.opentelemetry` a `1.67.0` únicamente en el tooling `swiftExportClasspathResolvable`. No se ejecutó SonarCloud localmente ni se cambiaron estados en la plataforma.

## 1. Implementación acotada

- [Build Gradle](<../../../server/build.gradle.kts#L13-L16>): `extra["opentelemetry.version"] = "1.67.0"`, propiedad oficial de `spring-boot-dependencies:4.0.9` (que importa `io.opentelemetry:opentelemetry-bom:${opentelemetry.version}`).
- [Lock](<../../../server/gradle.lockfile#L58-L60>): regenerado selectivamente con `./gradlew dependencies --configuration swiftExportClasspathResolvable --update-locks 'io.opentelemetry:*'`. Cambian exactamente tres líneas (`opentelemetry-api`, `opentelemetry-common` y `opentelemetry-context` de `1.55.0` a `1.67.0`) en la única configuración `swiftExportClasspathResolvable`.
- No se agregan dependencias a `dependencies { ... }`, no se incorpora ningún SDK/propagador OpenTelemetry al runtime productivo ni se utiliza `force` global.

### Por qué estaba en 1.55.0

El plugin Kotlin Gradle `2.2.21` declara `org.jetbrains.kotlin:swift-export-embeddable:2.2.21` en `swiftExportClasspathResolvable`, que solicita `io.opentelemetry:opentelemetry-api:1.41.0`. El plugin `io.spring.dependency-management:1.1.7` importa `opentelemetry-bom` administrado por Spring Boot `4.0.9` (`1.55.0`), por lo que la propiedad `extra["opentelemetry.version"] = "1.67.0"` actualiza toda la familia dentro de esa configuración de tooling sin afectar `compileClasspath`, `productionRuntimeClasspath` ni `testRuntimeClasspath`.

**Deuda de mantenimiento explícita:** revisar o retirar este pin al actualizar Spring Boot o Kotlin cuando el BOM administrado alcance `>=1.67.0`.

## 2. Motivo de seguridad

| Registro Sonar | CVE | Severidad Sonar / CVSS | Descripción y alcance en el repositorio |
|---|---|---|---|
| `f7e719be-ea44-4a2e-b1ee-025fb16cfabb` | `CVE-2026-45292` | Medium / 5.3 (`CWE-770`) | Asignación ilimitada de memoria/CPU al parsear `baggage` en `opentelemetry-api` `< 1.62.0`. |
| `7c5f7a6a-387a-472b-971c-30a549a0bdfe` | `CVE-2026-118259` | Low / 5.3 (`CWE-1333`) | Backtracking cuadrático en `W3CTraceContextPropagator.extract()` `< 1.67.0`. |

En este backend JVM, `io.opentelemetry` sólo reside en `swiftExportClasspathResolvable` (worker Swift de KGP, no invocado en el proyecto y ausente del `BootJar`). Subir directamente a `1.67.0` (`Complete fix` indicado por SonarCloud) resuelve ambos CVEs por higiene del lock y tooling.

## 3. Validación y evidencia

Todas las verificaciones se ejecutaron en una copia limpia extraída por `git archive` del commit `cf2bda1` (Java Corretto `17.0.15`, Gradle `9.5.1`, macOS `arm64`):

- **Lock y árboles (`0` marcadores `FAILED`):**
  - `dependencyInsight --configuration swiftExportClasspathResolvable --dependency io.opentelemetry:opentelemetry-api --offline` confirma `1.41.0 -> 1.67.0`.
  - [Árbol SwiftExport](<evidence/opentelemetry-1.67.0-tree.txt>) muestra `opentelemetry-api`, `opentelemetry-context` y `opentelemetry-common` alineados en `1.67.0`.
  - `compileClasspath`, `productionRuntimeClasspath` y `testRuntimeClasspath` no contienen ninguna dependencia `opentelemetry`.
- **Build completo offline (`./gradlew build --offline`):** exit 0 en `161,192 s`, con **58 suites / 624 casos: 619 aprobados, 5 omitidos preexistentes y 0 fallos/errores** (incluye los 7 casos de `CoreAmqpConfigurationTest`).
- **Artefacto `BootJar` (`server-0.0.1-SNAPSHOT.jar`):** conserva **131 librerías** en `BOOT-INF/lib/`, **0** librerías OpenTelemetry, **0** librerías Bouncy Castle y 792 clases propias en bytecode Java 17 (major `61`). Su SHA256 (`061f1955e4d045026b8b18b15dc420586b3e2e89b09688327874fc54d646fd94`) es idéntico al de la tanda RabbitMQ previa.
- **Integridad:** los SHA256 de `server/build.gradle.kts` y `server/gradle.lockfile` en el checkout principal coinciden exactamente con los probados en el candidato.

[Resumen completo, comandos, hashes y evidencias](<evidence/opentelemetry-1.67.0.json>).

## 4. Límites y próximo paso

- No se ejecutaron Testcontainers, suites PostgreSQL opt-in, Core remoto ni reanálisis local de SonarCloud.
- Abrir PR hacia `develop` para validar CI Linux y confirmar en SonarCloud el cierre del último riesgo **Medium** (`CVE-2026-45292`) y su par **Low** (`CVE-2026-118259`).

**Rollback propuesto:** revertir conjuntamente la propiedad `extra["opentelemetry.version"]` en `server/build.gradle.kts` y las tres entradas de `io.opentelemetry` en `server/gradle.lockfile`, y reconstruir offline.
