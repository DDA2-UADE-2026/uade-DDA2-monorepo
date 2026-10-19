package com.uade.dda2.server.feature.benefit.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import com.uade.dda2.server.feature.application.repository.*
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.benefit.dto.request.*
import com.uade.dda2.server.feature.benefit.dto.response.*
import com.uade.dda2.server.feature.benefit.entity.*
import com.uade.dda2.server.feature.benefit.error.BenefitGrantErrors
import com.uade.dda2.server.feature.benefit.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.benefit.mapper.toResponse
import com.uade.dda2.server.feature.benefit.repository.*
import com.uade.dda2.server.feature.benefit.validator.*
import com.uade.dda2.server.feature.log.entity.*
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.UUID

const val BENEFIT_MANAGE_PERMISSION = "applications:management:benefits:manage"

@Service
class BenefitGrantLifecycleService(
    private val applications: ApplicationRepository,
    private val grants: BenefitGrantRepository,
    private val changes: BenefitGrantChangeRepository,
    private val editions: ProgramEditionRepository,
    private val requirements: ProgramRequirementRepository,
    private val documents: ApplicationDocumentRepository,
    private val documentRequirements: ProgramDocumentRequirementRepository,
    private val incompatibilities: ProgramIncompatibilityRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val actorValidator: BenefitGrantValidator,
    private val validator: BenefitGrantLifecycleValidator,
    private val logs: LogService,
    private val json: JsonMapper,
    private val timeProperties: EnrollmentPeriodExpirationProperties,
) {
    @Transactional
    fun renew(applicationId: UUID, grantId: UUID, request: RenewBenefitGrantRequest): BenefitGrantResponse =
        change(applicationId, grantId, BenefitGrantChangeAction.RENEWED, request.reason, request)

    @Transactional
    fun suspend(applicationId: UUID, grantId: UUID, request: ChangeBenefitGrantStateRequest): BenefitGrantResponse =
        change(applicationId, grantId, BenefitGrantChangeAction.SUSPENDED, request.reason)

    @Transactional
    fun finalize(applicationId: UUID, grantId: UUID, request: ChangeBenefitGrantStateRequest): BenefitGrantResponse =
        change(applicationId, grantId, BenefitGrantChangeAction.FINALIZED, request.reason)

    @Transactional(readOnly = true)
    fun history(applicationId: UUID, grantId: UUID): List<BenefitGrantChangeResponse> {
        authorize("applications:management:view")
        if (!applications.existsById(applicationId)) throw ApplicationErrors.notFound()
        if (!grants.existsByIdAndApplicationId(grantId, applicationId)) throw BenefitGrantErrors.notFound()
        return changes.findAllByGrantIdOrderBySequenceNumberAsc(grantId).map { it.toResponse() }
    }

    private fun change(applicationId: UUID, grantId: UUID, action: BenefitGrantChangeAction,
        reason: String?, renewal: RenewBenefitGrantRequest? = null): BenefitGrantResponse {
        val actor = authorize(BENEFIT_MANAGE_PERMISSION)
        // Never fetch-join here: lock application, applicant, edition, then grant, matching assignment/resolution.
        val application = applications.findByIdForUpdate(applicationId) ?: throw ApplicationErrors.notFound()
        val applicantId = requireNotNull(application.user.id)
        users.findByIdForUpdate(applicantId) ?: throw ApplicationErrors.applicantNotFound()
        val editionId = requireNotNull(application.programEdition.id)
        val edition = requireNotNull(editions.findByIdForUpdate(editionId))
        val grant = grants.findByIdAndApplicationIdForUpdate(grantId, applicationId) ?: throw BenefitGrantErrors.notFound()
        val normalizedReason = validator.reason(reason)
        validator.validateStage(grant, action)
        val changedAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
        val localTime = LocalDateTime.ofInstant(changedAt.toInstant(), timeProperties.zone())
        val previousStatus = grant.status
        val previousEnd = grant.endDate
        val catalog = if (renewal != null) requirements.findAllByProgramEditionId(editionId) else emptyList()
        val renewalData = renewal?.let {
            validator.validateRenewal(it, grant, edition, localTime.toLocalDate(), catalog).also {
                validateRenewalEligibility(applicantId, applicationId, edition)
            }
        }
        if (renewalData != null) grant.endDate = renewalData.endDate
        grant.status = when (action) {
            BenefitGrantChangeAction.RENEWED -> BenefitGrantStatus.ACTIVE
            BenefitGrantChangeAction.SUSPENDED -> BenefitGrantStatus.SUSPENDED
            BenefitGrantChangeAction.FINALIZED -> BenefitGrantStatus.FINALIZED
            BenefitGrantChangeAction.ASSIGNED -> throw BenefitGrantErrors.invalidTransition()
        }
        val event = BenefitGrantChange(grant = grant, sequenceNumber = changes.lastSequence(grantId) + 1,
            action = action, previousStatus = previousStatus, status = grant.status,
            previousStartDate = grant.startDate, previousEndDate = previousEnd,
            startDate = grant.startDate, endDate = grant.endDate, reason = normalizedReason,
            responsible = actor, changedAt = changedAt)
        if (renewalData != null) catalog.forEach {
            val requirementId = requireNotNull(it.id)
            event.requirementChecks.add(BenefitGrantChangeRequirement(change = event, requirementId = requirementId,
                type = it.type, value = it.value, description = it.description, fulfilled = renewalData.checks.getValue(requirementId)))
        }
        // Flush the new status before counting. The application lock serializes every grant of this application.
        val saved = changes.saveAndFlush(event)
        if (action == BenefitGrantChangeAction.FINALIZED && grants.countUnfinished(applicationId) == 0L) {
            if (edition.currentEnrollment <= 0) throw BenefitGrantErrors.inconsistentEnrollment()
            edition.currentEnrollment -= 1
            application.status = ApplicationStatus.CLOSED
            logs.record(user = actor, action = LogAction.UPDATE, entityType = LogEntityType.APPLICATION,
                entityId = applicationId.toString(),
                oldValues = json.writeValueAsString(mapOf("status" to ApplicationStatus.APPROVED)),
                newValues = json.writeValueAsString(mapOf("status" to ApplicationStatus.CLOSED,
                    "grantChangeId" to saved.id, "responsibleUserId" to actor.id, "changedAt" to changedAt,
                    "editionId" to editionId, "currentEnrollment" to edition.currentEnrollment)))
        }
        application.updatedAt = localTime
        logs.record(user = actor, action = LogAction.CREATE, entityType = LogEntityType.BENEFIT_GRANT_CHANGE,
            entityId = requireNotNull(saved.id).toString(), newValues = json.writeValueAsString(saved.toAuditSnapshot()))
        return grant.toResponse()
    }

    private fun validateRenewalEligibility(applicantId: Long, applicationId: UUID, edition: ProgramEdition) {
        val states = documents.findStatesByApplicationIdIn(listOf(applicationId)).associateBy { it.requirementId }
        if (documentRequirements.findAllByProgramEditionIdOrderByNameAsc(requireNotNull(edition.id))
                .filter { it.required }.any { states[it.id]?.status != ApplicationDocumentStatus.VALID })
            throw BenefitGrantErrors.documentsNotValid()
        val programId = requireNotNull(edition.program.id)
        val incompatibleIds = incompatibilities.findAllByProgramId(programId).map {
            requireNotNull(if (it.program.id == programId) it.incompatibleWithProgram.id else it.program.id)
        }.distinct()
        if (incompatibleIds.isNotEmpty() && applications.existsOverlappingApproval(applicantId, applicationId,
                incompatibleIds, edition.startDate, edition.endDate)) throw BenefitGrantErrors.incompatibleApplication()
    }

    private fun authorize(permission: String): User {
        val principal = currentUser.principal()
        return actorValidator.validateActor(users.findByIdWithRoles(principal.id), principal, permission)
    }
}
