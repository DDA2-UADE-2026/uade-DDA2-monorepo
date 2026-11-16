# 02. Programas, convocatorias y solicitudes

[Índice](README.md) · [Anterior](01-responsabilidad-y-acceso.md) · [Siguiente: evaluación y resolución](03-evaluacion-visitas-y-resolucion.md)

## 1. Preparar la oferta social

| Paso | Actor y acción | Resultado o control | Evento externo |
| --- | --- | --- | --- |
| 1 | Personal autorizado crea un programa. | Nombre único y objetivo general; portada opcional JPEG/PNG de hasta 10 MB. | Sin evento externo. |
| 2 | Crea una edición del programa. | Nombre dentro del programa, fechas y cupo positivo; comienza `DRAFT` con cero incorporaciones. | Sin evento externo. |
| 3 | Configura beneficios de la edición. | Tipos `TAX_EXEMPTION`, `HOUSING_SUBSIDY`, `FOOD_ASSISTANCE` y `UTILITY_SUBSIDY`; descripción y monto opcionales. | Sin evento externo: todavía no existe un otorgamiento. |
| 4 | Define requisitos generales. | Edad mínima, ingreso máximo, residencia e hijos; se conservan condiciones para el control profesional. | Sin evento externo. |
| 5 | Define requisitos documentales. | Código, nombre, explicación y obligatoriedad; con negocio Core habilitado se limita a diez por edición. | Sin evento externo. |
| 6 | Registra incompatibilidades entre programas. | Relación simétrica; no se admite consigo mismo ni repetir la pareja. | Sin evento externo. |
| 7 | Activa la edición. | `DRAFT` o `SUSPENDED` pasa a `ACTIVE`. Abrir una convocatoria sigue siendo una acción separada. | Sin evento externo. |
| 8 | El ciudadano consulta la oferta autenticada. | Ve ediciones activas no vencidas, requisitos, beneficios, convocatorias y disponibilidad. Una edición puede mostrarse antes de su fecha inicial. | Sin evento externo. |

Los requisitos generales configurados no calculan automáticamente edad, ingresos o grupo familiar del solicitante. Su control se declara en la resolución y renovación. Las incompatibilidades tampoco impiden por sí solas presentar una solicitud: intervienen en las comprobaciones de concesión correspondientes.

El catálogo documental de una edición queda bloqueado desde su primera solicitud. Los cambios de catálogo de beneficios no alteran las copias ya guardadas en otorgamientos anteriores.

### Estados de la edición

| Origen | Acción | Destino | Condición relevante |
| --- | --- | --- | --- |
| `DRAFT` | Activar | `ACTIVE` | No abre convocatorias automáticamente. |
| `ACTIVE` | Suspender | `SUSPENDED` | No puede quedar una convocatoria abierta de la edición. |
| `SUSPENDED` | Activar | `ACTIVE` | No reabre convocatorias. |
| `DRAFT`, `ACTIVE`, `SUSPENDED` | Cerrar | `CLOSED` | No puede haber convocatoria abierta. Cierre definitivo. |

Editar fechas debe conservar dentro del rango las convocatorias y vigencias relacionadas. El cupo no puede quedar por debajo de las incorporaciones registradas. Las eliminaciones están sujetas a dependencias; cerrar conserva la historia. La fecha final no cambia automáticamente el estado de la edición.

## 2. Abrir una convocatoria

| Paso | Acción | Regla y efecto | Evento externo |
| --- | --- | --- | --- |
| 1 | Crear período de inscripción. | Pertenece a una edición y comienza `SCHEDULED`. | Sin evento externo. |
| 2 | Validar fechas. | Inicio y fin inclusivos, contenidos en la edición y sin superposición con otros períodos de esa edición, incluso cerrados. | Sin evento externo. |
| 3 | Abrir. | `SCHEDULED -> OPEN`, edición activa, fecha actual dentro del rango y sin otra convocatoria abierta de la edición. | Sin evento externo. |
| 4 | Suspender, si corresponde. | `OPEN -> SUSPENDED`; detiene nuevas presentaciones. | Sin evento externo. |
| 5 | Reabrir. | `SUSPENDED -> OPEN` si conserva las condiciones de apertura. | Sin evento externo. |
| 6 | Cerrar manualmente. | `CLOSED` es terminal. No modifica solicitudes existentes. | Sin evento externo. |
| 7 | Procesar vencimientos. | El sistema cierra convocatorias vencidas. Aunque el barrido no haya corrido, la validación por fecha impide nuevas solicitudes vencidas. | Sin evento externo. |

La apertura exige intervención municipal: llegar a la fecha no abre el período. Agotar el cupo de la edición tampoco cierra automáticamente la convocatoria ni impide recibir más solicitudes.

## 3. Presentación propia o asistida

Ambos caminos usan las mismas reglas de titular, edición, convocatoria y duplicados.

