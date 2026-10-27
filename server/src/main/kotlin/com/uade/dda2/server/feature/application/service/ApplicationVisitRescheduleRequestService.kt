package com.uade.dda2.server.feature.application.service

import com.uade.dda2.server.config.AppointmentProperties
import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationVisitRescheduleRequest
import com.uade.dda2.server.feature.application.dto.request.ReviewApplicationVisitRescheduleRequest
import com.uade.dda2.server.feature.application.dto.response.AdminApplicationVisitRescheduleRequestDetailResponse
import com.uade.dda2.server.feature.application.dto.response.AdminApplicationVisitRescheduleRequestListResponse
import com.uade.dda2.server.feature.application.dto.response.CitizenApplicationVisitRescheduleRequestDetailResponse
import com.uade.dda2.server.feature.application.dto.response.CitizenApplicationVisitRescheduleRequestListResponse
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisit
import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleRequest
import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleRequestStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitStatus
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import com.uade.dda2.server.feature.application.error.ApplicationVisitErrors
import com.uade.dda2.server.feature.application.error.ApplicationVisitRescheduleRequestErrors
import com.uade.dda2.server.feature.application.mapper.toAdminDetailResponse
import com.uade.dda2.server.feature.application.mapper.toAdminListItemResponse
import com.uade.dda2.server.feature.application.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.application.mapper.toCitizenDetailResponse
import com.uade.dda2.server.feature.application.mapper.toCitizenListItemResponse
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.application.repository.ApplicationVisitRepository
import com.uade.dda2.server.feature.application.repository.ApplicationVisitRescheduleRequestRepository
import com.uade.dda2.server.feature.application.validator.ApplicationVisitRescheduleRequestValidator
import com.uade.dda2.server.feature.application.validator.ApplicationVisitValidator
import com.uade.dda2.server.feature.appointment.service.AgendaAvailabilityService
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.security.JwtPrincipal
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

const val APPLICATION_OWN_VISIT_RESCHEDULE_REQUEST_CREATE_PERMISSION =
    "applications:own:visits:reschedule-requests:create"
const val APPLICATION_VISIT_RESCHEDULE_REQUEST_VIEW_PERMISSION =
    "applications:visits:reschedule-requests:view"
const val APPLICATION_VISIT_RESCHEDULE_REQUEST_REVIEW_PERMISSION =
    "applications:visits:reschedule-requests:review"

data class ApplicationVisitRescheduleRequestFilters(
    val status: ApplicationVisitRescheduleRequestStatus? = null,
    val applicationId: UUID? = null,
    val applicationNumber: Long? = null,
    val visitId: UUID? = null,
    val applicantUserId: Long? = null,
    val responsibleId: Long? = null,
    val programId: UUID? = null,
    val programEditionId: UUID? = null,
    val requestedFrom: OffsetDateTime? = null,
    val requestedTo: OffsetDateTime? = null,
)

