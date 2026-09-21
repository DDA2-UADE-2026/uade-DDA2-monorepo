package com.uade.dda2.server.feature.center.mapper

import com.uade.dda2.server.feature.center.dto.request.UpdateProfessionalAvailabilityRequest
import com.uade.dda2.server.feature.center.dto.response.ProfessionalAvailabilityResponse
import com.uade.dda2.server.feature.center.entity.ProfessionalAvailability

fun ProfessionalAvailability.updateFrom(request: UpdateProfessionalAvailabilityRequest) {
    dayOfWeek = request.dayOfWeek
    startTime = request.startTime
    endTime = request.endTime
}

fun ProfessionalAvailability.toResponse(): ProfessionalAvailabilityResponse =
    ProfessionalAvailabilityResponse(
        id = requireNotNull(id),
        assignmentId = requireNotNull(assignment.id),
        professionalId = requireNotNull(assignment.professional.id),
        centerId = requireNotNull(assignment.centerService.center.id),
        serviceId = requireNotNull(assignment.centerService.service.id),
        serviceName = assignment.centerService.service.name,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ProfessionalAvailability.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "assignmentId" to assignment.id,
        "dayOfWeek" to dayOfWeek,
        "startTime" to startTime.toString(),
        "endTime" to endTime.toString(),
        "active" to active,
        "createdAt" to createdAt.toString(),
        "updatedAt" to updatedAt.toString(),
    )
