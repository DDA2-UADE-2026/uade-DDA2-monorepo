# Actualización Spring Boot 4.0.7 → 4.0.9

**Fecha:** 16/11/2026. **Rama:** `feature/security-boot-4-0-9`. **Base:** `55b41c3cb37d5836302a845a207ac39201017ca4`, después del pull de `develop`.

**Estado:** implementación y build/tests locales aprobados; rama preparada para revisión mediante PR hacia `develop`. La solicitud del PR queda a cargo del usuario; CI y Sonar del PR deben validarse antes del merge. Bouncy Castle queda deliberadamente sin actualizar para una decisión posterior.

## 1. Cambios implementados

- [Build Gradle](<../../../server/build.gradle.kts#L1-L8>): únicamente el plugin Boot pasa de 4.0.7 a 4.0.9. Kotlin sigue en 2.2.21 y dependency-management en 1.1.7; Java sigue en 17.
- [Lock](<../../../server/gradle.lockfile>): regenerado por Gradle con `dependencies --update-locks` para familias administradas de Boot. No se eliminó ni editó manualmente, no se usó `--refresh-dependencies` y no se desbloquearon BC/Kotlin/OTel.
- Sin cambios de código de aplicación/tests ni del frontend. La documentación y ajustes locales previos de preparación se conservan en un commit separado del upgrade.

Al aplicar Boot junto con dependency-management, el plugin importa el BOM de su versión: [documentación oficial](https://docs.spring.io/spring-boot/4.0/gradle-plugin/managing-dependencies.html). Se utilizó [actualización selectiva del lock](https://docs.gradle.org/9.5.1/userguide/dependency_locking.html#selectively_updating_lock_state_entries), no una actualización global de rangos.

### Versiones efectivas principales

| Familia | Antes | Después |
|---|---|---|
| Módulos Boot | 4.0.7 | 4.0.9 |
| Spring Framework | 7.0.8 | 7.0.10 |
| Spring Security | 7.0.6 | 7.0.8 |
| Spring AMQP | 4.0.4 | 4.0.6 |
| Spring Data JPA / Commons | 4.0.6 | 4.0.8 |
| Jackson 2 / 3 | 2.21.4 / 3.1.4 | 2.21.7 / 3.1.7 |
| Tomcat embed | 11.0.22 | 11.0.27 |
| Netty | 4.2.15.Final | 4.2.19.Final |
| PostgreSQL JDBC | 42.7.11 | 42.7.14 |
| Hibernate ORM / Validator | 7.2.19.Final / 9.0.1.Final | 7.2.25.Final / 9.0.2.Final |
| Micrometer | 1.16.6 | 1.16.8 |
| Logback / SLF4J / Log4j | 1.5.34 / 2.0.18 / 2.25.4 | 1.5.38 / 2.0.20 / 2.25.5 |
| Lombok | 1.18.46 | 1.18.48 |
| JSpecify / webjars-locator-lite | 1.0.0 / 1.1.3 | 1.0.1 / 1.1.5 |

`jackson-annotations` permanece en 2.21, según los BOM. Las dos familias Jackson se conservan. El objetivo se contrastó con los [BOM publicados 4.0.9](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.9/spring-boot-dependencies-4.0.9.pom).

## 2. Interpretación del diff del lock

**138 coordenadas difieren:** 121 cambian versión en configuraciones existentes y 17 sólo completan presencia en configuraciones. Hay 19 asociaciones módulo–scope nuevas y ninguna eliminada:

- 16 corresponden a `testImplementationDependenciesMetadata`, incluyendo Testcontainers/Docker Java y annotations con **las mismas versiones**. Es completar cobertura del lock; no se atribuye necesariamente al upgrade Boot ni significa ejecución de containers.
- Tres incorporan Checker Qual 3.55.1 a runtime/productionRuntime/testRuntime. Es dependencia **fija de PostgreSQL 42.7.14**, no de Hibernate Validator ni una selección dinámica arbitraria. Swift conserva su Checker Qual 3.43.0.
- JSpecify 1.0.1 alcanza también el scope Swift como efecto de la gestión de versiones; Kotlin y OTel no cambian.

Se mantienen el nodo platform transitivo `spring-boot-dependencies:4.0.6` de Springwolf y Swagger 2.2.43 compile / 2.2.52 runtime. No equivalen a módulos Boot 4.0.6 ni son motivo para agregar un platform/force global. Springwolf, Springdoc, Swagger, JJWT, cliente RabbitMQ, BC, Kotlin y OTel conservan sus versiones.

[Delta completo del lock](<evidence/springboot-4.0.9-lock-delta.csv>).

## 3. Validación sobre la misma base actualizada

Se repitió baseline original Boot 4.0.7 sobre **55b41c3**, no se reutilizó el conteo del commit anterior de preparación. Ambas ejecuciones usaron archivos de `git archive` aislados, sin dotenv/configuración privada; en el candidato sólo difieren el build y el lock. Java Corretto 17.0.15, Gradle 9.5.1, macOS arm64, entorno allowlist, homes/temporales controlados y cache del audit reutilizada.

| Ejecución | Build | Tests aprobados | Omitidos | Fallos/errores | Tiempo de comando |
|---|---|---:|---:|---:|---:|
| Baseline Boot 4.0.7 / 55b41c3 | exit 0, offline | 612 | 5 | 0 | 138,395 s |
| Candidato Boot 4.0.9 / misma fuente | exit 0 | 612 | 5 | 0 | 130,717 s |

Ambas: **617 casos / 57 suites**. Los cinco skips son las asunciones PostgreSQL opt-in preexistentes. Se ejecutaron los tests OpenAPI/AsyncAPI del build. No se agregaron skips ni se cambiaron assertions para lograr verde. Los tiempos no son un benchmark: baseline usa cache/artifacts distintos y candidato descarga dependencias nuevas.

Verificaciones adicionales:

- Las **29 configuraciones bloqueadas**, incluyendo metadata, BC y Swift, resuelven sin dependencias irresueltas. Verificación estricta sin escritura del lock.
- BootJar: **131 librerías runtime**, clases de aplicación major **61 / Java 17**, sin BC, OTel/Swift ni Testcontainers. La librería adicional respecto al baseline previo es Checker Qual del driver PostgreSQL actualizado.
- Build y lock integrados en el checkout principal idénticos a los probados; frontend intacto.
- No se ejecutaron `testContainers`, Core remoto, suites PostgreSQL opt-in, bases compartidas, bootRun, smoke con Tomcat real, publicación o despliegue. No se afirma aislamiento JVM/red absoluto.

[Resultados, comandos, hashes, suites y cobertura de scopes](<evidence/springboot-4.0.9.json>).

## 4. Bouncy Castle: sin cambios en esta tanda

Las cuatro entradas y su configuración son idénticas a la base: bcpg/bcpkix **1.80**, bcprov/bcutil **1.80.2**, sólo `kotlinBouncyCastleConfiguration`. Se resolvieron con sus locks activos, preservando `[1.80,1.81)` donde corresponde. No se añadieron overrides ni librerías BC al runtime.

Su necesidad/prioridad de actualización se analizará después, como pidió el usuario; este cambio no pretende resolver sus avisos ni modifica excepciones Sonar.

## 5. Pendiente antes de integrar

1. Revisar los commits separados de upgrade y documentación de preparación.
2. Solicitar PR hacia `develop`, con CI Linux/Java 17 y Testcontainers, y SonarCloud sobre el commit del PR.
3. Confirmar versiones y cierre real de riesgos en Sonar; los **59 registros potencialmente cubiertos** del análisis inicial siguen siendo una proyección hasta reanalizar. No se modificó el snapshot histórico de 122 riesgos.
4. Verificar smoke/TLS/integración en entorno desechable o de desarrollo autorizado antes de desplegar.

Rollback propuesto: revertir la tanda completa (plugin y lock) y reconstruir; no cambiar bases de datos ni usar reset destructivo. Publicar la rama feature no dispara el workflow de imágenes; éste requiere CI exitoso de un push a develop/main. No se realizó merge ni despliegue como parte de esta tarea.
