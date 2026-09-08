package com.uade.dda2.server.feature.log.dto.response

import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime

@Schema(description = "Registro inmutable de un evento de auditoría.")
data class LogResponse(
    @field:Schema(description = "Identificador del registro.", example = "42", accessMode = Schema.AccessMode.READ_ONLY)
    val id: Long,
    @field:Schema(
        description = "Usuario que originó el evento. Es nulo para procesos automáticos o si el usuario fue eliminado.",
        nullable = true,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val actor: LogActorResponse?,
    @field:Schema(description = "Acción auditada.", example = "UPDATE", accessMode = Schema.AccessMode.READ_ONLY)
    val action: LogAction,
    @field:Schema(description = "Tipo de entidad afectada.", example = "USER", accessMode = Schema.AccessMode.READ_ONLY)
    val entityType: LogEntityType,
    @field:Schema(description = "Identificador de la entidad afectada.", example = "12", accessMode = Schema.AccessMode.READ_ONLY)
    val entityId: String,
    @field:Schema(
        description = "Estado previo de la entidad.",
        type = "object",
        example = "{\"active\":true}",
        nullable = true,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val oldValues: JsonNode?,
    @field:Schema(
        description = "Estado posterior de la entidad.",
        type = "object",
        example = "{\"active\":false}",
        nullable = true,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val newValues: JsonNode?,
    @field:Schema(
        description = "Fecha y hora UTC en que se registró el evento.",
        example = "2026-09-01T18:30:00Z",
        format = "date-time",
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val createdAt: OffsetDateTime,
)
