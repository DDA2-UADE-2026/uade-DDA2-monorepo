# Planes de intervención y seguimiento social

Cada solicitud representa un caso y permite un único plan de intervención. El ciudadano se obtiene de la solicitud; no se elige otro titular al crear el plan.

El trabajador asignado crea el plan y registra todos sus seguimientos. La consulta completa está disponible para personal con permiso profesional de lectura, aunque no sea el responsable. Las respuestas ciudadanas conservan su contrato y no incluyen planes ni información profesional.

## Creación del plan

Se exige evaluación profesional registrada y trabajador asignado. Se admiten solicitudes en `IN_EVALUATION`, `IN_VISIT`, `WAITLISTED`, `APPROVED`, `REJECTED` y `CLOSED`. El seguimiento social puede comenzar o continuar después de resolver el trámite.

El plan contiene al menos un objetivo y cada objetivo al menos una acción. Las descripciones se recortan y deben tener de 1 a 1000 caracteres. Se conserva el orden recibido.

Cada acción tiene inicio `startDate` y fin previsto `dueDate`, inclusivos, con inicio no posterior al fin. Se admite planificación pasada o futura, independiente de las fechas de programas o beneficios. Las acciones comienzan en `PENDING` y quedan a cargo del responsable autenticado, que debe coincidir con el trabajador asignado.

La estructura inicial, sus fechas y su responsable quedan fijos. Esta historia no incorpora edición, eliminación ni reasignación.

## Seguimientos y resultados

Cada entrada pertenece a una acción y conserva fecha real, notas, resultado, autor, instante UTC de registro y estado anterior/resultante.

| Estado actual | Estados admitidos |
| --- | --- |
| `PENDING` | `IN_PROGRESS`, `COMPLETED` |
| `IN_PROGRESS` | `IN_PROGRESS`, `COMPLETED` |
| `COMPLETED` | Ninguno |

Las notas son obligatorias y tienen de 1 a 2000 caracteres tras recortar. El resultado es obligatorio al completar; es opcional en avances y, si se informa, debe cumplir el mismo rango. Completar es definitivo y no admite reapertura ni nuevas notas.

La fecha real `occurredOn` debe estar entre el inicio de la acción y hoy en la zona configurada. No puede retroceder respecto al último seguimiento de esa acción. Se permiten varios registros del mismo día, cargas históricas anteriores a la creación del plan y seguimientos posteriores al fin previsto.

El historial queda disponible en secuencia ascendente. Los registros anteriores no se modifican ni eliminan.

## API y permisos

| Método y ruta | Resultado |
| --- | --- |
| `POST /api/applications/{id}/intervention-plan` | `201`, plan creado |
| `GET /api/admin/applications/{id}/intervention-plan` | `200`, estructura y estados actuales |
| `POST /api/applications/{id}/intervention-plan/actions/{actionId}/follow-ups` | `201`, seguimiento registrado |
| `GET /api/admin/applications/{id}/intervention-plan/actions/{actionId}/follow-ups` | `200`, historial de la acción |

El alta requiere `applications:interventions:create`, los seguimientos `applications:interventions:follow-ups:create` y las consultas `applications:interventions:view`. Los permisos se asignan inicialmente a ADMIN y pueden configurarse en los roles existentes. Se valida usuario activo y permiso tanto en JWT como en el rol activo vigente de base de datos.

Cuerpo de creación:

```json
{
  "objectives": [
    {
      "description": "Fortalecer la autonomía del ciudadano.",
      "actions": [
        {
          "description": "Realizar acompañamiento para acceder a recursos.",
          "startDate": "2026-10-19",
          "dueDate": "2026-11-19"
        }
      ]
    }
  ]
}
```

Cuerpo de seguimiento:

```json
{
  "status": "IN_PROGRESS",
  "occurredOn": "2026-10-19",
  "notes": "Se realizó el primer acompañamiento.",
  "result": null
}
```

No se admiten campos adicionales en ningún nivel. Los identificadores, ciudadano, responsable/autor y fechas de registro los obtiene el servidor. El detalle contiene identificadores del plan, solicitud, ciudadano, objetivos y acciones; responsable, creación UTC, descripciones, vigencias previstas y estados actuales.

El historial agrega secuencia, estados anterior/resultante, fecha real, notas, resultado, autor y registro UTC. Una acción existente sin seguimientos devuelve `[]`.

| Código | Situación |
| --- | --- |
| `400` | Cuerpo, textos, fechas, identificadores o estado de entrada inválidos; campos adicionales. |
| `401` / `403` | Acceso inválido, usuario inactivo o permiso ausente de JWT/rol activo vigente. |
| `404` | Solicitud, plan o acción inexistentes o ajenos a la ruta. |
| `409` | Plan duplicado, falta de evaluación/asignación, otro trabajador, etapa no admitida o acción completada. |

## Conservación y límites

Crear un plan o seguimiento actualiza únicamente `Application.updatedAt` del caso. Se conserva su estado, resolución, trabajador, visitas, beneficios y cupos. Completar todas las acciones tampoco cierra la solicitud.

Estado de acción, seguimiento, fechas y auditoría se confirman en una transacción; cualquier fallo revierte todo. Los logs generales conservan identificadores, responsables, fechas y transiciones, sin descripciones, notas ni resultados. Se impide eliminar responsables y autores referenciados.

La migración no genera planes para casos anteriores. No se incorporan ciclo independiente de estados del plan, adjuntos, reservas de agenda, notificaciones ni integración con otros módulos.
