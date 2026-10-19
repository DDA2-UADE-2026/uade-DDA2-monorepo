package com.uade.dda2.server.feature.benefit.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.benefit.error.BenefitGrantErrors
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

@Schema(description = "Otorgamiento individual. Titular, estado, datos del beneficio, responsable y fecha los obtiene el servidor.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class CreateBenefitGrantRequest(
    @field:Schema(description = "Beneficio del catalogo de la edicion de la solicitud.", requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    val programBenefitId: UUID? = null,
    @field:Schema(description = "Inicio inclusivo de vigencia, dentro de la edicion y no posterior a hoy.", requiredMode = Schema.RequiredMode.REQUIRED, format = "date", example = "2026-10-01")
    val startDate: LocalDate? = null,
    @field:Schema(description = "Fin inclusivo de vigencia, dentro de la edicion y no anterior a hoy.", requiredMode = Schema.RequiredMode.REQUIRED, format = "date", example = "2026-12-31")
    val endDate: LocalDate? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw BenefitGrantErrors.invalidData("Solo se admiten programBenefitId, startDate y endDate.")
    }
}
