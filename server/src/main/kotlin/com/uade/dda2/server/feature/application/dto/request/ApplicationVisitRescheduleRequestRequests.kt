package com.uade.dda2.server.feature.application.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleDecision
import com.uade.dda2.server.feature.application.error.ApplicationVisitRescheduleRequestErrors
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(
    description = "Motivo ciudadano para solicitar la reprogramacion de una visita o entrevista propia.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
)
data class CreateApplicationVisitRescheduleRequest(
    @field:Schema(
        description = "Motivo de la solicitud. Se recorta y no se copia a auditoria.",
        example = "No podre asistir por un compromiso medico.",
        minLength = 1,
        maxLength = 1000,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val reason: String? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw ApplicationVisitRescheduleRequestErrors.invalidData("La solicitud solo admite reason.")
    }
}

@Schema(
    description = "Decision del responsable sobre una solicitud ciudadana de reprogramacion.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
)
data class ReviewApplicationVisitRescheduleRequest(
    @field:Schema(
        description = "Decision final: APPROVED o REJECTED.",
        example = "APPROVED",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val status: ApplicationVisitRescheduleDecision? = null,
    @field:Schema(description = "Nuevo inicio futuro, obligatorio al aprobar.", example = "2026-10-25T10:00:00-03:00")
    val startsAt: OffsetDateTime? = null,
    @field:Schema(description = "Nueva duracion positiva, obligatoria al aprobar.", example = "60", minimum = "1")
    val durationMinutes: Int? = null,
    @field:Schema(description = "Nuevo lugar, obligatorio al aprobar.", minLength = 1, maxLength = 300)
    val location: String? = null,
    @field:Schema(
        description = "Comentario opcional al aprobar y obligatorio al rechazar. No se copia a auditoria.",
        minLength = 1,
        maxLength = 1000,
    )
    val comment: String? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw ApplicationVisitRescheduleRequestErrors.invalidData(
            "La revision solo admite status, startsAt, durationMinutes, location y comment.",
        )
    }
}
