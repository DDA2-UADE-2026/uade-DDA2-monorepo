# Checklist para las próximas actualizaciones

Este documento define criterios; **no se ejecutaron upgrades durante la preparación**. El [baseline](<fase-preparacion.md>) fija resultados preexistentes y límites de comparación. Las versiones candidatas deben revalidarse el día de implementarlas.

## 1. Gate previo común

- [ ] Elegir explícitamente el commit base de la actualización. El checkout preparado `05333b1` no es el último analizado por Sonar `05bf960`.
- [ ] Si cambia el commit, recapturar hashes, tests y riesgos; no comparar suites diferentes como si fueran una regresión de dependencias.
- [ ] Repetir baseline en CI Node 24 / Java 17 sobre la revisión elegida. El baseline local macOS no reemplaza Linux/CI ni un deploy.
- [ ] Conservar versiones de runtime, lock, variables no secretas y resultados/exit codes.
- [ ] Trabajar en rama/PR independiente por tanda, sin `npm audit fix --force`, `--refresh-dependencies` o actualizaciones indiscriminadas.
- [ ] Revisar scripts de instalación; mantener `npm ci --ignore-scripts` según CI mientras el proyecto lo soporte.
- [ ] Comparar árboles completos y versiones seleccionadas, no sólo cambios del manifiesto o el número de avisos.
- [ ] Actualizar lock con el gestor correspondiente sólo cuando se implemente el upgrade, nunca editarlo a mano para forzar un resultado.
- [ ] Acordar qué fallos/omisiones son preexistentes; no convertir nuevos fallos en skips para pasar.

## 2. Tanda backend base: Boot 4.0.9

**Objetivo:** mantener Boot 4.0.x y usar su BOM coordinado. Los 59 registros potencialmente cubiertos incluyen un candidato a falso positivo ya parcheado; no prometer 59 vulnerabilidades explotables eliminadas.

| Familia | Objetivo publicado revisado |
|---|---|
| Spring Framework | 7.0.10 |
| Spring Security | 7.0.8 |
| Spring Data JPA | 4.0.8 vía Data BOM 2025.1.8 |
| Spring AMQP | 4.0.6 |
| Jackson 2 / 3 | 2.21.7 / 3.1.7 |
| Tomcat embed | 11.0.27, todos los embeds alineados |
| PostgreSQL JDBC | 42.7.14 |
| Netty | 4.2.19.Final, familia alineada |
| Micrometer | 1.16.8 |
| Log4j API / bridge | 2.25.5 |
| Logback classic / core | 1.5.38; **no cubre el aviso de classic que pide 1.6.5** |

Fuente del objetivo: [Boot BOM 4.0.9](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.9/spring-boot-dependencies-4.0.9.pom). Que una versión sea publicada y atienda un rango no prueba compatibilidad del proyecto.

- [ ] Revisar interacción Springwolf → plataforma Boot 4.0.6 y Swagger compile/runtime.
- [ ] Mantener Jackson 2 y 3 mientras sus consumidores los requieran; verificar core/databind/módulos y serialización de eventos.
- [ ] Comparar árboles compile/runtime/production/test y `dependencyInsight` de Jackson/Swagger/plataformas.
- [ ] `build`, tests de JWT/roles/cookies/CSRF/CORS y contratos OpenAPI/AsyncAPI.
- [ ] Uploads y Content-Disposition: filenames, controles CR/LF, descarga inline, límites y permisos.
- [ ] Smoke con Tomcat real, sin apuntar a Core o bases compartidas: MockMvc no cubre completamente parsing/headers del contenedor.
- [ ] Revisar BootJar: no agregar librerías de tests/plugin a `BOOT-INF/lib`.
- [ ] Validar persistencia/concurrencia con PostgreSQL desechable en tarea independiente.

## 3. Tanda frontend acotada

### jsondiffpatch 0.7.7

- [ ] Elevar mínimo declarado y actualizar lock con npm, sin otros upgrades no justificados.
- [ ] Tests del diff de auditoría: objetos, arrays, movimientos, eliminaciones, strings y entradas inválidas.
- [ ] Mantener render de texto; no introducir formatter HTML ni `dangerouslySetInnerHTML` como parte del parche.
- [ ] Lint, Vitest y typecheck/build; inspeccionar cambios de bundle.

### js-yaml 4.3.2

