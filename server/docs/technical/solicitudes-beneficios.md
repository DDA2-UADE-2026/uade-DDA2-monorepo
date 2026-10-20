# Solicitudes de beneficios

## Planes de intervención y seguimientos

`feature.intervention` separa controladores, servicio transaccional, validador, repositorios y mapeadores. `InterventionPlan` tiene vínculo único e inmutable con `Application`; el ciudadano se deriva de `application.user`. Objetivos y acciones conservan posiciones, textos y fechas iniciales. Solo `InterventionAction.status` cambia; `InterventionFollowUp` es inmutable y tiene secuencia única por acción.

Las escrituras usan `POST /api/applications/{id}/intervention-plan` y `POST .../actions/{actionId}/follow-ups`; devuelven `201`. Las lecturas usan las mismas rutas con prefijo `/api/admin` y devuelven `200`. Los permisos son `applications:interventions:create`, `applications:interventions:follow-ups:create` y `applications:interventions:view`, inicialmente para ADMIN. El validador comprueba usuario activo y permiso del JWT y del rol activo actual. Crear/registrar exige coincidencia con trabajador asignado y responsable guardado; leer no exige coincidencia.

La creación requiere evaluación y admite `IN_EVALUATION/IN_VISIT/WAITLISTED/APPROVED/REJECTED/CLOSED`. Los DTO rechazan campos adicionales también en objetivos y acciones. Descripciones: 1–1000 caracteres recortados; notas y resultado: 1–2000. El resultado es obligatorio para completar. Las fechas previstas son independientes de programas y beneficios. Los seguimientos admiten fechas reales desde el inicio hasta hoy en la zona configurada, sin retroceder respecto al registro previo, incluso antes del alta del plan o después de su fin previsto.

El servicio bloquea solicitud, plan y acción, en ese orden, según corresponda. Las consultas de bloqueo no hacen fetch joins; la pertenencia de acción se valida mediante subconsulta EXISTS para no bloquear otras tablas en la consulta externa. La autorización en base se carga después de esperar los bloqueos. Bajo bloqueo se releen estado y último seguimiento, se calcula la secuencia y se valida cronología. Un cierre de solicitud por beneficios comparte el primer bloqueo y no impide continuar el seguimiento.

Los registros UTC tienen precisión de microsegundos. `Application.updatedAt` usa la zona configurada. Plan/objetivos/acciones o seguimiento/estado, fecha de solicitud y auditoría se confirman juntos. Los logs `intervention_plan` e `intervention_follow_up` excluyen textos profesionales. Las FK y el guard de baja de usuarios protegen responsables/autores. La API no tiene edición, eliminación ni reasignación, y no amplía respuestas ciudadanas.

Antes del despliegue ejecutar [2026-10-19-intervention-plans.sql](../database/migrations/2026-10-19-intervention-plans.sql) sobre el esquema existente. Crea tablas, unicidades, índices, CHECK y permisos de forma idempotente, sin fabricar planes ni seguimientos. Modelo completo en DBML y [contrato funcional](../functional/intervention-plans.md).

Pruebas: `InterventionValidatorTest` y `InterventionPlanFlowTest`. La tarea `testInterventionPlansPostgres` usa exclusivamente PostgreSQL desechable `127.0.0.1:55439/intervention_plan_test`, usuario `application_test`, contraseña de `APPLICATION_TEST_POSTGRES_PASSWORD`, con `create-drop`; nunca usa `DB_URL`. Incluye migración repetida en esquema privado, rollback y concurrencia de alta, avances, finalización y cierre por beneficios.

Verificación del 19 de octubre de 2026: suite completa de 466 casos, con 462 aprobados y cuatro migraciones exclusivas de PostgreSQL omitidas en H2. Los 27 casos de flujo de intervención pasan en PostgreSQL 16.4, sin omisiones. `bootJar` y `generateOpenApiDocs` finalizan correctamente; las rutas y esquemas de entrada publicados incluyen los campos obligatorios, estados permitidos, seguridad y errores documentados. El servidor desechable queda detenido y sus archivos temporales se eliminan.

```powershell
.\gradlew.bat test bootJar generateOpenApiDocs --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
.\gradlew.bat testInterventionPlansPostgres --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
```

## Ciclo de vida de otorgamientos

Las rutas `POST /api/admin/applications/{id}/benefit-grants/{grantId}/renew|suspend|finalize` exigen `applications:management:benefits:manage` y devuelven `200` con `BenefitGrantResponse`. `GET .../{grantId}/history` exige `applications:management:view`. Los DTO rechazan campos extra y el servidor determina actor e instante UTC. Se valida permiso en JWT y rol activo vigente, además del usuario activo.

`BenefitGrantLifecycleService` separa coordinación transaccional de validación y mapeo. Admite renovar `ACTIVE/SUSPENDED -> ACTIVE`, suspender `ACTIVE -> SUSPENDED` y finalizar `ACTIVE/SUSPENDED -> FINALIZED`. Exige solicitud aprobada. Renovar conserva inicio y copia del catálogo, extiende el fin e incluye hoy en la zona configurada, dentro de edición activa. Relee checklist actual, exige todos los resultados positivos y documentos obligatorios `VALID` y revalida aprobaciones incompatibles con fechas de edición superpuestas inclusivas. Suspender/finalizar admite edición inactiva o vencida.

El orden explícito de bloqueo es solicitud, titular, edición y otorgamiento, sin fetch joins en las consultas de bloqueo de solicitud/otorgamiento. Asignación y finalización comparten el bloqueo de solicitud; catálogo, requisitos y fechas comparten el de edición. La secuencia del historial se calcula bajo esos bloqueos. La finalización hace flush antes de contar los otorgamientos no finalizados; el último cierra `Application` en `CLOSED` y decrementa una sola reserva, con protección contra contador no positivo. Conserva resolución y trabajador. No se reabre la solicitud.

`benefit_grant_change` y `benefit_grant_change_requirement` son entidades inmutables. La unicidad `(grant_id, sequence_number)` ordena las actuaciones; los CHECK protegen transición, motivo y vigencia. El catálogo de requisitos no tiene FK desde sus copias históricas. Se protege la baja de actores históricos mediante validación y FK. Los cambios, controles, estado, fechas, eventual cierre/cupo y logs participan en una transacción. El log `benefit_grant_change` excluye motivo, descripción y monto; el cierre de solicitud tiene además log `application`.

