# Guía funcional de programas y solicitudes

Esta guía explica cómo el municipio organiza su oferta de programas sociales, habilita convocatorias y recibe solicitudes de las personas interesadas. Está pensada para alguien que no conoce el proyecto y no requiere conocimientos técnicos.

Describe el funcionamiento disponible actualmente. Cuando una etapa todavía no está disponible, se indica expresamente. Los recorridos explican acciones de los usuarios sin presuponer nombres de botones ni un diseño particular de pantallas.

## Por dónde empezar

| Lectura | Qué vas a entender |
| --- | --- |
| [Program — Programas y ediciones](program.md) | Qué ofrece el municipio, cómo configura cada edición, sus beneficios, requisitos, cupos y estados. |
| [EnrollmentPeriod — Convocatorias](enrollmentperiod.md) | Cuándo se pueden recibir solicitudes y cómo se abre, pausa, retoma o cierra una convocatoria. |
| [Application — Solicitudes](application.md) | Cómo una persona presenta y consulta su solicitud, qué confirmación recibe y cuándo puede volver a solicitar. |

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

**Persona solicitante.** Ingresa con su cuenta, consulta la oferta y, cuando opera con un rol habilitado para solicitar, presenta y consulta sus propias solicitudes. No presenta solicitudes a nombre de otra persona.

Una misma persona puede tener funciones municipales y ciudadanas. Debe operar con el rol adecuado para la tarea que desea realizar; tener varios roles no mezcla sus atribuciones.

**Sistema.** Comprueba las condiciones de presentación, confirma las solicitudes, evita duplicados y cierra automáticamente las convocatorias cuya fecha final ya pasó.

## Recorrido completo

1. El personal municipal crea el programa y describe su objetivo.
2. Crea una edición con fechas y cupo máximo. La edición comienza en borrador.
3. Configura los beneficios y requisitos que correspondan. También puede registrar incompatibilidades entre programas.
4. Activa la edición.
5. Crea una convocatoria dentro de las fechas de esa edición. Inicialmente queda programada.
6. Cuando llega el período previsto, abre la convocatoria de manera explícita.
7. La persona interesada ingresa, consulta el programa y elige una convocatoria abierta y vigente.
8. Presenta su solicitud. El sistema comprueba las condiciones y las solicitudes previas de esa persona.
9. Si la presentación es válida, recibe un número único, una fecha y el estado **Presentada**.
10. La persona puede consultar después el listado y el detalle de sus solicitudes.

La implementación actual termina en la presentación y consulta. La evaluación, aprobación, rechazo, lista de espera e incorporación efectiva corresponden a etapas posteriores que todavía no se operan desde estas funcionalidades.

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