- [ ] Confirmar consumidor: `@hey-api/json-schema-ref-parser@1.4.4` fija exactamente 4.2.0.
- [ ] Preferir actualización compatible del padre; si no existe, override acotado con justificación y retiro futuro.
- [ ] No usar 4.3.0 como solución completa ni saltar a 5.x sólo por `latest`.
- [ ] Probar YAML merge keys y generación API **desde fixture confiable capturado**, no endpoint remoto ni credenciales de desarrollador.
- [ ] Revisar diff generado y compatibilidad del cliente antes de incorporarlo; no sobrescribir código generado en el checkout principal durante la preparación.

## 4. Tanda cliente Java RabbitMQ 5.37.0

- [ ] Confirmar versiones, compatibilidad y release notes del cliente. No confundirlo con versión del broker.
- [ ] Aplicar override controlado; Boot 4.0.9 todavía administra 5.27.1.
- [ ] Probar con PostgreSQL/RabbitMQ **exclusivos y desechables**: publisher confirms/returns, ACK/NACK, DLQ, reconexión, deduplicación y transacciones.
- [ ] TLS con truststore/hostname verification y certificados de prueba representativos; no aceptar todos los certificados para lograr verde.
- [ ] Límites de frame, mensajes grandes/malformados y comportamiento de recuperación.
- [ ] Confirmar que no se introduce parser JSON-RPC ni serialización Java para mensajes no confiables.
- [ ] No ejecutar `testCoreConnectivity` sin autorización y entorno propios; no usar Core real para una regresión rutinaria.

## 5. Tanda tooling: Kotlin / BC / OTel

- [ ] Confirmar uso de tareas de firma/publicación/Swift del plugin.
- [ ] Preferir versión compatible del plugin que arregle su grafo; no inventar una versión de plugin ni asumir que cualquier upgrade incorpora el parche.
- [ ] Si hay restricción manual, limitarla a configuración correspondiente y documentar motivo/retirada.
- [ ] BC: evaluar cuatro módulos alineados en 1.86; 1.84/1.85 son parciales para el conjunto observado.
- [ ] OTel: evaluar familia/BOM 1.67.0 del tooling, sin añadir SDK al runtime de la app.
- [ ] Verificar tareas usadas, integridad de firmas y publicación; no firmar/publicar artefactos reales durante la preparación.
- [ ] Comparar BootJar y production runtime: BC/OTel del plugin deben seguir fuera.

## 6. Cambios condicionados y sin parche

- [ ] Logback classic/core 1.6.5 alineados: verificar soporte de logging Boot, appenders, arranque, formato y rotación. No actualizar core y classic a líneas dispares.
- [ ] minimatch 11.x: cambio mayor/motor nuevo. Actualizar padres compatibles, no override global de consumidores 10.x; probar lint/router/CLI/globs y engines.
- [ ] Commons Codec 1.23.0: tests, con revisión de uso Blake3; cambiar librería no repara código de derivación erróneo.
- [ ] Los 11 sin parche: registrar mitigación y caducidad conforme a [excepciones](<excepciones.md>); no retirar métricas o bajar JAXB mayor indiscriminadamente.
- [ ] Los dos falsos positivos candidatos: revisar por ID y evidencia, no aceptar los demás CVEs del paquete en bloque.

## 7. Cierre y reversión

- [ ] Git diff sólo de archivos previstos; sin dotenv, credenciales, caches, build outputs o checkout de pruebas.
- [ ] Revisión de release notes y deprecaciones de todas las versiones realmente cambiadas.
- [ ] Test baseline igual o mejor, sin fallos nuevos ocultos ni skips añadidos para pasar.
- [ ] Reanalizar Sonar en el **commit de la actualización**; comparar IDs/CVEs/versiones y avisos nuevos, además del contador.
- [ ] Si cambia el scope, conservar cobertura de CI/tooling: desaparecer por exclusión no equivale a parchear.
- [ ] Confirmar configuración pública de build y despliegue en su entorno; un build con URLs omitidas no acredita deploy correcto.
- [ ] Plan de rollback: revertir el commit/PR completo de esa tanda (manifiesto, lock y ajustes relacionados), reconstruir y repetir smoke. No aplicar reset destructivo ni restaurar bases de datos como parte del rollback de una librería.

### Tareas deliberadamente fuera de la preparación

Testcontainers, suites PostgreSQL opt-in, Core remoto, migraciones, pruebas de carga/exploit, publicación de paquetes/imágenes, cambios de settings/estados Sonar y despliegue. Para `testCentersPostgres` extremar cuidado: por defecto usa una base local con `ddl-auto=update`, **no una base desechable segura por defecto**.
