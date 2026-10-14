package com.uade.dda2.server.feature.application.mapper

import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitDetailResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitListItemResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitResponse
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisit
import java.time.Duration
import java.time.ZoneId

fun ApplicationVisit.toResponse(zone: ZoneId): ApplicationVisitResponse = ApplicationVisitResponse(
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

fun ApplicationVisit.toListItemResponse(zone: ZoneId): ApplicationVisitListItemResponse = ApplicationVisitListItemResponse(
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
    cancelledAt = cancelledAt,
    cancelledById = cancelledBy?.id,
    cancelledByName = cancelledBy?.name,
    resultRecordedAt = resultRecordedAt,
    resultRecordedById = resultRecordedBy?.id,
    resultRecordedByName = resultRecordedBy?.name,
)

fun ApplicationVisit.toDetailResponse(zone: ZoneId): ApplicationVisitDetailResponse = ApplicationVisitDetailResponse(
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
