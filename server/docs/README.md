# Guía funcional de programas y solicitudes

Esta guía explica cómo el municipio organiza su oferta de programas sociales, habilita convocatorias y recibe solicitudes de las personas interesadas. Está pensada para alguien que no conoce el proyecto y no requiere conocimientos técnicos.

Describe el funcionamiento disponible actualmente. Cuando una etapa todavía no está disponible, se indica expresamente. Los recorridos explican acciones de los usuarios sin presuponer nombres de botones ni un diseño particular de pantallas.

## Por dónde empezar

| Lectura | Qué vas a entender |
| --- | --- |
| [Program — Programas y ediciones](functional/program.md) | Qué ofrece el municipio, cómo configura cada edición, sus beneficios, requisitos, documentos, cupos y estados. |
| [EnrollmentPeriod — Convocatorias](functional/enrollmentperiod.md) | Cuándo se pueden recibir solicitudes y cómo se abre, pausa, retoma o cierra una convocatoria. |
| [Application — Solicitudes](functional/application.md) | Cómo una persona presenta y consulta su solicitud, entrega documentos protegidos, consulta sus visitas y cómo el municipio evalúa y gestiona esas actuaciones. |
| [Activity — Actividades comunitarias](functional/activity.md) | Cómo el personal autorizado crea, publica y cierra actividades comunitarias. |
| [Flujo completo de solicitudes](functional/application-flow.md) | Recorrido paso a paso de administración, presentación ciudadana, carga de archivos y revisión; incluye cómo definir la documentación requerida. |
| [Appointment — Turnos](functional/appointments.md) | Cómo un ciudadano consulta horarios disponibles y confirma su turno de salud comunitaria. |
| [Otorgamiento de beneficios](functional/benefit-grants.md) | Registro de beneficios activos y su vigencia para solicitudes aprobadas. |

## Organización de `/docs`

| Carpeta | Contenido |
| --- | --- |
| `functional/` | Guías de negocio y recorridos de programas, convocatorias, solicitudes y documentos. |
| `technical/` | Contratos, seguridad, decisiones técnicas y verificación del backend. |
| `database/` | Modelo DBML y datos iniciales para recrear el entorno de pruebas. |
| `postman/` | Colección de Postman del backend. |

Recursos principales:

- [Autenticación con cookies y Bearer](technical/cookie-auth.md)
- [Contrato técnico de solicitudes y documentos](technical/solicitudes-beneficios.md)
- [Análisis de seguridad y preparación para Ciudadanos](technical/seguridad-ciudadanos-analisis.md)
- [Diagrama de entidades del seguimiento social](database/social-development-entity-diagram.md)
- [Modelo de datos](database/api-entities.dbml)
- [Datos iniciales](database/init.sql)
- [Colección de Postman](postman/program-feature.postman_collection.json)

El orden recomendado es programa, convocatoria y solicitud. Primero se prepara la oferta, luego se habilita su recepción y finalmente las personas se presentan.

## Cuatro conceptos que no conviene confundir

| Concepto | Qué representa | Ejemplo ilustrativo |
| --- | --- | --- |
| Programa | La iniciativa municipal y su objetivo general. | Apoyo Alimentario. |
| Edición del programa | Una realización concreta, con fechas, cupo, beneficios y requisitos propios. | Edición 2026, de marzo a noviembre. |
| Convocatoria o período de inscripción | Una ventana de recepción de solicitudes dentro de esa edición. | Primera convocatoria, del 1 al 15 de marzo. |
| Solicitud | La presentación de una persona para esa convocatoria. | La solicitud número 15432 de Ana. |

Un programa puede tener varias ediciones. Una edición puede tener varias convocatorias, que no se superponen entre sí. Una convocatoria puede recibir muchas solicitudes de distintas personas.

El beneficio es la ayuda ofrecida por una edición, por ejemplo asistencia alimentaria. Presentar una solicitud no significa que esa ayuda ya haya sido concedida.

## Quién participa

**Personal municipal autorizado.** Crea y mantiene los programas, configura sus ediciones y administra las convocatorias. Las acciones que puede realizar dependen de las autorizaciones de su rol.

**Administrativo autorizado para asistencia.** Puede registrar una solicitud para otra persona ya existente en el sistema, sin restricción de jurisdicción. Quedan identificados tanto el titular como quien realizó el registro. El acceso posterior a documentos ajenos requiere permisos administrativos documentales separados.

