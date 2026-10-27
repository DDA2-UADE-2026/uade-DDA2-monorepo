# Consulta de beneficios propios

El ciudadano autenticado puede consultar todos los beneficios que le fueron otorgados, reuniendo las solicitudes de cualquier programa o edición. Se incluyen `ACTIVE`, `SUSPENDED`, `FINALIZED` y `EXPIRED`, también cuando la solicitud está cerrada o la edición ya no está activa.

Una solicitud aprobada no equivale a un beneficio otorgado: si todavía no tiene otorgamientos no agrega elementos al listado. Cuando la solicitud fue registrada por un administrativo, los beneficios corresponden a su titular y no al registrante.

## Consulta

`GET /api/benefit-grants?page=0&size=20`

El titular se obtiene de la autenticación. La API no permite elegir otro ciudadano. Requiere `applications:own:benefits:view` en el rol activo del JWT y en ese mismo rol vigente en la base de datos, además de usuario activo y pertenencia al rol. El permiso se asigna inicialmente a `CIUDADANO` y `ADMIN`, y puede configurarse en otros roles. Siempre permite consultar únicamente beneficios propios.

Cada elemento muestra:

- Identificador del otorgamiento y del beneficio del catálogo.
- Solicitud y número de solicitud, programa y edición con sus identificadores y nombres.
- Tipo, descripción y monto copiados al otorgar; descripción y monto pueden ser `null`.
- Estado actual y fechas inclusivas `startDate` y `endDate`.

Los cambios posteriores del catálogo no cambian la descripción, tipo ni monto otorgados. Los nombres del programa y edición se toman de su configuración actual. No se publican responsables, motivos administrativos, evaluaciones, documentos ni historial de cambios.

## Páginas y ausencia de beneficios

El resultado contiene `content`, `page`, `size`, `totalElements`, `totalPages` y `message`. La página comienza en cero, el tamaño predeterminado es 20 y se permiten tamaños entre 1 y 100. Los otorgamientos se ordenan por fecha de asignación descendente y, ante igualdad, identificador descendente.

Cuando el titular no tiene beneficios asignados:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "message": "No tenés beneficios asignados."
}
```

Si tiene beneficios, `message` es `null`. Pedir una página fuera de rango devuelve `content: []`, los totales reales y `message: null`; no significa que el ciudadano carezca de beneficios.

| Código | Situación |
| --- | --- |
| `200` | Consulta correcta, con resultados o sin ellos. |
| `400` | Página negativa, tamaño fuera de 1–100 o parámetros de paginación con formato inválido. |
| `401` | Sin autenticación válida o usuario inexistente/inactivo. |
| `403` | Permiso ausente del JWT o del rol activo vigente, o rol ya no asignado al usuario. |

## Cambios de estado y vigencia

Cada consulta vuelve a leer los datos persistidos. Suspender muestra `SUSPENDED`; renovar a tiempo muestra `ACTIVE` con el nuevo fin; finalizar muestra `FINALIZED`; el proceso automático muestra `EXPIRED` cuando confirma el vencimiento. Un beneficio terminado permanece visible aunque la solicitud haya pasado a `CLOSED`.

La consulta no fuerza vencimientos ni modifica fechas, solicitud, reservas, historial o auditoría. Si todavía no se ejecutó el barrido, el estado puede seguir `ACTIVE`/`SUSPENDED` después de `endDate`; esa demora no extiende la vigencia. No hay estados calculados distintos de los guardados por las actuaciones administrativas y automáticas.

Las consultas existentes de solicitudes y la API administrativa mantienen sus contratos. Esta historia agrega solamente el listado general propio; no incorpora detalle individual, consulta por solicitud, filtros, historial ciudadano ni cambios de frontend.

Antes del despliegue, ejecutar `docs/database/migrations/2026-10-26-citizen-benefit-grants.sql`. La migración es repetible y solo agrega el permiso y sus asignaciones iniciales; no crea beneficios ni modifica datos de otorgamiento. Después de incorporar el permiso hay que iniciar sesión o seleccionar nuevamente el rol para obtener un JWT que lo incluya.
