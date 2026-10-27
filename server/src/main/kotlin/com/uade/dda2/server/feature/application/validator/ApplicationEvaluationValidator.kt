package com.uade.dda2.server.feature.application.validator

import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.error.UnauthorizedException
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationEvaluationRequest
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.EvaluationPriority
import com.uade.dda2.server.feature.application.entity.VulnerabilityLevel
import com.uade.dda2.server.feature.application.error.ApplicationEvaluationErrors
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.security.JwtPrincipal
import org.springframework.stereotype.Component

data class ValidatedApplicationEvaluation(
    val vulnerability: VulnerabilityLevel,
    val priority: EvaluationPriority,
    val recommendation: String,
)

@Component
class ApplicationEvaluationValidator {
    fun validateActor(user: User?, principal: JwtPrincipal, permission: String): User {
        if (user == null || !user.active) {
            throw UnauthorizedException("AUTH_UNAUTHENTICATED", "Usuario inexistente o inactivo.")
        }
        val activeRole = user.roles.firstOrNull { it.name.equals(principal.activeRole, ignoreCase = true) }
        if (
            activeRole == null ||
            permission !in principal.permissions ||
            activeRole.permissions.none { it.name == permission }
        ) {
            throw ForbiddenException("AUTH_FORBIDDEN", "El rol activo no esta autorizado para esta operacion.")
        }
        return user
    }

    fun validateApplication(application: Application, professional: User) {
        if (application.status !in ApplicationStatus.EVALUATION_ALLOWED) {
            throw ApplicationEvaluationErrors.statusNotAllowed()
        }
        // Quien evalúa queda asignado al caso, y el titular nunca puede trabajar su propia solicitud.
        if (application.user.id == professional.id) {
            throw ApplicationEvaluationErrors.ownApplication()
        }
        if (application.assignedWorker?.id != null && application.assignedWorker?.id != professional.id) {
            throw ApplicationEvaluationErrors.assignedToAnotherWorker()
        }
    }

    fun validateRequest(request: CreateApplicationEvaluationRequest): ValidatedApplicationEvaluation {
        val vulnerability = request.vulnerability
            ?: throw ApplicationEvaluationErrors.invalidData("La vulnerabilidad es obligatoria.")
        val priority = request.priority
            ?: throw ApplicationEvaluationErrors.invalidData("La prioridad es obligatoria.")
        val recommendation = request.recommendation?.trim()
        if (recommendation.isNullOrEmpty() || recommendation.length > 1000) {
            throw ApplicationEvaluationErrors.invalidData("La recomendacion debe tener entre 1 y 1000 caracteres.")
        }
        return ValidatedApplicationEvaluation(vulnerability, priority, recommendation)
    }
}
