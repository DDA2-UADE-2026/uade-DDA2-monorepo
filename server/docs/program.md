# Program — Programas y ediciones

[Inicio de la guía](README.md) · [Convocatorias](enrollmentperiod.md) · [Solicitudes](application.md)

## Qué resuelve esta funcionalidad

Permite que el municipio defina su oferta de programas sociales y organice cada realización del programa. Las personas interesadas pueden consultar qué se ofrece, en qué fechas, con qué beneficios y requisitos, y qué convocatorias están abiertas.

El **programa** conserva el nombre y el objetivo general. La **edición** concreta esa propuesta para unas fechas y un cupo determinados. Beneficios y requisitos pertenecen a una edición, por lo que pueden ser diferentes entre ediciones de un mismo programa.

Por ejemplo, Apoyo Alimentario puede tener una edición 2026 y otra 2027. Ambas comparten el programa, pero pueden ofrecer ayudas, exigir condiciones y tener cupos diferentes.

## Qué puede hacer cada participante

| Participante | Acciones disponibles, según sus autorizaciones |
| --- | --- |
| Personal municipal | Consultar, crear, editar y, cuando corresponda, eliminar programas y ediciones; administrar beneficios, requisitos e incompatibilidades; activar, suspender y cerrar ediciones. |
| Persona que consulta la oferta | Ver el catálogo disponible y el detalle de programas, ediciones, beneficios, requisitos, incompatibilidades y convocatorias habilitadas. Debe haber ingresado al sistema. |

Consultar un programa no presenta una solicitud ni reserva un lugar.

## Crear y mantener un programa

El personal municipal indica un **nombre obligatorio** y puede agregar un **objetivo** que explique para qué existe el programa.

Reglas del nombre:

- No puede quedar vacío y admite hasta 200 caracteres.
- No puede repetirse en otro programa.
- Cambiar mayúsculas por minúsculas o agregar espacios al principio o al final no crea un nombre diferente.

El nombre y el objetivo se pueden editar. El programa general no tiene estados de borrador, activo o cerrado: esos estados corresponden a sus ediciones.

Un programa no se puede eliminar si tiene ediciones o incompatibilidades registradas. La eliminación no sirve para borrar de una sola vez toda su historia.

## Crear una edición

Dentro de un programa existente, el personal municipal define:

| Dato | Regla |
| --- | --- |
| Nombre | Obligatorio, hasta 200 caracteres y único dentro de ese programa. Se ignoran diferencias de mayúsculas y espacios exteriores. |
| Fecha inicial | Marca el comienzo del rango de la edición. |
| Fecha final | No puede ser anterior a la inicial; ambas pueden coincidir. |
| Cupo máximo | Cantidad máxima prevista de incorporaciones; debe ser mayor que cero. |

La nueva edición comienza en **Borrador** y con **cero incorporaciones**. El mismo nombre de edición puede utilizarse en programas distintos. Actualmente no se impide que las fechas de ediciones diferentes se superpongan.

Las convocatorias de la edición deberán quedar completamente dentro de sus fechas. En el funcionamiento actual no se puede abrir una convocatoria de esa edición antes de su fecha inicial.

## Beneficios ofrecidos

Cada edición puede tener varios beneficios. El personal municipal indica el tipo y puede agregar una descripción de hasta 500 caracteres y un monto cuando corresponda.

| Tipo de beneficio | Qué representa |
| --- | --- |
| Exención impositiva | Una ayuda vinculada a impuestos. |
| Subsidio habitacional | Una ayuda vinculada a vivienda. |
| Asistencia alimentaria | Una ayuda vinculada a alimentación. |
| Subsidio de servicios | Una ayuda vinculada a servicios. |

El monto es opcional. Si se informa, puede ser cero o positivo, nunca negativo. Registrar un monto no genera pagos ni entregas de ayuda.

Los beneficios se pueden agregar, modificar y quitar mientras la edición no esté cerrada. Actualmente no se exige que haya un único beneficio de cada tipo.

## Requisitos de la edición

Los requisitos describen condiciones que se esperan de las personas postulantes. Cada uno tiene un tipo, un valor obligatorio y una descripción opcional de hasta 500 caracteres.

