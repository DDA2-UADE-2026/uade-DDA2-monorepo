package com.uade.dda2.server.feature.appointment.dto.response

import com.uade.dda2.server.feature.appointment.entity.AppointmentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Servicio municipal habilitado para solicitar turnos.")
data class AppointmentServiceResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val durationMinutes: Int,
)

@Schema(description = "Centro que ofrece efectivamente el servicio seleccionado.")
data class AppointmentCenterResponse(
    val centerServiceId: UUID,
    val centerId: UUID,
    val name: String,
    val address: String,
    val phone: String?,
    val email: String?,
)

@Schema(description = "Horario actualmente otorgable al ciudadano autenticado.")
data class AvailableAppointmentSlotResponse(
    val professionalAssignmentId: UUID,
    val professionalId: Long,
    val professionalName: String,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
)

@Schema(description = "Detalle vigente de un turno propio, confirmado o cancelado.")
data class AppointmentResponse(
    val id: UUID,
    val status: AppointmentStatus,
    val serviceId: UUID,
    val serviceName: String,
    val centerId: UUID,
    val centerName: String,
    val centerAddress: String,
    val professionalId: Long,
    val professionalName: String,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val createdAt: OffsetDateTime,
)

@Schema(description = "Detalle administrativo del turno y estado de retención de su horario.")
data class AdminAppointmentResponse(
    val appointment: AppointmentResponse,
    val citizenId: Long,
    val citizenName: String,
    val slotRetained: Boolean,
)

@Schema(description = "Ocupación de un centro en un día para el calendario administrativo.")
data class AdminDaySummaryResponse(
    val date: LocalDate,
    val confirmed: Int,
    val free: Int,
    val retained: Int,
    val hasAgenda: Boolean,
)

@Schema(description = "Horario libre de un centro en una fecha, con servicio y profesional.")
data class AdminFreeSlotResponse(
    val centerServiceId: UUID,
    val serviceId: UUID,
    val serviceName: String,
    val professionalAssignmentId: UUID,
    val professionalId: Long,
    val professionalName: String,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
)
