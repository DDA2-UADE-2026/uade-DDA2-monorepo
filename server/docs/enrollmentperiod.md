# EnrollmentPeriod — Convocatorias o períodos de inscripción

[Inicio de la guía](README.md) · [Programas y ediciones](program.md) · [Solicitudes](application.md)

## Qué resuelve esta funcionalidad

Una convocatoria establece **durante qué período se reciben solicitudes para una edición de un programa**. En esta guía, convocatoria y período de inscripción significan lo mismo.

La edición define la oferta general para unas fechas. La convocatoria define una ventana concreta de recepción dentro de esas fechas. Por ejemplo, una edición de marzo a noviembre puede tener una convocatoria en marzo y otra en julio.

Cada convocatoria pertenece a una sola edición. No se comparte entre programas ni se cambia de edición desde este flujo.

## Quién la administra

El personal municipal autorizado puede crear, consultar y modificar convocatorias, y realizar las acciones de apertura, suspensión, reapertura y cierre.

Las personas interesadas consultan las convocatorias abiertas y vigentes desde la oferta del programa. No pueden modificar sus fechas ni cambiar sus estados.

El sistema realiza además un cierre automático de las convocatorias vencidas. La apertura siempre requiere una acción municipal.

## Crear una convocatoria

El personal municipal elige el programa y la edición, indica la fecha inicial y la fecha final, y puede agregar observaciones administrativas de hasta 1000 caracteres.

La convocatoria se crea siempre en **Programada**, incluso si la fecha inicial es hoy o ya pasó. Crear la convocatoria no la abre ni permite empezar a presentar solicitudes.

Para prepararla no se exige que la edición ya esté activa; esa condición se exige al abrir o reabrir la recepción. La validación de creación tampoco exige que las fechas sean futuras.

## Reglas de fechas

### El primer día y el último están incluidos

Una convocatoria del 1 al 15 de marzo puede recibir solicitudes tanto el día 1 como el día 15, siempre que esté abierta y la edición esté activa. El día 16 ya está fuera de su vigencia.

La fecha final no puede ser anterior a la inicial. Se permite una convocatoria de un solo día.

### Debe quedar dentro de la edición

Ningún día de la convocatoria puede quedar antes del inicio o después del final de la edición. Una edición del 1 de marzo al 30 de noviembre no admite una convocatoria que empiece en febrero o termine en diciembre.

Si luego se cambian las fechas de la edición, también deben seguir conteniendo todas sus convocatorias registradas.

### Las convocatorias de una misma edición no se superponen

La prohibición abarca todas las convocatorias de esa edición, incluso las suspendidas o cerradas.

| Primera convocatoria | Nueva convocatoria propuesta | Resultado |
| --- | --- | --- |
| 1 al 15 de marzo | 10 al 20 de marzo | No se admite: comparten varios días. |
| 1 al 15 de marzo | 15 al 20 de marzo | No se admite: ambas incluyen el día 15. |
| 1 al 15 de marzo | 16 al 20 de marzo | Se admite si cumple las demás condiciones. |

Esta regla no impide convocatorias simultáneas de ediciones diferentes.

## Estados de la convocatoria

| Estado | Qué significa | ¿Recibe nuevas solicitudes? |
| --- | --- | --- |
| Programada (SCHEDULED) | Está preparada, pero todavía no se abrió. | No, aunque ya haya llegado la fecha inicial. |
| Abierta (OPEN) | La recepción fue habilitada. | Sí, únicamente dentro de sus fechas y con la edición activa. |
| Suspendida (SUSPENDED) | La recepción está pausada. | No. Puede retomarse si todavía cumple las condiciones. |
| Cerrada (CLOSED) | La convocatoria finalizó. | No. No se reabre ni se modifica. |

Que figure como abierta no alcanza por sí solo: también se comprueba la vigencia por fecha. Una convocatoria abierta que ya venció no acepta solicitudes, aunque el cierre automático todavía no se haya reflejado en su estado.

## Transiciones manuales

| Estado actual | Acción municipal | Estado resultante | Condiciones |
| --- | --- | --- | --- |
| Programada | Abrir | Abierta | La edición está activa, hoy está dentro del rango y no existe otra convocatoria abierta de esa edición. |
| Abierta | Suspender | Suspendida | La convocatoria debe estar abierta. La pausa no cambia sus fechas. |
| Suspendida | Reabrir | Abierta | La edición está activa, hoy está dentro del rango y no existe otra convocatoria abierta de esa edición. |
| Abierta | Cerrar | Cerrada | Se puede finalizar antes de su fecha final. |
| Suspendida | Cerrar | Cerrada | Se puede finalizar sin reabrirla primero. |
| Cerrada | Ninguna transición | Permanece cerrada | No se reabre, ni siquiera para corregir o ampliar sus fechas. |

No se vuelve al estado programada. Tampoco hay una acción manual de cierre directo de una convocatoria programada, ni una acción de eliminación en este flujo.

Además de impedir fechas superpuestas, el sistema exige **como máximo una convocatoria en estado abierto por edición**. Si una anterior aún figura abierta, hay que resolver su cierre antes de abrir otra, aunque las fechas de ambas no se superpongan.

## Cierre automático por vencimiento

El sistema revisa periódicamente las convocatorias. Con la configuración habitual, lo hace cada día a las 00:05, hora de Argentina.