| Tipo de requisito | Valor admitido | Ejemplo de interpretación |
| --- | --- | --- |
| Edad mínima | Un número entero igual o mayor que cero. | Tener al menos 18 años. |
| Ingreso máximo | Un número igual o mayor que cero; admite decimales. | No superar el ingreso indicado. |
| Años de residencia | Un número entero igual o mayor que cero. | Contar con al menos 2 años de residencia. |
| Tener hijos | Una condición afirmativa o negativa. | Indicar si se exige tener hijos. |

Una edad mínima es un único umbral: no se carga como un intervalo de edades. El valor no puede quedar vacío y admite hasta 255 caracteres.

Los requisitos se pueden agregar, modificar y quitar mientras la edición no esté cerrada. No se exige actualmente que cada tipo aparezca una sola vez ni se resuelven automáticamente contradicciones entre requisitos cargados.

**Al presentar una solicitud, el sistema todavía no verifica si la persona cumple estos requisitos.** Hoy se configuran y se muestran como parte de la oferta. La comprobación de elegibilidad corresponde a una etapa posterior.

## Incompatibilidades entre programas

El personal municipal puede registrar que dos programas son incompatibles. La relación se establece entre programas completos, no entre dos ediciones específicas.

- Ambos programas deben existir.
- Un programa no puede ser incompatible consigo mismo.
- La relación funciona en ambos sentidos: si Apoyo A es incompatible con Apoyo B, la incompatibilidad también se ve al consultar Apoyo B.
- La misma pareja no se puede registrar dos veces, aunque se invierta el orden.
- La relación se puede quitar.
- Una incompatibilidad no se transmite por asociación: registrar A con B y B con C no registra automáticamente A con C.

Actualmente estas incompatibilidades se informan en el detalle del programa, pero **no provocan el rechazo automático de una nueva solicitud**. No está implementada todavía la evaluación conjunta de ayudas o incorporaciones incompatibles.

## Estados de una edición

| Estado | Qué significa | Qué permite |
| --- | --- | --- |
| Borrador (DRAFT) | La edición está en preparación. Es el estado inicial. | Configurar sus datos, beneficios, requisitos y convocatorias. No habilita presentaciones. |
| Activa (ACTIVE) | La edición está habilitada. | Recibir solicitudes si además hay una convocatoria abierta y vigente. Sus datos y configuración aún pueden editarse. |
| Suspendida (SUSPENDED) | La edición está pausada. | Mantener y ajustar su configuración, pero no recibir nuevas solicitudes. |
| Cerrada (CLOSED) | Se finalizó esa edición. | Conservar su información para consulta administrativa. No se reactiva, modifica ni elimina. |

### Transiciones disponibles

| Estado actual | Acción municipal | Estado resultante | Condición adicional |
| --- | --- | --- | --- |
| Borrador | Activar | Activa | No se exige una convocatoria abierta ni un mínimo de beneficios o requisitos cargados. |
| Borrador | Cerrar | Cerrada | No puede haber una convocatoria abierta. |
| Activa | Suspender | Suspendida | Primero deben dejar de estar abiertas sus convocatorias. |
| Activa | Cerrar | Cerrada | Primero deben dejar de estar abiertas sus convocatorias. |
| Suspendida | Activar | Activa | Las convocatorias no se reabren automáticamente. |
| Suspendida | Cerrar | Cerrada | No puede haber una convocatoria abierta. |
| Cerrada | Ninguna transición | Permanece cerrada | El cierre es definitivo dentro del flujo actual. |

No se vuelve a borrador ni se cambia al mismo estado como una nueva transición. Llegar a la fecha inicial no activa la edición automáticamente; llegar a la fecha final tampoco cambia automáticamente su estado a cerrada.

La acción de activar no exige que el día actual esté dentro de las fechas de la edición. La disponibilidad en el catálogo y la recepción de solicitudes tienen sus propias comprobaciones de fecha, explicadas en esta guía y en la de convocatorias.

Activar una edición no abre sus convocatorias. Suspenderla o cerrarla tampoco cambia automáticamente el estado de las solicitudes ya presentadas.

## Modificar y eliminar una edición

