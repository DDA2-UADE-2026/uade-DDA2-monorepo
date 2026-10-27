package com.uade.dda2.server.feature.intervention.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.intervention.dto.request.PublishInterventionFollowUpRequest
import com.uade.dda2.server.feature.intervention.dto.response.InterventionPublicationResponse
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

const val INTERVENTION_PUBLISH_PERMISSION = "applications:interventions:follow-ups:publish"
data class InterventionPublicationWrite(val publication: InterventionPublicationResponse, val created: Boolean)

@Service
class InterventionPublicationService(
    private val applications: ApplicationRepository,
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
    private val clock: Clock,
) {
    @Transactional
    fun publish(applicationId: UUID, actionId: UUID, followUpId: UUID, request: PublishInterventionFollowUpRequest): InterventionPublicationWrite {
        val locked = lock(applicationId, actionId, followUpId)
        val summary = validator.validatePublication(request)
        val previous = locked.publication
        if (previous?.visible == true && previous.summary == summary) return InterventionPublicationWrite(previous.toPublicationResponse(), false)
        val oldValues = previous?.toPublicationAuditSnapshot()
        val now = now()
        val publication = previous ?: InterventionFollowUpPublication(followUp = locked.followUp,
            summary = summary, publishedAt = now, updatedAt = now, updatedBy = locked.actor)
        publication.summary = summary
        publication.visible = true
        publication.publishedAt = now
        publication.updatedAt = now
        publication.updatedBy = locked.actor
        val saved = publications.saveAndFlush(publication)
        record(locked, saved, oldValues, if (previous == null || oldValues?.get("visible") == false) "PUBLISHED" else "CORRECTED")
        return InterventionPublicationWrite(saved.toPublicationResponse(), previous == null)
    }

    @Transactional
    fun withdraw(applicationId: UUID, actionId: UUID, followUpId: UUID) {
        val locked = lock(applicationId, actionId, followUpId)
        val publication = locked.publication ?: return
        if (!publication.visible) return
        val oldValues = publication.toPublicationAuditSnapshot()
        publication.visible = false
        publication.updatedAt = now()
        publication.updatedBy = locked.actor
        publications.flush()
        record(locked, publication, oldValues, "WITHDRAWN")
    }

    private fun lock(applicationId: UUID, actionId: UUID, followUpId: UUID): Locked {
        val principal = currentUser.principal()
        if (INTERVENTION_PUBLISH_PERMISSION !in principal.permissions)
            throw ForbiddenException("AUTH_FORBIDDEN", "El rol activo no esta autorizado para esta operacion.")
        // Never fetch join these locking queries: application -> plan -> action -> follow-up -> publication.
        val application = applications.findByIdForUpdate(applicationId) ?: throw ApplicationErrors.notFound()
        val plan = plans.findByApplicationIdForUpdate(applicationId) ?: throw InterventionErrors.planNotFound()
        actions.findByIdAndPlanIdForUpdate(actionId, requireNotNull(plan.id)) ?: throw InterventionErrors.actionNotFound()
        val followUp = followUps.findByIdAndActionIdForUpdate(followUpId, actionId) ?: throw InterventionErrors.followUpNotFound()
        val publication = publications.findByFollowUpIdForUpdate(followUpId)
        val actor = validator.validateActor(users.findByIdWithRoles(principal.id), principal, INTERVENTION_PUBLISH_PERMISSION)
        validator.validateResponsibility(application, actor, plan)
        return Locked(application, plan, followUp, publication, actor)
    }

    private fun record(locked: Locked, publication: InterventionFollowUpPublication, oldValues: Map<String, Any?>?, operation: String) {
        locked.application.updatedAt = LocalDateTime.ofInstant(publication.updatedAt.toInstant(), timeProperties.zone())
        logs.record(user = locked.actor, action = LogAction.UPDATE, entityType = LogEntityType.INTERVENTION_FOLLOW_UP,
            entityId = requireNotNull(locked.followUp.id).toString(), oldValues = oldValues?.let { json.writeValueAsString(it) },
            newValues = json.writeValueAsString(publication.toPublicationAuditSnapshot() + mapOf(
                "operation" to operation, "applicationId" to locked.application.id, "planId" to locked.plan.id,
                "actionId" to locked.followUp.action.id)))
    }
    private fun now() = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
    private data class Locked(val application: Application, val plan: InterventionPlan, val followUp: InterventionFollowUp,
        val publication: InterventionFollowUpPublication?, val actor: User)
}