Antes del despliegue ejecutar [2026-10-19-benefit-grant-lifecycle.sql](../database/migrations/2026-10-19-benefit-grant-lifecycle.sql), **después** de la migración de otorgamientos. Es idempotente y sustituye restricciones CHECK sobre estado, incluida la generada por Hibernate; `ddl-auto=update` no reemplaza esta migración. Registra solo la asignación inicial conocida de los otorgamientos existentes, sin cambios ficticios, sin ajustar cupos ni solicitudes. Agrega el permiso inicial de ADMIN. Las asignaciones nuevas registran su evento inicial en la misma transacción.

Pruebas: `BenefitGrantLifecycleValidatorTest` y `BenefitGrantFlowTest` cubren transiciones, requisitos/documentos, vigencia, privacidad, seguridad, snapshots, cierre/cupos, rollback de auditoría y concurrencia. La tarea existente `testBenefitGrantsPostgres` ejecuta también estas pruebas y las migraciones sobre esquemas privados en PostgreSQL desechable. Contrato funcional: [Ciclo de vida de beneficios](../functional/benefit-grant-lifecycle.md).

Verificación del ciclo de vida (19 de octubre de 2026): suite completa de 434 casos, con 431 aprobados y tres migraciones exclusivas de PostgreSQL omitidas en H2; los 81 casos de flujo de otorgamientos y mantenimiento pasan en PostgreSQL 16.4, sin omisiones. `bootJar` y `generateOpenApiDocs` finalizan correctamente. El proceso de pruebas limita a ocho los contextos de Spring retenidos y utiliza hasta 1 GiB de heap para evitar agotar memoria en la regresión completa.

## Otorgamientos individuales

`POST /api/admin/applications/{id}/benefit-grants` exige `applications:management:benefits:assign`; `GET` sobre la misma ruta exige `applications:management:view`. Se comprueban usuario activo, rol activo y permiso tanto en JWT como en base de datos. El alta acepta exclusivamente `programBenefitId`, `startDate` y `endDate` y devuelve `201` con `BenefitGrantResponse`; la consulta devuelve una lista ordenada por `assignedAt`/`id`, o `[]`.

`BenefitGrantService` exige solicitud `APPROVED`, edición `ACTIVE`, beneficio de esa edición, vigencia inclusiva dentro de las fechas de edición que contenga el día actual en la zona configurada y ausencia de aprobaciones incompatibles del titular con fechas de edición superpuestas. Reutiliza la regla de incompatibilidad de resolución; no reevalúa checklist ni documentos. La solicitud conserva estado y resolución y solo cambia `updatedAt`; el cupo no cambia.

`benefit_grant` referencia solicitud, beneficio y administrativo y copia tipo, descripción e importe al momento de otorgar. La unicidad `(application_id, program_benefit_id)` impide repetir un beneficio. La fecha de registro es UTC, con precisión de microsegundos; las vigencias son `date`. El estado inicial es `ACTIVE` y no hay vencimiento automático en esta historia.

La transacción bloquea solicitud, titular y edición, en ese orden. El CRUD de beneficios comparte el bloqueo de edición. Se bloquean bajas de beneficios otorgados y responsables referenciados; cambiar el catálogo no modifica copias existentes. La edición tampoco puede acortarse dejando otorgamientos fuera de rango. Otorgamiento, `updatedAt` y log `benefit_grant` se confirman juntos; el log no copia descripción ni importe.

Antes del despliegue ejecutar `database/migrations/2026-10-19-benefit-grants.sql` desde `/docs`, después del esquema existente. Es idempotente, incorpora el permiso inicial de `ADMIN` y no genera otorgamientos ni modifica cupos anteriores. Contrato y reglas funcionales: [Otorgamiento individual](../functional/benefit-grants.md).

`BenefitGrantValidatorTest` y `BenefitGrantFlowTest` cubren reglas, seguridad, copia de datos, protección de referencias, fechas de edición, rollback y concurrencia. La tarea `testBenefitGrantsPostgres` repite el flujo en **PostgreSQL desechable** `127.0.0.1:55439/benefit_grant_test`, usuario `application_test`, contraseña de `APPLICATION_TEST_POSTGRES_PASSWORD`; usa `create-drop` y nunca `DB_URL`.

Verificación del 19 de octubre de 2026: 386 casos en la suite completa, con 384 aprobados y dos pruebas de migración específicas de PostgreSQL omitidas en H2. Los 38 casos de flujo de otorgamiento pasan en PostgreSQL 16.4, incluida la creación desde cero de la tabla por migración en un esquema aislado, su ejecución repetida y la unicidad. `bootJar` y `generateOpenApiDocs` finalizan correctamente; el servidor desechable queda detenido.

```powershell
.\gradlew.bat test bootJar generateOpenApiDocs --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
.\gradlew.bat testBenefitGrantsPostgres --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
```

## Resoluciones administrativas

`POST /api/admin/applications/{id}/resolutions` exige `applications:management:resolve` y devuelve `201`.
`GET` sobre la misma ruta exige `applications:management:view` y devuelve el historial cronológico, o `[]` si no existe. Ambas operaciones verifican usuario activo y permiso en JWT y rol activo vigente en base de datos.

La capa de negocio exige evaluación y ausencia de actuaciones `SCHEDULED`, valida la transición (`IN_EVALUATION` a aprobado/rechazado/espera; `WAITLISTED` a aprobado/rechazado), motivo de 1–1000 caracteres tras recortar y un booleano por cada requisito vigente. Las decisiones de aprobación y espera requieren todos los controles cumplidos, documentos obligatorios `VALID`, edición `ACTIVE` no vencida e inexistencia de aprobaciones incompatibles del titular con vigencias superpuestas inclusive. Espera exige cupo agotado; aprobación exige disponibilidad y aumenta `currentEnrollment` una vez. Rechazo no aplica estas condiciones adicionales. No se exige convocatoria abierta.

`ApplicationResolutionService` bloquea solicitud, titular y edición en ese orden. Las consultas de bloqueo de `ApplicationRepository` no hacen fetch join de edición para no adquirir su bloqueo antes del titular. Las modificaciones de requisitos de negocio comparten el bloqueo de edición. La aprobación se serializa por titular para evitar aprobaciones incompatibles y por edición para proteger el último cupo. Estado, fechas, último responsable, contador, historial y auditoría se confirman en una transacción.

`application_resolution` conserva estado anterior, decisión, motivo, responsable y fecha UTC; la unicidad `(application_id, previous_status)` impide resolver dos veces una etapa. `application_resolution_requirement` conserva el identificador histórico y una copia de tipo/valor/descripción/resultado, sin FK al catálogo editable. Las entidades históricas son inmutables y no tienen operaciones de corrección o borrado. `application.resolved_by_user_id` identifica al último responsable, sin reemplazar al trabajador asignado. El detalle ciudadano no agrega campos de resolución. La auditoría `application_resolution` guarda identificadores, transición y resultados, pero excluye el motivo.

