package com.uade.dda2.server.feature.appointment.mapper

import com.uade.dda2.server.feature.appointment.dto.response.AppointmentResponse
import com.uade.dda2.server.feature.appointment.entity.Appointment
import java.time.ZoneId

fun Appointment.toResponse(zone: ZoneId): AppointmentResponse {
    val assignment = professionalAssignment
    val centerService = assignment.centerService
    return AppointmentResponse(
        id = requireNotNull(id),
        status = status,
        serviceId = requireNotNull(centerService.service.id),
        serviceName = centerService.service.name,
        centerId = requireNotNull(centerService.center.id),
        centerName = centerService.center.name,
        centerAddress = centerService.center.address,
        professionalId = requireNotNull(assignment.professional.id),
        professionalName = assignment.professional.name,
        startsAt = startsAt.atZoneSameInstant(zone).toOffsetDateTime(),
        endsAt = endsAt.atZoneSameInstant(zone).toOffsetDateTime(),
        createdAt = createdAt,
    )
}

fun Appointment.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "citizenId" to citizen.id,
        "professionalAssignmentId" to professionalAssignment.id,
        "startsAt" to startsAt.toString(),
        "endsAt" to endsAt.toString(),
        "status" to status.name,
        "createdAt" to createdAt.toString(),
        "updatedAt" to updatedAt.toString(),
    )
