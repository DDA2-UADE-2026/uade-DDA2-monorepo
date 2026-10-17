package com.uade.dda2.server.feature.application.mapper

import com.uade.dda2.server.feature.application.dto.response.AdminApplicationVisitRescheduleRequestDetailResponse
import com.uade.dda2.server.feature.application.dto.response.AdminApplicationVisitRescheduleRequestListItemResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitRescheduleRequestSummaryResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitRescheduleScheduleResponse
import com.uade.dda2.server.feature.application.dto.response.CitizenApplicationVisitRescheduleRequestDetailResponse
import com.uade.dda2.server.feature.application.dto.response.CitizenApplicationVisitRescheduleRequestListItemResponse
import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleRequest
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId

fun ApplicationVisitRescheduleRequest.toSummaryResponse(): ApplicationVisitRescheduleRequestSummaryResponse =
    ApplicationVisitRescheduleRequestSummaryResponse(
        id = requireNotNull(id),
        status = status,
        requestedAt = requestedAt,
        reviewedAt = reviewedAt,
    )

fun ApplicationVisitRescheduleRequest.toCitizenListItemResponse(
    zone: ZoneId,
): CitizenApplicationVisitRescheduleRequestListItemResponse = CitizenApplicationVisitRescheduleRequestListItemResponse(
    id = requireNotNull(id),
    applicationId = requireNotNull(visit.application.id),
    applicationNumber = requireNotNull(visit.application.applicationNumber),
    visitId = requireNotNull(visit.id),
    visitType = visit.type,
    status = status,
    requestedAt = requestedAt,
    reviewedAt = reviewedAt,
    approvedStartsAt = approvedStartsAt?.atZoneSameInstant(zone)?.toOffsetDateTime(),
    approvedEndsAt = approvedEndsAt?.atZoneSameInstant(zone)?.toOffsetDateTime(),
    approvedDurationMinutes = durationMinutes(approvedStartsAt, approvedEndsAt),
)

fun ApplicationVisitRescheduleRequest.toCitizenDetailResponse(
    zone: ZoneId,
): CitizenApplicationVisitRescheduleRequestDetailResponse = CitizenApplicationVisitRescheduleRequestDetailResponse(
    id = requireNotNull(id),
    applicationId = requireNotNull(visit.application.id),
    applicationNumber = requireNotNull(visit.application.applicationNumber),
    visitId = requireNotNull(visit.id),
    visitType = visit.type,
    status = status,
    reason = reason,
    requestedAt = requestedAt,
    scheduleAtRequest = schedule(originalStartsAt, originalEndsAt, originalLocation, zone),
    reviewedAt = reviewedAt,
    reviewComment = reviewComment,
    approvedSchedule = approvedSchedule(zone),
)

fun ApplicationVisitRescheduleRequest.toAdminListItemResponse(
    zone: ZoneId,
): AdminApplicationVisitRescheduleRequestListItemResponse = AdminApplicationVisitRescheduleRequestListItemResponse(
    id = requireNotNull(id),
    applicationId = requireNotNull(visit.application.id),
    applicationNumber = requireNotNull(visit.application.applicationNumber),
    visitId = requireNotNull(visit.id),
    visitType = visit.type,
    applicantUserId = requireNotNull(visit.application.user.id),
    applicantName = visit.application.user.name,
    responsibleId = requireNotNull(visit.responsible.id),
    responsibleName = visit.responsible.name,
    programId = requireNotNull(visit.application.programEdition.program.id),
    programName = visit.application.programEdition.program.name,
    programEditionId = requireNotNull(visit.application.programEdition.id),
    programEditionName = visit.application.programEdition.name,
    status = status,
    requestedAt = requestedAt,
    reviewedAt = reviewedAt,
    originalStartsAt = originalStartsAt.atZoneSameInstant(zone).toOffsetDateTime(),
    originalEndsAt = originalEndsAt.atZoneSameInstant(zone).toOffsetDateTime(),
    originalDurationMinutes = durationMinutes(originalStartsAt, originalEndsAt)!!,
    approvedStartsAt = approvedStartsAt?.atZoneSameInstant(zone)?.toOffsetDateTime(),
    approvedEndsAt = approvedEndsAt?.atZoneSameInstant(zone)?.toOffsetDateTime(),
    approvedDurationMinutes = durationMinutes(approvedStartsAt, approvedEndsAt),
)