Antes del despliegue ejecutar `docs/database/migrations/2026-10-18-application-resolutions.sql`; puede repetirse y no inventa resoluciones ni ajusta cupos anteriores. `init.sql` incorpora el permiso inicial para `ADMIN`. La documentación funcional completa está en [Resolución de solicitudes](../functional/application-resolution.md).

Pruebas: `ApplicationResolutionFlowTest` ejecuta seguridad, reglas, historial, privacidad, rollback y concurrencia en H2; la tarea `testApplicationResolutionsPostgres` repite la suite en la base **desechable** `127.0.0.1:55439/application_resolution_test`, usuario `application_test` y contraseña `APPLICATION_TEST_POSTGRES_PASSWORD`. Usa `create-drop` y nunca `DB_URL`.

Verificación del 18 de octubre de 2026: la suite completa ejecuta 344 casos, con 343 aprobados y uno reservado para PostgreSQL omitido en H2. Los 48 casos de flujo de resolución pasan en PostgreSQL 16.4, incluida la ejecución repetida de la migración. `bootJar` y `generateOpenApiDocs` finalizan correctamente. El servidor desechable se detiene al terminar.

```powershell
.\gradlew.bat test generateOpenApiDocs --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
.\gradlew.bat testApplicationResolutionsPostgres --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
```

El backend implementa presentación propia y asistida, consultas, documentos protegidos, evaluación social, gestión de visitas o entrevistas, consulta ciudadana y solicitudes ciudadanas de reprogramación mediante JWT con rol activo. La solicitud pertenece a `users.id`: se obtiene del token en la presentación propia y de un `userId` existente en la asistida. `registeredByUserId` identifica por separado a quien la registró y siempre se obtiene del token. No depende de una identidad externa ni de `citizen_snapshot`. No se implementan decisiones sobre la solicitud social, correcciones o reevaluaciones ni integración con Ciudadanos.

## Contrato HTTP

| Método y ruta | Permiso del rol activo | Resultado |
| --- | --- | --- |
| `POST /api/applications` | `applications:own:create` | `201 Created` al presentar; `200 OK` al recuperar una presentación mediante idempotencia |
| `POST /api/admin/applications` | `applications:management:create` | Presentación asistida para otro usuario existente; `201` nueva, `200` reintento |
| `GET /api/applications?page=0&size=20` | `applications:own:view` | Página de solicitudes propias, por número descendente |
| `GET /api/applications/{id}` | `applications:own:view` | Detalle propio; una solicitud ajena devuelve `404`, igual que una inexistente |
| `GET /api/applications/{applicationId}/visits?page=0&size=20` | `applications:own:visits:view` | Página de actuaciones de una solicitud propia, por inicio descendente |
| `GET /api/applications/{applicationId}/visits/{visitId}` | `applications:own:visits:view` | Detalle ciudadano con motivo de cancelación cuando corresponde |
| `GET /api/admin/applications?page=0&size=20` | `applications:management:view` | Página de solicitudes de todos los titulares, por número descendente |
| `GET /api/admin/applications/{id}` | `applications:management:view` | Detalle administrativo con la entidad completa |
| `POST /api/applications/{applicationId}/evaluation` | `applications:evaluations:create` | Registra la evaluación, asigna al actor y cambia la solicitud a `IN_EVALUATION`; `201 Created` |
| `GET /api/admin/application-evaluations` | `applications:evaluations:view` | Página filtrada de evaluaciones, sin la recomendación sensible |
| `GET /api/admin/application-evaluations/{evaluationId}` | `applications:evaluations:view` | Detalle completo de la evaluación y sus relaciones |
| `POST /api/applications/{applicationId}/visits` | `applications:visits:create` | Programa una visita o entrevista; `201 Created` y transición a `IN_VISIT` en la primera |
| `PUT /api/applications/{applicationId}/visits/{visitId}/schedule` | `applications:visits:manage` | Reprograma horario, duración y lugar de una actuación futura |
| `PATCH /api/applications/{applicationId}/visits/{visitId}/cancel` | `applications:visits:manage` | Cancela con motivo, libera la agenda y puede volver la solicitud a `IN_EVALUATION` |
| `POST /api/applications/{applicationId}/visits/{visitId}/result` | `applications:visits:results:create` | Registra una vez el resultado final y puede volver la solicitud a `IN_EVALUATION`; `201 Created` |
| `GET /api/admin/application-visits` | `applications:visits:view` | Página filtrada de actuaciones ordenada por inicio ascendente |
| `GET /api/admin/application-visits/{visitId}` | `applications:visits:view` | Detalle completo de la actuación y su contexto |
| `GET /api/applications/{applicationId}/documents` | `applications:own:documents:view` | Metadatos de entregas propias, sin bytes |
| `PUT /api/applications/{applicationId}/documents/{requirementId}` | `applications:own:documents:manage` | Primera carga `201` o reemplazo `200`, mediante `multipart/form-data` |
| `DELETE /api/applications/{applicationId}/documents/{applicationDocumentId}` | `applications:own:documents:manage` | Elimina la entrega y su archivo, `204` |
| `GET /api/applications/{applicationId}/documents/{applicationDocumentId}/content` | `applications:own:documents:view` | Contenido propio protegido con disposición `inline` |
| `GET /api/admin/applications/{applicationId}/documents` | `applications:management:documents:view` | Metadatos de cualquier solicitud |
| `GET /api/admin/applications/{applicationId}/documents/{applicationDocumentId}/content` | `applications:management:documents:view` | Contenido protegido para revisión |
| `PATCH /api/admin/applications/{applicationId}/documents/{applicationDocumentId}/review` | `applications:management:documents:review` | Revisa una entrega `PENDING` como `VALID` u `OBSERVED` |
| CRUD `/api/admin/program-editions/{editionId}/document-requirements` | `programs:management:view\|create\|edit` | Administra el catálogo mientras la edición no tenga solicitudes |

Todos requieren `Authorization: Bearer <accessToken>`. Los JWT de selección de rol no sirven. El actor autenticado debe existir, estar activo y conservar el rol activo asignado. Los permisos se evalúan sobre el JWT, sin mezclar los de otros roles. El alta, gestión y consulta de evaluaciones y actuaciones comprueban además que su permiso siga asignado al rol en la base, por lo que una revocación se aplica aunque el JWT anterior todavía lo declare.

La consulta propia no recibe un identificador de usuario: siempre filtra por el solicitante autenticado. La consulta administrativa no filtra por titular ni por el administrativo que registró la solicitud. En ambas, `page` empieza en cero y `size` admite valores entre 1 y 100.

