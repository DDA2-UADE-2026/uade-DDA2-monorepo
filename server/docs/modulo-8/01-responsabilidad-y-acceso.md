# 01. Responsabilidad, datos y acceso

[Índice](README.md) · [Siguiente: programas y solicitudes](02-programas-y-solicitudes.md)

## 1. Propósito y límites

Nuestro módulo corresponde a **Desarrollo Social, Salud Comunitaria y Beneficios**. Su unidad central de trabajo social es la solicitud de una persona a una convocatoria de un programa. Sobre ese caso se registran documentos, responsables, evaluaciones, visitas, resoluciones, beneficios y planes de intervención.

La salud comunitaria agrega centros, servicios, profesionales, agendas, turnos y constancias de atención. Las actividades reúnen talleres, campañas y jornadas con inscripción y asistencia.

| Responsabilidad de M8 | Resultado |
| --- | --- |
| Definir oferta social | Programas, ediciones, beneficios de catálogo, requisitos, incompatibilidades y convocatorias. |
| Recibir presentaciones | Solicitudes propias o asistidas, número de referencia, titular y registrante. |
| Evaluar casos | Revisión documental, vulnerabilidad, prioridad, recomendación, entrevistas y visitas. |
| Resolver y administrar ayuda | Aprobación, rechazo, espera, otorgamientos y su historial de cambios. |
| Acompañar al ciudadano | Planes, objetivos, acciones y seguimientos profesionales. |
| Organizar atención comunitaria | Centros, servicios, horarios, asignaciones profesionales y turnos. |
| Gestionar actividades | Publicación, inscripción y registro de asistencia. |
| Dar seguimiento operativo | Indicadores, auditoría, tickets derivados y contexto tributario recibido. |

M8 no liquida tributos ni aplica por su cuenta una exención en la base de Rentas. Tampoco administra el ticket central de Atención Ciudadana ni es propietario de la identidad municipal o del expediente de M1. La constancia de atención comunitaria no constituye una historia clínica.

## 2. Conceptos que se usan en todos los flujos

| Concepto | Significado y relación |
| --- | --- |
| Programa | Iniciativa general; por ejemplo, Apoyo Alimentario. |
| Edición | Una realización del programa con fechas y cupo. Sus beneficios y requisitos pueden cambiar entre ediciones. |
| Convocatoria | Ventana de recepción de solicitudes dentro de una edición. |
| Solicitud o caso | Presentación de un titular a una convocatoria. |
| Beneficio de catálogo | Prestación ofrecida por una edición. Todavía no está concedida a una persona. |
| Otorgamiento | Prestación efectivamente concedida a una solicitud aprobada; tiene UUID, vigencia, estado e historial propios. |
| Evaluación | Registro profesional de vulnerabilidad, prioridad y recomendación. |
| Visita o entrevista | Actuación agendada sobre el caso, con responsable y resultado. |
| Plan de intervención | Conjunto de objetivos y acciones de acompañamiento de una solicitud. |
| Ticket | Gestión derivada por M2. Puede existir en M8 antes de que se cree una solicitud social. |
| Expediente/documento externo | Referencia recibida de M1; conserva los identificadores de ese módulo. |
| Centro y servicio | Lugar municipal y prestación que se ofrece allí. |
| Asignación profesional | Relación de un profesional con un servicio de un centro; su disponibilidad permite generar turnos. |
| Actividad | Campaña, taller o jornada con fechas, capacidad e inscriptos. |

Tres diferencias determinan el funcionamiento: **presentar no reserva cupo**, **aprobar no crea un otorgamiento** y **entregar documentación no equivale a validarla**.

## 3. Participantes y autorización

| Participante | Trabajo habitual |
| --- | --- |
| Ciudadano | Consulta oferta, presenta y consulta lo propio, entrega archivos, consulta actuaciones y beneficios, solicita reprogramaciones, reserva turnos y se inscribe en actividades. |
| Administrativo social | Prepara oferta y convocatorias, registra presentaciones asistidas, revisa documentación, gestiona tickets y mantiene beneficios según permisos. |
| Trabajador social | Evalúa casos asignados, agenda actuaciones, registra resultados y mantiene planes/seguimientos. |
| Coordinador o responsable autorizado | Resuelve solicitudes y consulta la gestión agregada. |
| Profesional de centro | Consulta sus turnos y registra atención; con los permisos correspondientes, registra asistencia a actividades. |
| Administrador | Gestiona usuarios, roles, permisos y configuración operativa. Algunas rutas exigen expresamente el rol `ADMIN`. |
| Auditor o consultor autorizado | Consulta indicadores, historiales y logs dentro de sus permisos. |
| Sistema | Cierra convocatorias vencidas, vence otorgamientos y procesa eventos mediante Core. |

Los nombres describen funciones. En numerosas rutas manda el permiso del **rol activo**, y además pueden exigirse titularidad, asignación profesional o un rol específico. Tener un rol entre los asignados no combina automáticamente sus permisos con los del rol elegido.

