package com.uade.dda2.server.feature.center.mapper

import com.uade.dda2.server.feature.center.dto.response.ProfessionalAssignmentResponse
import com.uade.dda2.server.feature.center.entity.ProfessionalAssignment

fun ProfessionalAssignment.toResponse(): ProfessionalAssignmentResponse =
    ProfessionalAssignmentResponse(
        id = requireNotNull(id),
        professionalId = requireNotNull(professional.id),
        professionalName = professional.name,
        professionalEmail = professional.email,
        centerServiceId = requireNotNull(centerService.id),
        centerId = requireNotNull(centerService.center.id),
        serviceId = requireNotNull(centerService.service.id),
        serviceName = centerService.service.name,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ProfessionalAssignment.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "professionalId" to professional.id,
        "centerServiceId" to centerService.id,
        "active" to active,
        "createdAt" to createdAt.toString(),
        "updatedAt" to updatedAt.toString(),
    )
