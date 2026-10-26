package com.uade.dda2.server.feature.application.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.application.dto.request.AssignApplicationWorkerRequest
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationRequest
import com.uade.dda2.server.feature.application.dto.request.CreateAssistedApplicationRequest
import com.uade.dda2.server.feature.application.dto.response.AdminApplicationListResponse
import com.uade.dda2.server.feature.application.dto.response.AdminApplicationResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationListResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationResponse
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitStatus
import com.uade.dda2.server.feature.application.repository.ApplicationEvaluationRepository
import com.uade.dda2.server.feature.application.repository.ApplicationVisitRepository
import com.uade.dda2.server.feature.program.mapper.toAvailableDocumentRequirementResponse
import com.uade.dda2.server.feature.program.repository.ProgramDocumentRequirementRepository
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import com.uade.dda2.server.feature.application.mapper.toAdminListItemResponse
import com.uade.dda2.server.feature.application.mapper.toAdminResponse
import com.uade.dda2.server.feature.application.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.application.mapper.toResponse
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.application.validator.ApplicationValidator
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.HexFormat
import java.util.UUID

data class ApplicationSubmission(val application: ApplicationResponse, val replayed: Boolean)

const val APPLICATION_ASSIGN_PERMISSION = "applications:management:assign"

// Solicitudes todavía en trabajo social: antes de la resolución y fuera de borrador.
private val ASSIGNABLE_STATUSES = ApplicationStatus.EVALUATION_ALLOWED + ApplicationStatus.IN_VISIT