Cuando la fecha final ya quedó atrás, pasa a **Cerrada** cualquier convocatoria que siga en uno de estos estados:

- Programada, aunque nunca haya llegado a abrirse.
- Abierta.
- Suspendida.

Una convocatoria que termina hoy no se cierra por vencimiento antes de que termine ese día. El cierre automático se aplica cuando la fecha actual ya es posterior a su fecha final.

La recepción de solicitudes no depende de esperar esa revisión: fuera del rango de fechas ya no se admiten nuevas presentaciones. No existe una apertura automática equivalente al llegar el primer día.

## Modificar fechas y observaciones

Se pueden modificar las fechas y las observaciones mientras la convocatoria esté programada, abierta o suspendida. Una convocatoria cerrada no admite cambios.

Toda modificación debe seguir cumpliendo el orden de las fechas, el rango de la edición y la ausencia de superposición con otras convocatorias.

Modificar las fechas **no cambia automáticamente el estado**. Por ejemplo, si una convocatoria abierta se reprograma para la semana siguiente, seguirá figurando como abierta, pero no recibirá solicitudes hasta entrar en el nuevo rango. Si se necesita expresar una pausa, corresponde suspenderla.

Cambiar fechas o suspender una convocatoria no elimina las solicitudes ya presentadas. Las nuevas condiciones se aplican a los intentos posteriores.

Cerrar anticipadamente tampoco acorta las fechas registradas. Si una convocatoria del 1 al 15 se cierra el día 5, su rango sigue siendo del 1 al 15 y no permite crear otra de la misma edición que ocupe esos días. Además, al estar cerrada, ya no se puede editar ese rango.

## Cómo se coordina con la edición

Para suspender o cerrar una edición no puede quedar ninguna convocatoria suya en estado abierto.

**Pausa temporal de toda la edición:** suspender primero su convocatoria abierta y después suspender la edición.

**Reanudación:** activar primero la edición y después reabrir la convocatoria. Esto último solo es posible si la convocatoria sigue suspendida y aún está dentro de sus fechas.

**Finalización:** cerrar la convocatoria abierta antes de cerrar la edición. Si quedan convocatorias programadas o suspendidas, el cierre de la edición no cambia automáticamente sus estados; tampoco podrán abrirse o reabrirse mientras la edición esté cerrada. El cierre por vencimiento sigue aplicándose.

El estado de una convocatoria no decide si las solicitudes recibidas se aprueban o rechazan. Cerrar la recepción conserva esas solicitudes para los pasos posteriores.

## Recorridos del personal municipal

### Preparar y abrir

1. Consultar la edición y sus fechas.
2. Elegir un rango que no se superponga con otras convocatorias de esa edición.
3. Crear la convocatoria y revisar sus observaciones. Queda programada.
4. Activar la edición si aún no lo está.
5. Cuando llegue un día comprendido en la convocatoria, abrirla explícitamente.
6. Desde ese momento las personas autorizadas pueden intentar presentar solicitudes.

### Pausar y retomar

1. Consultar una convocatoria abierta.
2. Suspenderla cuando sea necesario detener la recepción.
3. Las solicitudes existentes se conservan y no se aceptan nuevas durante la pausa.
4. Para retomar, comprobar que la edición esté activa y que la convocatoria no haya vencido.
5. Reabrirla. No se crea una convocatoria diferente: las solicitudes previas siguen perteneciendo a la misma.

### Finalizar y organizar otra convocatoria

1. Cerrar manualmente una convocatoria abierta o suspendida, o dejar que venza.
2. Conservar su información y las solicitudes que recibió.
3. Para volver a recibir solicitudes, crear otra convocatoria dentro de las fechas de la edición, sin superponer rangos.
4. Abrir la nueva convocatoria cuando corresponda. La posibilidad de que cada persona vuelva a solicitar se rige por las [reglas de solicitudes](application.md).

## Ejemplo de funcionamiento

Una edición va de marzo a noviembre. Su primera convocatoria está prevista del **1 al 15 de marzo**.

| Momento | Acción o situación | Resultado |
| --- | --- | --- |
| 20 de febrero | Se crea la convocatoria. | Queda programada. No recibe solicitudes. |
| 1 de marzo | Llega la fecha inicial, sin acción municipal. | Sigue programada. Todavía no recibe solicitudes. |
| 1 de marzo | El personal abre la convocatoria con la edición activa. | Comienza la recepción. |
| 8 de marzo | Se suspende. | Se detienen nuevas presentaciones, sin borrar las anteriores. |
| 10 de marzo | Se reabre con la edición activa. | Se retoma la recepción en la misma convocatoria. |
| 15 de marzo | Último día, si sigue abierta. | Todavía acepta presentaciones válidas. |
| 16 de marzo | La fecha final ya pasó. | No admite nuevas solicitudes. El cierre automático la deja cerrada. |

Si una persona ya se presentó antes de la suspensión, la reapertura no le permite presentar otra solicitud en esa misma convocatoria.

## Qué no hace esta funcionalidad

No incorpora personas al programa, no asigna beneficios y no resuelve solicitudes. Tampoco utiliza el cupo disponible como una condición para abrir o mantener abierta una convocatoria.

Una edición con cupo agotado puede conservar su recepción abierta. Si el municipio quiere detenerla, debe utilizar la suspensión o el cierre de la convocatoria.
