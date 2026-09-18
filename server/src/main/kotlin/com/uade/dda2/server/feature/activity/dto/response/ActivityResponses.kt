package com.uade.dda2.server.feature.activity.dto.response

import com.uade.dda2.server.feature.activity.entity.ActivityStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Usuario que creó la actividad.")
data class ActivityCreatedByResponse(
    val id: Long,
    val name: String,
)

@Schema(description = "Detalle de una actividad comunitaria.")
data class ActivityResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val location: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val capacity: Int,
    val status: ActivityStatus,
    val createdBy: ActivityCreatedByResponse,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

@Schema(description = "Actividad incluida en un listado administrativo.")
data class ActivityListItemResponse(
    val id: UUID,
    val name: String,
    val location: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val capacity: Int,
    val status: ActivityStatus,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

@Schema(description = "Página de actividades comunitarias.")
data class ActivityListResponse(
    val content: List<ActivityListItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