El POST propio acepta únicamente este cuerpo; cualquier campo adicional se rechaza con `400`:

```json
{
  "enrollmentPeriodId": "e14a6f34-b991-476d-8f13-0c6fbe301c51"
}
```

El POST asistido acepta únicamente `userId` y `enrollmentPeriodId`:

```json
{
  "userId": 42,
  "enrollmentPeriodId": "e14a6f34-b991-476d-8f13-0c6fbe301c51"
}
```

Se comprueba la existencia del titular en `users`, sin filtros de jurisdicción, roles, credenciales o vinculación externa. No se necesita el JWT del titular. La autorización para esta operación corresponde al administrativo. Enviar `registeredByUserId`, `registeredBy`, `registradoPor`, `citizenId` o cualquier otro campo adicional devuelve `400`. Para presentar para sí mismo, el actor debe utilizar el POST propio con su permiso correspondiente; la ruta asistida lo rechaza con `403`.

El POST de evaluación acepta únicamente:

```json
{
  "vulnerability": "HIGH",
  "priority": "URGENT",
  "recommendation": "Se recomienda intervención prioritaria."
}
```

`vulnerability` admite `LOW`, `MEDIUM`, `HIGH` o `CRITICAL`; `priority` admite `LOW`, `MEDIUM`, `HIGH` o `URGENT`. La recomendación se recorta y debe tener de 1 a 1000 caracteres. Profesional, fecha, solicitud y estado se derivan del contexto y cualquier campo adicional devuelve `400 APPLICATION_EVALUATION_INVALID_DATA`.

La respuesta `201` incluye `id`, `applicationId`, las tres decisiones, `professionalId`, `professionalName` y `evaluatedAt` UTC.

El listado administrativo admite `page`, `size`, `applicationId`, `applicationNumber`, `applicantUserId`, `professionalId`, `programId`, `programEditionId`, `vulnerability`, `priority`, `evaluatedFrom` y `evaluatedTo`. El rango es inclusivo, utiliza ISO-8601 con offset y exige que el inicio no sea posterior al fin. El orden es `evaluatedAt` descendente, con `id` descendente como desempate. Cada elemento contiene contexto de solicitud, titular, programa, edición y profesional, pero omite `recommendation`.

El detalle por `evaluationId` devuelve también la recomendación, correos del titular y profesional, convocatoria y todos los identificadores relacionados. No existe un endpoint de edición o eliminación: la evaluación continúa siendo inmutable.

El POST de visita acepta exclusivamente:

```json
{
  "type": "VISIT",
  "startsAt": "2026-10-20T10:00:00-03:00",
  "durationMinutes": 60,
  "location": "Domicilio declarado por el ciudadano"
}
```

`type` admite `VISIT` o `INTERVIEW`; `startsAt` debe ser futuro, `durationMinutes` un entero positivo y `location` un texto recortado de 1 a 300 caracteres. El servidor normaliza el instante, calcula `endsAt`, asigna responsable y `scheduledAt`, fija `SCHEDULED` y devuelve también el estado de la solicitud. Cualquier campo administrado o desconocido devuelve `400 APPLICATION_VISIT_INVALID_DATA`.

La reprogramación acepta exclusivamente `startsAt`, `durationMinutes` y `location`, con las mismas reglas temporales y de longitud del alta. Conserva identificador, tipo, solicitud, responsable y `scheduledAt`; actualiza `lastRescheduledAt` en UTC y rechaza un cuerpo que no cambie ningún valor. La cancelación acepta exclusivamente `reason`, obligatorio y recortado de 1 a 1000 caracteres. Persiste `CANCELLED`, `cancelledAt` y `cancelledBy`, y libera el intervalo inmediatamente.

El resultado acepta exclusivamente:

```json
{
  "status": "REALIZED",
  "observations": "La familia recibió al profesional.",
  "conclusions": "Se requiere continuar con la evaluación."
}
```

`status` es obligatorio y admite `REALIZED` o `NOT_REALIZED`. `observations` y `conclusions` son opcionales; si se envían, se recortan y deben tener entre 1 y 2000 caracteres. El servidor asigna `resultRecordedAt` en UTC y `resultRecordedBy` al actor. Solo el responsable todavía asignado puede finalizar una actuación `SCHEDULED` cuyo `endsAt` ya se alcanzó y cuya solicitud permanece en `IN_VISIT`. El resultado es inmutable: un segundo intento se rechaza.

El listado de actuaciones admite `page`, `size`, `applicationId`, `applicationNumber`, `applicantUserId`, `responsibleId`, `programId`, `programEditionId`, `type`, `status`, `windowStart` y `windowEnd`. Sin `status` incluye `SCHEDULED`, `CANCELLED`, `REALIZED` y `NOT_REALIZED`. La ventana es semiabierta y selecciona intervalos superpuestos (`startsAt < windowEnd` y `endsAt > windowStart`). Se ordena por `startsAt` e `id` ascendentes. El listado expone actor y fecha de cancelación o resultado, pero omite motivo, observaciones y conclusiones; el detalle incorpora los textos completos.

La consulta ciudadana restringe siempre por `application.id` y `application.user.id`, tomado del JWT. El listado incluye los cuatro estados y se ordena por `startsAt` e `id` descendentes. Sus DTO solo contienen identificadores de actuación y solicitud, número de solicitud, modalidad, horario vigente, duración, lugar, estado, indicador y fecha de última reprogramación y fecha de cancelación. El detalle agrega únicamente `cancellationReason`; nunca expone observaciones, conclusiones ni actores internos. La paginación admite `page >= 0` y `size` entre 1 y 100.

`ApplicationVisitRescheduleRequest` conserva el historial separado de peticiones ciudadanas. El alta `POST /api/applications/{applicationId}/visits/{visitId}/reschedule-requests` acepta exclusivamente `reason`, recortado de 1 a 1000 caracteres, y snapshot del horario/lugar actual asignado por el servidor. Exige propiedad, solicitud `IN_VISIT`, actuación `SCHEDULED` futura y ausencia de otra `PENDING`; no modifica la visita. Una unicidad parcial PostgreSQL sobre `visit_id WHERE status = 'PENDING'` respalda la regla.

Los endpoints ciudadanos de historial usan `applications:own:visits:view`, ordenan por `requestedAt` e `id` descendentes y restringen simultáneamente por solicitud, visita y titular. El listado omite motivo/comentario y el detalle los incluye sin identidades internas. Las respuestas de visita agregan `latestRescheduleRequest` con id, estado y fechas solamente.

