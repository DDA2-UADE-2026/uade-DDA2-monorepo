package com.uade.dda2.server.feature.center.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Detalle de un centro municipal.")
data class MunicipalCenterResponse(
    val id: UUID,
    val name: String,
    val address: String,
    val phone: String?,
    val email: String?,
    val active: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

@Schema(description = "Página de centros municipales.")
data class MunicipalCenterListResponse(
    val content: List<MunicipalCenterResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
