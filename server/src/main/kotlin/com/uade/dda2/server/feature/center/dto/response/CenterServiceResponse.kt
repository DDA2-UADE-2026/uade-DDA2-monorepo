package com.uade.dda2.server.feature.center.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Servicio del catálogo asignado a un centro municipal.")
data class CenterServiceResponse(
    val id: UUID,
    val centerId: UUID,
    val service: MunicipalServiceResponse,
    val active: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