La bandeja `/api/admin/application-visit-reschedule-requests` exige `applications:visits:reschedule-requests:view`, ordena por antigüedad y filtra por estado, solicitud, número, visita, ciudadano, responsable, programa, edición y ventana semiabierta de solicitud. El listado no expone motivo, comentario ni lugares; el detalle sí los muestra al rol autorizado.

La revisión exige `applications:visits:reschedule-requests:review` y coincidencia entre actor, responsable y trabajador asignado. `APPROVED` requiere horario completo, cambio real y agenda libre para responsable y titular; actualiza la visita, `lastRescheduledAt` y `Application.updatedAt`. `REJECTED` prohíbe horario y requiere comentario. Un conflicto o aprobación tardía revierte todo y conserva `PENDING`; cuando el horario original comenzó solo se admite rechazo. Mientras haya una pendiente, reprogramar, cancelar o registrar resultado directamente devuelve `APPLICATION_VISIT_RESCHEDULE_REQUEST_PENDING`.

El alta exige evaluación previa, estado `IN_EVALUATION` o `IN_VISIT`, trabajador asignado y coincidencia entre ese trabajador y el actor. La agenda común combina actuaciones donde una persona participa como responsable o titular, turnos `CONFIRMED` del ciudadano y turnos `CONFIRMED` o `CANCELLED` con intervalo retenido del profesional. Los intervalos son semiabiertos: terminar exactamente cuando comienza el siguiente no es conflicto. La misma ocupación se aplica a alta, reprogramación, oferta de horarios y resúmenes administrativos de turnos.

Respuesta de presentación propia:

```http
HTTP/1.1 201 Created
Location: /api/applications/2aa19e54-6f3f-4eef-99b1-8e1af9d8db26
Idempotency-Replayed: false
```

```json
{
  "id": "2aa19e54-6f3f-4eef-99b1-8e1af9d8db26",
  "applicationNumber": 15432,
  "userId": 42,
  "registeredByUserId": 42,
  "programEditionId": "2199c5bd-3329-4172-9b28-89d3bbf236f6",
  "enrollmentPeriodId": "e14a6f34-b991-476d-8f13-0c6fbe301c51",
  "status": "SUBMITTED",
  "submittedAt": "2026-08-31T13:30:00",
  "createdAt": "2026-08-31T13:30:00",
  "updatedAt": "2026-08-31T13:30:00",
  "pendingDocuments": [
    {
      "requirementId": "14c850cb-2871-4dc1-b7bc-8ff66ee69380",
      "code": "DNI_FRONT",
      "name": "Frente del DNI",
      "reason": "MISSING",
      "observation": null
    }
  ]
}
```

El listado utiliza `content`, `page`, `size`, `totalElements` y `totalPages`. El detalle y los elementos del listado tienen el mismo contrato que la respuesta del POST. No exponen el hash, la clave de idempotencia ni datos personales del solicitante.

La respuesta asistida tiene el mismo cuerpo, con `userId` del titular y `registeredByUserId` del administrativo que registró originalmente la solicitud. Devuelve `Idempotency-Replayed` y no informa `Location`. Registrar para otra persona no concede acceso a su detalle propio: la consulta administrativa depende de su propio permiso. El titular sí recibe la solicitud asistida en su listado y detalle propios.

El campo `status` devuelve el estado interno. La presentación crea `SUBMITTED`; registrar una evaluación lo cambia a `IN_EVALUATION`. La correspondencia completa con otros estados públicos simplificados sigue pendiente de definición.

## Consulta administrativa de solicitudes

`GET /api/admin/applications` devuelve una página con las solicitudes de todos los titulares, sin filtros y ordenada por `applicationNumber` descendente, igual que el listado propio. Utiliza la misma envoltura `content`, `page`, `size`, `totalElements` y `totalPages`. Cada elemento agrega al contrato propio el nombre y el correo del titular tomados de `users`, el trabajador asignado y `resolvedAt`, y omite los documentos: `pendingDocuments` y `documentRequirements` se calculan solo en el detalle, para no multiplicar consultas por página.

`GET /api/admin/applications/{id}` devuelve la entidad completa. Además de los campos del detalle propio incorpora `userName`, `userEmail`, `registeredByUserName`, `originTicketId`, `resolutionReason`, `assignedWorkerUserId`, `assignedWorkerName`, `resolvedAt`, `idempotencyKey` y `requestHash`. La clave y el hash se exponen únicamente en esta vista administrativa, nunca en el detalle propio. Una solicitud inexistente devuelve `404 APPLICATION_NOT_FOUND`; no existe un filtro de titular que convierta una solicitud ajena en inexistente, porque la vista es global por definición.

Ninguna de las dos rutas devuelve archivos, metadatos de entregas ni la evaluación. Los documentos y la evaluación conservan rutas y permisos separados. Ambas consultas son de solo lectura: no cambian estado, no asignan trabajadores y no registran auditoría.

## Catálogo y entregas documentales

`ProgramDocumentRequirement` define el catálogo por edición. El `code` se recorta y normaliza a mayúsculas, admite letras, números y guion bajo, y es único junto con `program_edition_id`. El detalle de programas disponibles incorpora `documentRequirements` en cada edición. Crear requiere `programs:management:create`, leer `programs:management:view` y actualizar o eliminar `programs:management:edit`.

Las mutaciones del catálogo bloquean la edición y comprueban que no exista ninguna `Application` asociada. Desde la primera solicitud, cualquier alta, actualización o baja devuelve `409 PROGRAM_DOCUMENT_REQUIREMENTS_LOCKED`. Así se serializa con la presentación, que bloquea la misma edición, y no queda una ventana entre la comprobación y el alta.

`Document` almacena UUID, nombre original saneado, MIME, tamaño, bytes, cargador y fecha. No existe un controller general ni una ruta pública. `ApplicationDocument` vincula solicitud, requisito y archivo con unicidad por `(application_id, requirement_id)` y por `document_id`.

La carga valida, antes de persistir:

- archivo no vacío y tamaño máximo de 10 MiB;
- extensión `pdf`, `jpg`, `jpeg` o `png`;
- MIME declarado exacto `application/pdf`, `image/jpeg` o `image/png`;
- firma real `%PDF-`, JPEG `FF D8 FF` o PNG `89 50 4E 47 0D 0A 1A 0A` coherente con extensión y MIME.

Un exceso detectado por el servicio o por el límite multipart devuelve `413 APPLICATION_DOCUMENT_FILE_TOO_LARGE`. Los demás formatos inválidos devuelven `400`. El límite de petición por defecto es 11 MB para permitir el envoltorio multipart sin aumentar el límite de archivo de 10 MB.

