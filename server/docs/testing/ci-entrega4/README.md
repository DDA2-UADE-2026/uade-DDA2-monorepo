# CI: repetibilidad — revisión del 12/11/2026

**Estado: preparado localmente, tres ejecuciones completas idénticas NO ACREDITADAS.**

## Ejecuciones revisadas

Se consultaron las 20 ejecuciones recientes disponibles en GitHub (8 eran CI) y se inspeccionaron los jobs de las tres CI más recientes. El JSON adjunto conserva SHA, URL, resultado y jobs.

- 41644659797, d7bfe59: backend aprobado, frontend omitido.
- 41644040233, 42a6c2d: frontend y backend aprobados.
- 41627866301, df48881: frontend y backend aprobados.

Son commits diferentes; no acreditan tres ejecuciones idénticas del código local actual. No se volvió a ejecutar un commit viejo para presentarlo como evidencia de cambios nuevos.

La revisión previa del TPO en notes/Plan_implementacion_y_validacion_CI_02nov.md identifica tres ejecuciones del mismo SHA y configuración. El PDF original mencionado anteriormente no estuvo disponible en su ruta de Downloads durante esta revisión; no se atribuye una nueva verificación textual al PDF.

## Cambios preparados

- CI admite workflow_dispatch: fuerza frontend y backend sin filtros por archivos.
- El tag v2.0-entrega4, cuando el equipo lo cree, también fuerza ambos componentes. No se creó ningún tag.
- Se conservan los filtros de cambios de push/PR; cambios en scripts/testing también activan ambos componentes.
- Timeout de 15 minutos por job. Esto no acredita aún el tiempo total del pipeline ni el arranque integral del TPO.
- Frontend ejecuta tests con 2 workers y emite JUnit. Backend conserva gradlew build.
- Artefactos por SHA/intento con resúmenes sanitizados de tests. No contienen stdout, respuestas ni textos de excepciones.
- El workflow Deploy existente exige evento push. Una ejecución manual de CI no satisface esa condición y no publica imágenes.

Validación local: Actionlint 1.7.12 sin errores para ci.yml (sin shellcheck/pyflakes), YAML parseado correctamente y exportador JUnit comprobado con fixture de PASS/FAIL/SKIPPED y texto sensible que no aparece en el resultado. Esto no equivale a una ejecución remota de CI.

## Obtener las tres evidencias

1. Publicar/revisar los cambios y disponer del workflow en GitHub. Fijar el commit de entrega a evaluar; no cambiar código, lockfile ni configuración entre intentos. La publicación/merge no se realizó en esta tarea.
2. Lanzar CI manualmente sobre la referencia elegida y verificar que GITHUB_SHA sea el esperado. Deben ejecutarse y aprobar ambos jobs; skipped/cancelled no cuentan como aprobación.
3. Si aprueba, usar Re-run all jobs dos veces, esperando que finalice cada intento. Así los tres intentos pertenecen al mismo run/SHA. Si hay fallos, conservarlos; no presentar solo las corridas favorables ni reejecutar únicamente el job fallido.
4. Guardar URL, SHA, números de intento, versiones/imagen del runner, duración, jobs y artefactos client-tests y server-tests. Comparar también cantidad y selección de tests y omisiones.
5. Registrar los tres intentos y su resultado real. Si cambió el código o el entorno/configuración, iniciar una nueva serie comparable.

## Límites

Este CI cubre lint/tests/build frontend y tests/build backend. No contiene todavía todos los controles del TPO (Sonar, ZAP, infraestructura real, E2E, etc.). Tres aprobaciones de este workflow no acreditan automáticamente esos controles. El análisis Sonar requiere proyecto y permisos pendientes.

No se hizo push, merge ni ejecución remota desde esta tarea. La evidencia revisada es histórica y está separada de los resultados locales.

Nota de interpretación de artefactos: el resumen conserva resultados JUnit. Un test marcado como fallo esperado puede ser reportado PASS por el runner; no equivale a una funcionalidad corregida. Las omisiones no se ejecutaron. La suite frontend conocida incluye un fallo esperado que debe identificarse junto al resumen.
