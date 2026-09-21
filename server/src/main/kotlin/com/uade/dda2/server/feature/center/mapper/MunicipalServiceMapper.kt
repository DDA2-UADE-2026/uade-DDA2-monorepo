package com.uade.dda2.server.feature.center.mapper

import com.uade.dda2.server.feature.center.dto.request.CreateMunicipalServiceRequest
import com.uade.dda2.server.feature.center.dto.request.UpdateMunicipalServiceRequest
import com.uade.dda2.server.feature.center.dto.response.MunicipalServiceListResponse
import com.uade.dda2.server.feature.center.dto.response.MunicipalServiceResponse
import com.uade.dda2.server.feature.center.entity.MunicipalService
import org.springframework.data.domain.Page

fun CreateMunicipalServiceRequest.toEntity(): MunicipalService =
    MunicipalService(name = name, description = description, durationMinutes = durationMinutes)

fun MunicipalService.updateFrom(request: UpdateMunicipalServiceRequest) {
    name = request.name
    description = request.description
    durationMinutes = request.durationMinutes
}

fun MunicipalService.toResponse(): MunicipalServiceResponse =
    MunicipalServiceResponse(
        id = requireNotNull(id),
        name = name,
        description = description,
        durationMinutes = durationMinutes,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Page<MunicipalService>.toListResponse(): MunicipalServiceListResponse =
    MunicipalServiceListResponse(content.map(MunicipalService::toResponse), number, size, totalElements, totalPages)

fun MunicipalService.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "durationMinutes" to durationMinutes,
        "active" to active,
        "createdAt" to createdAt.toString(),
        "updatedAt" to updatedAt.toString(),
    )
