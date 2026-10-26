package com.uade.dda2.server.feature.intervention.validator

import com.uade.dda2.server.error.*
import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.intervention.dto.request.*
import com.uade.dda2.server.feature.intervention.entity.*
import com.uade.dda2.server.feature.intervention.error.InterventionErrors
import com.uade.dda2.server.security.JwtPrincipal
import org.springframework.stereotype.Component
import java.time.LocalDate

data class ValidatedInterventionObjective(val description: String, val actions: List<ValidatedInterventionAction>)
data class ValidatedInterventionAction(val description: String, val startDate: LocalDate, val dueDate: LocalDate)
data class ValidatedInterventionFollowUp(val status: InterventionActionStatus, val occurredOn: LocalDate, val notes: String, val result: String?)

@Component
class InterventionValidator {
    fun validatePublication(request: PublishInterventionFollowUpRequest): String =
        text(request.summary, 2000, "El resumen publico", InterventionErrors::invalidPublication)
    fun validateActor(user: User?, principal: JwtPrincipal, permission: String): User {
        if (user == null || !user.active) throw UnauthorizedException("AUTH_UNAUTHENTICATED", "Usuario inexistente o inactivo.")
        val role = user.roles.firstOrNull { it.name == principal.activeRole }
        if (role == null || permission !in principal.permissions || role.permissions.none { it.name == permission })
            throw ForbiddenException("AUTH_FORBIDDEN", "El rol activo no esta autorizado para esta operacion.")
        return user
    }

    fun validateCreation(application: Application, actor: User, hasEvaluation: Boolean) {
        if (!hasEvaluation) throw InterventionErrors.evaluationRequired()
        validateResponsibility(application, actor)
        if (application.status !in setOf(ApplicationStatus.IN_EVALUATION, ApplicationStatus.IN_VISIT,
                ApplicationStatus.WAITLISTED, ApplicationStatus.APPROVED, ApplicationStatus.REJECTED, ApplicationStatus.CLOSED))
            throw InterventionErrors.statusNotAllowed()
    }

    fun validateResponsibility(application: Application, actor: User, plan: InterventionPlan? = null) {
        if (application.assignedWorker == null) throw InterventionErrors.workerRequired()
        if (application.assignedWorker?.id != actor.id || (plan != null && plan.responsible.id != actor.id))
            throw InterventionErrors.differentWorker()
    }

    fun validatePlan(request: CreateInterventionPlanRequest): List<ValidatedInterventionObjective> {
        val objectives = request.objectives
        if (objectives.isNullOrEmpty() || objectives.any { it == null })
            throw InterventionErrors.invalidPlan("Se requiere al menos un objetivo valido.")
        return objectives.filterNotNull().map { objective ->
            val description = text(objective.description, 1000, "La descripcion del objetivo", InterventionErrors::invalidPlan)
            val actions = objective.actions
            if (actions.isNullOrEmpty() || actions.any { it == null })
                throw InterventionErrors.invalidPlan("Cada objetivo requiere al menos una accion valida.")
            ValidatedInterventionObjective(description, actions.filterNotNull().map { action ->
                val actionDescription = text(action.description, 1000, "La descripcion de la accion", InterventionErrors::invalidPlan)
                val start = action.startDate ?: throw InterventionErrors.invalidPlan("El inicio de la accion es obligatorio.")
                val due = action.dueDate ?: throw InterventionErrors.invalidPlan("El fin previsto de la accion es obligatorio.")
                if (start > due) throw InterventionErrors.invalidPlan("El inicio no puede ser posterior al fin previsto.")
                ValidatedInterventionAction(actionDescription, start, due)
            })
        }
    }

    fun validateFollowUp(request: CreateInterventionFollowUpRequest, action: InterventionAction,
        last: InterventionFollowUp?, today: LocalDate): ValidatedInterventionFollowUp {
        if (action.status == InterventionActionStatus.COMPLETED) throw InterventionErrors.completed()
        val status = request.status ?: throw InterventionErrors.invalidFollowUp("El estado es obligatorio.")
        val date = request.occurredOn ?: throw InterventionErrors.invalidFollowUp("La fecha real es obligatoria.")
        if (date < action.startDate || date > today || (last != null && date < last.occurredOn))
            throw InterventionErrors.invalidFollowUp("La fecha real debe estar entre el inicio y hoy, sin retroceder respecto al ultimo seguimiento.")
        val notes = text(request.notes, 2000, "Las notas", InterventionErrors::invalidFollowUp)
        val result = request.result?.let { text(it, 2000, "El resultado", InterventionErrors::invalidFollowUp) }
        if (status == InterventionFollowUpStatus.COMPLETED && result == null)
            throw InterventionErrors.invalidFollowUp("El resultado es obligatorio al completar.")
        return ValidatedInterventionFollowUp(InterventionActionStatus.valueOf(status.name), date, notes, result)
    }

    private fun text(value: String?, max: Int, label: String, invalid: (String) -> RuntimeException): String {
        val trimmed = value?.trim()
        if (trimmed.isNullOrEmpty() || trimmed.length > max) throw invalid("$label deben tener entre 1 y $max caracteres.")
        return trimmed
    }
}
