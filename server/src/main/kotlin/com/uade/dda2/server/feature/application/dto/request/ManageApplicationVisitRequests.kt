package com.uade.dda2.server.feature.application.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.application.error.ApplicationVisitErrors
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(
    description = "Nuevo horario y lugar de una visita o entrevista social pendiente.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
)
data class RescheduleApplicationVisitRequest(
    @field:Schema(
        description = "Nuevo inicio futuro en ISO-8601 con offset.",
        example = "2026-10-22T14:00:00-03:00",
        type = "string",
        format = "date-time",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val startsAt: OffsetDateTime? = null,

    @field:Schema(
        description = "Nueva duracion positiva en minutos.",
        example = "90",
        minimum = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val durationMinutes: Int? = null,

    @field:Schema(
        description = "Nuevo lugar. Se recorta y no se copia al log de auditoria.",
        example = "Centro comunitario",
        minLength = 1,
        maxLength = 300,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val location: String? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw ApplicationVisitErrors.invalidData(
            "La reprogramacion solo admite startsAt, durationMinutes y location.",
        )
    }
}

@Schema(
    description = "Motivo requerido para cancelar una visita o entrevista social pendiente.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
)
data class CancelApplicationVisitRequest(
    @field:Schema(
        description = "Motivo de cancelacion. Se recorta y no se copia al log de auditoria.",
        example = "El ciudadano solicito cambiar la fecha.",
        minLength = 1,
        maxLength = 1000,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val reason: String? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw ApplicationVisitErrors.invalidData("La cancelacion solo admite reason.")
    }
}