@Service
class ApplicationVisitRescheduleRequestService(
    private val applications: ApplicationRepository,
    private val visits: ApplicationVisitRepository,
    private val requests: ApplicationVisitRescheduleRequestRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val visitValidator: ApplicationVisitValidator,
    private val requestValidator: ApplicationVisitRescheduleRequestValidator,
    private val agenda: AgendaAvailabilityService,
    private val logs: LogService,
    private val json: JsonMapper,
    private val applicationTime: EnrollmentPeriodExpirationProperties,
    private val appointmentTime: AppointmentProperties,
    private val entityManager: EntityManager,
) {
    @Transactional
    fun createOwn(
        applicationId: UUID,
        visitId: UUID,
        request: CreateApplicationVisitRescheduleRequest,
    ): CitizenApplicationVisitRescheduleRequestDetailResponse {
        val reason = requestValidator.validateCreate(request)
        val principal = authorizeFor(APPLICATION_OWN_VISIT_RESCHEDULE_REQUEST_CREATE_PERMISSION)
        applications.findByIdAndUserId(applicationId, principal.id) ?: throw ApplicationErrors.notFound()
        val locked = lockVisit(applicationId, visitId, principal, APPLICATION_OWN_VISIT_RESCHEDULE_REQUEST_CREATE_PERMISSION)
        if (locked.applicantId != principal.id) throw ApplicationErrors.notFound()
        val now = utcNow()
        validateRequestable(locked.application, locked.visit, now)
        if (requests.existsByVisitIdAndStatus(visitId, ApplicationVisitRescheduleRequestStatus.PENDING)) {
            throw ApplicationVisitRescheduleRequestErrors.alreadyPending()
        }

        val created = requests.saveAndFlush(
            ApplicationVisitRescheduleRequest(
                visit = locked.visit,
                requestedBy = locked.actor,
                reason = reason,
                requestedAt = now,
                originalStartsAt = locked.visit.startsAt,
                originalEndsAt = locked.visit.endsAt,
                originalLocation = locked.visit.location,
            ),
        )
        locked.application.updatedAt = LocalDateTime.ofInstant(now.toInstant(), applicationTime.zone())
        logs.record(
            user = locked.actor,
            action = LogAction.CREATE,
            entityType = LogEntityType.APPLICATION_VISIT_RESCHEDULE_REQUEST,
            entityId = requireNotNull(created.id).toString(),
            newValues = json.writeValueAsString(created.toAuditSnapshot()),
        )
        return created.toCitizenDetailResponse(appointmentTime.zone())
    }

    @Transactional(readOnly = true)
    fun listOwn(
        applicationId: UUID,
        visitId: UUID,
        page: Int,
        size: Int,
    ): CitizenApplicationVisitRescheduleRequestListResponse {
        val principal = authorizeFor(APPLICATION_OWN_VISIT_VIEW_PERMISSION)
        ensureOwnVisit(applicationId, visitId, principal.id)
        val result = requests.findAllOwn(
            applicationId,
            visitId,
            principal.id,
            PageRequest.of(
                page,
                size,
                Sort.by("requestedAt").descending().and(Sort.by("id").descending()),
            ),
        )
        return CitizenApplicationVisitRescheduleRequestListResponse(
            content = result.content.map { it.toCitizenListItemResponse(appointmentTime.zone()) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional(readOnly = true)
    fun getOwn(
        applicationId: UUID,
        visitId: UUID,
        requestId: UUID,
    ): CitizenApplicationVisitRescheduleRequestDetailResponse {
        val principal = authorizeFor(APPLICATION_OWN_VISIT_VIEW_PERMISSION)
        ensureOwnVisit(applicationId, visitId, principal.id)
        return (requests.findOwnDetail(requestId, applicationId, visitId, principal.id)
            ?: throw ApplicationVisitRescheduleRequestErrors.notFound())
            .toCitizenDetailResponse(appointmentTime.zone())
    }

    @Transactional(readOnly = true)
    fun listAdmin(
        page: Int,
        size: Int,
        filters: ApplicationVisitRescheduleRequestFilters,
    ): AdminApplicationVisitRescheduleRequestListResponse {
        authorizeFor(APPLICATION_VISIT_RESCHEDULE_REQUEST_VIEW_PERMISSION)
        val requestedFrom = filters.requestedFrom?.toInstant()?.atOffset(ZoneOffset.UTC)
        val requestedTo = filters.requestedTo?.toInstant()?.atOffset(ZoneOffset.UTC)
        if (requestedFrom != null && requestedTo != null && !requestedFrom.isBefore(requestedTo)) {
            throw ApplicationVisitRescheduleRequestErrors.invalidFilter(
                "requestedFrom debe ser anterior a requestedTo.",
            )
        }
        val result = requests.search(
            status = filters.status,
            applicationId = filters.applicationId,
            applicationNumber = filters.applicationNumber,
            visitId = filters.visitId,
            applicantUserId = filters.applicantUserId,
            responsibleId = filters.responsibleId,
            programId = filters.programId,
            programEditionId = filters.programEditionId,
            requestedFrom = requestedFrom,
            requestedTo = requestedTo,
            pageable = PageRequest.of(
                page,
                size,
                Sort.by("requestedAt").ascending().and(Sort.by("id").ascending()),
            ),
        )
        return AdminApplicationVisitRescheduleRequestListResponse(
            content = result.content.map { it.toAdminListItemResponse(appointmentTime.zone()) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional(readOnly = true)
    fun getAdmin(requestId: UUID): AdminApplicationVisitRescheduleRequestDetailResponse {
        authorizeFor(APPLICATION_VISIT_RESCHEDULE_REQUEST_VIEW_PERMISSION)
        return (requests.findAdminDetailById(requestId) ?: throw ApplicationVisitRescheduleRequestErrors.notFound())
            .toAdminDetailResponse(appointmentTime.zone())
    }

    @Transactional
    fun review(
        applicationId: UUID,
        visitId: UUID,
        requestId: UUID,
        request: ReviewApplicationVisitRescheduleRequest,
    ): AdminApplicationVisitRescheduleRequestDetailResponse {
        val review = requestValidator.validateReview(request)
        val principal = authorizeFor(APPLICATION_VISIT_RESCHEDULE_REQUEST_REVIEW_PERMISSION)
        val locked = lockVisit(applicationId, visitId, principal, APPLICATION_VISIT_RESCHEDULE_REQUEST_REVIEW_PERMISSION)
        if (
            locked.application.assignedWorker?.id != locked.actor.id ||
            locked.visit.responsible.id != locked.actor.id
        ) {
            throw ApplicationVisitErrors.assignedToAnotherWorker()
        }
        val rescheduleRequest = requests.findByIdForUpdate(requestId)
            ?: throw ApplicationVisitRescheduleRequestErrors.notFound()
        entityManager.refresh(rescheduleRequest, LockModeType.PESSIMISTIC_WRITE)
        if (rescheduleRequest.visit.id != visitId || rescheduleRequest.visit.application.id != applicationId) {
            throw ApplicationVisitRescheduleRequestErrors.notFound()
        }
        if (rescheduleRequest.status != ApplicationVisitRescheduleRequestStatus.PENDING) {
            throw ApplicationVisitRescheduleRequestErrors.notReviewable()
        }

        val now = utcNow()
        val oldRequestValues = json.writeValueAsString(rescheduleRequest.toAuditSnapshot())
        rescheduleRequest.status = review.status
        rescheduleRequest.reviewedBy = locked.actor
        rescheduleRequest.reviewedAt = now
        rescheduleRequest.reviewComment = review.comment
        if (review.status == ApplicationVisitRescheduleRequestStatus.APPROVED) {
            approve(locked, rescheduleRequest, request, requestId, now)
        }
        requests.saveAndFlush(rescheduleRequest)
        locked.application.updatedAt = LocalDateTime.ofInstant(now.toInstant(), applicationTime.zone())
        logs.record(
            user = locked.actor,
            action = LogAction.UPDATE,
            entityType = LogEntityType.APPLICATION_VISIT_RESCHEDULE_REQUEST,
            entityId = requestId.toString(),
            oldValues = oldRequestValues,
            newValues = json.writeValueAsString(rescheduleRequest.toAuditSnapshot()),
        )
        return (requests.findAdminDetailById(requestId) ?: throw ApplicationVisitRescheduleRequestErrors.notFound())
            .toAdminDetailResponse(appointmentTime.zone())
    }

    private fun approve(
        locked: LockedVisit,
        rescheduleRequest: ApplicationVisitRescheduleRequest,
        request: ReviewApplicationVisitRescheduleRequest,
        requestId: UUID,
        now: OffsetDateTime,
    ) {
        if (
            locked.application.status != ApplicationStatus.IN_VISIT ||
            locked.visit.status != ApplicationVisitStatus.SCHEDULED ||
            !locked.visit.startsAt.toInstant().isAfter(now.toInstant()) ||
            locked.visit.startsAt.toInstant() != rescheduleRequest.originalStartsAt.toInstant() ||
            locked.visit.endsAt.toInstant() != rescheduleRequest.originalEndsAt.toInstant() ||
            locked.visit.location != rescheduleRequest.originalLocation
        ) {
            throw ApplicationVisitRescheduleRequestErrors.notReviewable()
        }
        val schedule = visitValidator.validateSchedule(
            request.startsAt,
            request.durationMinutes,
            request.location,
            now,
            ApplicationVisitRescheduleRequestErrors::invalidData,
        )
        if (
            locked.visit.startsAt.toInstant() == schedule.startsAt.toInstant() &&
            locked.visit.endsAt.toInstant() == schedule.endsAt.toInstant() &&
            locked.visit.location == schedule.location
        ) {
            throw ApplicationVisitRescheduleRequestErrors.invalidData(
                "La aprobacion debe modificar el horario, la duracion o el lugar.",
            )
        }
        if (agenda.hasConflict(locked.responsibleId, schedule.startsAt, schedule.endsAt, locked.visitId)) {
            throw ApplicationVisitErrors.responsibleConflict()
        }
        if (
            locked.applicantId != locked.responsibleId &&
            agenda.hasConflict(locked.applicantId, schedule.startsAt, schedule.endsAt, locked.visitId)
        ) {
            throw ApplicationVisitErrors.applicantConflict()
        }

        val oldVisitValues = json.writeValueAsString(locked.visit.toAuditSnapshot())
        val locationChanged = locked.visit.location != schedule.location
        locked.visit.startsAt = schedule.startsAt
        locked.visit.endsAt = schedule.endsAt
        locked.visit.location = schedule.location
        locked.visit.lastRescheduledAt = now
        rescheduleRequest.approvedStartsAt = schedule.startsAt
        rescheduleRequest.approvedEndsAt = schedule.endsAt
        rescheduleRequest.approvedLocation = schedule.location
        visits.saveAndFlush(locked.visit)
        logs.record(
            user = locked.actor,
            action = LogAction.UPDATE,
            entityType = LogEntityType.APPLICATION_VISIT,
            entityId = locked.visitId.toString(),
            oldValues = oldVisitValues,
            newValues = json.writeValueAsString(
                locked.visit.toAuditSnapshot() + mapOf(
                    "operation" to "RESCHEDULE_REQUEST_APPROVED",
                    "rescheduleRequestId" to requestId.toString(),
                    "locationChanged" to locationChanged,
                ),
            ),
        )
    }

    private fun validateRequestable(application: Application, visit: ApplicationVisit, now: OffsetDateTime) {
        if (
            application.status != ApplicationStatus.IN_VISIT ||
            visit.status != ApplicationVisitStatus.SCHEDULED ||
            !visit.startsAt.toInstant().isAfter(now.toInstant()) ||
            application.assignedWorker?.id != visit.responsible.id
        ) {
            throw ApplicationVisitRescheduleRequestErrors.notAllowed()
        }
    }

    private fun ensureOwnVisit(applicationId: UUID, visitId: UUID, userId: Long) {
        applications.findByIdAndUserId(applicationId, userId) ?: throw ApplicationErrors.notFound()
        visits.findOwnDetailById(visitId, applicationId, userId) ?: throw ApplicationVisitErrors.notFound()
    }

    private fun authorizeFor(permission: String): JwtPrincipal {
        val principal = currentUser.principal()
        visitValidator.validateActor(users.findByIdWithRoles(principal.id), principal, permission)
        return principal
    }

    private fun lockVisit(
        applicationId: UUID,
        visitId: UUID,
        principal: JwtPrincipal,
        permission: String,
    ): LockedVisit {
        val snapshot = visits.findDetailById(visitId)
        if (snapshot == null) {
            if (!applications.existsById(applicationId)) throw ApplicationErrors.notFound()
            throw ApplicationVisitErrors.notFound()
        }
        if (snapshot.application.id != applicationId) throw ApplicationVisitErrors.notFound()
        val applicantId = requireNotNull(snapshot.application.user.id)
        val responsibleId = requireNotNull(snapshot.responsible.id)
        val assignedWorkerId = snapshot.application.assignedWorker?.id
        val application = applications.findByIdForUpdate(applicationId) ?: throw ApplicationErrors.notFound()
        entityManager.refresh(application, LockModeType.PESSIMISTIC_WRITE)
        val lockedUsers = users.findAllByIdsForUpdate(
            listOfNotNull(principal.id, applicantId, responsibleId, assignedWorkerId).distinct().sorted(),
        ).associateBy { requireNotNull(it.id) }
        val actor = visitValidator.validateActor(users.findByIdWithRoles(principal.id), principal, permission)
        if (
            lockedUsers[applicantId] == null ||
            lockedUsers[responsibleId] == null ||
            application.user.id != applicantId ||
            application.assignedWorker?.id != assignedWorkerId
        ) {
            throw ApplicationVisitErrors.concurrentModification()
        }
        val visit = visits.findByIdForUpdate(visitId) ?: throw ApplicationVisitErrors.notFound()
        entityManager.refresh(visit, LockModeType.PESSIMISTIC_WRITE)
        if (
            visit.application.id != applicationId ||
            visit.responsible.id != responsibleId
        ) {
            throw ApplicationVisitErrors.concurrentModification()
        }
        return LockedVisit(
            application = application,
            visit = visit,
            actor = actor,
            visitId = visitId,
            applicantId = applicantId,
            responsibleId = responsibleId,
        )
    }

    private fun utcNow(): OffsetDateTime =
        OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)

    private data class LockedVisit(
        val application: Application,
        val visit: ApplicationVisit,
        val actor: User,
        val visitId: UUID,
        val applicantId: Long,
        val responsibleId: Long,
    )
}
