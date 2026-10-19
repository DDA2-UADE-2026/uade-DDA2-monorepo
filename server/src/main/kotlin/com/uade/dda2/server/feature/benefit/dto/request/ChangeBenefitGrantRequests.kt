package com.uade.dda2.server.feature.benefit.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.benefit.error.BenefitGrantErrors
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

@Schema(description = "Renovacion del mismo otorgamiento. Conserva inicio y datos copiados del beneficio.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class RenewBenefitGrantRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 1000, example = "Se mantiene la necesidad del beneficio.")
    val reason: String? = null,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date", description = "Fin inclusivo, posterior al actual, no anterior a hoy y dentro de la edicion.")
    val endDate: LocalDate? = null,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Un resultado por requisito vigente; lista vacia si no hay requisitos.")
    val requirementChecks: List<BenefitGrantRequirementCheckRequest?>? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw BenefitGrantErrors.invalidData("Solo se admiten reason, endDate y requirementChecks.")
    }
}

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class BenefitGrantRequirementCheckRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    val requirementId: UUID? = null,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val fulfilled: Boolean? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw BenefitGrantErrors.invalidData("Cada control solo admite requirementId y fulfilled.")
    }
}

@Schema(description = "Cambio de estado con motivo obligatorio. Responsable y fecha se obtienen del servidor.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class ChangeBenefitGrantStateRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 1000, example = "Finalizo la necesidad de asistencia.")
    val reason: String? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw BenefitGrantErrors.invalidData("Solo se admite reason.")
    }
}