**Persona solicitante.** Ingresa con su cuenta, consulta la oferta y, cuando opera con un rol habilitado, presenta y consulta sus propias solicitudes, entrega sus documentos protegidos, revisa sus visitas o entrevistas y puede pedir su reprogramación con motivo. No presenta solicitudes a nombre de otra persona.

**Trabajador social autorizado.** Registra la evaluación, queda asignado a la solicitud, programa sus visitas o entrevistas sin superponer agendas y registra su resultado una vez finalizadas.

Una misma persona puede tener funciones municipales y ciudadanas. Debe operar con el rol adecuado para la tarea que desea realizar; tener varios roles no mezcla sus atribuciones.

**Sistema.** Comprueba las condiciones de presentación, confirma las solicitudes, evita duplicados y cierra automáticamente las convocatorias cuya fecha final ya pasó.

## Recorrido completo

La etapa de decisión se documenta en [Resolución de solicitudes](functional/application-resolution.md).

1. El personal municipal crea el programa y describe su objetivo.
2. Crea una edición con fechas y cupo máximo. La edición comienza en borrador.
3. Configura los beneficios, requisitos generales y documentos solicitados. También puede registrar incompatibilidades entre programas.
4. Activa la edición.
5. Crea una convocatoria dentro de las fechas de esa edición. Inicialmente queda programada.
6. Cuando llega el período previsto, abre la convocatoria de manera explícita.
7. La persona interesada ingresa, consulta el programa y elige una convocatoria abierta y vigente.
8. Presenta su solicitud. El sistema comprueba las condiciones y las solicitudes previas de esa persona.
9. Si la presentación es válida, recibe un número único, una fecha y el estado **Presentada**.
10. La persona consulta sus solicitudes, entrega documentos faltantes y reemplaza los observados. El personal documental autorizado puede revisarlos.
11. Un profesional autorizado registra vulnerabilidad, prioridad y recomendación; la solicitud queda asignada y pasa a evaluación. El personal con permiso de consulta administra luego la bandeja filtrada y el detalle.
12. Ese mismo trabajador programa una visita domiciliaria o entrevista futura. La primera actuación lleva la solicitud a **En visita** y queda visible en una agenda administrativa filtrable.
13. Cuando termina la actuación, su responsable registra si fue realizada o no realizada, junto con observaciones o conclusiones opcionales. Si no quedan otras pendientes, la solicitud vuelve a evaluación.
14. El personal autorizado resuelve la evaluación: aprueba, rechaza o envía a lista de espera, conservando motivo, responsable e historial.
15. Para una solicitud aprobada, el administrativo puede otorgar cada beneficio de su edición con vigencia actual. Queda activo, conserva los datos otorgados y no vuelve a ocupar cupo.
14. La persona titular consulta el horario, lugar, modalidad y estado de las actuaciones de su solicitud, incluida su última reprogramación o cancelación, sin acceder a notas sociales internas.
15. Si no puede asistir, solicita la reprogramación con un motivo; la actuación no cambia hasta que el responsable aprueba un nuevo horario, duración y lugar, o rechaza la petición con un comentario.

La implementación actual incluye presentación, consulta, revisión de documentos, evaluación social, gestión de visitas o entrevistas y solicitudes ciudadanas de reprogramación. La aprobación o rechazo de la solicitud social, la lista de espera y la incorporación efectiva corresponden a etapas posteriores.

## Tres decisiones diferentes

| Situación | Qué significa | Qué no garantiza |
| --- | --- | --- |
| La edición está activa y el programa aparece en el catálogo. | La oferta puede consultarse. | Que haya una convocatoria abierta en ese momento. |
| La convocatoria está abierta, vigente y pertenece a una edición activa. | Se pueden intentar nuevas presentaciones. | Que una persona pueda repetir una solicitud que ya tiene. |
| La solicitud quedó presentada. | El municipio recibió esa presentación. | Que la persona haya sido aprobada, incorporada o haya recibido el beneficio. |

El cupo máximo corresponde a incorporaciones efectivas. Alcanzarlo no cierra automáticamente una convocatoria ni impide recibir solicitudes.

## Cómo leer los estados

Cada objeto tiene sus propios estados. Una edición cerrada, una convocatoria cerrada y una solicitud cerrada representan situaciones distintas.

Las guías muestran el nombre en español y, entre paréntesis, el nombre con el que se identifica ese estado en el sistema. Las tablas de transiciones indican únicamente los cambios disponibles hoy. No hay que deducir que todos los estados pueden conectarse entre sí.
