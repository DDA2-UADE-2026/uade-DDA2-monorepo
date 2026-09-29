package com.uade.dda2.server.feature.appointment.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.appointment.entity.AttentionResult
import com.uade.dda2.server.feature.appointment.error.AttentionErrors
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Registra asistencia. ATENDIDO exige fecha y descripcion; AUSENTE no admite ninguna de ellas.", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class CreateAttentionRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val result: AttentionResult,
    val attendedOn: LocalDate? = null,
    val description: String? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw AttentionErrors.invalidData("El registro solo admite result, attendedOn y description.")
    }
}

@Schema(description = "Corrige una constancia usando la version consultada previamente.", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class CorrectAttentionRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val version: Long,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val result: AttentionResult,
    val attendedOn: LocalDate? = null,
    val description: String? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw AttentionErrors.invalidData("La correccion solo admite version, result, attendedOn y description.")
    }
}