| Paso | Presentación propia | Presentación asistida | Evento asociado |
| --- | --- | --- | --- |
| 1. Identificar al actor | El titular se obtiene de su sesión autorizada. | El administrativo autenticado indica un `userId` existente; no crea una identidad municipal. | Sin evento externo. |
| 2. Elegir convocatoria | Selecciona la convocatoria de interés. | La selecciona en nombre del titular. | Sin evento externo. |
| 3. Validar recepción | Cuenta/permiso, convocatoria abierta y vigente, edición activa y duplicados. | Se autoriza al registrante y se aplican las reglas al titular elegido. No puede usar asistencia para solicitar para sí mismo. | Sin evento externo. |
| 4. Guardar solicitud | Se asignan UUID, número único y fecha; queda `SUBMITTED`. | Se guardan por separado `user` y `registeredBy`. El registrante no queda asignado como trabajador social por ese hecho. | Hecho interno `ApplicationSubmitted`. |
| 5. Aplicar integración M1 | Con negocio habilitado, se controlan restricciones de bloqueo/fallecimiento. Si la edición pide documentos, se exige UUID canónico y hasta diez requisitos. | Igual control para el titular. | Salida `socialDocumentationRequested`, si hay requisitos documentales. |
| 6. Confirmar | Se devuelve la solicitud persistida y puede consultarse entre las propias. | El titular también ve la solicitud asistida en sus consultas propias. | La salida queda en outbox en la misma transacción. |

La API propia es `POST /api/applications`; la asistida, `POST /api/admin/applications`. Presentar no aprueba, no reserva cupo, no asigna beneficios y no crea por sí mismo un ticket en M2.

Si la edición no pide documentos, el alta no genera `socialDocumentationRequested`. El control de vinculación canónica se exige cuando la operación necesita incluir al ciudadano en una publicación; no se inventa un UUID para una cuenta local sin vínculo.

### Duplicados e idempotencia

- Un titular solo puede tener una solicitud por convocatoria, cualquiera sea su estado.
- En otra convocatoria de la misma edición, todas sus solicitudes previas deben estar `REJECTED` o `CLOSED`.
- Otra edición se evalúa de manera independiente para esta regla.
- Una misma clave de idempotencia y contenido devuelve la presentación original; reutilizarla para otro contenido produce conflicto.
- Si el procesamiento falla, no debe quedar una solicitud parcial ni un pedido documental huérfano en outbox.

Los UUID identifican entidades. El número legible es una referencia para personas; no representa prioridad ni posición de espera.

## 4. Documentación cargada directamente en M8

| Paso | Acción | Resultado | Evento externo |
| --- | --- | --- | --- |
| 1 | Consultar requisitos y pendientes. | El titular conoce documentos obligatorios faltantes u observados. Los opcionales no se listan como faltantes obligatorios. | Sin evento externo. |
| 2 | Cargar archivo por requisito. | PDF, JPEG o PNG de hasta 10 MB; contenido privado y estado de revisión `PENDING`. | Sin evento externo. |
| 3 | Cargar en ventanilla. | Un administrativo con permiso documental carga/reemplaza para el titular; se atribuye la carga al actor real. | Sin evento externo. |
| 4 | Revisar. | Un administrativo decide `VALID` sin observación o `OBSERVED` con observación obligatoria. | Sin evento externo. |
| 5 | Reemplazar entrega. | El archivo previo se reemplaza y se limpia su revisión: vuelve a `PENDING`. | Sin evento externo. |
| 6 | Eliminar entrega propia permitida. | Solo dentro de los estados mutables; la asistencia administrativa no habilita por sí misma borrar archivos ajenos. | Sin evento externo. |

La mutación documental se restringe por estado de la solicitud. Una ya aprobada, rechazada o cerrada no admite este mantenimiento. El archivo y la revisión no cambian por sí solos el estado general de la solicitud.

Un documento obligatorio entregado pero aún `PENDING` deja de aparecer como **faltante**, aunque sigue impidiendo una aprobación por la ruta de resolución con control completo hasta ser `VALID`. Son controles distintos.

Las entregas de M1 siguen otro camino: `socialDocumentationDelivered` conserva referencias externas, no transfiere binarios. Se explica en [la integración documental](06-integraciones-paso-a-paso.md).

## 5. Consulta posterior y estados

El ciudadano consulta únicamente sus casos. La gestión administrativa agrega titular, registrante, profesional asignado, datos de resolución y pendientes según el permiso de la ruta. Tener permiso de presentación asistida no concede automáticamente lectura documental ajena.

`ApplicationStatus` enumera `DRAFT`, `SUBMITTED`, `IN_VALIDATION`, `PENDING_DOCUMENTATION`, `IN_EVALUATION`, `IN_VISIT`, `APPROVED`, `REJECTED`, `WAITLISTED` y `CLOSED`. La presentación implementada comienza en `SUBMITTED`. Que un valor exista en el enum no significa que haya una API genérica para cambiar libremente a ese estado ni que cargar un documento provoque esa transición.

## 6. Referencias de implementación

- [Servicio de solicitudes](../../src/main/kotlin/com/uade/dda2/server/feature/application/service/ApplicationService.kt).
- [Catálogo documental](../../src/main/kotlin/com/uade/dda2/server/feature/program/service/AdminProgramDocumentRequirementService.kt).
- [Puente de eventos](../../src/main/kotlin/com/uade/dda2/server/feature/integration/service/M8BusinessEventBridge.kt).
- [Programas](../functional/program.md), [convocatorias](../functional/enrollmentperiod.md) y [contrato de documentos locales](../technical/solicitudes-beneficios.md).
