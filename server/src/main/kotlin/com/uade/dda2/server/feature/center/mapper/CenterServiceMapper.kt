package com.uade.dda2.server.feature.center.mapper

import com.uade.dda2.server.feature.center.dto.response.CenterServiceResponse
import com.uade.dda2.server.feature.center.entity.CenterService

fun CenterService.toResponse(): CenterServiceResponse =
    CenterServiceResponse(
        id = requireNotNull(id),
        centerId = requireNotNull(center.id),
        service = service.toResponse(),
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun CenterService.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "centerId" to center.id,
        "serviceId" to service.id,
        "active" to active,
        "createdAt" to createdAt.toString(),
        "updatedAt" to updatedAt.toString(),
    )
