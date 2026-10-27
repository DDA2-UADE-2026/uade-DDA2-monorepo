package com.uade.dda2.server.feature.application.validator

import com.uade.dda2.server.feature.application.dto.request.CreateApplicationResolutionRequest
import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.application.error.ApplicationResolutionErrors
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.ProgramRequirement
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID
import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.error.UnauthorizedException
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.security.JwtPrincipal

data class ValidatedApplicationResolution(
    val decision: ApplicationResolutionDecision,
    val reason: String,
    val requirementChecks: Map<UUID, Boolean>,
)

@Component
class ApplicationResolutionValidator {
    fun validateActor(user: User?, principal: JwtPrincipal, permission: String): User {
        if (user == null || !user.active) throw UnauthorizedException("AUTH_UNAUTHENTICATED", "Usuario inexistente o inactivo.")
        val role = user.roles.firstOrNull { it.name == principal.activeRole }
        if (role == null || permission !in principal.permissions || role.permissions.none { it.name == permission }) {
            throw ForbiddenException("AUTH_FORBIDDEN", "El rol activo no esta autorizado para esta operacion.")
        }
        return user
    }

    fun validateRequest(request: CreateApplicationResolutionRequest, requirements: List<ProgramRequirement>): ValidatedApplicationResolution {
        val decision = request.decision ?: throw ApplicationResolutionErrors.invalidData("La decision es obligatoria.")
        val reason = request.reason?.trim()
        if (reason.isNullOrEmpty() || reason.length > 1000) {
            throw ApplicationResolutionErrors.invalidData("El motivo debe tener entre 1 y 1000 caracteres.")
        }
        val checks = request.requirementChecks
            ?: throw ApplicationResolutionErrors.invalidData("requirementChecks es obligatorio.")
        if (checks.any { it == null || it.requirementId == null || it.fulfilled == null } ||
            checks.map { it?.requirementId }.distinct().size != checks.size ||
            checks.map { it?.requirementId }.toSet() != requirements.map { it.id }.toSet()) {
            throw ApplicationResolutionErrors.invalidData("Se requiere un resultado por cada requisito vigente, sin duplicados ni requisitos ajenos.")
        }
        return ValidatedApplicationResolution(decision, reason, checks.filterNotNull().associate { requireNotNull(it.requirementId) to requireNotNull(it.fulfilled) })
    }

    fun validateStage(application: Application, decision: ApplicationResolutionDecision, hasEvaluation: Boolean, pendingVisits: Boolean) {
        if (application.status != ApplicationStatus.IN_EVALUATION &&
            !(application.status == ApplicationStatus.WAITLISTED && decision != ApplicationResolutionDecision.WAITLISTED)) {
            throw ApplicationResolutionErrors.statusNotAllowed()
        }
        if (!hasEvaluation) throw ApplicationResolutionErrors.evaluationRequired()
        if (pendingVisits) throw ApplicationResolutionErrors.pendingVisits()
    }

    fun validateEligibility(data: ValidatedApplicationResolution, edition: ProgramEdition, today: LocalDate,
        documentsValid: Boolean, incompatible: Boolean) {
        if (data.decision == ApplicationResolutionDecision.REJECTED) return
        if (edition.status != ProgramEditionStatus.ACTIVE || edition.endDate < today) throw ApplicationResolutionErrors.editionNotEligible()
        if (data.requirementChecks.values.any { !it }) throw ApplicationResolutionErrors.requirementsNotMet()
        if (!documentsValid) throw ApplicationResolutionErrors.documentsNotValid()
        if (incompatible) throw ApplicationResolutionErrors.incompatibleApplication()
        if (data.decision == ApplicationResolutionDecision.APPROVED && edition.currentEnrollment >= edition.maxCapacity) {
            throw ApplicationResolutionErrors.noCapacity()
        }
        if (data.decision == ApplicationResolutionDecision.WAITLISTED && edition.currentEnrollment < edition.maxCapacity) {
            throw ApplicationResolutionErrors.capacityAvailable()
        }
    }
}
