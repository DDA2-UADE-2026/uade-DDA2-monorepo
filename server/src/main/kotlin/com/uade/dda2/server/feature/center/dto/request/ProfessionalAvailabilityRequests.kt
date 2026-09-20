package com.uade.dda2.server.feature.center.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalTime

@Schema(description = "Franja semanal de disponibilidad profesional.")
data class CreateProfessionalAvailabilityRequest(
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
)

@Schema(description = "Datos editables de una disponibilidad profesional.")
data class UpdateProfessionalAvailabilityRequest(
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
)

@Schema(description = "Misma franja de disponibilidad replicada en varios días. Todo-o-nada: si un día falla, no se crea ninguna.")
data class CreateProfessionalAvailabilityBatchRequest(
    val days: List<DayOfWeek>,
    val startTime: LocalTime,
    val endTime: LocalTime,
)
