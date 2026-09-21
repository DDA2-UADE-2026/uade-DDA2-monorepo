package com.uade.dda2.server.feature.center.mapper

import com.uade.dda2.server.feature.center.dto.request.UpdateCenterOpeningHourRequest
import com.uade.dda2.server.feature.center.dto.response.CenterOpeningHourResponse
import com.uade.dda2.server.feature.center.entity.CenterOpeningHour

fun CenterOpeningHour.updateFrom(request: UpdateCenterOpeningHourRequest) {
    dayOfWeek = request.dayOfWeek
    startTime = request.startTime
    endTime = request.endTime
}

fun CenterOpeningHour.toResponse(): CenterOpeningHourResponse =
    CenterOpeningHourResponse(
        id = requireNotNull(id),
        centerId = requireNotNull(center.id),
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun CenterOpeningHour.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "centerId" to center.id,
        "dayOfWeek" to dayOfWeek,
        "startTime" to startTime.toString(),
        "endTime" to endTime.toString(),
        "active" to active,
        "createdAt" to createdAt.toString(),
        "updatedAt" to updatedAt.toString(),
    )
