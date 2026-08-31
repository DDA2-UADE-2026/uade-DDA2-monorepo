# Application — Solicitudes de beneficios

[Inicio de la guía](README.md) · [Programas y ediciones](program.md) · [Convocatorias](enrollmentperiod.md)

## Qué representa una solicitud

Una solicitud registra que una persona se presentó a una convocatoria de una edición de un programa social. Es su constancia de presentación, con un número propio, una fecha y un estado.

La solicitud pertenece a la persona que ingresó y la presentó. No pertenece al rol con el que ingresó: cambiar de rol no crea otra persona ni permite eludir las reglas sobre solicitudes repetidas.

**Presentar no equivale a recibir el beneficio.** La solicitud tampoco representa una incorporación efectiva ni una reserva de cupo.

Actualmente se puede presentar una solicitud y consultar su listado y detalle propios. La evaluación, la decisión municipal y la incorporación al programa todavía no se gestionan desde esta funcionalidad.

## Quién puede presentar y consultar

La persona debe haber ingresado al sistema, tener su cuenta activa y operar con un rol autorizado para la acción. El rol ciudadano es el previsto para presentar y consultar solicitudes propias.

Si una persona tiene también funciones municipales, debe elegir el rol adecuado. Tener un rol ciudadano entre sus roles asignados no habilita automáticamente todas sus acciones mientras está operando con otro rol.

La presentación siempre se hace a nombre de quien ingresó. No se puede elegir otra persona como solicitante. Las consultas de este flujo también son propias: conocer el número o la referencia de una solicitud ajena no habilita a verla.

No es obligatorio tener una cuenta vinculada con el servicio externo de Ciudadanos para usar esta funcionalidad. La integración futura con ese servicio no cambia quién es el titular de una solicitud dentro del sistema.

## Recorrido de una nueva presentación

1. La persona ingresa al sistema y opera con un rol habilitado para presentar solicitudes.
2. Consulta los programas disponibles y revisa las ediciones, los beneficios y los requisitos informados.
3. Elige una convocatoria abierta y vigente de la edición que le interesa.
4. Solicita presentar su postulación en esa convocatoria. La edición correspondiente se determina a partir de esa elección.
5. El sistema verifica la cuenta, la autorización, la convocatoria, la edición y las solicitudes previas de la misma persona.
6. Si todas las condiciones se cumplen, registra la solicitud como **Presentada** y asigna su número y fecha de presentación.
7. La persona recibe esa confirmación y puede consultar posteriormente la solicitud desde su listado propio.

La presentación actual no exige completar una evaluación social, cargar documentos ni seleccionar un trabajador municipal. Esas tareas pertenecen a etapas posteriores. Los requisitos del programa se pueden consultar, pero su cumplimiento todavía no se verifica automáticamente al presentar.

## Condiciones para que se acepte

| Condición | Explicación |
| --- | --- |
| Persona identificada y autorizada | Su cuenta está activa y el rol con el que opera permite presentar solicitudes propias. |
| Convocatoria existente | Debe haberse elegido una convocatoria del sistema. |
| Convocatoria abierta | Una programada, suspendida o cerrada no recibe nuevas presentaciones. |
| Fecha vigente | Hoy debe estar entre el primer y el último día de la convocatoria, incluyendo ambos. |
| Edición activa | La convocatoria debe pertenecer a una edición habilitada. |
| Sin solicitud anterior en esa convocatoria | Solo se admite una presentación por persona dentro de la misma convocatoria. |
| Sin otra solicitud bloqueante en esa edición | En convocatorias distintas de la misma edición se aplica la regla explicada a continuación. |

Las condiciones se comprueban cuando se intenta presentar. Haber consultado antes una convocatoria disponible no garantiza que siga abierta al confirmar la presentación.

## Cuándo se puede volver a solicitar

### Dentro de la misma convocatoria

Una persona puede tener **una sola solicitud**, cualquiera sea su estado.

Una solicitud rechazada o cerrada no permite crear otra en esa misma convocatoria. Una corrección, reconsideración o revisión debería trabajar sobre la solicitud original mediante un flujo posterior, que todavía no está disponible.

Suspender y reabrir la convocatoria tampoco habilita una segunda presentación: continúa siendo la misma convocatoria.

### En otra convocatoria de la misma edición

