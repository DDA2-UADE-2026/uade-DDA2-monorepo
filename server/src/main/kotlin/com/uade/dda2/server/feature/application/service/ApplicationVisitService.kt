package com.uade.dda2.server.feature.application.service

import com.uade.dda2.server.config.AppointmentProperties
import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.application.dto.request.CancelApplicationVisitRequest
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationVisitRequest
import com.uade.dda2.server.feature.application.dto.request.RescheduleApplicationVisitRequest
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitDetailResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitListResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitResponse
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisit
import com.uade.dda2.server.feature.application.entity.ApplicationVisitStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitType
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import com.uade.dda2.server.feature.application.error.ApplicationVisitErrors
import com.uade.dda2.server.feature.application.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.application.mapper.toCreationAuditSnapshot
import com.uade.dda2.server.feature.application.mapper.toDetailResponse
import com.uade.dda2.server.feature.application.mapper.toListItemResponse
import com.uade.dda2.server.feature.application.mapper.toResponse
import com.uade.dda2.server.feature.application.repository.ApplicationEvaluationRepository
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.application.repository.ApplicationVisitRepository
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

const val APPLICATION_VISIT_CREATE_PERMISSION = "applications:visits:create"
const val APPLICATION_VISIT_VIEW_PERMISSION = "applications:visits:view"
const val APPLICATION_VISIT_MANAGE_PERMISSION = "applications:visits:manage"

data class ApplicationVisitFilters(
    val applicationId: UUID? = null,
    val applicationNumber: Long? = null,
    val applicantUserId: Long? = null,
    val responsibleId: Long? = null,
    val programId: UUID? = null,
    val programEditionId: UUID? = null,
    val type: ApplicationVisitType? = null,
    val status: ApplicationVisitStatus? = null,
    val windowStart: OffsetDateTime? = null,
    val windowEnd: OffsetDateTime? = null,
)

