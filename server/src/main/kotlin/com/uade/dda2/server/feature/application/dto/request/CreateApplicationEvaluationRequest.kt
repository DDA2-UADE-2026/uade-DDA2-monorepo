package com.uade.dda2.server.feature.application.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.application.entity.EvaluationPriority
import com.uade.dda2.server.feature.application.entity.VulnerabilityLevel
import com.uade.dda2.server.feature.application.error.ApplicationEvaluationErrors
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Evaluacion social de una solicitud. El profesional y la fecha se obtienen del servidor.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
)
data class CreateApplicationEvaluationRequest(
    @field:Schema(description = "Nivel de vulnerabilidad detectado.", example = "HIGH", requiredMode = Schema.RequiredMode.REQUIRED)
    val vulnerability: VulnerabilityLevel? = null,

    @field:Schema(description = "Prioridad de intervencion.", example = "URGENT", requiredMode = Schema.RequiredMode.REQUIRED)
    val priority: EvaluationPriority? = null,

    @field:Schema(
        description = "Recomendacion profesional. Se recorta antes de persistir y no se copia al log de auditoria.",
        example = "Se recomienda intervencion prioritaria.",
        minLength = 1,
        maxLength = 1000,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val recommendation: String? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw ApplicationEvaluationErrors.invalidData(
            "La evaluacion solo admite vulnerability, priority y recommendation.",
        )
    }
}
