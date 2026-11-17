# SonarCloud: preparación del saneamiento de dependencias

Revisión inicial fechada el **16 de noviembre de 2026**. La fase de preparación no actualizó dependencias ni estados Sonar. La [actualización Spring Boot 4.0.9](<actualizacion-springboot-4.0.9.md>) ya está mergeada en `develop`; el usuario confirmó tests aprobados y **63 riesgos** en Sonar. [Bouncy Castle 1.87 sólo para tooling Kotlin](<actualizacion-bouncycastle-1.87.md>) también está mergeado; el usuario confirmó **29 riesgos** restantes. La tanda de [RabbitMQ Java client 5.37.0](<actualizacion-rabbitmq-client-5.37.0.md>) se encuentra en PR; sobre esa rama se preparó la tanda acotada de [frontend (`jsondiffpatch` 0.7.7 y `js-yaml` 4.3.2)](<actualizacion-frontend-jsondiffpatch-js-yaml.md>). Los resultados nuevos deberán confirmarse con CI y Sonar del próximo PR.

## Lectura recomendada

1. [Fase de preparación: ejecución y resultados](<fase-preparacion.md>).
2. [Inventario, árboles y scopes reales](<inventario-y-scopes.md>).
3. [Excepciones, falsos positivos candidatos y mitigaciones](<excepciones.md>).
4. [Checklist para las actualizaciones posteriores](<checklist-actualizaciones.md>).
5. [Spring Boot 4.0.9: cambios, pruebas y estado de la feature](<actualizacion-springboot-4.0.9.md>).
6. [Bouncy Castle 1.87: override acotado y pruebas PGP locales](<actualizacion-bouncycastle-1.87.md>).
7. [RabbitMQ Java client 5.37.0: actualización y regresiones; integración pendiente en dev](<actualizacion-rabbitmq-client-5.37.0.md>).
8. [Frontend: `jsondiffpatch` 0.7.7 y `js-yaml` 4.3.2 con override acotado y pruebas locales](<actualizacion-frontend-jsondiffpatch-js-yaml.md>).

## Evidencia versionable

- [Los 122 hallazgos, uno por uno](<evidence/riesgos-122.csv>): IDs Sonar, CVE, versión, ámbito revisado, precondiciones, recomendación y fuentes.
- [Dependencias afectadas agrupadas por versión y scope](<evidence/dependencias-y-scopes.csv>).
- [Revisiones, hashes y snapshot de metadatos Sonar](<evidence/baseline.json>).
- [Avisos upstream que contradicen dos clasificaciones](<evidence/advisories-discrepancias.json>).

Los árboles, logs y resultados resumidos de ejecución se enlazan desde la [fase de preparación](<fase-preparacion.md>). Los archivos originales de trabajo, caches y el checkout desechable permanecen fuera de control de versiones; no son requisitos para leer esta documentación.

## Resumen del análisis inicial (snapshot anterior al upgrade)

**122 registros / 109 CVEs distintos / 40 paquetes / 41 combinaciones paquete-versión**. Severidad Sonar: **43 Medium, 78 Low y 1 Info**. No confundir severidad Sonar con CVSS ni presencia de una librería con explotación demostrada.

| Ruta de trabajo posterior | Registros | Decisión |
|---|---:|---|
| Boot 4.0.7 → 4.0.9 y familias administradas | 59 | Primera actualización candidata, con lock y pruebas; incluye un falso positivo candidato ya parcheado según upstream. |
| Bouncy Castle del plugin Kotlin | 34 | Tooling de firma/publicación; tratar por separado, sin `force` global ni añadirlo al runtime. |
| RabbitMQ Java client 5.27.1 → 5.37.0 | 10 | Cambio separado con integración; Boot 4.0.9 conserva el cliente anterior. |
| jsondiffpatch 0.7.7 y js-yaml 4.3.2 | 3 | Parches acotados; js-yaml requiere resolver al consumidor que fija 4.2.0. |
| Logback classic, minimatch, OTel y Codec | 5 | Cambios condicionados a compatibilidad, ámbito y precondiciones. |
| Sin parche forward identificado en la revisión | 11 | Mitigación, seguimiento o aceptación explícita con evidencia; no downgrade automático. |

Estos grupos no se solapan. Son **rutas propuestas, no actualizaciones ejecutadas ni promesas de cerrar 122 avisos**. Verificar publicación, compatibilidad y avisos nuevos de nuevo al implementarlas.

## Advertencia sobre la revisión

El checkout probado en la preparación inicial fue **05333b1**; el análisis Sonar observado entonces correspondía a **05bf960**, tres commits posteriores. Los seis archivos de dependencias comparados eran iguales, pero los tests y ciertas configuraciones no. Ver detalles y fechas históricas en la [fase de preparación](<fase-preparacion.md>).

Después del pull, la actualización Boot se validó repitiendo baseline y candidato sobre **55b41c3**; sus resultados se registran aparte en la [actualización Spring Boot 4.0.9](<actualizacion-springboot-4.0.9.md>). Ninguna validación local reemplaza CI ni el reanálisis Sonar del nuevo commit.
