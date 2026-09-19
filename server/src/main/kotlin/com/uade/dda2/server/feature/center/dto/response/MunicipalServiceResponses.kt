package com.uade.dda2.server.feature.center.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Detalle de un servicio municipal compartido.")
data class MunicipalServiceResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val active: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

@Schema(description = "Página de servicios municipales.")
data class MunicipalServiceListResponse(
    val content: List<MunicipalServiceResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
