# Colección de Postman del backend

Importar [program-feature.postman_collection.json](program-feature.postman_collection.json), en formato Postman Collection 2.1. La colección incluye autenticación, configuración de programas y convocatorias, solicitudes, documentos, evaluaciones, visitas, resolución, beneficios y planes de intervención.

La variable global `baseUrl` toma `http://localhost:8080` si no está configurada. Las variables de las historias nuevas están en la colección. Evitar variables de entorno con los mismos nombres para que no oculten los identificadores que guardan los scripts.

## Autenticación y permisos

Completar el login y, si corresponde, seleccionar el rol activo. Copiar el JWT operativo a `adminAccessToken` para las operaciones administrativas. Para crear planes y registrar seguimientos, copiar el JWT del trabajador asignado a `interventionAccessToken`; puede ser la misma persona que registró la evaluación. Un token de selección de rol no sirve para estas operaciones.

| Operación | Permiso del rol activo | Token usado |
| --- | --- | --- |
| Consultar caso, resoluciones, otorgamientos e historial de beneficios | `applications:management:view` | `adminAccessToken` |
| Resolver solicitud | `applications:management:resolve` | `adminAccessToken` |
| Otorgar beneficio | `applications:management:benefits:assign` | `adminAccessToken` |
| Renovar, suspender o finalizar beneficio | `applications:management:benefits:manage` | `adminAccessToken` |
| Crear plan | `applications:interventions:create` | `interventionAccessToken` |
| Registrar seguimiento | `applications:interventions:follow-ups:create` | `interventionAccessToken` |
| Consultar plan o seguimientos | `applications:interventions:view` | `adminAccessToken` |
| Consultar catálogos de requisitos y beneficios | `programs:management:view` | `adminAccessToken` |

Los permisos nuevos se asignan inicialmente a `ADMIN`; se pueden configurar en otros roles. El servidor valida usuario activo, pertenencia al rol activo y permiso vigente en JWT y base de datos. Tener el permiso de escritura de planes no reemplaza la exigencia de ser el trabajador asignado y responsable. La lectura profesional solo exige su permiso.

## Preparar el caso

1. Configurar `applicationId` con una solicitud existente. Para una solicitud asistida, copiar el valor de `assistedApplicationId` si se quiere continuar con ese caso.
2. Ejecutar **Resolución de solicitudes → Consultar caso y preparar variables**. Guarda `applicationProgramEditionId`, `applicationUserId` y `applicationAssignedWorkerUserId`. Al cambiar de caso limpia los identificadores, fechas y checklists del recorrido anterior; configurar las fechas después de esta consulta.
3. Ejecutar **Consultar requisitos vigentes y preparar checklists** antes de resolver o renovar. Reemplaza `resolutionRequirementChecks` y `benefitRenewalRequirementChecks` con un resultado `false` por requisito actual. Revisarlos manualmente antes de enviar la operación; la consulta no certifica el cumplimiento.

Los checklists son arrays JSON, sin comillas alrededor del array:

```json
[
  {
    "requirementId": "850e8400-e29b-41d4-a716-446655440003",
    "fulfilled": true
  }
]
```

Sustituir el identificador ilustrativo por el real. Debe existir exactamente un resultado por requisito vigente, sin duplicados ni requisitos ajenos. `[]` sirve únicamente para ediciones sin requisitos. Volver a consultar si cambió el catálogo; esa consulta vuelve a poner todos los resultados en `false`.

Los scripts omiten peticiones cuando faltan variables obligatorias o contienen el UUID vacío de ejemplo. Informan los nombres pendientes en la consola de Postman.

## Resolución de solicitudes

Ejecutar **Aprobar**, **Rechazar** o **Enviar a lista de espera** individualmente. Son alternativas: ejecutar toda la carpeta en orden sobre el mismo caso produce conflictos de estado esperados.

- Desde `IN_EVALUATION` se admiten las tres decisiones; desde `WAITLISTED`, aprobación o rechazo posterior. Se necesita evaluación profesional y ninguna visita o entrevista `SCHEDULED`.
- Todas incluyen motivo de 1 a 1000 caracteres y checklist completo. Aprobar y esperar requieren elegibilidad, documentos obligatorios `VALID`, edición `ACTIVE` no vencida y ausencia de incompatibilidades.
- Aprobar requiere cupo disponible y reserva uno. Esperar requiere cupo agotado. El rechazo admite incumplimientos de elegibilidad.
- El POST guarda `applicationResolutionId`. **Consultar historial de resoluciones** permite ver todas las decisiones, incluidos ambos registros cuando se resuelve una espera.

## Otorgamiento y mantenimiento de beneficios

1. Usar una solicitud `APPROVED` de una edición `ACTIVE`. Ejecutar **Otorgamiento de beneficios → Consultar beneficios de la edición del caso**, elegir uno y copiar su ID a `benefitId`. La colección no elige automáticamente un beneficio.
2. Configurar `benefitGrantStartDate` y `benefitGrantEndDate` en formato `AAAA-MM-DD`. Ambas deben quedar dentro de la edición e incluir hoy: `startDate <= hoy <= endDate`. Hoy usa la zona configurada del servidor, por defecto `America/Argentina/Buenos_Aires`.
3. Ejecutar **Otorgar beneficio a solicitud aprobada**. Guarda `benefitGrantId`, `benefitGrantCurrentEndDate` y un snapshot de la respuesta. Repetir con otro `benefitId` permite varios beneficios; repetir el mismo devuelve `409`. El cupo ya se reservó al aprobar.
4. Para operar sobre un otorgamiento anterior, copiar su ID a `benefitGrantId` y ejecutar **Consultar otorgamientos de la solicitud** para cargar el snapshot y fin actual.
5. En **Ciclo de vida de beneficios otorgados**, se puede suspender un beneficio `ACTIVE`, renovar uno `ACTIVE`/`SUSPENDED` o finalizar uno `ACTIVE`/`SUSPENDED`. La carpeta permite recorrer suspensión → renovación/reactivación → finalización → historial.

