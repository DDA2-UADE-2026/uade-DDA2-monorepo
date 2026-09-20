package com.uade.dda2.server.feature.center.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Profesional que se asignará al servicio del centro.")
data class AssignProfessionalRequest(
    val professionalId: Long,
)
