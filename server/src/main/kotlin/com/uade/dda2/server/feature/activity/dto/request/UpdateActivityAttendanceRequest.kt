package com.uade.dda2.server.feature.activity.dto.request

import com.uade.dda2.server.feature.activity.entity.ActivityAttendanceStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Asistencia de un ciudadano inscripto en una actividad.")
data class UpdateActivityAttendanceRequest(
    @field:NotNull(message = "La asistencia es obligatoria.")
    val attendance: ActivityAttendanceStatus,
)
