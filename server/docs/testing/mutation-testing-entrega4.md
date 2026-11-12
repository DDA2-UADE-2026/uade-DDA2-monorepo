# Mutación del dominio — Entrega 4

Estado: **CUMPLE el umbral de mutación de 60% para el alcance definido**, con ejecución local verificable. No equivale a aprobación de toda la Entrega 4 ni a un análisis de todo el backend.

## Versión y entorno

Ejecución: 11/11/2026. Evidencia preparada: 12/11/2026. Rama `feature/backend-branch-coverage`; commit base `3a8eeaa89009467d509ab35b1b2a577bf7a45936`. La configuración de mutación y los tests fortalecidos son cambios locales posteriores a ese commit: no se atribuye el resultado al commit limpio ni al tag de entrega. `evidence-context.json` conserva hashes SHA-256 de las fuentes y tests del alcance.

Kotlin 2.2.21; Java de compilación y PIT Temurin 17.0.16+8; launcher local Gradle OpenJDK 22.0.2; Gradle 9.5.1; Spring Boot 4.0.7. PIT 1.22.1; plugin Gradle 1.19.0; adaptador pitest-junit5-plugin 1.2.3. El motor efectivo es JUnit Platform/Jupiter **6.0.3**, pese al nombre del adaptador.

La primera configuración falló por launcher JUnit 1.10.1 incompatible con el engine 6.0.3. Se alineó a 6.0.3 solo al activar el init script. La compatibilidad se comprobó ejecutando tests y mutación; no se alteró el build convencional ni dependencias de producción.

## Dominio definido antes de medir

`targetClasses = com.uade.dda2.server.feature.*.validator.*Validator`

`targetTests = com.uade.dda2.server.feature.*.validator.*Test`

El proyecto organiza reglas por funcionalidades, no en un único paquete llamado `domain`. Se analiza la capa de validación de negocio completa: 27 clases de reglas, estados, permisos, fechas, cupos y archivos. No se recortó este alcance después del baseline. Ambas corridas contienen los mismos 701 mutantes.

Quedan fuera de esta medición servicios de orquestación, controladores, repositorios, entidades/DTO, mappers, configuración técnica y frontend. Es una medición de este dominio de validación, no de todas las capas del monorepo. No hay exclusiones adicionales de clases ni métodos dentro del patrón. Se usa `DEFAULTS`, sin desactivar mutadores, con los filtros estándar de PIT (incluido el de bytecode Kotlin generado). No se instaló Arcmutate comercial; sus sugerencias se conservan en el log y no se presentan como errores.

Clases incluidas:

- `com.uade.dda2.server.feature.activity.validator.ActivityValidator`
- `com.uade.dda2.server.feature.application.validator.ApplicationDocumentValidator`
- `com.uade.dda2.server.feature.application.validator.ApplicationEvaluationValidator`
- `com.uade.dda2.server.feature.application.validator.ApplicationResolutionValidator`
- `com.uade.dda2.server.feature.application.validator.ApplicationValidator`
- `com.uade.dda2.server.feature.application.validator.ApplicationVisitRescheduleRequestValidator`
- `com.uade.dda2.server.feature.application.validator.ApplicationVisitValidator`
- `com.uade.dda2.server.feature.appointment.validator.AppointmentValidator`
- `com.uade.dda2.server.feature.appointment.validator.AttentionValidator`
- `com.uade.dda2.server.feature.auth.validator.AuthValidator`
- `com.uade.dda2.server.feature.benefit.validator.BenefitGrantLifecycleValidator`
- `com.uade.dda2.server.feature.benefit.validator.BenefitGrantValidator`
- `com.uade.dda2.server.feature.center.validator.CenterLifecycleValidator`
- `com.uade.dda2.server.feature.center.validator.CenterOpeningHourValidator`
- `com.uade.dda2.server.feature.center.validator.EffectiveScheduleValidator`
- `com.uade.dda2.server.feature.center.validator.MunicipalCenterValidator`
- `com.uade.dda2.server.feature.center.validator.MunicipalServiceValidator`
- `com.uade.dda2.server.feature.center.validator.ProfessionalAssignmentValidator`
- `com.uade.dda2.server.feature.center.validator.ProfessionalAvailabilityValidator`
- `com.uade.dda2.server.feature.enrollmentperiod.validator.EnrollmentPeriodValidator`
- `com.uade.dda2.server.feature.intervention.validator.InterventionValidator`
- `com.uade.dda2.server.feature.program.validator.AdminProgramBenefitValidator`
- `com.uade.dda2.server.feature.program.validator.AdminProgramEditionValidator`
- `com.uade.dda2.server.feature.program.validator.AdminProgramIncompatibilityValidator`
- `com.uade.dda2.server.feature.program.validator.AdminProgramRequirementValidator`
- `com.uade.dda2.server.feature.program.validator.AdminProgramValidator`
- `com.uade.dda2.server.feature.program.validator.ProgramImageValidator`

## Resultados reales

| Métrica | Baseline | Final |
| --- | ---: | ---: |
| Total de mutantes | 701 | 701 |
| KILLED | 330 | 445 |
| SURVIVED | 98 | 106 |
| NO_COVERAGE | 273 | 150 |
| TIMED_OUT / RUN_ERROR / MEMORY_ERROR / NON_VIABLE | 0 | 0 |
| Mutation score: KILLED / total | 47,08% | **63,48%** |
| Umbral | 60% | 60% |
| Resultado de la tarea | Falla por umbral | BUILD SUCCESSFUL |

