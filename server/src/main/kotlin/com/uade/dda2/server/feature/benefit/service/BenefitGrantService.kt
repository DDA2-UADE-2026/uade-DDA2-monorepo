package com.uade.dda2.server.feature.benefit.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.benefit.dto.request.CreateBenefitGrantRequest
import com.uade.dda2.server.feature.benefit.dto.response.BenefitGrantResponse
import com.uade.dda2.server.feature.benefit.entity.BenefitGrant
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantChange
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantChangeAction
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantChangeRepository
import com.uade.dda2.server.feature.benefit.error.BenefitGrantErrors
import com.uade.dda2.server.feature.benefit.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.benefit.mapper.toResponse
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
import com.uade.dda2.server.feature.benefit.validator.BenefitGrantValidator
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.error.ProgramBenefitErrors
import com.uade.dda2.server.feature.program.repository.ProgramBenefitRepository
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramIncompatibilityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

const val BENEFIT_ASSIGN_PERMISSION = "applications:management:benefits:assign"

@Service
class BenefitGrantService(
    private val applications: ApplicationRepository,
    private val grants: BenefitGrantRepository,
    private val changes: BenefitGrantChangeRepository,
    private val editions: ProgramEditionRepository,
    private val benefits: ProgramBenefitRepository,
    private val incompatibilities: ProgramIncompatibilityRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: BenefitGrantValidator,
    private val logs: LogService,
    private val json: JsonMapper,
    private val timeProperties: EnrollmentPeriodExpirationProperties,
) {
    @Transactional
    fun create(applicationId: UUID, request: CreateBenefitGrantRequest): BenefitGrantResponse {
        val actor = authorize(BENEFIT_ASSIGN_PERMISSION)
        // Same lock order as resolutions: application, applicant, edition. Catalog writers lock the edition too.
        val application = applications.findByIdForUpdate(applicationId) ?: throw ApplicationErrors.notFound()
        val userId = requireNotNull(application.user.id)
        users.findByIdForUpdate(userId) ?: throw ApplicationErrors.applicantNotFound()
        val editionId = requireNotNull(application.programEdition.id)
        val edition = requireNotNull(editions.findByIdForUpdate(editionId))
        validator.validateStage(application, edition)
        val assignedAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
        val localTime = LocalDateTime.ofInstant(assignedAt.toInstant(), timeProperties.zone())
        val data = validator.validateRequest(request, edition, localTime.toLocalDate())
        val benefit = benefits.findByIdAndProgramEditionId(data.programBenefitId, editionId)
            ?: throw ProgramBenefitErrors.notFound(data.programBenefitId, editionId)
        if (grants.existsByApplicationIdAndProgramBenefitId(applicationId, data.programBenefitId)) throw BenefitGrantErrors.alreadyExists()
        val programId = requireNotNull(edition.program.id)
        val incompatibleIds = incompatibilities.findAllByProgramId(programId).map {
            requireNotNull(if (it.program.id == programId) it.incompatibleWithProgram.id else it.program.id)
        }.distinct()
        if (incompatibleIds.isNotEmpty() && applications.existsOverlappingApproval(userId, applicationId,
                incompatibleIds, edition.startDate, edition.endDate)) throw BenefitGrantErrors.incompatibleApplication()

        val grant = grants.saveAndFlush(BenefitGrant(application = application, programBenefit = benefit,
            benefitType = benefit.benefitType, description = benefit.description, amount = benefit.amount,
            startDate = data.startDate, endDate = data.endDate, assignedBy = actor, assignedAt = assignedAt))
        application.updatedAt = localTime
        changes.save(BenefitGrantChange(grant = grant, sequenceNumber = 1, action = BenefitGrantChangeAction.ASSIGNED,
            previousStatus = null, status = grant.status, previousStartDate = null, previousEndDate = null,
            startDate = grant.startDate, endDate = grant.endDate, reason = null, responsible = actor, changedAt = assignedAt))
        logs.record(user = actor, action = LogAction.CREATE, entityType = LogEntityType.BENEFIT_GRANT,
            entityId = requireNotNull(grant.id).toString(), newValues = json.writeValueAsString(grant.toAuditSnapshot()))
        return grant.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(applicationId: UUID): List<BenefitGrantResponse> {
        authorize("applications:management:view")
        if (!applications.existsById(applicationId)) throw ApplicationErrors.notFound()
        return grants.findAllByApplicationIdOrderByAssignedAtAscIdAsc(applicationId).map { it.toResponse() }
    }

    private fun authorize(permission: String): User {
        val principal = currentUser.principal()
        return validator.validateActor(users.findByIdWithRoles(principal.id), principal, permission)
    }
}
