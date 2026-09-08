package com.uade.dda2.server.feature.application.validator

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.error.UnauthorizedException
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import com.uade.dda2.server.security.JwtPrincipal
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ApplicationValidator {
    fun validateUser(user: User?, principal: JwtPrincipal, permission: String): User {
        if (user == null || !user.active) throw UnauthorizedException("AUTH_UNAUTHENTICATED", "Usuario inexistente o inactivo.")
        if (user.roles.none { it.name == principal.activeRole } || permission !in principal.permissions) {
            throw ForbiddenException("AUTH_FORBIDDEN", "El rol activo no está autorizado para esta operación.")
        }
        return user
    }

    fun validatePeriod(period: EnrollmentPeriod, today: LocalDate) {
        if (period.status != EnrollmentPeriodStatus.OPEN) throw ApplicationErrors.periodNotOpen()
        if (today < period.openDate || today > period.closeDate) throw ApplicationErrors.outsidePeriod()
        if (period.programEdition.status != ProgramEditionStatus.ACTIVE) throw ApplicationErrors.editionNotActive()
    }

    fun validateIdempotencyKey(key: String?) {
        if (key != null && (key.length !in 1..128 || key.any { it.code !in 33..126 })) {
            throw BadRequestException("APPLICATION_INVALID_IDEMPOTENCY_KEY", "Idempotency-Key debe tener entre 1 y 128 caracteres ASCII visibles, sin espacios.")
        }
    }
}