La primera carga crea `Document` y `ApplicationDocument` en `PENDING`. Reemplazar conserva el UUID de `ApplicationDocument`, crea otro `Document`, elimina el anterior y limpia estado, observación, revisor y fecha de revisión. Eliminar borra vínculo y archivo. Las tres operaciones requieren propiedad de la solicitud y se bloquean cuando su estado es `APPROVED`, `REJECTED` o `CLOSED`.

La revisión acepta únicamente una entrega `PENDING`. `OBSERVED` exige una observación no vacía; `VALID` la prohíbe. Cargar, reemplazar, eliminar o revisar cambia `Application.updatedAt`, sin modificar `Application.status`.

`pendingDocuments` se calcula tanto en el listado como en el detalle. Contiene requisitos `required=true` sin entrega (`MISSING`) o con entrega `OBSERVED`. Excluye requisitos opcionales y entregas `PENDING` o `VALID`.

Los listados consultan proyecciones de metadatos y no leen la columna binaria. Solo las rutas `/content` la recuperan y responden los bytes con el MIME almacenado, tamaño, nombre UTF-8 seguro, `Content-Disposition: inline` y `Cache-Control: private, no-store`. Una solicitud o documento ajeno devuelve `404` igual que uno inexistente.

Cada mutación bloquea la fila de la solicitud. Documento, vínculo, `updatedAt` y log `application_document` se confirman en una transacción; un fallo revierte todo. La auditoría contiene identificadores, nombre, MIME, tamaño, estado y revisión, nunca `content`. Las bajas de usuarios se bloquean también si el usuario cargó o revisó documentos.

## Reglas de presentación

- La convocatoria existe, está `OPEN` y cumple `openDate <= hoy <= closeDate`.
- La edición se deriva de la convocatoria y debe estar `ACTIVE`.
- Se admite una sola solicitud por usuario y convocatoria, en cualquier estado, incluso `REJECTED` o `CLOSED`.
- En otra convocatoria de la misma edición, todas las solicitudes anteriores deben estar `REJECTED` o `CLOSED`. `DRAFT`, `SUBMITTED`, `IN_VALIDATION`, `PENDING_DOCUMENTATION`, `IN_EVALUATION`, `IN_VISIT`, `APPROVED` y `WAITLISTED` bloquean una nueva.
- Las solicitudes de otras ediciones no bloquean por esta regla. La evaluación de incompatibilidades pertenece a historias futuras.
- El cupo agotado no impide presentar ni se incrementa `currentEnrollment`.
- Se guarda `SUBMITTED` con las fechas asignadas por el servidor. El horario utiliza `ENROLLMENT_PERIOD_TIME_ZONE` (por defecto `America/Argentina/Buenos_Aires`), compartido con la configuración del cierre automático de convocatorias.
- `originTicketId`, resolución y trabajador asignado quedan nulos al presentar. El alta de la evaluación completa el trabajador. Las relaciones con personas utilizan siempre `User`.
- `registeredBy` es obligatorio e inmutable: en la presentación propia coincide con el titular; en la asistida referencia al administrativo autenticado. No reemplaza a `assignedWorker`.
- Ambas presentaciones comparten las reglas anteriores, sin cambiar duplicados, cupo ni incompatibilidades. Los documentos se gestionan después sobre la solicitud ya creada.

## Idempotencia y concurrencia

`Idempotency-Key` es opcional. Admite entre 1 y 128 caracteres ASCII visibles sin espacios y distingue mayúsculas. Se conserva durante la vida de la solicitud, sin vencimiento automático. La clave es un identificador técnico de petición, no una sesión.

La combinación `(user_id, idempotency_key)` es única. `user_id` siempre corresponde al titular, incluso en asistencia. Se guarda un SHA-256 del texto canónico `application:v1:<UUID de la convocatoria>`; el orden o formato del JSON no altera su significado. El titular forma parte de la clave de búsqueda; cambiar de titular utiliza otra clave independiente, aunque el administrativo sea el mismo.

| Petición | Resultado |
| --- | --- |
| Misma persona, clave y convocatoria | Devuelve la solicitud existente con `200` e `Idempotency-Replayed: true`; no genera otro número ni otra auditoría |
| Misma persona y clave, otra convocatoria | `409 APPLICATION_IDEMPOTENCY_CONFLICT` |
| Otra persona, misma clave | Clave independiente; se aplican las validaciones habituales |
| Sin clave o con una nueva, misma convocatoria ya solicitada | `409 APPLICATION_ALREADY_EXISTS_FOR_PERIOD` si la convocatoria sigue habilitada |
| Fallo de la transacción inicial | No queda solicitud ni reserva de clave; se puede reintentar |

Un reintento válido recupera la solicitud antes de volver a validar la convocatoria: también funciona después de su cierre. Se mantienen las comprobaciones de usuario y autorización. Devuelve los datos actuales de la misma solicitud; no una copia congelada del primer JSON.

La clave se comparte entre los dos flujos. Un reintento autorizado para el mismo titular, clave y convocatoria devuelve la original, aunque lo haga otro administrativo o el titular desde el flujo propio. Siempre conserva `registeredByUserId` y la auditoría original. El actor nunca forma parte del hash ni reemplaza al titular para comprobar duplicados.

Cada presentación bloquea la fila del solicitante hasta que termina la transacción. Esto serializa sus peticiones, incluso para convocatorias diferentes. Después se bloquean edición y convocatoria, en ese orden, compatible con sus operaciones administrativas. La base respalda además las restricciones únicas de usuario/convocatoria y usuario/clave.

Presentación y log `CREATE` de tipo `application` se confirman en una única transacción. Si falla la auditoría o la persistencia, no se devuelve confirmación y se revierte la solicitud. El actor del log es quien registra; el snapshot incluye `userId` y `registeredByUserId` por separado. Las claves y hashes no se incluyen en el log.

El alta de evaluación bloquea la solicitud. Solo admite `SUBMITTED`, `IN_VALIDATION`, `PENDING_DOCUMENTATION` o `IN_EVALUATION`, exige que no haya una evaluación previa y no reemplaza un trabajador diferente. Evaluación, asignación, cambio de estado y log `application_evaluation` se confirman juntos. La auditoría guarda categorías, profesional, fecha y transición, pero nunca la recomendación.

