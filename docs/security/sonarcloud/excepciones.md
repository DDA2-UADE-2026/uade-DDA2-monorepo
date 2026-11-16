# Excepciones y decisiones pendientes

**Revisión: 16/11/2026.** Ningún estado, severidad o asignación fue modificado en SonarCloud. Las decisiones siguientes son propuestas para revisión humana, no aceptaciones ejecutadas. Propietario propuesto: mantenedores del área indicada; asignación efectiva pendiente.

## 1. Dos candidatos a falso positivo de versión

La evidencia upstream está conservada en [advisories-discrepancias.json](<evidence/advisories-discrepancias.json>), con fecha de consulta, rango, primera versión corregida y referencias. Los IDs exactos Sonar y links están en [riesgos-122.csv](<evidence/riesgos-122.csv>).

### FP-01 · Logback core · CVE-2026-10532

| Campo | Evidencia |
|---|---|
| Versión del proyecto | `ch.qos.logback:logback-core:1.5.34` |
| Hallazgo Sonar | [36187660-89af-4481-b912-97d0b847e4ff](https://sonarcloud.io/dependency-risks/36187660-89af-4481-b912-97d0b847e4ff/what?id=DDA2-UADE-2026_uade-DDA2-monorepo) |
| Rango upstream GitHub Reviewed | `<1.5.34` |
| Primera versión corregida upstream | **1.5.34**, que ya está bloqueada |
| Precondición adicional | Deserialización en SimpleSocketServer / SimpleSSLSocketServer; no basta la presencia de Logback. |
| Contradicción | Sonar informa 1.5.34 y sugiere 1.5.35; su descripción menciona versiones hasta 1.5.33. |
| Propuesta | Verificar artefacto/árbol efectivo y análisis reciente; adjuntar advisory + lock y tramitar corrección/no afectado por versión. |
| Estado de la preparación | Evidencia reunida; **no se cambió OPEN**. |

Fuente: [GHSA-jhq6-gfmj-v8fx](https://api.github.com/advisories/GHSA-jhq6-gfmj-v8fx). El mantenimiento general puede actualizar Logback core con el BOM, pero no es necesario exclusivamente por este CVE según upstream.

### FP-02 · Bouncy Castle provider · CVE-2026-0636

| Campo | Evidencia |
|---|---|
| Versión del proyecto | `org.bouncycastle:bcprov-jdk18on:1.80.2` |
| Hallazgo Sonar | [45344b76-2baf-427d-a1f4-e13053d5f1c2](https://sonarcloud.io/dependency-risks/45344b76-2baf-427d-a1f4-e13053d5f1c2/what?id=DDA2-UADE-2026_uade-DDA2-monorepo) |
| Ramas vulnerables upstream | `>=1.74,<1.80.2`; `>=1.81,<1.81.1`; `>=1.82,<1.84` |
| Versiones corregidas upstream | **1.80.2, 1.81.1 y 1.84** |
| Ámbito del proyecto | Sólo `kotlinBouncyCastleConfiguration`, no runtime productivo. |
| Contradicción | Sonar presenta >=1.84 como salida sin reflejar el backport 1.80.2 para la rama usada. |
| Propuesta | Verificar versión efectiva y presentar evidencia del backport; corregir/no afectado por versión tras revisión. |
| Estado de la preparación | Evidencia reunida; **no se cambió OPEN**. |

Fuente: [GHSA-c3fc-8qff-9hwx](https://api.github.com/advisories/GHSA-c3fc-8qff-9hwx). Esto **no exime los otros avisos de BC**; la familia conserva una tarea de saneamiento del build.

### Criterio para cerrar la revisión de ambos

- [ ] Confirmar coordenada, versión e integridad del artefacto realmente resuelto.
- [ ] Confirmar que advisory/rango no fue retirado o actualizado desde esta captura.
- [ ] Adjuntar ID Sonar, snapshot, árbol y advisory en el ticket/registro del equipo.
- [ ] Revisar el resultado de un nuevo análisis antes de decidir estado.
- [ ] No confundir “paquete no afectado por este rango” con “paquete libre de los demás CVEs”.

## 2. Once registros sin parche forward identificado

No se recomiendan downgrades mayores, paquetes de otra coordenada ni versiones ficticias para eliminar el aviso. La evidencia consultada no garantiza que no se publique un parche después.

| Dependencia | Registros | Ámbito / mitigación propuesta | Condición de revisión |
|---|---:|---|---|
| braces 3.0.3 | 1 | Tooling. Evitar patrones de llaves profundamente anidados/controlados por terceros; tiempo/memoria acotados en CI. | Parche upstream o cambio del consumidor. |
| enquirer 2.4.1 | 1 | Tooling CLI/dotenvx. No aceptar `question.name` de terceros ni rutas de prototipo; prompts constantes. | Parche upstream; nuevo consumidor o prompts configurables. |
| picomatch 2.3.2 y 4.0.7 | 2 | Tooling. Controlar globs/extglobs; 4.0.7 también está afectada. No override 2→4 como supuesto fix. | Parche de ambas ramas/consumidores; cambio de inputs de globs. |
| uri-js 4.4.1 | 3 | ESLint/AJV en desarrollo. Schemas/configuraciones confiables, PR sin secretos y aislamiento. No forzar AJV mayor. | Cambio soportado de ESLint/AJV o parche del parser. |
| json-smart 2.6.0 | 1 | Sólo tests por JsonPath. No usar validación strict como frontera productiva; fixtures confiables. | Parche upstream o aparición de uso runtime. |
| JAXB core 4.0.9 | 1 | Runtime Hibernate; sin XML externo propio observado. Desactivar entidades externas/DTD si se incorpora unmarshalling no confiable. | Nuevo endpoint/importación XML o parche 4.x para **jaxb-core**. |
| HdrHistogram 2.2.2 | 1 | Runtime de métricas; advisory disputado y requisito de acceso local. Evaluar aceptación documentada, no fallo HTTP demostrado. | Actualización/disputa del aviso o cambio de límites/frontera de métricas. |
| LatencyUtils 2.0.3 | 1 | Runtime de métricas; ataque local descrito. No retirar indiscriminadamente si afecta observabilidad. | Parche, cambio de Micrometer o exposición nueva de métricas. |

**Caducidad propuesta para cualquier aceptación:** reevaluar antes del próximo upgrade y, si sigue abierta, el **23/11/2026**. Es una fecha documental propuesta; no se creó una tarea programada ni se asignó automáticamente al equipo.

## 3. Precondiciones no observadas: no equivalen a falso positivo de versión

| Familia / CVEs | Contraste | Decisión propuesta |
|---|---|---|
| jsondiffpatch / 122234 | Se usa `diff()` + texto React, no formatter HTML/annotated. | Actualizar a 0.7.7 por bajo impacto; no declarar XSS demostrado. |
| minimatch / 118764 | Requiere `optimizationLevel >= 2`; llamadas revisadas de consumidores no lo habilitan. | No forzar major global; evaluar alcance y versión compatible del padre. |
| Spring MVC vistas/SSE y WebFlux/Jetty | Stack REST MVC/Tomcat; esas funcionalidades no se encontraron. | Upgrade del framework por BOM, sin estado SAFE masivo. |
| Jackson polimórfico/identidad/tipos especiales | No se observaron las combinaciones de anotaciones/tipos de varios avisos. | Mantener ambos BOM actualizados. La coerción string→número y parsing desde String son escenarios distintos que no dependen de default typing. |
| RabbitMQ JSON-RPC / URI / helper TLS | No se observó JSON-RPC ni configurador por URI; TLS propio usa SSLContext y hostname verification. | Priorizar los avisos de frames/broker; no borrar los demás sin revisión por ID. |
| Logback classic / 104721 | No se encontró SiftingAppender/MDC/FileAppender con ruta externa. | Evaluar diferir el salto a 1.6.x con evidencia; Boot 4.0.9 por sí solo no corrige ese rango. |

“Tooling”, “no observado”, “sin parche”, “disputado” y “ya parcheado según upstream” son razones distintas. Conservarlas por separado, junto con su supuesto, responsable y condición de caducidad. No inventar pruebas de no explotabilidad ni aceptar en bloque por severidad Low.

## 4. Límites de seguridad de la preparación

El checkout de pruebas no incorpora dotenv privados ni propiedades locales. No se usaron credenciales de Core/DB. El backend no se arrancó como servicio y no se ejecutaron suites PostgreSQL/Core remotas ni Testcontainers. Esto no valida la seguridad del despliegue ni su configuración TLS real.

No publicar logs de debug sin sanitizar: pueden contener rutas locales, datos sintéticos, URLs y, en otras ejecuciones, configuración privada. Las evidencias versionadas se limitan a metadatos, hashes, recuentos y fragmentos técnicos revisados.
