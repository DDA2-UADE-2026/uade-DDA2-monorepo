package com.uade.dda2.server.feature.appointment.service

import com.uade.dda2.server.config.AppointmentProperties
import com.uade.dda2.server.feature.appointment.dto.response.AppointmentResponse
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
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AdminAppointmentService(
    private val appointments: AppointmentRepository,
    private val centers: MunicipalCenterRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: AppointmentValidator,
    private val properties: AppointmentProperties,
    private val logs: LogService,
    private val json: JsonMapper,
    private val entityManager: EntityManager,
) {
    @Transactional(readOnly = true)
    fun get(id: UUID): AppointmentResponse {
        admin("appointments:management:view")
        return (appointments.findDetailById(id) ?: throw AppointmentErrors.resourceNotFound()).toResponse(properties.zone())
    }

    @Transactional
    fun cancel(id: UUID): AppointmentResponse {
        val locked = lockAppointment(id)
        validator.validateManageable(locked.appointment, OffsetDateTime.now(properties.zone()))
        val old = json.writeValueAsString(locked.appointment.toAuditSnapshot())
        locked.appointment.status = AppointmentStatus.CANCELLED
        appointments.saveAndFlush(locked.appointment)
        record(locked.actor, locked.appointment, "CANCEL", old)
        return locked.appointment.toResponse(properties.zone())
    }

    @Transactional
    fun release(id: UUID): AppointmentResponse {
        val locked = lockAppointment(id)
        if (locked.appointment.status != AppointmentStatus.CANCELLED) throw AppointmentErrors.notManageable()
        if (locked.appointment.slotReleasedAt != null) throw AppointmentErrors.slotAlreadyReleased()
        val old = json.writeValueAsString(locked.appointment.toAuditSnapshot())
        locked.appointment.slotReleasedAt = OffsetDateTime.now(properties.zone())
        appointments.saveAndFlush(locked.appointment)
        record(locked.actor, locked.appointment, "RELEASE", old)
        return locked.appointment.toResponse(properties.zone())
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
        centers.findByIdForUpdate(centerId) ?: throw AppointmentErrors.resourceNotFound()
        val lockedUsers = users.findAllByIdsForUpdate(
            listOfNotNull(principal.id, citizenId, professionalId, additionalProfessionalId).distinct().sorted(),
        ).associateBy { requireNotNull(it.id) }
        val actor = validator.validateAdmin(lockedUsers[principal.id], principal, "appointments:management:manage")
        val appointment = appointments.findByIdForUpdate(id) ?: throw AppointmentErrors.resourceNotFound()
        entityManager.refresh(appointment)
        if (appointment.professionalAssignment.id != assignmentId ||
            appointment.citizen.id != citizenId ||
            lockedUsers[professionalId] == null || lockedUsers[citizenId] == null) {
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
}