## 4. Ingreso y administración de acceso

| Paso | Funcionamiento | Evento externo |
| --- | --- | --- |
| 1. Crear o mantener cuentas locales | El personal autorizado administra usuarios, roles y permisos mediante `/users`, `/roles` y `/permissions`. Se conservan referencias históricas; las dependencias pueden impedir una eliminación. | Sin evento externo. |
| 2. Iniciar sesión | La API valida credenciales locales y usuario activo. Existen login Bearer en `/auth/login` y login con cookies en `/auth/session/login`. | Sin evento externo. |
| 3. Elegir rol | Si hay varios roles, se completa la selección antes de obtener un contexto operativo. También se puede cambiar de rol. | Sin evento externo. |
| 4. Consultar sesión | `/auth/me` informa usuario, roles y permisos del contexto de acceso. Las cookies requieren el manejo CSRF previsto por la aplicación. | Sin evento externo. |
| 5. Ejecutar una acción | Los controladores y servicios comprueban las autorizaciones aplicables. Las rutas propias filtran por el usuario autenticado. | Solo se genera un evento cuando el caso de uso de los capítulos siguientes lo establece. |
| 6. Cerrar sesión con cookies | Se elimina el contexto de cookies del navegador mediante `/auth/session/logout`. | Sin evento externo. |

M1 confirmó el login de servidor a servidor y la emisión de una sesión propia de M8. El [contrato de login integrado](../technical/m1-auth-contract.md) registra las condiciones de éxito y los detalles pendientes de confirmación. El endpoint proxy y el procedimiento de vinculación todavía no están implementados. Tener un campo `externalCitizenId` no implementa por sí mismo ese procedimiento.

El token HTTP de máquina de Core es otra credencial: permite al backend consultar servicios técnicos. No reemplaza la autenticación del ciudadano ni se entrega al frontend como su sesión.

## 5. Propiedad de la información e identificadores

| Información | Propietario | Qué conserva M8 |
| --- | --- | --- |
| Identidad municipal | M1 Ciudadanos | `users.external_citizen_id` y una copia local opcional `citizen_snapshot` para usuarios ya vinculados. |
| Expedientes/documentos municipales | M1 | Referencias externas y revisión local, separadas de archivos binarios cargados en M8. |
| Ticket central, estado, prioridad y clasificación | M2 Atención Ciudadana | Snapshot recibido, gestión local, vínculo opcional con solicitud y datos de correlación. |
| Deudas y exenciones tributarias | M5 Rentas | Contexto de consulta y relación con un beneficio propio cuando corresponde. |
| Programas, casos, beneficios, planes y agendas de M8 | M8 | Entidades y reglas de negocio completas en su base independiente. |
| Catálogo de eventos, ruteo y trazabilidad técnica | M9 Core | Outbox/inbox propias y clientes de diagnóstico. |

Los IDs de negocio transmitidos por los diez eventos son UUID. El ID local de `users` es numérico y no se usa como `citizenId`: ese campo debe ser el UUID canónico de M1. Números de solicitud, ticket, expediente, DNI y CUIL son referencias o datos; no reemplazan los UUID de correlación.

Si una operación necesita publicar un ciudadano sin vinculación válida, devuelve `409 M8_CITIZEN_LINK_REQUIRED` y revierte su transacción. Los eventos `citizenUpdated` de personas desconocidas se confirman sin crear cuentas.

## 6. Lecturas, indicadores y auditoría

La consulta ciudadana oculta notas profesionales, responsables internos e historiales administrativos cuando el contrato propio no los expone. Los seguimientos de intervención solo muestran al ciudadano resúmenes publicados deliberadamente.

El tablero `/api/admin/indicators` requiere `indicators:view`. Agrega presentaciones, evaluaciones, resoluciones, documentos, visitas, cobertura, cupos, turnos y actividades. El rango de fechas aplica a movimientos; cupos y pendientes reflejan estado actual. Sin fechas utiliza los últimos 90 días y admite como máximo 1100. El filtro de programa restringe datos de solicitudes, no turnos y actividades independientes.

Los logs conservan trazabilidad de operaciones y actores. Son distintos del historial inmutable de resoluciones, cambios de beneficios o seguimientos y de la inbox/outbox de integración. Las consultas de indicadores y logs no emiten eventos de negocio.

## 7. Referencias

- [Acceso con cookies y Bearer](../technical/cookie-auth.md).
- [Entidad de usuario](../../src/main/kotlin/com/uade/dda2/server/feature/auth/entity/User.kt).
- [Política de vinculación y restricciones M1](../../src/main/kotlin/com/uade/dda2/server/feature/integration/service/M8BusinessPolicy.kt).
- [Indicadores](../../src/main/kotlin/com/uade/dda2/server/feature/indicator/controller/IndicatorController.kt).
- [Integraciones paso a paso](06-integraciones-paso-a-paso.md).
