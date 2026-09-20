package com.uade.dda2.server.feature.center.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Servicio del catálogo que se asignará al centro.")
data class AssignCenterServiceRequest(
    val serviceId: UUID,
)