@Service
class ApplicationVisitService(
    private val applications: ApplicationRepository,
    private val evaluations: ApplicationEvaluationRepository,
    private val visits: ApplicationVisitRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: ApplicationVisitValidator,
    private val agenda: AgendaAvailabilityService,
    private val logs: LogService,
    private val json: JsonMapper,
    private val applicationTime: EnrollmentPeriodExpirationProperties,
    private val appointmentTime: AppointmentProperties,
    private val entityManager: EntityManager,
) {
    @Transactional
    fun create(applicationId: UUID, request: CreateApplicationVisitRequest): ApplicationVisitResponse {
        val principal = currentUser.principal()
        validator.validateActor(
            users.findByIdWithRoles(principal.id),
            principal,
            APPLICATION_VISIT_CREATE_PERMISSION,
        )
        val application = applications.findByIdForUpdate(applicationId) ?: throw ApplicationErrors.notFound()
        val responsibleId = application.assignedWorker?.id
            ?: throw ApplicationVisitErrors.assignedToAnotherWorker()
        val applicantId = requireNotNull(application.user.id)
        val lockedUsers = users.findAllByIdsForUpdate(listOf(principal.id, responsibleId, applicantId).distinct().sorted())
            .associateBy { requireNotNull(it.id) }
        val actor = validator.validateActor(
            users.findByIdWithRoles(principal.id),
            principal,
            APPLICATION_VISIT_CREATE_PERMISSION,
        )
        if (lockedUsers[responsibleId] == null || lockedUsers[applicantId] == null) {
            throw ApplicationVisitErrors.assignedToAnotherWorker()
        }
        validator.validateApplication(application, actor, evaluations.existsByApplicationId(applicationId))
        val now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
        val data = validator.validateRequest(request, now)
        if (agenda.hasConflict(responsibleId, data.startsAt, data.endsAt)) {
            throw ApplicationVisitErrors.responsibleConflict()
        }
        if (applicantId != responsibleId && agenda.hasConflict(applicantId, data.startsAt, data.endsAt)) {
            throw ApplicationVisitErrors.applicantConflict()
        }

        val previousStatus = application.status
        application.status = ApplicationStatus.IN_VISIT
        application.updatedAt = LocalDateTime.ofInstant(now.toInstant(), applicationTime.zone())

        val visit = visits.saveAndFlush(
            ApplicationVisit(
                application = application,
                type = data.type,
                startsAt = data.startsAt,
                endsAt = data.endsAt,
                location = data.location,
                responsible = actor,
                scheduledAt = now,
            ),
        )
        logs.record(
            user = actor,
            action = LogAction.CREATE,
            entityType = LogEntityType.APPLICATION_VISIT,
            entityId = requireNotNull(visit.id).toString(),
            newValues = json.writeValueAsString(visit.toCreationAuditSnapshot(previousStatus)),
        )
        return visit.toResponse(appointmentTime.zone())
    }

    @Transactional
    fun reschedule(
        applicationId: UUID,
        visitId: UUID,
        request: RescheduleApplicationVisitRequest,
    ): ApplicationVisitResponse {
        val principal = authorizeManage()
        val locked = lockVisit(applicationId, visitId, principal)
        val now = utcNow()
        validator.validateManageable(locked.application, locked.visit, locked.actor, now)
        val data = validator.validateRescheduleRequest(request, now)
        if (
            locked.visit.startsAt.toInstant() == data.startsAt.toInstant() &&
            locked.visit.endsAt.toInstant() == data.endsAt.toInstant() &&
            locked.visit.location == data.location
        ) {
            throw ApplicationVisitErrors.invalidData("La reprogramacion debe modificar el horario, la duracion o el lugar.")
        }
        if (
            agenda.hasConflict(
                locked.responsibleId,
                data.startsAt,
                data.endsAt,
                excludeVisitId = visitId,
            )
        ) {
            throw ApplicationVisitErrors.responsibleConflict()
        }
        if (
            locked.applicantId != locked.responsibleId &&
            agenda.hasConflict(
                locked.applicantId,
                data.startsAt,
                data.endsAt,
                excludeVisitId = visitId,
            )
        ) {
            throw ApplicationVisitErrors.applicantConflict()
        }

        val previousApplicationStatus = locked.application.status
        val previousLocation = locked.visit.location
        val oldValues = json.writeValueAsString(locked.visit.toAuditSnapshot())
        locked.visit.startsAt = data.startsAt
        locked.visit.endsAt = data.endsAt
        locked.visit.location = data.location
        locked.application.updatedAt = LocalDateTime.ofInstant(now.toInstant(), applicationTime.zone())
        visits.saveAndFlush(locked.visit)
        recordUpdate(
            actor = locked.actor,
            visit = locked.visit,
            operation = "RESCHEDULE",
            oldValues = oldValues,
            previousApplicationStatus = previousApplicationStatus,
            extra = mapOf("locationChanged" to (previousLocation != data.location)),
        )
        return locked.visit.toResponse(appointmentTime.zone())
    }

    @Transactional
    fun cancel(
        applicationId: UUID,
        visitId: UUID,
        request: CancelApplicationVisitRequest,
    ): ApplicationVisitResponse {
        val principal = authorizeManage()
        val reason = validator.validateCancellationRequest(request)
        val locked = lockVisit(applicationId, visitId, principal)
        val now = utcNow()
        validator.validateManageable(locked.application, locked.visit, locked.actor, now)

        val previousApplicationStatus = locked.application.status
        val oldValues = json.writeValueAsString(locked.visit.toAuditSnapshot())
        locked.visit.status = ApplicationVisitStatus.CANCELLED
        locked.visit.cancellationReason = reason
        locked.visit.cancelledAt = now
        locked.visit.cancelledBy = locked.actor
        visits.saveAndFlush(locked.visit)
        if (
            !visits.existsByApplicationIdAndStatusAndIdNot(
                applicationId,
                ApplicationVisitStatus.SCHEDULED,
                visitId,
            )
        ) {
            locked.application.status = ApplicationStatus.IN_EVALUATION
        }
        locked.application.updatedAt = LocalDateTime.ofInstant(now.toInstant(), applicationTime.zone())
        recordUpdate(
            actor = locked.actor,
            visit = locked.visit,
            operation = "CANCEL",
            oldValues = oldValues,
            previousApplicationStatus = previousApplicationStatus,
            extra = mapOf("cancellationReasonRecorded" to true),
        )
        return locked.visit.toResponse(appointmentTime.zone())
    }

    @Transactional(readOnly = true)
    fun list(page: Int, size: Int, filters: ApplicationVisitFilters): ApplicationVisitListResponse {
        authorize(APPLICATION_VISIT_VIEW_PERMISSION)
        val windowStart = filters.windowStart?.toInstant()?.atOffset(ZoneOffset.UTC)
        val windowEnd = filters.windowEnd?.toInstant()?.atOffset(ZoneOffset.UTC)
        if (windowStart != null && windowEnd != null && !windowStart.isBefore(windowEnd)) {
            throw ApplicationVisitErrors.invalidFilter("windowStart debe ser anterior a windowEnd.")
        }
        val results = visits.search(
            applicationId = filters.applicationId,
            applicationNumber = filters.applicationNumber,
            applicantUserId = filters.applicantUserId,
            responsibleId = filters.responsibleId,
            programId = filters.programId,
            programEditionId = filters.programEditionId,
            type = filters.type,
            status = filters.status,
            windowStart = windowStart,
            windowEnd = windowEnd,
            pageable = PageRequest.of(
                page,
                size,
                Sort.by("startsAt").ascending().and(Sort.by("id").ascending()),
            ),
        )
        return ApplicationVisitListResponse(
            content = results.content.map { it.toListItemResponse(appointmentTime.zone()) },
            page = results.number,
            size = results.size,
            totalElements = results.totalElements,
            totalPages = results.totalPages,
        )
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): ApplicationVisitDetailResponse {
        authorize(APPLICATION_VISIT_VIEW_PERMISSION)
        return (visits.findDetailById(id) ?: throw ApplicationVisitErrors.notFound())
            .toDetailResponse(appointmentTime.zone())
    }

    private fun authorize(permission: String) {
        val principal = currentUser.principal()
        validator.validateActor(users.findByIdWithRoles(principal.id), principal, permission)
    }

    private fun authorizeManage(): JwtPrincipal {
        val principal = currentUser.principal()
        validator.validateActor(
            users.findByIdWithRoles(principal.id),
            principal,
            APPLICATION_VISIT_MANAGE_PERMISSION,
        )
        return principal
    }

    private fun lockVisit(applicationId: UUID, visitId: UUID, principal: JwtPrincipal): LockedApplicationVisit {
        val snapshot = managementSnapshot(applicationId, visitId)
        val application = applications.findByIdForUpdate(applicationId) ?: throw ApplicationErrors.notFound()
        entityManager.refresh(application)
        if (
            application.status != snapshot.applicationStatus ||
            application.user.id != snapshot.applicantId ||
            application.assignedWorker?.id != snapshot.assignedWorkerId
        ) {
            throw ApplicationVisitErrors.concurrentModification()
        }
        val lockedUsers = users.findAllByIdsForUpdate(
            listOfNotNull(
                principal.id,
                snapshot.applicantId,
                snapshot.responsibleId,
                snapshot.assignedWorkerId,
            ).distinct().sorted(),
        ).associateBy { requireNotNull(it.id) }
        val actor = validator.validateActor(
            users.findByIdWithRoles(principal.id),
            principal,
            APPLICATION_VISIT_MANAGE_PERMISSION,
        )
        if (
            lockedUsers[snapshot.applicantId] == null ||
            lockedUsers[snapshot.responsibleId] == null ||
            (snapshot.assignedWorkerId != null && lockedUsers[snapshot.assignedWorkerId] == null)
        ) {
            throw ApplicationVisitErrors.concurrentModification()
        }
        val visit = visits.findByIdForUpdate(visitId) ?: throw ApplicationVisitErrors.notFound()
        entityManager.refresh(visit, LockModeType.PESSIMISTIC_WRITE)
        if (!snapshot.matches(visit, application)) {
            throw ApplicationVisitErrors.concurrentModification()
        }
        return LockedApplicationVisit(
            application = application,
            visit = visit,
            actor = actor,
            applicantId = snapshot.applicantId,
            responsibleId = snapshot.responsibleId,
        )
    }

    private fun managementSnapshot(applicationId: UUID, visitId: UUID): ApplicationVisitManagementSnapshot {
        val visit = visits.findDetailById(visitId)
        if (visit == null) {
            if (!applications.existsById(applicationId)) throw ApplicationErrors.notFound()
            throw ApplicationVisitErrors.notFound()
        }
        if (visit.application.id != applicationId) throw ApplicationVisitErrors.notFound()
        return ApplicationVisitManagementSnapshot(
            applicationId = requireNotNull(visit.application.id),
            applicationStatus = visit.application.status,
            applicantId = requireNotNull(visit.application.user.id),
            assignedWorkerId = visit.application.assignedWorker?.id,
            visitId = requireNotNull(visit.id),
            type = visit.type,
            responsibleId = requireNotNull(visit.responsible.id),
            startsAt = visit.startsAt.toInstant(),
            endsAt = visit.endsAt.toInstant(),
            location = visit.location,
            status = visit.status,
            scheduledAt = visit.scheduledAt.toInstant(),
            cancellationReason = visit.cancellationReason,
            cancelledAt = visit.cancelledAt?.toInstant(),
            cancelledById = visit.cancelledBy?.id,
        )
    }

    private fun recordUpdate(
        actor: User,
        visit: ApplicationVisit,
        operation: String,
        oldValues: String,
        previousApplicationStatus: ApplicationStatus,
        extra: Map<String, Any?>,
    ) {
        val newValues = visit.toAuditSnapshot() +
            mapOf(
                "operation" to operation,
                "previousApplicationStatus" to previousApplicationStatus.name,
            ) + extra
        logs.record(
            user = actor,
            action = LogAction.UPDATE,
            entityType = LogEntityType.APPLICATION_VISIT,
            entityId = requireNotNull(visit.id).toString(),
            oldValues = oldValues,
            newValues = json.writeValueAsString(newValues),
        )
    }

    private fun utcNow(): OffsetDateTime =
        OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)

    private data class LockedApplicationVisit(
        val application: Application,
        val visit: ApplicationVisit,
        val actor: User,
        val applicantId: Long,
        val responsibleId: Long,
    )

    private data class ApplicationVisitManagementSnapshot(
        val applicationId: UUID,
        val applicationStatus: ApplicationStatus,
        val applicantId: Long,
        val assignedWorkerId: Long?,
        val visitId: UUID,
        val type: ApplicationVisitType,
        val responsibleId: Long,
        val startsAt: Instant,
        val endsAt: Instant,
        val location: String,
        val status: ApplicationVisitStatus,
        val scheduledAt: Instant,
        val cancellationReason: String?,
        val cancelledAt: Instant?,
        val cancelledById: Long?,
    ) {
        fun matches(visit: ApplicationVisit, application: Application): Boolean =
            visit.id == visitId &&
                visit.application.id == applicationId &&
                application.id == applicationId &&
                application.status == applicationStatus &&
                application.user.id == applicantId &&
                application.assignedWorker?.id == assignedWorkerId &&
                visit.type == type &&
                visit.responsible.id == responsibleId &&
                visit.startsAt.toInstant() == startsAt &&
                visit.endsAt.toInstant() == endsAt &&
                visit.location == location &&
                visit.status == status &&
                visit.scheduledAt.toInstant() == scheduledAt &&
                visit.cancellationReason == cancellationReason &&
                visit.cancelledAt?.toInstant() == cancelledAt &&
                visit.cancelledBy?.id == cancelledById
    }
}
