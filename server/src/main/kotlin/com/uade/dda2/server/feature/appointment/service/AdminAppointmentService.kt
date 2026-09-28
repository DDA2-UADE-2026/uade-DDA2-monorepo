package com.uade.dda2.server.feature.appointment.service

import com.uade.dda2.server.config.AppointmentProperties
import com.uade.dda2.server.feature.appointment.dto.response.AppointmentResponse
import com.uade.dda2.server.feature.appointment.dto.response.AdminAppointmentResponse
import com.uade.dda2.server.feature.appointment.dto.request.RescheduleAppointmentRequest
import com.uade.dda2.server.feature.appointment.dto.response.AvailableAppointmentSlotResponse
import com.uade.dda2.server.feature.appointment.entity.Appointment
import com.uade.dda2.server.feature.appointment.entity.AppointmentStatus
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
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AdminAppointmentService(
    private val appointments: AppointmentRepository,
    private val centers: MunicipalCenterRepository,
    private val centerServices: CenterServiceRepository,
    private val municipalServices: MunicipalServiceRepository,
    private val assignments: ProfessionalAssignmentRepository,
    private val slots: AppointmentSlotService,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: AppointmentValidator,
    private val properties: AppointmentProperties,
    private val logs: LogService,
    private val json: JsonMapper,
    private val entityManager: EntityManager,
) {
    @Transactional(readOnly = true)
    fun get(id: UUID): AdminAppointmentResponse {
        admin("appointments:management:view")
        return (appointments.findDetailById(id) ?: throw AppointmentErrors.resourceNotFound()).toAdminResponse()
    }

    @Transactional(readOnly = true)
    fun list(centerId: UUID, date: LocalDate): List<AdminAppointmentResponse> {
        admin("appointments:management:view")
        val start = date.atStartOfDay(properties.zone()).toOffsetDateTime()
        val end = date.plusDays(1).atStartOfDay(properties.zone()).toOffsetDateTime()
        return appointments.findByCenterIdInRange(centerId, start, end).map { it.toAdminResponse() }
    }

    @Transactional(readOnly = true)
    fun listSlots(id: UUID, date: LocalDate): List<AvailableAppointmentSlotResponse> {
        admin("appointments:management:view")
        val appointment = appointments.findDetailById(id) ?: throw AppointmentErrors.resourceNotFound()
        val now = OffsetDateTime.now(properties.zone())
        validator.validateManageable(appointment, now)
        return slots.availableSlots(
            centerServiceId = requireNotNull(appointment.professionalAssignment.centerService.id),
            date = date,
            citizenId = requireNotNull(appointment.citizen.id),
            now = now,
            excludeAppointmentId = id,
        ).filterNot { it.professionalAssignmentId == appointment.professionalAssignment.id &&
            it.startsAt.toInstant() == appointment.startsAt.toInstant() &&
            it.endsAt.toInstant() == appointment.endsAt.toInstant() }
    }

    @Transactional
    fun reschedule(id: UUID, request: RescheduleAppointmentRequest): AdminAppointmentResponse {
        val target = assignments.findAppointmentLockDataById(request.professionalAssignmentId)
            ?: throw AppointmentErrors.slotUnavailable()
        val locked = lockAppointment(id, target.professionalId)
        val appointment = locked.appointment
        val now = OffsetDateTime.now(properties.zone()).truncatedTo(ChronoUnit.MICROS)
        validator.validateManageable(appointment, now)
        val currentAssignment = appointment.professionalAssignment
        if (target.centerServiceId != currentAssignment.centerService.id ||
            target.centerId != currentAssignment.centerService.center.id ||
            target.serviceId != currentAssignment.centerService.service.id) throw AppointmentErrors.slotUnavailable()
        val startsAt = request.startsAt.toInstant().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC)
        val endsAt = request.endsAt.toInstant().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC)
        validator.validateRequestedRange(startsAt, endsAt, now)
        if (currentAssignment.id == request.professionalAssignmentId &&
            appointment.startsAt.toInstant() == startsAt.toInstant() &&
            appointment.endsAt.toInstant() == endsAt.toInstant()) {
            throw AppointmentErrors.invalidRequest("El nuevo horario debe ser diferente del horario actual.")
        }
        municipalServices.findByIdForUpdate(target.serviceId) ?: throw AppointmentErrors.slotUnavailable()
        centerServices.findByIdForUpdate(target.centerServiceId) ?: throw AppointmentErrors.slotUnavailable()
        val assignment = assignments.findByIdForUpdate(request.professionalAssignmentId)
            ?: throw AppointmentErrors.slotUnavailable()
        val date = startsAt.atZoneSameInstant(properties.zone()).toLocalDate()
        val onGrid = slots.availableSlots(
            centerServiceId = target.centerServiceId,
            date = date,
            citizenId = requireNotNull(appointment.citizen.id),
            now = now,
            includeOccupancy = false,
        ).any { it.professionalAssignmentId == request.professionalAssignmentId &&
            it.startsAt.toInstant() == startsAt.toInstant() && it.endsAt.toInstant() == endsAt.toInstant() }
        if (!onGrid) throw AppointmentErrors.slotUnavailable()
        if (appointments.findOverlapsByCitizenId(requireNotNull(appointment.citizen.id), startsAt, endsAt)
                .any { it.id != id }) throw AppointmentErrors.citizenOverlap()
        if (appointments.findOverlapsByProfessionalId(target.professionalId, startsAt, endsAt)
                .any { it.id != id }) throw AppointmentErrors.slotUnavailable()

        val old = json.writeValueAsString(appointment.toAuditSnapshot())
        appointment.professionalAssignment = assignment
        appointment.startsAt = startsAt
        appointment.endsAt = endsAt
        appointments.saveAndFlush(appointment)
        record(locked.actor, appointment, "RESCHEDULE", old)
        return appointment.toAdminResponse()
    }

    @Transactional
    fun cancel(id: UUID): AdminAppointmentResponse {
        val locked = lockAppointment(id)
        validator.validateManageable(locked.appointment, OffsetDateTime.now(properties.zone()))
        val old = json.writeValueAsString(locked.appointment.toAuditSnapshot())
        locked.appointment.status = AppointmentStatus.CANCELLED
        appointments.saveAndFlush(locked.appointment)
        record(locked.actor, locked.appointment, "CANCEL", old)
        return locked.appointment.toAdminResponse()
    }

    @Transactional
    fun release(id: UUID): AdminAppointmentResponse {
        val locked = lockAppointment(id)
        if (locked.appointment.status != AppointmentStatus.CANCELLED) throw AppointmentErrors.notManageable()
        if (locked.appointment.slotReleasedAt != null) throw AppointmentErrors.slotAlreadyReleased()
        val old = json.writeValueAsString(locked.appointment.toAuditSnapshot())
        locked.appointment.slotReleasedAt = OffsetDateTime.now(properties.zone())
        appointments.saveAndFlush(locked.appointment)
        record(locked.actor, locked.appointment, "RELEASE", old)
        return locked.appointment.toAdminResponse()
    }

    internal data class LockedAppointment(val appointment: Appointment, val actor: User)

    // Same lock order as citizen booking and HU-17: center, users sorted by ID, appointment.
    internal fun lockAppointment(id: UUID, additionalProfessionalId: Long? = null): LockedAppointment {
        val principal = currentUser.principal()
        val snapshot = appointments.findDetailById(id) ?: throw AppointmentErrors.resourceNotFound()
        val centerId = requireNotNull(snapshot.professionalAssignment.centerService.center.id)
        val assignmentId = requireNotNull(snapshot.professionalAssignment.id)
        val citizenId = requireNotNull(snapshot.citizen.id)
        val professionalId = requireNotNull(snapshot.professionalAssignment.professional.id)
        val previousStart = snapshot.startsAt.toInstant()
        val previousEnd = snapshot.endsAt.toInstant()
        val previousStatus = snapshot.status
        val previousRelease = snapshot.slotReleasedAt
        centers.findByIdForUpdate(centerId) ?: throw AppointmentErrors.resourceNotFound()
        val lockedUsers = users.findAllByIdsForUpdate(
            listOfNotNull(principal.id, citizenId, professionalId, additionalProfessionalId).distinct().sorted(),
        ).associateBy { requireNotNull(it.id) }
        val actor = validator.validateAdmin(lockedUsers[principal.id], principal, "appointments:management:manage")
        val appointment = appointments.findByIdForUpdate(id) ?: throw AppointmentErrors.resourceNotFound()
        entityManager.refresh(appointment)
        if (appointment.professionalAssignment.id != assignmentId ||
            appointment.citizen.id != citizenId ||
            appointment.startsAt.toInstant() != previousStart ||
            appointment.endsAt.toInstant() != previousEnd ||
            appointment.status != previousStatus || appointment.slotReleasedAt != previousRelease ||
            lockedUsers[professionalId] == null || lockedUsers[citizenId] == null ||
            (additionalProfessionalId != null && lockedUsers[additionalProfessionalId] == null)) {
            throw AppointmentErrors.notManageable()
        }
        return LockedAppointment(appointment, actor)
    }

    internal fun record(actor: User, appointment: Appointment, operation: String, old: String) {
        logs.record(
            user = actor,
            action = LogAction.UPDATE,
            entityType = LogEntityType.APPOINTMENT,
            entityId = requireNotNull(appointment.id).toString(),
            oldValues = old,
            newValues = json.writeValueAsString(appointment.toAuditSnapshot() + ("operation" to operation)),
        )
    }

    private fun admin(permission: String): User {
        val principal = currentUser.principal()
        return validator.validateAdmin(users.findByIdWithRoles(principal.id), principal, permission)
    }

    private fun Appointment.toAdminResponse(): AdminAppointmentResponse = AdminAppointmentResponse(
        appointment = toResponse(properties.zone()),
        citizenId = requireNotNull(citizen.id),
        citizenName = citizen.name,
        slotRetained = status == AppointmentStatus.CANCELLED && slotReleasedAt == null,
    )
}
