package com.uade.dda2.server.feature.activity.mapper

import com.uade.dda2.server.feature.activity.dto.request.CreateActivityRequest
import com.uade.dda2.server.feature.activity.dto.request.UpdateActivityRequest
import com.uade.dda2.server.feature.activity.dto.response.ActivityCreatedByResponse
import com.uade.dda2.server.feature.activity.dto.response.ActivityListItemResponse
import com.uade.dda2.server.feature.activity.dto.response.ActivityListResponse
import com.uade.dda2.server.feature.activity.dto.response.ActivityResponse
import com.uade.dda2.server.feature.activity.entity.Activity
import com.uade.dda2.server.feature.auth.entity.User
import org.springframework.data.domain.Page

fun CreateActivityRequest.toEntity(createdBy: User): Activity =
    Activity(
        name = name.trim(),
        description = description.trim(),
        location = location.trim(),
        startDate = startDate,
        endDate = endDate,
        capacity = capacity,
        createdBy = createdBy,
    )

fun Activity.updateFrom(request: UpdateActivityRequest) {
    name = request.name.trim()
    description = request.description.trim()
    location = request.location.trim()
    startDate = request.startDate
    endDate = request.endDate
    capacity = request.capacity
}

fun Activity.toResponse(): ActivityResponse =
    ActivityResponse(
        id = requireNotNull(id),
        name = name,
        description = description,
        location = location,
        startDate = startDate,
        endDate = endDate,
        capacity = capacity,
        status = status,
        createdBy = ActivityCreatedByResponse(
            id = requireNotNull(createdBy.id),
            name = createdBy.name,
        ),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Page<Activity>.toListResponse(): ActivityListResponse =
    ActivityListResponse(
        content = content.map { activity ->
            ActivityListItemResponse(
                id = requireNotNull(activity.id),
                name = activity.name,
                location = activity.location,
                startDate = activity.startDate,
                endDate = activity.endDate,
                capacity = activity.capacity,
                status = activity.status,
                createdAt = activity.createdAt,
                updatedAt = activity.updatedAt,
            )
        },
        page = number,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

fun Activity.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "location" to location,
        "startDate" to startDate.toString(),
        "endDate" to endDate.toString(),
        "capacity" to capacity,
        "status" to status,
        "createdBy" to createdBy.id,
        "createdAt" to createdAt.toString(),
        "updatedAt" to updatedAt.toString(),
    )
