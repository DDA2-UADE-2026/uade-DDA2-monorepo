package com.uade.dda2.server.feature.application.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Trabajador al que se asigna la solicitud para evaluarla y gestionar sus visitas.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
)
data class AssignApplicationWorkerRequest(
    @field:Schema(
        description = "ID interno del usuario a asignar. Debe estar activo y tener un rol con applications:visits:create " +
            "(y applications:evaluations:create si la solicitud todavia no fue evaluada).",
        example = "7",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val userId: Long? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw ApplicationErrors.invalidAssignment("La asignacion solo admite userId.")
    }
}
