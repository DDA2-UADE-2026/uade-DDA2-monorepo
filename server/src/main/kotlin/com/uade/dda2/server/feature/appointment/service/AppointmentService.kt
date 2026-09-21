package com.uade.dda2.server.feature.appointment.service

import com.uade.dda2.server.config.AppointmentProperties
import com.uade.dda2.server.feature.appointment.dto.request.CreateAppointmentRequest
import com.uade.dda2.server.feature.appointment.dto.response.AppointmentResponse
import com.uade.dda2.server.feature.appointment.entity.Appointment
import com.uade.dda2.server.feature.appointment.error.AppointmentErrors
import com.uade.dda2.server.feature.appointment.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.appointment.mapper.toResponse
import com.uade.dda2.server.feature.appointment.repository.AppointmentRepository
import com.uade.dda2.server.feature.appointment.validator.AppointmentValidator
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.center.repository.MunicipalCenterRepository
import com.uade.dda2.server.feature.center.repository.CenterServiceRepository
import com.uade.dda2.server.feature.center.repository.MunicipalServiceRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAssignmentRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.HexFormat
import java.util.UUID

data class AppointmentSubmission(val appointment: AppointmentResponse, val replayed: Boolean)

@Service
class AppointmentService(
    private val appointments: AppointmentRepository,
    private val assignments: ProfessionalAssignmentRepository,
    private val centers: MunicipalCenterRepository,
    private val centerServices: CenterServiceRepository,
    private val municipalServices: MunicipalServiceRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: AppointmentValidator,
    private val slots: AppointmentSlotService,
    private val properties: AppointmentProperties,
    private val logs: LogService,
    private val json: JsonMapper,
) {
    @Transactional
    fun create(request: CreateAppointmentRequest, idempotencyKey: String): AppointmentSubmission {
        validator.validateIdempotencyKey(idempotencyKey)
        val now = OffsetDateTime.now(properties.zone()).truncatedTo(ChronoUnit.MICROS)
        val startsAt = normalize(request.startsAt)
        val endsAt = normalize(request.endsAt)
        val principal = currentUser.principal()
        val lockData = assignments.findAppointmentLockDataById(request.professionalAssignmentId)
            ?: throw AppointmentErrors.slotUnavailable()
        centers.findByIdForUpdate(lockData.centerId) ?: throw AppointmentErrors.slotUnavailable()
        val lockedUsers = users.findAllByIdsForUpdate(listOf(principal.id, lockData.professionalId).distinct().sorted())
            .associateBy { requireNotNull(it.id) }
        val citizen = validator.validateCitizen(lockedUsers[principal.id], principal, "appointments:own:create")
        val requestHash = hashRequest(request.professionalAssignmentId, startsAt, endsAt)
        appointments.findByCitizenIdAndIdempotencyKey(principal.id, idempotencyKey)?.let { existing ->
            if (existing.requestHash != requestHash) throw AppointmentErrors.idempotencyConflict()
            return AppointmentSubmission(existing.toResponse(properties.zone()), replayed = true)
        }
        validator.validateRequestedRange(startsAt, endsAt, now)
        municipalServices.findByIdForUpdate(lockData.serviceId) ?: throw AppointmentErrors.slotUnavailable()
        centerServices.findByIdForUpdate(lockData.centerServiceId) ?: throw AppointmentErrors.slotUnavailable()
        val assignment = assignments.findByIdForUpdate(request.professionalAssignmentId)
            ?: throw AppointmentErrors.slotUnavailable()
        if (lockedUsers[lockData.professionalId] == null) throw AppointmentErrors.slotUnavailable()

        val date = startsAt.atZoneSameInstant(properties.zone()).toLocalDate()
        val matchesCurrentGrid = slots.availableSlots(
            centerServiceId = requireNotNull(assignment.centerService.id),
            date = date,
            citizenId = principal.id,
            now = now,
            includeOccupancy = false,
        ).any {
            it.professionalAssignmentId == request.professionalAssignmentId &&
                it.startsAt.toInstant() == startsAt.toInstant() &&
                it.endsAt.toInstant() == endsAt.toInstant()
        }
        if (!matchesCurrentGrid) throw AppointmentErrors.slotUnavailable()
        if (appointments.findOverlapsByCitizenId(principal.id, startsAt, endsAt).isNotEmpty()) {
            throw AppointmentErrors.citizenOverlap()
        }
        if (appointments.findOverlapsByProfessionalId(lockData.professionalId, startsAt, endsAt).isNotEmpty()) {
            throw AppointmentErrors.slotUnavailable()
        }

        val appointment = appointments.saveAndFlush(
            Appointment(
                citizen = citizen,
                professionalAssignment = assignment,
                startsAt = startsAt,
                endsAt = endsAt,
                idempotencyKey = idempotencyKey,
                requestHash = requestHash,
            ),
        )
        logs.record(
            user = citizen,
            action = LogAction.CREATE,
            entityType = LogEntityType.APPOINTMENT,
            entityId = requireNotNull(appointment.id).toString(),
            newValues = json.writeValueAsString(appointment.toAuditSnapshot()),
        )
        return AppointmentSubmission(appointment.toResponse(properties.zone()), replayed = false)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): AppointmentResponse {
        val citizen = authorizedCitizen("appointments:own:view")
        val appointment = appointments.findByIdAndCitizenId(id, requireNotNull(citizen.id))
            ?: throw AppointmentErrors.resourceNotFound()
        return appointment.toResponse(properties.zone())
    }

    private fun authorizedCitizen(permission: String): User {
        val principal = currentUser.principal()
        return validator.validateCitizen(users.findByIdWithRoles(principal.id), principal, permission)
    }

    private fun normalize(value: OffsetDateTime): OffsetDateTime =
        value.toInstant().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC)

    private fun hashRequest(assignmentId: UUID, startsAt: OffsetDateTime, endsAt: OffsetDateTime): String =
        HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(
                "appointment:v1:$assignmentId:${startsAt.toInstant()}:${endsAt.toInstant()}".toByteArray(Charsets.UTF_8),
            ),
        )
}
