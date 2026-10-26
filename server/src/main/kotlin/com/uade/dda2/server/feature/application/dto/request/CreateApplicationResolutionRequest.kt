package com.uade.dda2.server.feature.application.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.application.entity.ApplicationResolutionDecision
import com.uade.dda2.server.feature.application.error.ApplicationResolutionErrors
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Resolucion administrativa. Responsable y fecha se obtienen del servidor.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class CreateApplicationResolutionRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "APPROVED")
    val decision: ApplicationResolutionDecision? = null,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 1000,
        example = "Cumple los requisitos y dispone de cupo.")
    val reason: String? = null,
    @field:Schema(description = "Un resultado por cada requisito vigente. Lista vacia si no hay requisitos.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    val requirementChecks: List<ApplicationRequirementCheckRequest?>? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw ApplicationResolutionErrors.invalidData("Solo se admiten decision, reason y requirementChecks.")
    }
}

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class ApplicationRequirementCheckRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    val requirementId: UUID? = null,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resultado verificado por el responsable.")
    val fulfilled: Boolean? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw ApplicationResolutionErrors.invalidData("Cada control solo admite requirementId y fulfilled.")
    }
}
