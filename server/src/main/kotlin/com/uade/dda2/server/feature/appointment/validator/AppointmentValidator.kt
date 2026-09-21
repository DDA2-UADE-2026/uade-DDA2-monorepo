package com.uade.dda2.server.feature.appointment.validator

import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.error.UnauthorizedException
import com.uade.dda2.server.feature.appointment.error.AppointmentErrors
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.security.JwtPrincipal
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

@Component
class AppointmentValidator {
    fun validateCitizen(user: User?, principal: JwtPrincipal, permission: String): User {
        if (user == null || !user.active) {
            throw UnauthorizedException("AUTH_UNAUTHENTICATED", "Usuario inexistente o inactivo.")
        }
        val citizenRole = user.roles.firstOrNull { it.name.equals("CIUDADANO", ignoreCase = true) }
        val databasePermissions = citizenRole?.permissions?.map { it.name }.orEmpty()
        if (
            !principal.activeRole.equals("CIUDADANO", ignoreCase = true) ||
            citizenRole == null ||
            permission !in principal.permissions ||
            permission !in databasePermissions
        ) {
            throw ForbiddenException("AUTH_FORBIDDEN", "El rol activo no esta autorizado para esta operacion.")
        }
        return user
    }

    fun validateDate(date: LocalDate, now: OffsetDateTime, zone: ZoneId) {
        if (date < now.atZoneSameInstant(zone).toLocalDate()) throw AppointmentErrors.timeInPast()
    }

    fun validateRequestedRange(startsAt: OffsetDateTime, endsAt: OffsetDateTime, now: OffsetDateTime) {
        if (!startsAt.isBefore(endsAt)) {
            throw AppointmentErrors.invalidRequest("La hora de inicio debe ser anterior a la hora de fin.")
        }
        if (!startsAt.toInstant().isAfter(now.toInstant())) throw AppointmentErrors.timeInPast()
    }

    fun validateIdempotencyKey(key: String) {
        if (key.length !in 1..128 || key.any { it.code !in 33..126 }) {
            throw AppointmentErrors.invalidIdempotencyKey()
        }
    }
}
