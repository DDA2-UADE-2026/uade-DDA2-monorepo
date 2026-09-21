package com.uade.dda2.server.feature.center.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalTime

@Schema(description = "Franja semanal de apertura de un centro.")
data class CreateCenterOpeningHourRequest(
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
)

@Schema(description = "Datos editables de una franja semanal de apertura.")
data class UpdateCenterOpeningHourRequest(
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
)

@Schema(description = "Misma franja de apertura replicada en varios días. Todo-o-nada: si un día falla, no se crea ninguna.")
data class CreateCenterOpeningHoursRequest(
    val days: List<DayOfWeek>,
    val startTime: LocalTime,
    val endTime: LocalTime,
)
