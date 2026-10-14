package com.uade.dda2.server.feature.application.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationEvaluationRequest
import com.uade.dda2.server.feature.application.dto.response.ApplicationEvaluationResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationEvaluationDetailResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationEvaluationListResponse
import com.uade.dda2.server.feature.application.entity.ApplicationEvaluation
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.EvaluationPriority
import com.uade.dda2.server.feature.application.entity.VulnerabilityLevel
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import com.uade.dda2.server.feature.application.error.ApplicationEvaluationErrors
import com.uade.dda2.server.feature.application.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.application.mapper.toDetailResponse
import com.uade.dda2.server.feature.application.mapper.toListItemResponse
import com.uade.dda2.server.feature.application.mapper.toResponse
import com.uade.dda2.server.feature.application.repository.ApplicationEvaluationRepository
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.application.validator.ApplicationEvaluationValidator
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

const val APPLICATION_EVALUATION_CREATE_PERMISSION = "applications:evaluations:create"
const val APPLICATION_EVALUATION_VIEW_PERMISSION = "applications:evaluations:view"

data class ApplicationEvaluationFilters(
    val applicationId: UUID? = null,
    val applicationNumber: Long? = null,
    val applicantUserId: Long? = null,
    val professionalId: Long? = null,
    val programId: UUID? = null,
    val programEditionId: UUID? = null,
    val vulnerability: VulnerabilityLevel? = null,
    val priority: EvaluationPriority? = null,
    val evaluatedFrom: OffsetDateTime? = null,
    val evaluatedTo: OffsetDateTime? = null,
)

@Service
class ApplicationEvaluationService(
    private val applications: ApplicationRepository,
    private val evaluations: ApplicationEvaluationRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: ApplicationEvaluationValidator,
    private val logs: LogService,
    private val json: JsonMapper,
    private val timeProperties: EnrollmentPeriodExpirationProperties,
) {
    @Transactional
    fun create(applicationId: UUID, request: CreateApplicationEvaluationRequest): ApplicationEvaluationResponse {
        val principal = currentUser.principal()
        val professional = validator.validateActor(
            users.findByIdWithRoles(principal.id),
            principal,
            APPLICATION_EVALUATION_CREATE_PERMISSION,
        )
        val application = applications.findByIdForUpdate(applicationId) ?: throw ApplicationErrors.notFound()
        if (evaluations.existsByApplicationId(applicationId)) throw ApplicationEvaluationErrors.alreadyExists()
        validator.validateApplication(application, professional)
        val data = validator.validateRequest(request)
        val previousStatus = application.status
        val evaluatedAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)

        application.assignedWorker = professional
        application.status = ApplicationStatus.IN_EVALUATION
        application.updatedAt = LocalDateTime.ofInstant(evaluatedAt.toInstant(), timeProperties.zone())
            .truncatedTo(ChronoUnit.MICROS)

        val evaluation = evaluations.saveAndFlush(
            ApplicationEvaluation(
                application = application,
                vulnerability = data.vulnerability,
                priority = data.priority,
                recommendation = data.recommendation,
                professional = professional,
                evaluatedAt = evaluatedAt,
            ),
        )
        logs.record(
            user = professional,
            action = LogAction.CREATE,
            entityType = LogEntityType.APPLICATION_EVALUATION,
            entityId = requireNotNull(evaluation.id).toString(),
            newValues = json.writeValueAsString(evaluation.toAuditSnapshot(previousStatus)),
        )
        return evaluation.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(page: Int, size: Int, filters: ApplicationEvaluationFilters): ApplicationEvaluationListResponse {
        authorize(APPLICATION_EVALUATION_VIEW_PERMISSION)
        if (filters.evaluatedFrom != null && filters.evaluatedTo != null && filters.evaluatedFrom > filters.evaluatedTo) {
            throw ApplicationEvaluationErrors.invalidFilter("evaluatedFrom no puede ser posterior a evaluatedTo.")
        }
        val results = evaluations.search(
            applicationId = filters.applicationId,
            applicationNumber = filters.applicationNumber,
            applicantUserId = filters.applicantUserId,
            professionalId = filters.professionalId,
            programId = filters.programId,
            programEditionId = filters.programEditionId,
            vulnerability = filters.vulnerability,
            priority = filters.priority,
            evaluatedFrom = filters.evaluatedFrom,
            evaluatedTo = filters.evaluatedTo,
            pageable = PageRequest.of(
                page,
                size,
                Sort.by("evaluatedAt").descending().and(Sort.by("id").descending()),
            ),
        )
        return ApplicationEvaluationListResponse(
            content = results.content.map { it.toListItemResponse() },
            page = results.number,
            size = results.size,
            totalElements = results.totalElements,
            totalPages = results.totalPages,
        )
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): ApplicationEvaluationDetailResponse {
        authorize(APPLICATION_EVALUATION_VIEW_PERMISSION)
        return (evaluations.findDetailById(id) ?: throw ApplicationEvaluationErrors.notFound()).toDetailResponse()
    }

    private fun authorize(permission: String) {
        val principal = currentUser.principal()
        validator.validateActor(users.findByIdWithRoles(principal.id), principal, permission)
    }
}
