package com.uade.dda2.server.feature.application.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.application.entity.ApplicationResolutionDecision
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Resolucion de una solicitud evaluada: aprobarla, rechazarla o dejarla en lista de espera.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
)
data class ResolveApplicationRequest(
    @field:Schema(
        description = "APPROVED, REJECTED o WAITLISTED. Desde lista de espera solo se admite APPROVED o REJECTED.",
        example = "APPROVED",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val decision: ApplicationResolutionDecision? = null,

    @field:Schema(
        description = "Motivo visible para el titular, de 1 a 1000 caracteres. Obligatorio para REJECTED y WAITLISTED.",
        example = "Cumple los requisitos y la evaluacion social recomienda el ingreso.",
        maxLength = 1000,
        nullable = true,
    )
    val reason: String? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw ApplicationErrors.invalidResolution("La resolucion solo admite decision y reason.")
    }
}
