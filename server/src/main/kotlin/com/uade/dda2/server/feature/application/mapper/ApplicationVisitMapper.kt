package com.uade.dda2.server.feature.application.mapper

import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitDetailResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitListItemResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitRescheduleRequestSummaryResponse
import com.uade.dda2.server.feature.application.dto.response.CitizenApplicationVisitDetailResponse
import com.uade.dda2.server.feature.application.dto.response.CitizenApplicationVisitListItemResponse
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisit
import java.time.Duration
import java.time.ZoneId

fun ApplicationVisit.toResponse(
    zone: ZoneId,
    latestRescheduleRequest: ApplicationVisitRescheduleRequestSummaryResponse? = null,
): ApplicationVisitResponse = ApplicationVisitResponse(
    id = requireNotNull(id),
    applicationId = requireNotNull(application.id),
    applicationStatus = application.status,
    type = type,
    startsAt = startsAt.atZoneSameInstant(zone).toOffsetDateTime(),
    endsAt = endsAt.atZoneSameInstant(zone).toOffsetDateTime(),
    durationMinutes = durationMinutes(),
    location = location,
    status = status,
    responsibleId = requireNotNull(responsible.id),
    responsibleName = responsible.name,
    scheduledAt = scheduledAt,
    rescheduled = lastRescheduledAt != null,
    lastRescheduledAt = lastRescheduledAt,
    latestRescheduleRequest = latestRescheduleRequest,
    cancellationReason = cancellationReason,
    cancelledAt = cancelledAt,
    cancelledById = cancelledBy?.id,
    cancelledByName = cancelledBy?.name,
    observations = resultObservations,
    conclusions = resultConclusions,
    resultRecordedAt = resultRecordedAt,
    resultRecordedById = resultRecordedBy?.id,
    resultRecordedByName = resultRecordedBy?.name,
)

fun ApplicationVisit.toListItemResponse(
    zone: ZoneId,
    latestRescheduleRequest: ApplicationVisitRescheduleRequestSummaryResponse? = null,
): ApplicationVisitListItemResponse = ApplicationVisitListItemResponse(
    id = requireNotNull(id),
    applicationId = requireNotNull(application.id),
    applicationNumber = requireNotNull(application.applicationNumber),
    applicationStatus = application.status,
    applicantUserId = requireNotNull(application.user.id),
    applicantName = application.user.name,
    programId = requireNotNull(application.programEdition.program.id),
    programName = application.programEdition.program.name,
    programEditionId = requireNotNull(application.programEdition.id),
    programEditionName = application.programEdition.name,
    type = type,
    startsAt = startsAt.atZoneSameInstant(zone).toOffsetDateTime(),
    endsAt = endsAt.atZoneSameInstant(zone).toOffsetDateTime(),
    durationMinutes = durationMinutes(),
    location = location,
    status = status,
    responsibleId = requireNotNull(responsible.id),
    responsibleName = responsible.name,
    scheduledAt = scheduledAt,
    rescheduled = lastRescheduledAt != null,
    lastRescheduledAt = lastRescheduledAt,
    latestRescheduleRequest = latestRescheduleRequest,
    cancelledAt = cancelledAt,
    cancelledById = cancelledBy?.id,
    cancelledByName = cancelledBy?.name,
    resultRecordedAt = resultRecordedAt,
    resultRecordedById = resultRecordedBy?.id,
    resultRecordedByName = resultRecordedBy?.name,
)

