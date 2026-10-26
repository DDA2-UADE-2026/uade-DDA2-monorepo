# Diagrama de entidades del seguimiento social

Este diagrama resume la evaluación social, los planes de intervención y seguimientos, las visitas o entrevistas, sus resultados, las solicitudes ciudadanas de reprogramación, las resoluciones y los otorgamientos de beneficios. El modelo completo y sus restricciones de base se mantienen en [`api-entities.dbml`](api-entities.dbml).

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar name
        varchar email
        boolean active
    }

    PROGRAM_EDITION {
        uuid id PK
        uuid program_id FK
        varchar name
        varchar status
    }

    ENROLLMENT_PERIOD {
        uuid id PK
        uuid program_edition_id FK
        date open_date
        date close_date
        varchar status
    }

    APPLICATION {
        uuid id PK
        bigint application_number UK
        bigint user_id FK
        bigint registered_by_user_id FK
        bigint assigned_worker_user_id FK
        bigint resolved_by_user_id FK
        uuid program_edition_id FK
        uuid enrollment_period_id FK
        varchar status
        timestamp submitted_at
        timestamp resolved_at
        timestamp updated_at
    }

    INTERVENTION_PLAN {
        uuid id PK
        uuid application_id FK,UK
        bigint responsible_user_id FK
        timestamptz created_at
    }

    INTERVENTION_OBJECTIVE {
        uuid id PK
        uuid plan_id FK
        integer position
        varchar description
    }

    INTERVENTION_ACTION {
        uuid id PK
        uuid objective_id FK
        integer position
        varchar description
        date start_date
        date due_date
        varchar status
    }

    INTERVENTION_FOLLOW_UP {
        uuid id PK
        uuid action_id FK
        integer sequence_number
        varchar previous_status
        varchar status
        date occurred_on
        varchar notes
        varchar result
        bigint author_user_id FK
        timestamptz recorded_at
    }

    APPLICATION_EVALUATION {
        uuid id PK
        uuid application_id FK,UK
        varchar vulnerability
        varchar priority
        varchar recommendation
        bigint professional_user_id FK
        timestamptz evaluated_at
    }

    APPLICATION_RESOLUTION {
        uuid id PK
        uuid application_id FK
        varchar previous_status
        varchar decision
        varchar reason
        bigint resolved_by_user_id FK
        timestamptz resolved_at
    }

    BENEFIT_GRANT {
        uuid id PK
        uuid application_id FK
        uuid program_benefit_id FK
        varchar benefit_type
        varchar description
        decimal amount
        date start_date
        date end_date
        varchar status
        bigint assigned_by_user_id FK
        timestamptz assigned_at
    }

    BENEFIT_GRANT_CHANGE {
        uuid id PK
        uuid grant_id FK
        integer sequence_number
        varchar action
        varchar previous_status
        varchar status
        date previous_start_date
        date previous_end_date
        date start_date
        date end_date
        varchar reason
        bigint responsible_user_id FK
        timestamptz changed_at
    }

    BENEFIT_GRANT_CHANGE_REQUIREMENT {
        uuid id PK
        uuid change_id FK
        uuid requirement_id
        varchar type
        varchar requirement_value
        varchar description
        boolean fulfilled
    }

    INTERVENTION_FOLLOW_UP_PUBLICATION {
        uuid id PK
        uuid follow_up_id FK,UK
        varchar summary
        boolean visible
        timestamptz published_at
        timestamptz updated_at
        bigint updated_by_user_id FK
    }

    PROGRAM_BENEFIT {
        uuid id PK
        uuid program_edition_id FK
        varchar benefit_type
        varchar description
        decimal amount
    }

    APPLICATION_RESOLUTION_REQUIREMENT {
        uuid id PK
        uuid resolution_id FK
        uuid requirement_id
        varchar type
        varchar requirement_value
        varchar description
        boolean fulfilled
    }

    APPLICATION_VISIT {
        uuid id PK
        uuid application_id FK
        varchar type
        timestamptz starts_at
        timestamptz ends_at
        varchar location
        bigint responsible_user_id FK
        varchar status
        timestamptz scheduled_at
        timestamptz last_rescheduled_at
        varchar cancellation_reason
        timestamptz cancelled_at
        bigint cancelled_by_user_id FK
        varchar result_observations
        varchar result_conclusions
        timestamptz result_recorded_at
        bigint result_recorded_by_user_id FK
    }

    APPLICATION_VISIT_RESCHEDULE_REQUEST {
        uuid id PK
        uuid visit_id FK
        bigint requested_by_user_id FK
        varchar reason
        varchar status
        timestamptz requested_at
        timestamptz original_starts_at
        timestamptz original_ends_at
        varchar original_location
        bigint reviewed_by_user_id FK
        timestamptz reviewed_at
        varchar review_comment
        timestamptz approved_starts_at
        timestamptz approved_ends_at
        varchar approved_location
    }

    PROGRAM_EDITION ||--o{ ENROLLMENT_PERIOD : habilita
    PROGRAM_EDITION ||--o{ APPLICATION : recibe
    ENROLLMENT_PERIOD ||--o{ APPLICATION : agrupa
    USERS ||--o{ APPLICATION : titular
    USERS ||--o{ APPLICATION : registra
    USERS o|--o{ APPLICATION : tiene_asignadas
    USERS o|--o{ APPLICATION : ultima_resolucion

    APPLICATION ||--o| APPLICATION_EVALUATION : posee
    APPLICATION ||--o{ APPLICATION_RESOLUTION : resuelve
    APPLICATION ||--o| INTERVENTION_PLAN : organiza
    USERS ||--o{ INTERVENTION_PLAN : responsable
    INTERVENTION_PLAN ||--|{ INTERVENTION_OBJECTIVE : define
    INTERVENTION_OBJECTIVE ||--|{ INTERVENTION_ACTION : propone
    INTERVENTION_ACTION ||--o{ INTERVENTION_FOLLOW_UP : registra
    USERS ||--o{ INTERVENTION_FOLLOW_UP : autor
    INTERVENTION_FOLLOW_UP ||--o| INTERVENTION_FOLLOW_UP_PUBLICATION : publica_resumen
    USERS ||--o{ INTERVENTION_FOLLOW_UP_PUBLICATION : ultima_gestion
    APPLICATION ||--o{ BENEFIT_GRANT : recibe
    PROGRAM_EDITION ||--o{ PROGRAM_BENEFIT : ofrece
    PROGRAM_BENEFIT ||--o{ BENEFIT_GRANT : origina
    USERS ||--o{ BENEFIT_GRANT : otorga
    BENEFIT_GRANT ||--o{ BENEFIT_GRANT_CHANGE : registra
    USERS o|--o{ BENEFIT_GRANT_CHANGE : modifica
    BENEFIT_GRANT_CHANGE ||--o{ BENEFIT_GRANT_CHANGE_REQUIREMENT : reevalua
    USERS ||--o{ APPLICATION_RESOLUTION : decide
    APPLICATION_RESOLUTION ||--o{ APPLICATION_RESOLUTION_REQUIREMENT : conserva
    USERS ||--o{ APPLICATION_EVALUATION : realiza

    APPLICATION ||--o{ APPLICATION_VISIT : programa
    USERS ||--o{ APPLICATION_VISIT : responsable
    USERS o|--o{ APPLICATION_VISIT : cancela
    USERS o|--o{ APPLICATION_VISIT : registra_resultado

    APPLICATION_VISIT ||--o{ APPLICATION_VISIT_RESCHEDULE_REQUEST : recibe
    USERS ||--o{ APPLICATION_VISIT_RESCHEDULE_REQUEST : solicita
    USERS o|--o{ APPLICATION_VISIT_RESCHEDULE_REQUEST : revisa
```

## Decisiones representadas

- El ciudadano consulta exclusivamente sus planes y su estructura acordada. El estado del plan se calcula desde sus acciones, sin columna nueva. Solo se proyectan seguimientos con publicación visible; notas y resultados originales permanecen privados. La publicación es opcional, única por seguimiento y conserva únicamente el resumen actual, incluso retirado. Publicar, corregir o retirar se serializa con solicitud, plan, acción y seguimiento; la auditoría no copia textos. La migración no publica registros anteriores.

- Cada solicitud evaluada admite un plan de intervención, incluso rechazada o cerrada. El ciudadano se deriva del caso. Objetivos, acciones, responsable y fechas iniciales se conservan; solo cambia el estado de las acciones mediante seguimientos inmutables. Completar acciones no modifica estados ni cupos de la solicitud.
- Cada otorgamiento conserva una copia del beneficio configurado, su vigencia, estado inicial `ACTIVE` y administrativo responsable. La unicidad por solicitud y beneficio evita duplicados; no vuelve a ocupar el cupo reservado al aprobar.
- El historial inmutable registra asignación, renovación, suspensión, finalización y vencimiento con motivo, responsable y fecha UTC. Renovar extiende el mismo otorgamiento antes de vencer y reevalúa requisitos y documentos. El vencimiento automático cambia `ACTIVE/SUSPENDED` con `endDate < hoy` a `EXPIRED`, con responsable ID null y nombre Sistema. Suspender conserva la reserva; finalizar o vencer el último otorgamiento pendiente cierra la solicitud en `CLOSED` y libera exactamente una reserva. `FINALIZED` y `EXPIRED` son definitivos.

- Las resoluciones son inmutables y preservan una copia del requisito evaluado sin FK al catálogo editable. La solicitud mantiene el estado y el responsable de la última resolución; aprobar incrementa el cupo ocupado de la edición.

- `ApplicationEvaluation` es 1:1 con `Application`; la unicidad de `application_id` impide una segunda evaluación.
- Una solicitud puede tener muchas actuaciones `VISIT` o `INTERVIEW`.
- Cancelación y resultado forman parte de `ApplicationVisit`; no son entidades independientes. Sus columnas son mutuamente excluyentes mediante `ck_application_visit_lifecycle_data`.
- La reprogramación aplicada conserva el horario vigente y `last_rescheduled_at` en la visita. La petición ciudadana sí tiene entidad propia para preservar su motivo, snapshot, decisión e historial.
- Una visita admite muchas peticiones históricas, pero el índice parcial `uk_visit_reschedule_request_pending` permite como máximo una en estado `PENDING`.
- Las referencias opcionales a usuarios corresponden al trabajador asignado, actor de cancelación, profesional de resultado y revisor. Las claves foráneas impiden eliminar usuarios todavía referenciados.
- La trazabilidad se guarda en `logs` mediante `entity_type` y `entity_id`; ese vínculo es polimórfico y por eso no se dibuja como una clave foránea inexistente.