El alta y la gestión de una actuación bloquean primero la solicitud, luego responsable y titular en orden por identificador y, para modificar, la propia actuación. Las peticiones agregan al final el bloqueo de su propia fila. Se compara el estado observado antes del bloqueo para rechazar actualizaciones concurrentes obsoletas. Dentro de la transacción se revalidan autorización, etapa, asignación y agenda. La reprogramación excluye la actuación actual del cálculo de ocupación; una cancelada o finalizada deja de ocupar agenda. Actuación, petición, `Application.updatedAt` y logs se confirman juntos. `application_visit_reschedule_request` audita `CREATE`/`UPDATE` y una aprobación también audita la visita vinculada; nunca copia motivo, comentario ni lugares.

## Numeración y esquema

`id` sigue siendo UUID. `applicationNumber` se genera con la secuencia `application_number_seq`, global, desde 1, con incremento 1 y sin reinicio anual. Hibernate registra y crea esta secuencia con el esquema; no hay una migración ni un script automático de inserción de datos. Tampoco se calcula `MAX + 1`.

La secuencia garantiza unicidad y crecimiento de los valores asignados. Los números pueden tener saltos y las transacciones concurrentes pueden confirmarse en distinto orden. `SOL-000015432` es un formato de presentación, no el valor almacenado.

Al recrear la base de pruebas, iniciar el backend con la configuración habitual de generación del esquema y luego ejecutar manualmente `docs/database/init.sql`. No es necesario crear la secuencia a mano. El esquema documentado está en `docs/database/api-entities.dbml`.

Las relaciones no borran solicitudes en cascada. El ABM de usuarios rechaza eliminar a cualquier persona referenciada, incluidos solicitante y revisor de una petición de reprogramación; las FK lo respaldan. Para una PostgreSQL existente se aplican las migraciones de evaluación y visitas en orden y, después de `2026-10-17-citizen-application-visits.sql`, `2026-10-17-application-visit-reschedule-requests.sql`, que crea la tabla, checks, FK, índices, unicidad parcial y permisos.

## Permisos y pruebas manuales

El init incluye `CIUDADANO` con sus permisos propios anteriores y `applications:own:visits:reschedule-requests:create`. Los roles administrativos normales no reciben estos permisos automáticamente. `ADMIN` conserva la excepción de superusuario técnico con todos los permisos.

El init incorpora además `applications:visits:reschedule-requests:view` y `applications:visits:reschedule-requests:review` para `ADMIN`. Ningún endpoint exige un nombre literal de rol: todos revalidan el permiso del JWT contra el rol activo persistido.

Las cuentas de ejemplo `admin` y `viewer` también reciben `CIUDADANO` al ejecutar el init. En una base nueva, ambas deben seleccionar rol después del login. La gestión de roles del ABM conserva la asignación explícita existente; no se cambió para agregar roles automáticamente.

La colección `docs/postman/program-feature.postman_collection.json` incorpora la carpeta **Solicitudes propias**. Configurar un JWT con los permisos correspondientes y `enrollmentPeriodId`; ejecutar presentación, reintento, listado y detalle. La presentación guarda `applicationId`, `applicationNumber` y `applicationIdempotencyKey`. Borrar la clave antes de iniciar una presentación diferente, y conservarla para reintentar la anterior. Las peticiones negativas indican sus precondiciones.

La carpeta **Solicitudes asistidas** usa `assistedAccessToken` con permiso administrativo, `assistedApplicantUserId` y `enrollmentPeriodId`. Conserva `assistedApplicationId`, `assistedApplicationNumber`, `assistedRegisteredByUserId` y `assistedIdempotencyKey` para comprobar reintentos. Para comprobar el detalle propio del titular, configurar `assistedApplicantAccessToken`. No reutilizar la clave para otra convocatoria del mismo titular.

Las carpetas **Requisitos documentales**, **Documentos de solicitudes propias** y **Revisión documental administrativa** cubren el nuevo contrato. Configurar `documentRequirementId`, `applicationId`, `applicationDocumentId` y `applicationDocumentFile`; cambiar el rol activo antes de alternar entre operaciones ciudadanas y administrativas.

La carpeta **Evaluaciones sociales** usa `evaluationAccessToken` con los permisos de alta y consulta, y `applicationId` de una solicitud evaluable. La respuesta conserva `applicationEvaluationId`; luego permite listar con filtros, abrir el detalle completo y comprobar que un segundo alta devuelve conflicto.

La carpeta **Visitas y entrevistas sociales** conserva el flujo anterior y agrega crear petición, historial/detalle propio, bandeja/detalle administrativo, aprobación, rechazo, duplicado, conflicto de agenda, visita vencida y acceso ajeno. Usa `applicationVisitRescheduleRequestId`, `applicationVisitRequestedStartsAt` y los tokens ciudadano/trabajador con los permisos nuevos.

CORS admite `Idempotency-Key` y expone `Location` e `Idempotency-Replayed` a los orígenes ya configurados. No se modificó el frontend.

## Errores funcionales

