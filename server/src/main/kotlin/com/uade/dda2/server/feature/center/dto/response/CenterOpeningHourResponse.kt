package com.uade.dda2.server.feature.center.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Franja semanal de apertura de un centro.")
data class CenterOpeningHourResponse(
    val id: UUID,
    val centerId: UUID,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val active: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