Se pueden modificar el nombre, las fechas y el cupo mientras la edición no esté cerrada.

- La fecha final debe seguir siendo igual o posterior a la inicial.
- Las nuevas fechas deben seguir abarcando todas las convocatorias existentes, incluso las cerradas.
- El cupo no puede pasar a ser cero ni negativo.
- Tampoco puede quedar por debajo de la cantidad de personas ya incorporadas.
- No existe en este flujo una acción para mover la edición a otro programa.

Una edición solo puede eliminarse si no está cerrada, no tiene beneficios, requisitos ni convocatorias y no tiene personas incorporadas. Una edición con solicitudes tampoco es eliminable: esas solicitudes están asociadas a convocatorias de esa edición.

Cerrar y eliminar son acciones distintas. El cierre conserva la edición y su historia; la eliminación solo se permite cuando no hay información relacionada que deba preservarse.

## Cupo e incorporaciones

El cupo máximo expresa cuántas personas se prevé incorporar. La cantidad de incorporaciones registra personas efectivamente admitidas al programa, no la cantidad de solicitudes recibidas.

Una edición con cupo de 100 y 100 personas incorporadas puede seguir recibiendo solicitudes si su convocatoria continúa abierta y vigente. Presentar una solicitud no aumenta las incorporaciones ni reserva un lugar.

Para dejar de recibir presentaciones, el personal municipal debe suspender o cerrar la convocatoria. No debe interpretar el cupo agotado como un cierre automático. La incorporación efectiva y su efecto sobre el cupo pertenecen a una funcionalidad posterior.

## Qué aparece en el catálogo ciudadano

Un programa aparece si tiene al menos una edición **activa cuya fecha final sea hoy o posterior**. Puede mostrarse una edición cuyo inicio todavía sea futuro. También puede mostrarse un programa sin ninguna convocatoria abierta en ese momento.

En el detalle se muestran las ediciones que cumplen esa condición, con sus beneficios, requisitos, cupo máximo, incorporaciones actuales y lugares disponibles. Los lugares disponibles nunca se muestran como una cantidad negativa. Las incompatibilidades se informan a nivel del programa.

Las convocatorias que se ofrecen para presentar solicitudes deben estar abiertas y dentro de su rango de fechas. Una edición en borrador, suspendida o cerrada no se ofrece como edición disponible para solicitar.

Una edición activa cuya fecha final ya pasó deja de aparecer en la oferta disponible, aunque su estado no haya cambiado todavía. Puede seguir consultándose administrativamente.

## Recorrido municipal habitual

1. Crear el programa y explicar su objetivo.
2. Crear una edición con fechas y cupo.
3. Revisar beneficios, requisitos e incompatibilidades que se quieran informar.
4. Activar la edición cuando la oferta esté preparada.
5. Crear y abrir una convocatoria siguiendo la [guía de convocatorias](enrollmentperiod.md).
6. Si es necesario pausar toda la edición, suspender primero la convocatoria abierta y luego la edición.
7. Para retomar, activar primero la edición y luego reabrir la convocatoria, siempre que sus fechas lo permitan.
8. Para finalizar, cerrar la convocatoria abierta y luego cerrar la edición. Una convocatoria suspendida también puede cerrarse antes de finalizar la edición.

No hace falta eliminar el programa para dejar de recibir solicitudes o para finalizar una edición.

## Ejemplo completo

El municipio crea **Apoyo Alimentario** y su **Edición 2026**, del 1 de marzo al 30 de noviembre, con cupo de 100. Informa asistencia alimentaria y una condición de ingreso máximo.

Al activar la edición, el programa puede aparecer en el catálogo. Para recibir presentaciones, el personal crea una convocatoria del 1 al 15 de marzo y la abre cuando llega ese período.

Si el 10 de marzo necesita pausar la edición, primero suspende la convocatoria. Después puede suspender la edición. Si decide retomar el 12, activa la edición y reabre la convocatoria. Si intenta hacerlo después del 15, esa convocatoria ya no puede reabrirse.

Las solicitudes que se presentaron antes de la pausa siguen existiendo. La pausa no las rechaza ni las convierte en incorporaciones.