fun ApplicationVisitRescheduleRequest.toAdminDetailResponse(
    zone: ZoneId,
): AdminApplicationVisitRescheduleRequestDetailResponse = AdminApplicationVisitRescheduleRequestDetailResponse(
    id = requireNotNull(id),
    applicationId = requireNotNull(visit.application.id),
    applicationNumber = requireNotNull(visit.application.applicationNumber),
    visitId = requireNotNull(visit.id),
    visitType = visit.type,
    applicantUserId = requireNotNull(visit.application.user.id),
    applicantName = visit.application.user.name,
    applicantEmail = visit.application.user.email,
    responsibleId = requireNotNull(visit.responsible.id),
    responsibleName = visit.responsible.name,
    responsibleEmail = visit.responsible.email,
    programId = requireNotNull(visit.application.programEdition.program.id),
    programName = visit.application.programEdition.program.name,
    programEditionId = requireNotNull(visit.application.programEdition.id),
    programEditionName = visit.application.programEdition.name,
    requestedById = requireNotNull(requestedBy.id),
    requestedByName = requestedBy.name,
    status = status,
    reason = reason,
    requestedAt = requestedAt,
    scheduleAtRequest = schedule(originalStartsAt, originalEndsAt, originalLocation, zone),
    reviewedById = reviewedBy?.id,
    reviewedByName = reviewedBy?.name,
    reviewedByEmail = reviewedBy?.email,
    reviewedAt = reviewedAt,
    reviewComment = reviewComment,
    approvedSchedule = approvedSchedule(zone),
)

fun ApplicationVisitRescheduleRequest.toAuditSnapshot(): Map<String, Any?> = mapOf(
    "id" to id?.toString(),
    "applicationId" to visit.application.id?.toString(),
    "visitId" to visit.id?.toString(),
    "applicantUserId" to visit.application.user.id,
    "responsibleId" to visit.responsible.id,
    "requestedById" to requestedBy.id,
    "status" to status.name,
    "requestedAt" to requestedAt.toString(),
    "originalStartsAt" to originalStartsAt.toString(),
    "originalEndsAt" to originalEndsAt.toString(),
    "originalDurationMinutes" to durationMinutes(originalStartsAt, originalEndsAt),
    "reasonRecorded" to true,
    "reviewedById" to reviewedBy?.id,
    "reviewedAt" to reviewedAt?.toString(),
    "reviewCommentRecorded" to (reviewComment != null),
    "approvedStartsAt" to approvedStartsAt?.toString(),
    "approvedEndsAt" to approvedEndsAt?.toString(),
    "approvedDurationMinutes" to durationMinutes(approvedStartsAt, approvedEndsAt),
)

private fun ApplicationVisitRescheduleRequest.approvedSchedule(
    zone: ZoneId,
): ApplicationVisitRescheduleScheduleResponse? {
    val startsAt = approvedStartsAt ?: return null
    val endsAt = approvedEndsAt ?: return null
    val location = approvedLocation ?: return null
    return schedule(startsAt, endsAt, location, zone)
}

private fun schedule(
    startsAt: OffsetDateTime,
    endsAt: OffsetDateTime,
    location: String,
    zone: ZoneId,
): ApplicationVisitRescheduleScheduleResponse = ApplicationVisitRescheduleScheduleResponse(
    startsAt = startsAt.atZoneSameInstant(zone).toOffsetDateTime(),
    endsAt = endsAt.atZoneSameInstant(zone).toOffsetDateTime(),
    durationMinutes = requireNotNull(durationMinutes(startsAt, endsAt)),
    location = location,
)

private fun durationMinutes(startsAt: OffsetDateTime?, endsAt: OffsetDateTime?): Int? {
    if (startsAt == null || endsAt == null) return null
    return Math.toIntExact(Duration.between(startsAt, endsAt).toMinutes())
}