Se detectaron 115 mutantes adicionales. El aumento de sobrevivientes refleja mutantes que antes no se ejecutaban; no se eliminaron del denominador. El 81% de test strength del reporte final es otra métrica y no se usa como mutation score.

PIT tardó 2m29s en baseline y 1m54s en final; la invocación Gradle final completa tardó 2m36s. Los 256 casos del alcance pasaron, sin fallos ni omitidos. La ejecución convencional forzada, sin init script, también pasó en 11s. No se repitió en esta etapa toda la suite de integración ajena al alcance.

## Tests fortalecidos

- `ApplicationDomainMutationTest`: autorización, convocatoria abierta, fechas inclusivas, edición activa, idempotencia, estados de evaluación, impedimento de autoevaluación/asignación ajena y límites de recomendación.
- `ApplicationVisitMutationTest`: estados y asignación, normalización UTC y precisión, cálculo de fin, inicio futuro, duración, límites de lugar/motivos/resultados y condiciones temporales para modificar o registrar resultados.
- `AuthMutationTest`: cuenta local activa, credenciales presentes, contraseña contrastada con encoder y rechazo de credenciales inválidas.
- `EffectiveScheduleValidatorTest`: cobertura exacta de horario, franja insuficiente, colección vacía y descarte de franjas anteriores.

No se modificaron reglas de producción, no se ignoraron tests ni se debilitaron aserciones.

## Sobrevivientes y limitaciones

Quedan **106 SURVIVED y 150 NO_COVERAGE**. Se conservan todos en XML/HTML; superar 60% no significa ausencia de brechas.

- `BenefitGrantLifecycleValidator.validateRenewal`, línea 37: sobreviven cambios de límites de vigencia/extensión. Prioridad para futuros tests de fechas límite y combinaciones de rango; no se clasifican automáticamente como equivalentes.
- `EffectiveScheduleValidator.isCovered`, líneas 13 y 15: sobreviven cambios `<=`/`>` en la progresión del cursor; parecen redundantes por el flujo posterior para rangos válidos. Es una hipótesis de equivalencia, no una exclusión ni una detección contabilizada.
- `AdminProgramEditionValidator`: sobreviven comprobaciones de identificadores no nulos y una llamada de chequeo generada por Kotlin. Se mantienen en el resultado.
- Hay validadores sin tests unitarios seleccionados (por ejemplo, parte de centros). Sus mutantes NO_COVERAGE siguen penalizando el score; las pruebas HTTP de otras suites no se incorporaron a esta corrida unitaria.

## Reproducir desde server

Requiere Java 17 instalado y wrapper Gradle. No requiere levantar backend, Docker, PostgreSQL ni acceder a la base remota.

```powershell
.\gradlew.bat -I qa-mutation.init.gradle test --tests '*validator.*Test' pitest --no-configuration-cache --console=plain --max-workers=2
```

Comprobación convencional sin PIT:

```powershell
.\gradlew.bat test --rerun --tests '*validator.*Test' --no-configuration-cache --console=plain --max-workers=2
```

En Linux usar `bash gradlew` con los mismos argumentos. `mutationThreshold=60` hace fallar la tarea por debajo del mínimo. Los reportes se sobrescriben en la siguiente corrida; el ZIP preserva esta evidencia.

## Evidencia local

Rutas relativas a la raíz del repositorio:

- Final: `server/build/reports/pitest/index.html` y `mutations.xml`.
- Baseline: `server/build/reports/pitest-baseline/index.html` y `mutations.xml` original.
- Copia UTF-8 del XML inicial: `server/build/reports/pitest-baseline/mutations.utf8.xml`. El primer proceso escribió bytes Windows-1252 con encabezado UTF-8; se conserva el original y una transcodificación sin cambiar registros. El XML final es UTF-8 válido nativo.
- Contexto, versiones y hashes: `server/build/reports/pitest/evidence-context.json`.
- Logs: `tmp/qa-mutation-baseline.log`, `tmp/qa-mutation-final.log`, `tmp/qa-mutation-ordinary-tests.log`.
- Paquete para Drive: `output/mutation-evidence-2026-11-11.zip` (baseline, final, logs, configuración y tests del alcance).

## CI y entrega

`.github/workflows/mutation-testing.yml` agrega ejecución manual separada, Java 17, umbral 60 y artifact `backend-mutation-<SHA>` conservado 30 días, incluso si falla el umbral. Tiene límite propio de 30 minutos y no modifica ni agrega tareas al pipeline obligatorio. **Preparado localmente; aún no ejecutado ni validado en GitHub Actions.**

No se creó tag ni se hizo merge/push de estos cambios de mutación. El push anterior `3a8eeaa` contiene los 211 casos de cobertura, no este trabajo posterior. Antes de etiquetar la entrega, el equipo debe integrar estos cambios, ejecutar CI/PIT sobre la revisión final y crear `v2.0-entrega4` en el commit aprobado de `main`; no etiquetar el commit base por esta medición local.

## Referencias de configuración

- [Plugin Gradle PIT](https://gradle-pitest-plugin.solidsoft.info/).
- [Adaptador JUnit de PIT](https://github.com/pitest/pitest-junit5-plugin).
- [Notas de compatibilidad del launcher](https://github.com/szpak/gradle-pitest-plugin/blob/master/CHANGELOG.md).
