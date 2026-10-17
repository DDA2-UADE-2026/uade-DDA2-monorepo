package com.uade.dda2.server.feature.application.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.feature.application.entity.ApplicationVisitType
import com.uade.dda2.server.feature.application.error.ApplicationVisitErrors
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(
    description = "Datos para programar una visita o entrevista social. El responsable, fin y fecha de registro se derivan en el servidor.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
)
data class CreateApplicationVisitRequest(
    @field:Schema(description = "Tipo de actuacion social.", example = "VISIT", requiredMode = Schema.RequiredMode.REQUIRED)
    val type: ApplicationVisitType? = null,

    @field:Schema(
        description = "Inicio futuro en ISO-8601 con offset.",
        example = "2026-10-20T10:00:00-03:00",
        type = "string",
        format = "date-time",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val startsAt: OffsetDateTime? = null,

    @field:Schema(description = "Duracion positiva en minutos.", example = "60", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val durationMinutes: Int? = null,

    @field:Schema(
        description = "Lugar de la visita o entrevista. Se recorta antes de persistir y no se copia al log de auditoria.",
        example = "Domicilio declarado por el ciudadano",
        minLength = 1,
        maxLength = 300,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val location: String? = null,
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: Any?) {
        throw ApplicationVisitErrors.invalidData(
            "La programacion solo admite type, startsAt, durationMinutes y location.",
        )
    }
}
