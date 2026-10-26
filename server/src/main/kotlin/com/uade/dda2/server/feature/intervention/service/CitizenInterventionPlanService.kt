package com.uade.dda2.server.feature.intervention.service

import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.intervention.dto.response.*
import com.uade.dda2.server.feature.intervention.error.InterventionErrors
import com.uade.dda2.server.feature.intervention.mapper.*
import com.uade.dda2.server.feature.intervention.repository.*
import com.uade.dda2.server.feature.intervention.validator.InterventionValidator
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

const val INTERVENTION_OWN_VIEW_PERMISSION = "applications:own:interventions:view"

@Service
class CitizenInterventionPlanService(
    private val plans: InterventionPlanRepository,
    private val actions: InterventionActionRepository,
    private val publications: InterventionPublicationRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: InterventionValidator,
) {
    @Transactional(readOnly = true)
    fun list(page: Int, size: Int): CitizenInterventionPlanListResponse {
        val userId = authorize()
        val results = plans.findPublicPlansByUserId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
        val counts = if (results.isEmpty) emptyMap() else actions.countStatesByPlanIds(results.content.map { it.id }).associateBy { it.planId }
        return CitizenInterventionPlanListResponse(results.content.map { row ->
            val state = counts[row.id]
            row.toCitizenSummary(interventionProgress(state?.actionCount ?: 0, state?.pendingCount ?: 0, state?.completedCount ?: 0))
        }, results.number, results.size, results.totalElements, results.totalPages, if (results.totalElements == 0L) EMPTY_PLANS_MESSAGE else null)
    }

    @Transactional(readOnly = true)
    fun get(planId: UUID): CitizenInterventionPlanResponse {
        val userId = authorize()
        val plan = plans.findPublicPlanByIdAndUserId(planId, userId) ?: throw InterventionErrors.planNotFound()
        return plan.toCitizenDetail(actions.findPublicStructure(planId, userId))
    }

    @Transactional(readOnly = true)
    fun followUps(planId: UUID, actionId: UUID, page: Int, size: Int): CitizenInterventionFollowUpListResponse {
        val userId = authorize()
        plans.findPublicPlanByIdAndUserId(planId, userId) ?: throw InterventionErrors.planNotFound()
        if (!actions.existsByIdAndObjectivePlanId(actionId, planId)) throw InterventionErrors.actionNotFound()
        val results = publications.findVisibleByOwnerAndAction(userId, planId, actionId, PageRequest.of(page, size))
        return CitizenInterventionFollowUpListResponse(results.content.map { it.toCitizenResponse() }, results.number, results.size,
            results.totalElements, results.totalPages, if (results.totalElements == 0L) EMPTY_FOLLOW_UPS_MESSAGE else null)
    }

    private fun authorize(): Long {
        val principal = currentUser.principal()
        return requireNotNull(validator.validateActor(users.findByIdWithRoles(principal.id), principal, INTERVENTION_OWN_VIEW_PERMISSION).id)
    }

    companion object {
        const val EMPTY_PLANS_MESSAGE = "No tenés planes de intervención registrados."
        const val EMPTY_FOLLOW_UPS_MESSAGE = "No hay seguimientos habilitados para tu visualización."
    }
}