@Service
class ApplicationService(
    private val applications: ApplicationRepository,
    private val evaluations: ApplicationEvaluationRepository,
    private val visits: ApplicationVisitRepository,
    private val users: UserRepository,
    private val periods: EnrollmentPeriodRepository,
    private val editions: ProgramEditionRepository,
    private val currentUser: CurrentUserService,
    private val validator: ApplicationValidator,
    private val logs: LogService,
    private val json: JsonMapper,
    private val timeProperties: EnrollmentPeriodExpirationProperties,
    private val pendingDocuments: ApplicationPendingDocumentService,
    private val documentRequirements: ProgramDocumentRequirementRepository,
) {
    @Transactional
    fun submit(request: CreateApplicationRequest, idempotencyKey: String?): ApplicationSubmission {
        validator.validateIdempotencyKey(idempotencyKey)
        val principal = currentUser.principal()
        // Serialize submissions for this user, including different keys and different periods.
        val user = validator.validateUser(users.findByIdForUpdate(principal.id), principal, "applications:own:create")
        return submitFor(request, idempotencyKey, user, user)
    }

    @Transactional
    fun submitAssisted(request: CreateAssistedApplicationRequest, idempotencyKey: String?): ApplicationSubmission {
        validator.validateIdempotencyKey(idempotencyKey)
        val principal = currentUser.principal()
        val actor = validator.validateUser(users.findByIdWithRoles(principal.id), principal, "applications:management:create")
        if (request.userId == principal.id) throw ApplicationErrors.assistedSelfNotAllowed()
        // Both entry points serialize on the applicant, never on the administrative actor.
        val applicant = users.findByIdForUpdate(request.userId) ?: throw ApplicationErrors.applicantNotFound()
        return submitFor(CreateApplicationRequest(request.enrollmentPeriodId), idempotencyKey, applicant, actor)
    }

    private fun submitFor(request: CreateApplicationRequest, idempotencyKey: String?, user: User, actor: User): ApplicationSubmission {
        val userId = requireNotNull(user.id)
        val requestHash = hashRequest(request)
        if (idempotencyKey != null) {
            applications.findByUserIdAndIdempotencyKey(userId, idempotencyKey)?.let { existing ->
                if (existing.requestHash != requestHash || existing.enrollmentPeriod.id != request.enrollmentPeriodId) {
                    throw ApplicationErrors.idempotencyConflict()
                }
                return ApplicationSubmission(response(existing), replayed = true)
            }
        }

        val editionId = periods.findProgramEditionIdById(request.enrollmentPeriodId)
            ?: throw ApplicationErrors.periodNotFound()
        // Same lock order as the administrative period operations: edition, then period.
        editions.findByIdForUpdate(editionId) ?: throw ApplicationErrors.periodNotFound()
        val period = periods.findByIdForUpdate(request.enrollmentPeriodId) ?: throw ApplicationErrors.periodNotFound()
        // PostgreSQL timestamps store microseconds; keep first and replay responses identical.
        val now = LocalDateTime.now(timeProperties.zone()).truncatedTo(ChronoUnit.MICROS)
        validator.validatePeriod(period, now.toLocalDate())
        if (applications.existsByUserIdAndEnrollmentPeriodId(userId, request.enrollmentPeriodId)) {
            throw ApplicationErrors.duplicatePeriod()
        }
        if (applications.existsByUserIdAndProgramEditionIdAndStatusNotIn(userId, editionId, ApplicationStatus.ALLOWS_NEW_PERIOD)) {
            throw ApplicationErrors.blockingApplication()
        }

        val application = applications.saveAndFlush(Application(
            user = user, registeredBy = actor, programEdition = period.programEdition, enrollmentPeriod = period,
            submittedAt = now, idempotencyKey = idempotencyKey,
            requestHash = requestHash.takeIf { idempotencyKey != null },
        ))
        logs.record(user = actor, action = LogAction.CREATE, entityType = LogEntityType.APPLICATION,
            entityId = requireNotNull(application.id).toString(),
            newValues = json.writeValueAsString(application.toAuditSnapshot()))
        return ApplicationSubmission(response(application), replayed = false)
    }

    @Transactional(readOnly = true)
    fun list(page: Int, size: Int): ApplicationListResponse {
        val userId = authorizedUserId()
        val results = applications.findAllByUserId(userId, PageRequest.of(page, size, Sort.by("applicationNumber").descending()))
        val pendingByApplication = pendingDocuments.calculate(results.content)
        val editionIds = results.content.map { requireNotNull(it.programEdition.id) }.distinct()
        val catalogByEdition = if (editionIds.isEmpty()) emptyMap() else documentRequirements.findAllByProgramEditionIdIn(editionIds)
            .sortedWith(compareBy({ it.name }, { it.code }))
            .groupBy { requireNotNull(it.programEdition.id) }
        return ApplicationListResponse(results.content.map {
            it.toResponse(pendingByApplication[requireNotNull(it.id)].orEmpty(),
                catalogByEdition[it.programEdition.id].orEmpty().map { requirement -> requirement.toAvailableDocumentRequirementResponse() })
        }, results.number, results.size,
            results.totalElements, results.totalPages)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): ApplicationResponse =
        (applications.findByIdAndUserId(id, authorizedUserId()) ?: throw ApplicationErrors.notFound()).let {
            response(it)
        }

    @Transactional(readOnly = true)
    fun listAdmin(page: Int, size: Int, programId: UUID? = null): AdminApplicationListResponse {
        authorized("applications:management:view")
        // Same ordering as the own listing; the page covers every titular, optionally narrowed to one program.
        val pageable = PageRequest.of(page, size, Sort.by("applicationNumber").descending())
        val results = if (programId == null) applications.findAll(pageable)
        else applications.findAllByProgramEditionProgramId(programId, pageable)
        return AdminApplicationListResponse(results.content.map { it.toAdminListItemResponse() },
            results.number, results.size, results.totalElements, results.totalPages)
    }

    @Transactional(readOnly = true)
    fun getAdmin(id: UUID): AdminApplicationResponse {
        authorized("applications:management:view")
        val application = applications.findDetailById(id) ?: throw ApplicationErrors.notFound()
        return detail(application)
    }

    @Transactional
    fun assignWorker(id: UUID, request: AssignApplicationWorkerRequest): AdminApplicationResponse {
        val actor = authorized(APPLICATION_ASSIGN_PERMISSION)
        val userId = request.userId ?: throw ApplicationErrors.invalidAssignment("El usuario a asignar es obligatorio.")
        val application = applications.findByIdForUpdate(id) ?: throw ApplicationErrors.notFound()
        if (application.status !in ASSIGNABLE_STATUSES) throw ApplicationErrors.assignmentStatusNotAllowed()
        val assignee = users.findByIdWithRoles(userId)?.takeIf { it.active } ?: throw ApplicationErrors.assigneeNotFound()
        val required = buildList {
            add(APPLICATION_VISIT_CREATE_PERMISSION)
            if (!evaluations.existsByApplicationId(id)) add(APPLICATION_EVALUATION_CREATE_PERMISSION)
        }
        // Basta con que alguno de sus roles tenga los permisos: el rol activo se elige al iniciar sesión.
        if (assignee.roles.none { role -> required.all { permission -> role.permissions.any { it.name == permission } } }) {
            throw ApplicationErrors.assigneeNotEligible()
        }
        val previousWorkerId = application.assignedWorker?.id
        if (previousWorkerId == assignee.id) return detail(application)
        // El responsable de cada visita es el trabajador asignado: reasignar con visitas programadas las dejaría huérfanas.
        if (visits.existsByApplicationIdAndStatus(id, ApplicationVisitStatus.SCHEDULED)) {
            throw ApplicationErrors.assignmentWithScheduledVisits()
        }

        application.assignedWorker = assignee
        application.updatedAt = LocalDateTime.now(timeProperties.zone()).truncatedTo(ChronoUnit.MICROS)
        applications.saveAndFlush(application)
        logs.record(
            user = actor,
            action = LogAction.UPDATE,
            entityType = LogEntityType.APPLICATION,
            entityId = id.toString(),
            oldValues = json.writeValueAsString(mapOf("assignedWorkerUserId" to previousWorkerId)),
            newValues = json.writeValueAsString(mapOf("operation" to "ASSIGN_WORKER", "assignedWorkerUserId" to assignee.id)),
        )
        return detail(application)
    }

    @Transactional
    fun unassignWorker(id: UUID): AdminApplicationResponse {
        val actor = authorized(APPLICATION_ASSIGN_PERMISSION)
        val application = applications.findByIdForUpdate(id) ?: throw ApplicationErrors.notFound()
        if (application.status !in ASSIGNABLE_STATUSES) throw ApplicationErrors.assignmentStatusNotAllowed()
        val previousWorkerId = application.assignedWorker?.id ?: return detail(application)
        // Sin asignado, quien registra la evaluación queda asignado; una vez evaluada, las visitas necesitan uno.
        if (evaluations.existsByApplicationId(id)) throw ApplicationErrors.unassignmentAfterEvaluation()

        application.assignedWorker = null
        application.updatedAt = LocalDateTime.now(timeProperties.zone()).truncatedTo(ChronoUnit.MICROS)
        applications.saveAndFlush(application)
        logs.record(
            user = actor,
            action = LogAction.UPDATE,
            entityType = LogEntityType.APPLICATION,
            entityId = id.toString(),
            oldValues = json.writeValueAsString(mapOf("assignedWorkerUserId" to previousWorkerId)),
            newValues = json.writeValueAsString(mapOf("operation" to "UNASSIGN_WORKER", "assignedWorkerUserId" to null)),
        )
        return detail(application)
    }

    private fun detail(application: Application): AdminApplicationResponse = application.toAdminResponse(
        pendingDocuments.calculate(application),
        documentRequirements.findAllByProgramEditionIdOrderByNameAsc(requireNotNull(application.programEdition.id))
            .map { it.toAvailableDocumentRequirementResponse() },
    )

    private fun response(application: Application): ApplicationResponse = application.toResponse(
        pendingDocuments.calculate(application),
        documentRequirements.findAllByProgramEditionIdOrderByNameAsc(requireNotNull(application.programEdition.id))
            .map { it.toAvailableDocumentRequirementResponse() },
    )

    private fun authorizedUserId(): Long = requireNotNull(authorized("applications:own:view").id)

    private fun authorized(permission: String): User {
        val principal = currentUser.principal()
        return validator.validateUser(users.findByIdWithRoles(principal.id), principal, permission)
    }

    private fun hashRequest(request: CreateApplicationRequest): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest("application:v1:${request.enrollmentPeriodId}".toByteArray(Charsets.UTF_8)),
    )
}
