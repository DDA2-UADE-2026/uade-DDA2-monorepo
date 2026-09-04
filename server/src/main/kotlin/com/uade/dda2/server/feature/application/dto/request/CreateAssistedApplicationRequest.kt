package com.uade.dda2.server.feature.application.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.error.BadRequestException
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive
import java.util.UUID

@Schema(description = "Presentación asistida para un usuario existente. Quien registra se obtiene exclusivamente del JWT.",
    requiredProperties = ["userId", "enrollmentPeriodId"], additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class CreateAssistedApplicationRequest(
    @field:Positive
    @field:Schema(description = "ID interno del solicitante en users; no es un citizenId externo.", example = "42")
    val userId: Long,
    val enrollmentPeriodId: UUID,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw BadRequestException("APPLICATION_UNEXPECTED_FIELD", "El request solo admite userId y enrollmentPeriodId.")
    }
}
