package com.uade.dda2.server.feature.appointment.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.appointment.error.AppointmentErrors
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(
    description = "Solicita un turno propio previamente ofrecido por la consulta de horarios.",
    requiredProperties = ["professionalAssignmentId", "startsAt", "endsAt"],
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
)
data class CreateAppointmentRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val professionalAssignmentId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date-time")
    val startsAt: OffsetDateTime,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date-time")
    val endsAt: OffsetDateTime,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw AppointmentErrors.invalidRequest(
            "El request solo admite professionalAssignmentId, startsAt y endsAt.",
        )
    }
}
