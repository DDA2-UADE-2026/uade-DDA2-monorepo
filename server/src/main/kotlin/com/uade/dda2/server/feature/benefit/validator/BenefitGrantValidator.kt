package com.uade.dda2.server.feature.benefit.validator

import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.error.UnauthorizedException
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.benefit.dto.request.CreateBenefitGrantRequest
import com.uade.dda2.server.feature.benefit.error.BenefitGrantErrors
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import com.uade.dda2.server.security.JwtPrincipal
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

data class ValidatedBenefitGrant(val programBenefitId: UUID, val startDate: LocalDate, val endDate: LocalDate)

@Component
class BenefitGrantValidator {
    fun validateActor(user: User?, principal: JwtPrincipal, permission: String): User {
        if (user == null || !user.active) throw UnauthorizedException("AUTH_UNAUTHENTICATED", "Usuario inexistente o inactivo.")
        val role = user.roles.firstOrNull { it.name == principal.activeRole }
        if (role == null || permission !in principal.permissions || role.permissions.none { it.name == permission }) {
            throw ForbiddenException("AUTH_FORBIDDEN", "El rol activo no esta autorizado para esta operacion.")
        }
        return user
    }

    fun validateStage(application: Application, edition: ProgramEdition) {
        if (application.status != ApplicationStatus.APPROVED) throw BenefitGrantErrors.applicationNotApproved()
        if (edition.status != ProgramEditionStatus.ACTIVE) throw BenefitGrantErrors.editionNotActive()
    }

    fun validateRequest(request: CreateBenefitGrantRequest, edition: ProgramEdition, today: LocalDate): ValidatedBenefitGrant {
        val benefitId = request.programBenefitId ?: throw BenefitGrantErrors.invalidData("El beneficio es obligatorio.")
        val start = request.startDate ?: throw BenefitGrantErrors.invalidData("La fecha de inicio es obligatoria.")
        val end = request.endDate ?: throw BenefitGrantErrors.invalidData("La fecha de fin es obligatoria.")
        if (start > end || start > today || end < today || start < edition.startDate || end > edition.endDate) {
            throw BenefitGrantErrors.invalidData("La vigencia debe incluir hoy y estar contenida en las fechas de la edicion, con extremos inclusivos.")
        }
        return ValidatedBenefitGrant(benefitId, start, end)
    }
}