fun ApplicationVisit.toDetailResponse(
    zone: ZoneId,
    latestRescheduleRequest: ApplicationVisitRescheduleRequestSummaryResponse? = null,
): ApplicationVisitDetailResponse = ApplicationVisitDetailResponse(
    id = requireNotNull(id),
    applicationId = requireNotNull(application.id),
    applicationNumber = requireNotNull(application.applicationNumber),
    applicationStatus = application.status,
    applicantUserId = requireNotNull(application.user.id),
    applicantName = application.user.name,
    applicantEmail = application.user.email,
    programId = requireNotNull(application.programEdition.program.id),
    programName = application.programEdition.program.name,
    programEditionId = requireNotNull(application.programEdition.id),
    programEditionName = application.programEdition.name,
    enrollmentPeriodId = requireNotNull(application.enrollmentPeriod.id),
    type = type,
    startsAt = startsAt.atZoneSameInstant(zone).toOffsetDateTime(),
    endsAt = endsAt.atZoneSameInstant(zone).toOffsetDateTime(),
    durationMinutes = durationMinutes(),
    location = location,
    status = status,
    responsibleId = requireNotNull(responsible.id),
    responsibleName = responsible.name,
    responsibleEmail = responsible.email,
    scheduledAt = scheduledAt,
    rescheduled = lastRescheduledAt != null,
    lastRescheduledAt = lastRescheduledAt,
    latestRescheduleRequest = latestRescheduleRequest,
    cancellationReason = cancellationReason,
    cancelledAt = cancelledAt,
    cancelledById = cancelledBy?.id,
    cancelledByName = cancelledBy?.name,
    cancelledByEmail = cancelledBy?.email,
    observations = resultObservations,
    conclusions = resultConclusions,
    resultRecordedAt = resultRecordedAt,
    resultRecordedById = resultRecordedBy?.id,
    resultRecordedByName = resultRecordedBy?.name,
    resultRecordedByEmail = resultRecordedBy?.email,
)

fun ApplicationVisit.toCitizenListItemResponse(
    zone: ZoneId,
    latestRescheduleRequest: ApplicationVisitRescheduleRequestSummaryResponse? = null,
): CitizenApplicationVisitListItemResponse =
    CitizenApplicationVisitListItemResponse(
        id = requireNotNull(id),
        applicationId = requireNotNull(application.id),
        applicationNumber = requireNotNull(application.applicationNumber),
        type = type,
        startsAt = startsAt.atZoneSameInstant(zone).toOffsetDateTime(),
        endsAt = endsAt.atZoneSameInstant(zone).toOffsetDateTime(),
        durationMinutes = durationMinutes(),
        location = location,
        status = status,
        rescheduled = lastRescheduledAt != null,
        lastRescheduledAt = lastRescheduledAt,
        latestRescheduleRequest = latestRescheduleRequest,
        cancelledAt = cancelledAt,
    )

fun ApplicationVisit.toCitizenDetailResponse(
    zone: ZoneId,
    latestRescheduleRequest: ApplicationVisitRescheduleRequestSummaryResponse? = null,
): CitizenApplicationVisitDetailResponse =
    CitizenApplicationVisitDetailResponse(
        id = requireNotNull(id),
        applicationId = requireNotNull(application.id),
        applicationNumber = requireNotNull(application.applicationNumber),
        type = type,
        startsAt = startsAt.atZoneSameInstant(zone).toOffsetDateTime(),
        endsAt = endsAt.atZoneSameInstant(zone).toOffsetDateTime(),
        durationMinutes = durationMinutes(),
        location = location,
        status = status,
        rescheduled = lastRescheduledAt != null,
        lastRescheduledAt = lastRescheduledAt,
        latestRescheduleRequest = latestRescheduleRequest,
        cancelledAt = cancelledAt,
        cancellationReason = cancellationReason,
    )

fun ApplicationVisit.toAuditSnapshot(): Map<String, Any?> = mapOf(
    "id" to id.toString(),
    "applicationId" to application.id.toString(),
    "applicantUserId" to application.user.id,
    "type" to type.name,
    "startsAt" to startsAt.toString(),
    "endsAt" to endsAt.toString(),
    "durationMinutes" to durationMinutes(),
    "status" to status.name,
    "responsibleId" to responsible.id,
    "scheduledAt" to scheduledAt.toString(),
    "lastRescheduledAt" to lastRescheduledAt?.toString(),
    "cancelledAt" to cancelledAt?.toString(),
    "cancelledById" to cancelledBy?.id,
    "resultRecordedAt" to resultRecordedAt?.toString(),
    "resultRecordedById" to resultRecordedBy?.id,
    "applicationStatus" to application.status.name,
)

fun ApplicationVisit.toCreationAuditSnapshot(previousStatus: ApplicationStatus): Map<String, Any?> =
    toAuditSnapshot() + ("previousApplicationStatus" to previousStatus.name)

private fun ApplicationVisit.durationMinutes(): Int =
    Math.toIntExact(Duration.between(startsAt, endsAt).toMinutes())
