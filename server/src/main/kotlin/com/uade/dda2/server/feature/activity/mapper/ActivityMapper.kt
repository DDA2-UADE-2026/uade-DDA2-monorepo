package com.uade.dda2.server.feature.activity.mapper

import com.uade.dda2.server.feature.activity.dto.request.CreateActivityRequest
import com.uade.dda2.server.feature.activity.dto.request.UpdateActivityRequest
import com.uade.dda2.server.feature.activity.dto.response.ActivityCreatedByResponse
import com.uade.dda2.server.feature.activity.dto.response.ActivityListItemResponse
import com.uade.dda2.server.feature.activity.dto.response.ActivityListResponse
import com.uade.dda2.server.feature.activity.dto.response.ActivityResponse
import com.uade.dda2.server.feature.activity.dto.response.ActivityEnrollmentResponse
import com.uade.dda2.server.feature.activity.dto.response.CitizenActivityListResponse
import com.uade.dda2.server.feature.activity.dto.response.CitizenActivityResponse
import com.uade.dda2.server.feature.activity.entity.Activity
import com.uade.dda2.server.feature.activity.entity.ActivityEnrollment
import com.uade.dda2.server.feature.auth.entity.User
import org.springframework.data.domain.Page
import java.util.UUID

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

fun Activity.toCitizenResponse(enrolledCount: Long): CitizenActivityResponse =
    CitizenActivityResponse(
        id = requireNotNull(id),
        name = name,
        description = description,
        location = location,
        startDate = startDate,
        endDate = endDate,
        capacity = capacity,
        enrolledCount = enrolledCount,
        availableCapacity = (capacity.toLong() - enrolledCount).coerceAtLeast(0),
    )

fun Page<Activity>.toCitizenListResponse(enrollmentCounts: Map<UUID, Long>): CitizenActivityListResponse =
    CitizenActivityListResponse(
        content = content.map { activity ->
            activity.toCitizenResponse(enrollmentCounts[requireNotNull(activity.id)] ?: 0)
        },
        page = number,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

fun ActivityEnrollment.toResponse(): ActivityEnrollmentResponse =
    ActivityEnrollmentResponse(
        id = requireNotNull(id),
        activityId = requireNotNull(activity.id),
        citizenId = requireNotNull(citizen.id),
        enrolledAt = enrolledAt,
    )

fun ActivityEnrollment.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "activityId" to activity.id,
        "citizenId" to citizen.id,
        "enrolledAt" to enrolledAt.toString(),
    )
