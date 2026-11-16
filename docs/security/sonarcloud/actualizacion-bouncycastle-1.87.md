# Actualización Bouncy Castle 1.87 — sólo tooling Kotlin

**Fecha:** 16/11/2026. **Rama:** `feature/security-bouncycastle-tooling`. **Base:** `3e708fbfd2806bb5272c9040351859936599ce97`, con Spring Boot 4.0.9 ya mergeado en `develop`.

El usuario confirmó que el PR de Spring pasó tests y que Sonar bajó de 122 a **63 riesgos**. Ese dato es el baseline comunicado por el usuario; esta tarea no ejecutó un nuevo análisis Sonar ni cambió estados de riesgos.

## Alcance implementado

| Módulo | Antes | Después | Única configuración |
|---|---|---|---|
| bcpg-jdk18on | 1.80 | 1.87 | kotlinBouncyCastleConfiguration |
| bcpkix-jdk18on | 1.80 | 1.87 | kotlinBouncyCastleConfiguration |
| bcprov-jdk18on | 1.80.2 | 1.87 | kotlinBouncyCastleConfiguration |
| bcutil-jdk18on | 1.80.2 | 1.87 | kotlinBouncyCastleConfiguration |

- [Build](<../../../server/build.gradle.kts>): regla `eachDependency` exclusivamente en esa configuración, con grupo exacto `org.bouncycastle` y lista de los cuatro nombres. `useVersion("1.87")` más `because` documenta el override.
- [Lock](<../../../server/gradle.lockfile>): regenerado por Gradle con `dependencies --configuration kotlinBouncyCastleConfiguration --update-locks 'org.bouncycastle:*'`. **Sólo cambian las cuatro versiones**, sin scopes añadidos/eliminados ni diferencias en otros módulos.
- No se añaden dependencias directas: se conservan las `defaultDependencies` del plugin. No hay `force` global ni cambios en Boot, Kotlin, runtime de la aplicación o frontend.

## Por qué hace falta una regla local y no sólo constraints

El primer ensayo con constraints normales y otro con `strictly` terminaban con exit 0, pero dejaban `bcpg`/`bcpkix` en 1.80 y sólo subían `bcprov`/`bcutil`. El gate semántico de versiones rechazó ese lock antes de integrarlo.

El plugin Spring dependency-management 1.1.7 captura las versiones explícitas de las defaults de KGP como gestión implícita y las vuelve a aplicar durante resolución. `dependencyInsight` demostró selección por regla incluso frente a `strictly 1.87`. No es una versión BC impuesta por el BOM Boot.

La regla local posterior corrige las solicitudes sólo para esos cuatro módulos en el classpath del worker Kotlin. Se descartaron las constraints ineficaces; no se cambió `overriddenByDependencies` globalmente. Fuentes: [collector Spring](https://github.com/spring-gradle-plugins/dependency-management-plugin/blob/v1.1.7/src/main/java/io/spring/gradle/dependencymanagement/internal/ImplicitDependencyManagementCollector.java), [orden de reglas Gradle](https://github.com/gradle/gradle/blob/v9.5.1/subprojects/core-api/src/main/java/org/gradle/api/artifacts/ResolutionStrategy.java).

**Deuda de mantenimiento explícita:** este pin también podría bajar una solicitud futura superior a 1.87. Revisarlo/retirarlo al actualizar Kotlin o cuando el plugin incorpore una versión segura compatible por defecto. No dejarlo indefinidamente sin revisión.

## Dónde se utiliza

KGP 2.2.21 registra `generatePgpKeys` y `checkSigningConfiguration` aunque el proyecto no publique bibliotecas. BC se entrega a workers con aislamiento de classloader. La configuración se describe como helpers internos de publicación, no compilación de la aplicación.

Este backend no configura publicación Maven/firma de bibliotecas; la CI construye/tests y publica imágenes Docker. JWT usa JJWT/HS256. No se agrega BC al BootJar ni al código de negocio. Fuente: [helpers oficiales KGP](https://github.com/JetBrains/kotlin/blob/v2.2.21/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/tasks/publishing/helpers.kt#L28-L143).

El snapshot inicial tenía 34 hallazgos BC / 32 CVEs distintos; uno es candidato a falso positivo de versión ya documentado. Se actualiza por higiene del tooling y corrección de biblioteca, no porque se haya demostrado explotación del backend. No se promete que el contador 63 baje a un valor específico hasta reanalizar.

## Validación

**PASS local:** `./gradlew build --offline --no-daemon --console=plain` terminó con exit 0 en **144,162 s**, con **617 casos / 57 suites: 612 aprobados, 5 omitidos preexistentes y 0 fallos/errores**. No se cambiaron tests ni se añadieron skips.

Los cinco árboles capturados (compile, production runtime, test runtime, BC y Swift) resuelven sin marcadores `FAILED`. El BootJar conserva exactamente las **131 librerías runtime** del upgrade Boot 4.0.9; no contiene BC y las clases de aplicación siguen en Java 17 / major 61. El build y lock del checkout principal son idénticos a los probados; sólo esos dos archivos difieren de los inputs archivados del candidato.

[Resultados, hashes, delta y compatibilidad PGP](<evidence/bouncycastle-1.87.json>) · [Árbol BC definitivo](<evidence/bouncycastle-1.87-tree.txt>).

No se consideran cubiertos por los tests habituales de aplicación los workers PGP que normalmente no se ejecutan; se probaron expresamente a continuación.

### Prueba PGP local específica

- Tarea **real** `generatePgpKeys` de KGP 2.2.21, usando el classpath resuelto de los cuatro JAR 1.87.
- Identidad y contraseña únicamente sintéticas; claves de prueba en checkout ignorado, sin exportar su contenido a documentación.
- Ejecución con `--offline`, comprobando que `uploadPublicPgpKey` no se registra; no se invoca `checkSigningConfiguration` ni ningún keyserver.
- Helper Java descartable fuera de los sources: lectura de formatos binario y ASCII, firma/verificación local, rechazo de mensaje alterado y contraseña incorrecta.
- Descarga pública de los cuatro JAR antes del modo offline; class origins/versiones y hashes comprobados para evitar usar accidentalmente BC del wrapper.
- **Resultado:** generación real, compilación/ejecución del helper y captura individual offline con exit 0. En **`.gpg` y `.asc`** pasan firma/verificación, rechazo de alteración y rechazo de contraseña incorrecta. Cinco archivos de prueba generados con permisos 0600; sin claves incluidas en los reportes. Los hashes de build/lock no cambiaron durante la prueba.
- Se observó una deprecación del constructor de firma usado por el helper descartable, pero no errores de API ni de linkage. No implica cobertura de todos los formatos/algoritmos de BC ni del envío a keyservers.

### Límites

No se publican claves ni firmas, no se firma un artefacto real ni se prueba publicación Maven. Tampoco se ejecutan Core remoto, bases compartidas, Testcontainers, despliegues o Sonar. El entorno de pruebas tiene HOME/cache/temporales y variables controladas, pero no equivale a una VM o sandbox de red absoluto.

## Estado y siguiente paso

Cambio local en feature, sin commit/push/PR/merge hechos por esta tarea. Después de revisar el diff, el PR hacia `develop` debe validar CI Linux y reanalizar Sonar. La evidencia histórica inicial de 122 riesgos se conserva sin reescribirla como si fuera el resultado nuevo.

Rollback: revertir juntos el bloque acotado del build y las cuatro entradas generadas del lock; reconstruir y repetir verificaciones. No cambiar datos ni claves reales.
