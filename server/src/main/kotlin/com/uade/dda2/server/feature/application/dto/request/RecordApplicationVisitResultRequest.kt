package com.uade.dda2.server.feature.application.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.application.entity.ApplicationVisitResultStatus
import com.uade.dda2.server.feature.application.error.ApplicationVisitErrors
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Resultado final de una visita o entrevista social. El profesional y la fecha se derivan del servidor.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
)
data class RecordApplicationVisitResultRequest(
    @field:Schema(
        description = "Estado final de la actuacion.",
        example = "REALIZED",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val status: ApplicationVisitResultStatus? = null,

    @field:Schema(
        description = "Observaciones profesionales opcionales. Se recortan y no se copian a listados ni auditoria.",
        example = "La familia recibio al profesional.",
        minLength = 1,
        maxLength = 2000,
        nullable = true,
    )
    val observations: String? = null,

    @field:Schema(
        description = "Conclusiones profesionales opcionales. Se recortan y no se copian a listados ni auditoria.",
        example = "Se requiere continuar con la evaluacion.",
        minLength = 1,
        maxLength = 2000,
        nullable = true,
    )
    val conclusions: String? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw ApplicationVisitErrors.invalidResultData(
            "El resultado solo admite status, observations y conclusions.",
        )
    }
}
