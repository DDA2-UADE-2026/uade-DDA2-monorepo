package com.uade.dda2.server.feature.intervention.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.intervention.entity.InterventionFollowUpStatus
import com.uade.dda2.server.feature.intervention.error.InterventionErrors
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Estructura inicial inmutable. Ciudadano y responsable se obtienen del caso.", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class CreateInterventionPlanRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Al menos un objetivo, en el orden deseado.")
    val objectives: List<InterventionObjectiveRequest?>? = null,
) {
    @JsonAnySetter @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) { throw InterventionErrors.invalidPlan("Solo se admite objectives.") }
}

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class InterventionObjectiveRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 1000)
    val description: String? = null,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Al menos una accion, en el orden deseado.")
    val actions: List<InterventionActionRequest?>? = null,
) {
    @JsonAnySetter @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) { throw InterventionErrors.invalidPlan("Cada objetivo solo admite description y actions.") }
}

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class InterventionActionRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 1000)
    val description: String? = null,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date", description = "Inicio inclusivo. Admite fechas pasadas o futuras.")
    val startDate: LocalDate? = null,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date", description = "Fin previsto inclusivo, no anterior al inicio; no impide seguimientos posteriores.")
    val dueDate: LocalDate? = null,
) {
    @JsonAnySetter @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) { throw InterventionErrors.invalidPlan("Cada accion solo admite description, startDate y dueDate.") }
}

@Schema(description = "Seguimiento inmutable de una accion. Autor y registro UTC se obtienen del servidor.", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class CreateInterventionFollowUpRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "IN_PROGRESS")
    val status: InterventionFollowUpStatus? = null,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date", description = "Fecha real desde el inicio de la accion hasta hoy, sin retroceder respecto al seguimiento anterior.")
    val occurredOn: LocalDate? = null,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 2000)
    val notes: String? = null,
    @field:Schema(minLength = 1, maxLength = 2000, description = "Obligatorio para COMPLETED; opcional en avances.")
    val result: String? = null,
) {
    @JsonAnySetter @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) { throw InterventionErrors.invalidFollowUp("Solo se admiten status, occurredOn, notes y result.") }
}