| HTTP | Código | Causa |
| --- | --- | --- |
| 400 | `INVALID_REQUEST_BODY` | JSON inválido, UUID inválido, campo obligatorio ausente o campo extra |
| 400 | `APPLICATION_INVALID_IDEMPOTENCY_KEY` | Formato de clave inválido |
| 400 | `APPLICATION_EVALUATION_INVALID_DATA` | Categoría, recomendación o cuerpo de evaluación inválido |
| 400 | `APPLICATION_EVALUATION_INVALID_FILTER` | Rango de fechas de consulta invertido |
| 400 | `APPLICATION_VISIT_INVALID_DATA` | Tipo, fecha, duración, lugar, motivo, cuerpo o reprogramación sin cambios inválidos |
| 400 | `APPLICATION_VISIT_RESULT_INVALID_DATA` | Estado final, observaciones, conclusiones o cuerpo de resultado inválido |
| 400 | `APPLICATION_VISIT_INVALID_FILTER` | Paginación o ventana temporal de consulta inválida |
| 400 | `APPLICATION_VISIT_RESCHEDULE_REQUEST_INVALID_DATA` | Motivo, decisión, horario, comentario o cuerpo inválido |
| 400 | `APPLICATION_VISIT_RESCHEDULE_REQUEST_INVALID_FILTER` | Filtros o ventana de solicitud inválidos |
| 401 | `AUTH_UNAUTHENTICATED` / `AUTH_INVALID_TOKEN` | Falta de autenticación, token inválido o usuario inexistente/inactivo |
| 403 | `AUTH_FORBIDDEN` | Rol activo sin permiso o ya no asignado |
| 403 | `APPLICATION_ASSISTED_SELF_NOT_ALLOWED` | Se intentó usar la asistencia para uno mismo, en lugar de la presentación propia |
| 404 | `APPLICATION_NOT_FOUND` | Solicitud inexistente o ajena |
| 404 | `APPLICATION_EVALUATION_NOT_FOUND` | Evaluación inexistente |
| 404 | `APPLICATION_VISIT_NOT_FOUND` | Visita o entrevista inexistente |
| 404 | `APPLICATION_VISIT_RESCHEDULE_REQUEST_NOT_FOUND` | Petición inexistente, ajena o combinada con otra actuación |
| 409 | `APPLICATION_EVALUATION_ALREADY_EXISTS` | La solicitud ya tiene una evaluación |
| 409 | `APPLICATION_EVALUATION_STATUS_NOT_ALLOWED` | El estado de la solicitud no admite evaluación |
| 409 | `APPLICATION_EVALUATION_ASSIGNED_TO_ANOTHER_WORKER` | La solicitud está asignada a otro trabajador |
| 409 | `APPLICATION_VISIT_EVALUATION_REQUIRED` | La solicitud no tiene evaluación social |
| 409 | `APPLICATION_VISIT_STATUS_NOT_ALLOWED` | La solicitud no está en evaluación o visita |
| 409 | `APPLICATION_VISIT_ASSIGNED_TO_ANOTHER_WORKER` | El actor no es el trabajador asignado |
| 409 | `APPLICATION_VISIT_NOT_MODIFIABLE` | La actuación está cancelada, comenzó o la solicitud ya no está en visita |
| 409 | `APPLICATION_VISIT_RESULT_NOT_REGISTRABLE` | La actuación no está programada, todavía no terminó, ya tiene resultado o la solicitud no está en visita |
| 409 | `APPLICATION_VISIT_RESCHEDULE_REQUEST_NOT_ALLOWED` | La actuación propia no está programada, futura y en visita |
| 409 | `APPLICATION_VISIT_RESCHEDULE_REQUEST_ALREADY_PENDING` | Ya existe una petición pendiente para la actuación |
| 409 | `APPLICATION_VISIT_RESCHEDULE_REQUEST_NOT_REVIEWABLE` | La petición ya fue decidida o no admite aprobación |
| 409 | `APPLICATION_VISIT_RESCHEDULE_REQUEST_PENDING` | Una pendiente bloquea cambios directos sobre la actuación |
| 409 | `APPLICATION_VISIT_CONCURRENT_MODIFICATION` | Otra operación modificó la actuación o su solicitud |
| 409 | `APPLICATION_VISIT_RESPONSIBLE_CONFLICT` | La agenda del responsable se superpone |
| 409 | `APPLICATION_VISIT_APPLICANT_CONFLICT` | La agenda del titular se superpone |
| 404 | `APPLICATION_USER_NOT_FOUND` | El titular indicado en la presentación asistida no existe |
| 404 | `APPLICATION_ENROLLMENT_PERIOD_NOT_FOUND` | Convocatoria inexistente |
| 409 | `APPLICATION_ENROLLMENT_PERIOD_NOT_OPEN` | Convocatoria no abierta |
| 409 | `APPLICATION_OUTSIDE_ENROLLMENT_PERIOD` | Fecha fuera de la convocatoria |
| 409 | `APPLICATION_PROGRAM_EDITION_NOT_ACTIVE` | Edición no activa |
| 409 | `APPLICATION_ALREADY_EXISTS_FOR_PERIOD` | Ya existe una solicitud en esa convocatoria |
| 409 | `APPLICATION_ALREADY_EXISTS_FOR_EDITION` | Solicitud anterior de la edición aún bloqueante |
| 409 | `APPLICATION_IDEMPOTENCY_CONFLICT` | Clave utilizada con otro payload |
| 409 | `USER_HAS_APPLICATION_REFERENCES` | Intento de eliminar un usuario vinculado a solicitudes |
| 400 | `APPLICATION_DOCUMENT_EMPTY_FILE` / `APPLICATION_DOCUMENT_INVALID_FILE_NAME` / `APPLICATION_DOCUMENT_INVALID_FILE_TYPE` | Archivo vacío o inconsistente |
| 413 | `APPLICATION_DOCUMENT_FILE_TOO_LARGE` | Archivo mayor a 10 MB |
| 404 | `APPLICATION_DOCUMENT_NOT_FOUND` / `PROGRAM_DOCUMENT_REQUIREMENT_NOT_FOUND` | Entrega o requisito inexistente, ajeno o de otra edición |
| 409 | `APPLICATION_DOCUMENTS_FINALIZED` | Intento de modificar documentos de una solicitud aprobada, rechazada o cerrada |
| 409 | `APPLICATION_DOCUMENT_INVALID_REVIEW_STATUS` | Estado destino inválido o entrega que ya no está pendiente |
| 409 | `APPLICATION_DOCUMENT_OBSERVATION_REQUIRED` / `APPLICATION_DOCUMENT_OBSERVATION_NOT_ALLOWED` | Observación inconsistente con el resultado |
| 409 | `PROGRAM_DOCUMENT_REQUIREMENTS_LOCKED` | Intento de cambiar el catálogo después de la primera solicitud |
| 409 | `PROGRAM_DOCUMENT_REQUIREMENT_CODE_EXISTS` | Código documental repetido dentro de la edición |

## Verificación automatizada

`ApplicationVisitFlowTest` cubre además alta e historial de peticiones, exposición diferenciada, aprobación/rechazo, solicitudes sucesivas, bloqueos, agenda, seguridad, concurrencia, rollback y exclusión de motivo, comentario y lugares en auditoría.

Verificación del 17 de octubre de 2026: `compileKotlin`, `compileTestKotlin`, `bootJar`, `generateOpenApiDocs` y la suite completa de 268 pruebas finalizan correctamente. La colección Postman contiene 124 requests, incluidos los flujos de solicitudes de reprogramación, y se valida como JSON. En esta ruta que contiene `año`, las tareas de prueba requieren fijar la codificación del worker como muestra el comando siguiente; sin esa opción el worker puede fallar antes de cargar las clases.

```powershell
.\gradlew.bat compileKotlin compileTestKotlin bootJar
.\gradlew.bat test generateOpenApiDocs --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
```

El perfil de pruebas no usa las credenciales ni la base de la aplicación.

Para repetir la misma suite en PostgreSQL, la tarea `testApplicationsPostgres` usa exclusivamente `127.0.0.1:55439/application_test`, con usuario `application_test` y contraseña de `APPLICATION_TEST_POSTGRES_PASSWORD`. Debe ser una base desechable: la tarea crea y elimina su esquema. Nunca apunta a `DB_URL`.

```powershell
.\gradlew.bat testApplicationsPostgres --offline '-Dorg.gradle.jvmargs=-Dfile.encoding=windows-1252'
```
