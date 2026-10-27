package com.uade.dda2.server.feature.intervention.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import com.uade.dda2.server.feature.application.repository.*
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.intervention.dto.request.*
import com.uade.dda2.server.feature.intervention.dto.response.*
import com.uade.dda2.server.feature.intervention.entity.*
import com.uade.dda2.server.feature.intervention.error.InterventionErrors
import com.uade.dda2.server.feature.intervention.mapper.*
import com.uade.dda2.server.feature.intervention.repository.*
import com.uade.dda2.server.feature.intervention.validator.InterventionValidator
import com.uade.dda2.server.feature.log.entity.*
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.UUID

const val INTERVENTION_CREATE_PERMISSION = "applications:interventions:create"
const val INTERVENTION_FOLLOW_UP_PERMISSION = "applications:interventions:follow-ups:create"
const val INTERVENTION_VIEW_PERMISSION = "applications:interventions:view"

@Service
class InterventionPlanService(
    private val applications: ApplicationRepository,
    private val evaluations: ApplicationEvaluationRepository,
    private val plans: InterventionPlanRepository,
    private val actions: InterventionActionRepository,
    private val followUps: InterventionFollowUpRepository,
    private val publications: InterventionPublicationRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: InterventionValidator,
    private val logs: LogService,
    private val json: JsonMapper,
    private val timeProperties: EnrollmentPeriodExpirationProperties,
) {
    @Transactional
    fun create(applicationId: UUID, request: CreateInterventionPlanRequest): InterventionPlanResponse {
        checkToken(INTERVENTION_CREATE_PERMISSION)
        // Application only: never fetch-join edition/users into this locking query.
        val application = applications.findByIdForUpdate(applicationId) ?: throw ApplicationErrors.notFound()
        val actor = authorize(INTERVENTION_CREATE_PERMISSION)
        if (plans.existsByApplicationId(applicationId)) throw InterventionErrors.alreadyExists()
        validator.validateCreation(application, actor, evaluations.existsByApplicationId(applicationId))
        val data = validator.validatePlan(request)
        val createdAt = now()
        val plan = InterventionPlan(application = application, responsible = actor, createdAt = createdAt)
        data.forEachIndexed { position, objective ->
            val entity = InterventionObjective(plan = plan, position = position, description = objective.description)
            objective.actions.forEachIndexed { actionPosition, action ->
                entity.actions.add(InterventionAction(objective = entity, position = actionPosition,
                    description = action.description, startDate = action.startDate, dueDate = action.dueDate))
            }
            plan.objectives.add(entity)
        }
        val saved = plans.saveAndFlush(plan)
        application.updatedAt = localTime(createdAt)
        logs.record(user = actor, action = LogAction.CREATE, entityType = LogEntityType.INTERVENTION_PLAN,
            entityId = requireNotNull(saved.id).toString(), newValues = json.writeValueAsString(saved.toAuditSnapshot()))
        return saved.toResponse()
    }

    @Transactional
    fun followUp(applicationId: UUID, actionId: UUID, request: CreateInterventionFollowUpRequest): InterventionFollowUpResponse {
        checkToken(INTERVENTION_FOLLOW_UP_PERMISSION)
        // Same first lock as visits, resolutions and benefits. The plan and action follow, without fetch joins.
        val application = applications.findByIdForUpdate(applicationId) ?: throw ApplicationErrors.notFound()
        val plan = plans.findByApplicationIdForUpdate(applicationId) ?: throw InterventionErrors.planNotFound()
        val action = actions.findByIdAndPlanIdForUpdate(actionId, requireNotNull(plan.id)) ?: throw InterventionErrors.actionNotFound()
        val actor = authorize(INTERVENTION_FOLLOW_UP_PERMISSION)
        validator.validateResponsibility(application, actor, plan)
        val recordedAt = now()
        val last = followUps.findFirstByActionIdOrderBySequenceNumberDesc(actionId)
        val data = validator.validateFollowUp(request, action, last, localTime(recordedAt).toLocalDate())
        val entry = InterventionFollowUp(action = action, sequenceNumber = (last?.sequenceNumber ?: 0) + 1,
            previousStatus = action.status, status = data.status, occurredOn = data.occurredOn,
            notes = data.notes, result = data.result, author = actor, recordedAt = recordedAt)
        action.status = data.status
        val saved = followUps.saveAndFlush(entry)
        application.updatedAt = localTime(recordedAt)
        logs.record(user = actor, action = LogAction.CREATE, entityType = LogEntityType.INTERVENTION_FOLLOW_UP,
            entityId = requireNotNull(saved.id).toString(), newValues = json.writeValueAsString(saved.toAuditSnapshot()))
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun get(applicationId: UUID): InterventionPlanResponse {
        authorize(INTERVENTION_VIEW_PERMISSION)
        return findPlan(applicationId).toResponse()
    }

    @Transactional(readOnly = true)
    fun history(applicationId: UUID, actionId: UUID): List<InterventionFollowUpResponse> {
        authorize(INTERVENTION_VIEW_PERMISSION)
        val plan = findPlan(applicationId)
        if (actions.findByIdAndObjectivePlanId(actionId, requireNotNull(plan.id)) == null) throw InterventionErrors.actionNotFound()
        val entries = followUps.findAllByActionIdOrderBySequenceNumberAsc(actionId)
        val published = if (entries.isEmpty()) emptyMap() else publications.findAllByFollowUpIdIn(entries.map { requireNotNull(it.id) })
            .associateBy { requireNotNull(it.followUp.id) }
        return entries.map { it.toResponse(published[it.id]?.toPublicationResponse()) }
    }

    private fun findPlan(applicationId: UUID): InterventionPlan {
        if (!applications.existsById(applicationId)) throw ApplicationErrors.notFound()
        return plans.findByApplicationId(applicationId) ?: throw InterventionErrors.planNotFound()
    }
    private fun authorize(permission: String): User {
        val principal = currentUser.principal()
        return validator.validateActor(users.findByIdWithRoles(principal.id), principal, permission)
    }
    private fun checkToken(permission: String) {
        // Do not load the actor into the persistence context before waiting for resource locks.
        if (permission !in currentUser.principal().permissions)
            throw ForbiddenException("AUTH_FORBIDDEN", "El rol activo no esta autorizado para esta operacion.")
    }
    private fun now() = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
    private fun localTime(instant: OffsetDateTime) = LocalDateTime.ofInstant(instant.toInstant(), timeProperties.zone())
}