Se permite una nueva presentación únicamente si **todas las solicitudes anteriores de esa edición están rechazadas o cerradas** y se cumplen las demás condiciones de recepción.

| Estado de una solicitud anterior | ¿Permite solicitar en otra convocatoria de la misma edición? |
| --- | --- |
| Rechazada | Sí. |
| Cerrada | Sí. |
| Borrador | No. |
| Presentada | No. |
| En validación | No. |
| Pendiente de documentación | No. |
| En evaluación | No. |
| En visita | No. |
| Aprobada | No. |
| En lista de espera | No. |

Si existen varias anteriores, alcanza con que una siga en un estado que bloquea para impedir la nueva presentación. Que una de ellas esté cerrada no anula el efecto de otra que siga vigente para esta regla.

La convocatoria cerrada y la solicitud cerrada son cosas diferentes. **Cerrar la convocatoria no cambia el estado de sus solicitudes**. Una solicitud presentada continúa bloqueando una nueva presentación en otra convocatoria de esa edición aunque su convocatoria original haya terminado.

### En otra edición

Las solicitudes de una edición diferente no bloquean una nueva por esta regla, aunque las dos ediciones pertenezcan al mismo programa.

Actualmente tampoco se rechaza automáticamente una presentación por las incompatibilidades informadas entre programas. La evaluación de incorporaciones vigentes y de compatibilidad entre ayudas corresponde a una etapa posterior.

## Qué recibe la persona como confirmación

La confirmación incluye el **número de solicitud**, el estado **Presentada**, la **fecha y hora de presentación** y la referencia de la edición y convocatoria elegidas.

El número es único para todas las solicitudes del sistema. No empieza de nuevo para cada programa ni se reinicia cada año. Puede haber saltos entre números; eso no significa que se haya perdido una solicitud ni que el ciudadano deba volver a presentarla.

El número tampoco indica prioridad, posición en una lista de espera ni derecho a un cupo. Es una referencia para reconocer la solicitud.

Si el sistema no logra completar la presentación, no debe informar que la solicitud quedó presentada. Si se interrumpe la comunicación antes de ver la confirmación, conviene revisar el listado propio antes de interpretar que el intento falló.

## Repetir un intento no es presentar otra solicitud

Puede ocurrir que la persona pulse dos veces la acción de presentar o que la comunicación se interrumpa y haya un reintento.

El sistema distingue los reintentos identificados como parte del mismo envío de una nueva presentación independiente:

| Situación | Comportamiento |
| --- | --- |
| Se reintenta el mismo envío para la misma convocatoria y ya se había confirmado. | Recupera la solicitud existente, con el mismo número, sin crear otra. |
| Se intenta reutilizar ese mismo envío para otra convocatoria. | Se rechaza porque representa una elección diferente. |
| Se inicia otro envío para una convocatoria en la que ya existe una solicitud. | Se rechaza la nueva presentación por duplicada. La original se conserva. |
| El intento anterior no llegó a registrarse. | Se puede intentar nuevamente; se vuelven a comprobar las condiciones. |

Reconocer un reintento requiere que se conserve la referencia del mismo envío. Si esa referencia no se conserva, el sistema puede informar que ya existe una solicitud en lugar de devolver directamente la confirmación original. En ambos casos se evita crear una segunda solicitud en la misma convocatoria.

Recuperar la confirmación de una presentación ya registrada sigue siendo posible aunque la convocatoria se haya cerrado después. Eso no equivale a aceptar una solicitud nueva fuera de plazo. La persona debe seguir identificada y autorizada.

## Consultar las solicitudes propias

La persona puede abrir su listado de solicitudes. Se muestran por número descendente, primero las de numeración más alta, y puede recorrerlas por páginas.

Al elegir una, consulta su número, estado, fechas y la edición y convocatoria a las que pertenece. Una solicitud no desaparece del historial porque la convocatoria se cierre o la edición deje de ofrecerse en el catálogo.

Este listado no muestra solicitudes de otras personas. No es una bandeja municipal de evaluación ni una búsqueda de todos los expedientes del sistema.

## Estados y transiciones

### Qué transición existe hoy

El único avance disponible al operar esta funcionalidad es **completar una nueva presentación y dejarla en Presentada**. No hay guardado previo en borrador, ni acciones para editar, cancelar, eliminar, evaluar o resolver una solicitud.

