package com.uade.dda2.server.feature.application.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationResolutionRequest
import com.uade.dda2.server.feature.application.dto.response.ApplicationResolutionResponse
import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import com.uade.dda2.server.feature.application.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.application.mapper.toResponse
import com.uade.dda2.server.feature.application.repository.*
import com.uade.dda2.server.feature.application.validator.ApplicationResolutionValidator
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

const val APPLICATION_RESOLVE_PERMISSION = "applications:management:resolve"

@Service
class ApplicationResolutionService(
    private val applications: ApplicationRepository,
    private val resolutions: ApplicationResolutionRepository,
    private val evaluations: ApplicationEvaluationRepository,
    private val visits: ApplicationVisitRepository,
    private val documents: ApplicationDocumentRepository,
    private val documentRequirements: ProgramDocumentRequirementRepository,
    private val requirements: ProgramRequirementRepository,
    private val editions: ProgramEditionRepository,
    private val incompatibilities: ProgramIncompatibilityRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: ApplicationResolutionValidator,
    private val logs: LogService,
    private val json: JsonMapper,
    private val timeProperties: EnrollmentPeriodExpirationProperties,
) {
    @Transactional
    fun create(applicationId: UUID, request: CreateApplicationResolutionRequest): ApplicationResolutionResponse {
        val actor = authorize(APPLICATION_RESOLVE_PERMISSION)
        // No fetch joins here: lock only the application, then its applicant, then its edition.
        val application = applications.findByIdForUpdate(applicationId) ?: throw ApplicationErrors.notFound()
        val applicantId = requireNotNull(application.user.id)
        users.findByIdForUpdate(applicantId) ?: throw ApplicationErrors.applicantNotFound()
        val editionId = requireNotNull(application.programEdition.id)
        val edition = requireNotNull(editions.findByIdForUpdate(editionId))
        val catalog = requirements.findAllByProgramEditionId(editionId)
        val data = validator.validateRequest(request, catalog)
        validator.validateStage(application, data.decision, evaluations.existsByApplicationId(applicationId),
            visits.existsByApplicationIdAndStatus(applicationId, ApplicationVisitStatus.SCHEDULED))

        val states = documents.findStatesByApplicationIdIn(listOf(applicationId)).associateBy { it.requirementId }
        val documentsValid = documentRequirements.findAllByProgramEditionIdOrderByNameAsc(editionId)
            .filter { it.required }.all { states[it.id]?.status == ApplicationDocumentStatus.VALID }
        val programId = requireNotNull(edition.program.id)
        val incompatibleIds = incompatibilities.findAllByProgramId(programId).map {
            requireNotNull(if (it.program.id == programId) it.incompatibleWithProgram.id else it.program.id)
        }.distinct()
        val incompatible = incompatibleIds.isNotEmpty() && applications.existsOverlappingApproval(
            applicantId, applicationId, incompatibleIds, edition.startDate, edition.endDate)
        val resolvedAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
        val localTime = LocalDateTime.ofInstant(resolvedAt.toInstant(), timeProperties.zone())
        validator.validateEligibility(data, edition, localTime.toLocalDate(), documentsValid, incompatible)

        val resolution = ApplicationResolution(application = application, previousStatus = application.status,
            decision = data.decision, reason = data.reason, resolvedBy = actor, resolvedAt = resolvedAt)
        catalog.forEach { requirement ->
            val requirementId = requireNotNull(requirement.id)
            resolution.requirementChecks.add(ApplicationResolutionRequirement(resolution = resolution,
                requirementId = requirementId, type = requirement.type, value = requirement.value,
                description = requirement.description, fulfilled = data.requirementChecks.getValue(requirementId)))
        }
        application.status = data.decision.applicationStatus()
        application.resolutionReason = data.reason
        application.resolvedBy = actor
        application.resolvedAt = localTime
        application.updatedAt = localTime
        if (data.decision == ApplicationResolutionDecision.APPROVED) edition.currentEnrollment += 1
        val saved = resolutions.saveAndFlush(resolution)
        logs.record(user = actor, action = LogAction.CREATE, entityType = LogEntityType.APPLICATION_RESOLUTION,
            entityId = requireNotNull(saved.id).toString(), newValues = json.writeValueAsString(saved.toAuditSnapshot()))
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(applicationId: UUID): List<ApplicationResolutionResponse> {
        authorize("applications:management:view")
        if (!applications.existsById(applicationId)) throw ApplicationErrors.notFound()
        return resolutions.findAllByApplicationIdOrderByResolvedAtAscIdAsc(applicationId).map { it.toResponse() }
    }

    private fun authorize(permission: String): User {
        val principal = currentUser.principal()
        return validator.validateActor(users.findByIdWithRoles(principal.id), principal, permission)
    }
}
