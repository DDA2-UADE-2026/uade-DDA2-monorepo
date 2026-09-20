package com.uade.dda2.server.feature.center.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Profesional asignado a un servicio específico de un centro.")
data class ProfessionalAssignmentResponse(
    val id: UUID,
    val professionalId: Long,
    val professionalName: String,
    val professionalEmail: String,
    val centerServiceId: UUID,
    val centerId: UUID,
    val serviceId: UUID,
    val serviceName: String,
    val active: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
