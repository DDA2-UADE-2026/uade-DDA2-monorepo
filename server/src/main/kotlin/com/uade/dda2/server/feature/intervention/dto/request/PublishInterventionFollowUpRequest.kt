package com.uade.dda2.server.feature.intervention.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.intervention.error.InterventionErrors
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Version publica redactada explicitamente; nunca copia notas ni resultado profesional.", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class PublishInterventionFollowUpRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 2000)
    val summary: String? = null,
) {
    @JsonAnySetter @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) { throw InterventionErrors.invalidPublication("Solo se admite summary.") }
}
