package com.uade.dda2.server.feature.application.validator

import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.error.UnauthorizedException
import com.uade.dda2.server.feature.application.dto.request.CancelApplicationVisitRequest
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationVisitRequest
import com.uade.dda2.server.feature.application.dto.request.RecordApplicationVisitResultRequest
import com.uade.dda2.server.feature.application.dto.request.RescheduleApplicationVisitRequest
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisit
import com.uade.dda2.server.feature.application.entity.ApplicationVisitStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitType
import com.uade.dda2.server.feature.application.error.ApplicationVisitErrors
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.security.JwtPrincipal
import org.springframework.stereotype.Component
import java.time.DateTimeException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

data class ValidatedApplicationVisit(
    val type: ApplicationVisitType,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val location: String,
)

data class ValidatedApplicationVisitSchedule(
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val location: String,
)

data class ValidatedApplicationVisitResult(
    val status: ApplicationVisitStatus,
    val observations: String?,
    val conclusions: String?,
)

@Component
class ApplicationVisitValidator {
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

    fun validateApplication(application: Application, actor: User, hasEvaluation: Boolean) {
        if (!hasEvaluation) throw ApplicationVisitErrors.evaluationRequired()
        if (application.status !in VISIT_ALLOWED_STATUSES) throw ApplicationVisitErrors.statusNotAllowed()
        if (application.assignedWorker?.id != actor.id) throw ApplicationVisitErrors.assignedToAnotherWorker()
    }

    fun validateRequest(request: CreateApplicationVisitRequest, now: OffsetDateTime): ValidatedApplicationVisit {
        val type = request.type ?: throw ApplicationVisitErrors.invalidData("El tipo es obligatorio.")
        val schedule = validateSchedule(request.startsAt, request.durationMinutes, request.location, now)
        return ValidatedApplicationVisit(type, schedule.startsAt, schedule.endsAt, schedule.location)
    }

    fun validateRescheduleRequest(
        request: RescheduleApplicationVisitRequest,
        now: OffsetDateTime,
    ): ValidatedApplicationVisitSchedule =
        validateSchedule(request.startsAt, request.durationMinutes, request.location, now)

    fun validateCancellationRequest(request: CancelApplicationVisitRequest): String {
        val reason = request.reason?.trim()
        if (reason.isNullOrEmpty() || reason.length > 1000) {
            throw ApplicationVisitErrors.invalidData("El motivo debe tener entre 1 y 1000 caracteres.")
        }
        return reason
    }

    fun validateResultRequest(request: RecordApplicationVisitResultRequest): ValidatedApplicationVisitResult {
        val status = request.status
            ?: throw ApplicationVisitErrors.invalidResultData("El estado final es obligatorio.")
        return ValidatedApplicationVisitResult(
            status = ApplicationVisitStatus.valueOf(status.name),
            observations = optionalResultText(request.observations, "Las observaciones"),
            conclusions = optionalResultText(request.conclusions, "Las conclusiones"),
        )
    }

    fun validateManageable(
        application: Application,
        visit: ApplicationVisit,
        actor: User,
        now: OffsetDateTime,
    ) {
        if (application.assignedWorker?.id != actor.id || visit.responsible.id != actor.id) {
            throw ApplicationVisitErrors.assignedToAnotherWorker()
        }
        if (
            application.status != ApplicationStatus.IN_VISIT ||
            visit.status != ApplicationVisitStatus.SCHEDULED ||
            !visit.startsAt.toInstant().isAfter(now.toInstant())
        ) {
            throw ApplicationVisitErrors.notModifiable()
        }
    }

    fun validateResultRegistrable(
        application: Application,
        visit: ApplicationVisit,
        actor: User,
        now: OffsetDateTime,
    ) {
        if (application.assignedWorker?.id != actor.id || visit.responsible.id != actor.id) {
            throw ApplicationVisitErrors.assignedToAnotherWorker()
        }
        if (
            application.status != ApplicationStatus.IN_VISIT ||
            visit.status != ApplicationVisitStatus.SCHEDULED ||
            visit.endsAt.toInstant().isAfter(now.toInstant())
        ) {
            throw ApplicationVisitErrors.resultNotRegistrable()
        }
    }

    private fun optionalResultText(value: String?, label: String): String? {
        if (value == null) return null
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.length > 2000) {
            throw ApplicationVisitErrors.invalidResultData("$label deben tener entre 1 y 2000 caracteres cuando se informan.")
        }
        return trimmed
    }

    private fun validateSchedule(
        requestedStart: OffsetDateTime?,
        requestedDuration: Int?,
        requestedLocation: String?,
        now: OffsetDateTime,
    ): ValidatedApplicationVisitSchedule {
        val rawStart = requestedStart ?: throw ApplicationVisitErrors.invalidData("La fecha de inicio es obligatoria.")
        val duration = requestedDuration ?: throw ApplicationVisitErrors.invalidData("La duracion es obligatoria.")
        if (duration <= 0) throw ApplicationVisitErrors.invalidData("La duracion debe ser mayor a cero.")
        val location = requestedLocation?.trim()
        if (location.isNullOrEmpty() || location.length > 300) {
            throw ApplicationVisitErrors.invalidData("El lugar debe tener entre 1 y 300 caracteres.")
        }
        val startsAt = rawStart.toInstant().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC)
        if (!startsAt.toInstant().isAfter(now.toInstant())) {
            throw ApplicationVisitErrors.invalidData("La fecha de inicio debe ser futura.")
        }
        val endsAt = try {
            startsAt.plusMinutes(duration.toLong())
        } catch (_: DateTimeException) {
            throw ApplicationVisitErrors.invalidData("La fecha de fin calculada esta fuera del rango admitido.")
        } catch (_: ArithmeticException) {
            throw ApplicationVisitErrors.invalidData("La fecha de fin calculada esta fuera del rango admitido.")
        }
        return ValidatedApplicationVisitSchedule(startsAt, endsAt, location)
    }

    companion object {
        private val VISIT_ALLOWED_STATUSES = setOf(ApplicationStatus.IN_EVALUATION, ApplicationStatus.IN_VISIT)
    }
}