Mientras no se implementen las etapas posteriores, una solicitud presentada mediante este flujo permanece presentada. Por eso, hoy una solicitud existente normalmente impedirá que esa persona solicite otra convocatoria de la misma edición.

### Estados previstos para las etapas posteriores

Los nombres siguientes ya están contemplados, pero **no constituyen una secuencia de pasos implementada**. No se han habilitado acciones de transición entre ellos y no debe suponerse que siempre se recorran en el orden de esta tabla.

| Estado | Significado funcional previsto |
| --- | --- |
| Borrador (DRAFT) | Preparación parcial, todavía sin presentación confirmada. El guardado parcial no está disponible hoy. |
| Presentada (SUBMITTED) | La presentación fue recibida. Es el estado que genera el flujo actual. |
| En validación (IN_VALIDATION) | Etapa prevista de comprobaciones iniciales. |
| Pendiente de documentación (PENDING_DOCUMENTATION) | Etapa prevista para información o documentación pendiente. |
| En evaluación (IN_EVALUATION) | Etapa prevista para analizar la solicitud. |
| En visita (IN_VISIT) | Etapa prevista para actuaciones de visita. |
| Aprobada (APPROVED) | Estado previsto para una decisión favorable. Todavía no se implementó su relación operativa con la incorporación efectiva. |
| Rechazada (REJECTED) | Estado previsto para una decisión desfavorable. Permite solicitar otra convocatoria, pero nunca repetir la misma. |
| En lista de espera (WAITLISTED) | Estado previsto para solicitudes que siguen pendientes de una oportunidad de incorporación. Bloquea una nueva en la misma edición. |
| Cerrada (CLOSED) | Estado de finalización previsto. Permite solicitar otra convocatoria, sujeto a las demás reglas. |

No se describen plazos de evaluación, motivos obligatorios de rechazo, criterios de prioridad, responsables ni pasos de reconsideración porque esas reglas aún no están definidas e implementadas en este flujo.

## Cupo y decisión posterior

Una convocatoria puede recibir más solicitudes que el cupo máximo de su edición. Por ejemplo, una edición con cupo de 100 puede haber recibido 143 solicitudes.

También puede recibir nuevas solicitudes si ya hay 100 personas incorporadas, mientras la convocatoria esté abierta y vigente y se cumplan las demás condiciones. La presentación no modifica esa cantidad de incorporaciones.

El sistema no coloca automáticamente en lista de espera ni rechaza una solicitud por cupo agotado. Si el municipio decide detener la recepción, debe suspender o cerrar la convocatoria.

## Ejemplos de decisión

| Caso | Resultado |
| --- | --- |
| Ana no tiene solicitudes de esa edición y elige una convocatoria abierta y vigente. | Puede presentar si su cuenta y rol están habilitados y la edición está activa. |
| Ana ya se presentó y vuelve a intentarlo en la misma convocatoria. | Se recupera la solicitud si es un reintento reconocido; de lo contrario se rechaza la nueva presentación por duplicada. |
| Ana tiene una solicitud rechazada y quiere repetir esa convocatoria. | No puede crear otra en la misma. |
| Ana tiene una solicitud rechazada y se abre una nueva convocatoria de esa edición. | Puede solicitar, siempre que no exista otra solicitud de esa edición en un estado que bloquee. Este caso depende de la futura etapa de resolución. |
| Bruno tiene una solicitud presentada y se abre otra convocatoria de la misma edición. | No puede presentar otra mientras la anterior siga en ese estado. |
| Carla tiene una solicitud en la edición 2026 y quiere solicitar la edición 2027. | La solicitud de 2026 no la bloquea por esta regla. Se verifican las condiciones de la nueva convocatoria. |
| La convocatoria se suspendió después de que Diego consultó el programa, pero antes de presentar. | El nuevo intento se rechaza porque la recepción ya no está abierta. |

## Qué se conserva como constancia

Cada presentación confirmada deja constancia de quién la realizó, qué número recibió, a qué convocatoria y edición corresponde, y cuándo quedó presentada. Un reintento reconocido no crea una segunda constancia de presentación.

Las pausas y cierres de recepción no borran esas solicitudes. Los cambios que afecten su evaluación o resultado deberán realizarse mediante los flujos posteriores que correspondan.
