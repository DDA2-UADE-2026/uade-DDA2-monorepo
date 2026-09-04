package com.uade.dda2.server.feature.application.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.error.BadRequestException
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Presenta una solicitud propia. Solo admite enrollmentPeriodId; el usuario proviene del JWT.",
    requiredProperties = ["enrollmentPeriodId"], additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class CreateApplicationRequest(
    @field:Schema(description = "Convocatoria abierta y vigente; determina automáticamente la edición.", requiredMode = Schema.RequiredMode.REQUIRED)
    val enrollmentPeriodId: UUID,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw BadRequestException("APPLICATION_UNEXPECTED_FIELD", "El request solo admite enrollmentPeriodId.")
    }
}