Para renovar, configurar `benefitRenewalEndDate` con una fecha estrictamente posterior al fin actual, igual o posterior a hoy y dentro de la edición. Revisar `benefitRenewalRequirementChecks`, los documentos y las incompatibilidades actuales. La renovación conserva inicio, identidad y datos copiados del catálogo. Debe realizarse hasta el último día de vigencia inclusive; después devuelve `409` aunque el otorgamiento todavía figure `ACTIVE` o `SUSPENDED`.

Suspender conserva la reserva. Finalizar es definitivo y conserva las fechas pactadas. Al finalizar el último otorgamiento `ACTIVE`/`SUSPENDED`, la solicitud pasa a `CLOSED` y libera una sola reserva. Consultar de nuevo el caso y sus otorgamientos permite ver el resultado; **Consultar historial del beneficio** muestra la asignación inicial y todos los cambios. Renovar y suspender no cambian el cupo.

El [vencimiento automático](../functional/benefit-grant-expiration.md) pasa a `EXPIRED` los otorgamientos `ACTIVE`/`SUSPENDED` cuya `endDate` ya pasó. Se ejecuta al iniciar y cada cinco minutos; el último día sigue siendo válido. `EXPIRED` también es definitivo y puede cerrar la solicitud y liberar la reserva si era el último pendiente. El historial incluye autor `Sistema`, ID de responsable `null` y motivo automático. Los GET de otorgamientos e historial incluyen ejemplos de estas respuestas. Para comprobarlo en un entorno de prueba se puede otorgar con fin hoy y consultar después del cambio de día y del barrido; no hay endpoint para adelantarlo.

## Beneficios propios del ciudadano

La carpeta **Beneficios propios del ciudadano** consulta `GET /api/benefit-grants?page=0&size=20` usando `citizenAccessToken`, sin depender de `applicationId`. El rol activo necesita `applications:own:benefits:view` vigente en JWT y base de datos. Después de incorporar el permiso, iniciar sesión o seleccionar nuevamente el rol para obtener un token nuevo.

Incluye ejemplos con beneficios de diferentes estados, importe nulo, ausencia de otorgamientos y página fuera de rango, además de errores de paginación y acceso. El listado muestra los datos copiados al otorgar y las fechas/estado persistidos; las actuaciones administrativas y los vencimientos aparecen en la siguiente consulta una vez confirmados. Nunca publica responsables, motivos ni historial.

Cuando `totalElements = 0`, devuelve `message: "No tenés beneficios asignados."`; si existen otorgamientos, `message` es null incluso en una página fuera de rango. El token determina siempre al titular. No hay un parámetro para consultar a otra persona.

## Planes de intervención y seguimientos

El caso debe tener evaluación profesional y trabajador asignado. Admite creación en `IN_EVALUATION`, `IN_VISIT`, `WAITLISTED`, `APPROVED`, `REJECTED` o `CLOSED`. El trabajador asignado crea el plan y será su responsable.

1. Configurar `interventionActionStartDate` y `interventionActionDueDate`, con inicio menor o igual al fin previsto. Pueden ser pasadas o futuras y no dependen de las fechas de la edición.
2. Ejecutar **Crear plan del caso evaluado**. El ejemplo tiene un objetivo y una acción; se pueden agregar más al cuerpo. Conserva su orden y comienza con acciones `PENDING`. Guarda `interventionPlanId`, `interventionObjectiveId` e `interventionActionId` de la primera acción.
3. Para trabajar sobre otra acción o sobre un plan existente, ejecutar **Consultar plan completo administrativo** y copiar el ID elegido a `interventionActionId`.
4. Configurar `interventionOccurredOn` entre el inicio de esa acción y hoy, sin retroceder respecto al seguimiento anterior. Permite varios registros del mismo día, carga histórica anterior al plan y fechas posteriores al fin previsto de la acción.
5. Ejecutar **Registrar avance de acción** o **Completar acción con resultado**. Se admiten avances repetidos mientras no esté completada. Completar requiere resultado y es definitivo. El POST guarda `interventionFollowUpId`.
6. Consultar el historial de seguimientos para ver su secuencia, estados, fecha real, notas, resultado, autor y registro UTC. Devuelve `[]` cuando todavía no hay seguimientos.

La estructura del plan y los seguimientos anteriores son inmutables. El seguimiento continúa después del rechazo o cierre del caso. Completar una acción no cierra la solicitud ni modifica sus beneficios o cupos. Las consultas ciudadanas mantienen su estructura sin incorporar información profesional.

## Ejemplos y comprobaciones

Las cuatro carpetas nuevas incluyen los 12 contratos HTTP de las historias, variantes de decisión y seguimiento, consultas preparatorias y ejemplos guardados de éxito y errores `400`, `401`, `403`, `404` y `409`. Los ejemplos guardados usan identificadores y fechas ilustrativos; las peticiones usan variables con fechas a completar para el entorno real.

Los scripts comprueban códigos de éxito, asociación con la ruta, estados, vigencias, datos conservados, fechas UTC y orden del historial. Capturan identificadores solo después de respuestas exitosas. Los ejemplos de conflicto documentan las condiciones necesarias; no se ejecutan automáticamente como pruebas negativas ni preparan esos escenarios.

Para ejecutar un recorrido, seleccionar sus peticiones en Postman. La colección completa incluye alternativas y operaciones definitivas que no forman un único recorrido lineal.
