package com.uade.dda2.server.feature.appointment.service

import com.uade.dda2.server.config.AppointmentProperties
import com.uade.dda2.server.feature.appointment.dto.request.CorrectAttentionRequest
import com.uade.dda2.server.feature.appointment.dto.request.CreateAttentionRequest
import com.uade.dda2.server.feature.appointment.dto.response.CommunityAttentionResponse
import com.uade.dda2.server.feature.appointment.dto.response.ProfessionalAppointmentResponse
import com.uade.dda2.server.feature.appointment.entity.Appointment
import com.uade.dda2.server.feature.appointment.entity.AppointmentStatus
import com.uade.dda2.server.feature.appointment.entity.AttentionResult
import com.uade.dda2.server.feature.appointment.entity.CommunityAttention
import com.uade.dda2.server.feature.appointment.error.AppointmentErrors
import com.uade.dda2.server.feature.appointment.error.AttentionErrors
import com.uade.dda2.server.feature.appointment.mapper.toResponse
import com.uade.dda2.server.feature.appointment.repository.AppointmentRepository
import com.uade.dda2.server.feature.appointment.repository.CommunityAttentionRepository
import com.uade.dda2.server.feature.appointment.validator.AppointmentValidator
import com.uade.dda2.server.feature.appointment.validator.AttentionValidator
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class CommunityAttentionService(
    private val appointments: AppointmentRepository,
    private val attentions: CommunityAttentionRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val appointmentValidator: AppointmentValidator,
    private val attentionValidator: AttentionValidator,
    private val properties: AppointmentProperties,
    private val logs: LogService,
    private val json: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun list(date: LocalDate): List<ProfessionalAppointmentResponse> {
        val professional = professional("appointments:professional:view")
        val zone = properties.zone()
        val start = date.atStartOfDay(zone).toOffsetDateTime()
        val end = date.plusDays(1).atStartOfDay(zone).toOffsetDateTime()
        val found = appointments.findByProfessionalIdInRange(requireNotNull(professional.id), start, end)
        if (found.isEmpty()) return emptyList()
        val byAppointmentId = attentions.findAllByAppointmentIdIn(found.map { requireNotNull(it.id) })
            .associateBy { requireNotNull(it.appointment.id) }
        return found.map { it.toProfessionalResponse(byAppointmentId[it.id]) }
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): ProfessionalAppointmentResponse {
        val professional = professional("appointments:professional:view")
        val appointment = ownAppointment(id, professional)
        return appointment.toProfessionalResponse(attentions.findByAppointmentId(id))
    }

    @Transactional
    fun create(id: UUID, request: CreateAttentionRequest): CommunityAttentionResponse {
        val professional = professional("appointments:professional:manage")
        val appointment = lockedOwnAppointment(id, professional)
        val now = OffsetDateTime.now(properties.zone())
        requireRegistrable(appointment, now)
        if (attentions.findByAppointmentId(id) != null) throw AttentionErrors.alreadyRegistered()
        val data = attentionValidator.validate(request.result, request.attendedOn, request.description, now.toLocalDate())
        val attention = attentions.saveAndFlush(
            CommunityAttention(
                appointment = appointment,
                result = request.result,
                attendedOn = data.attendedOn,
                description = data.description,
            ),
        )
        record(professional, appointment, LogAction.CREATE, null, request.result)
        return attention.toResponse()
    }

    @Transactional
    fun correct(id: UUID, request: CorrectAttentionRequest): CommunityAttentionResponse {
        val professional = professional("appointments:professional:manage")
        val appointment = lockedOwnAppointment(id, professional)
        requireRegistrable(appointment, OffsetDateTime.now(properties.zone()))
        val attention = attentions.findByAppointmentId(id) ?: throw AttentionErrors.notFound()
        if (attention.version != request.version) throw AttentionErrors.changed()
        val data = attentionValidator.validate(request.result, request.attendedOn, request.description,
            OffsetDateTime.now(properties.zone()).toLocalDate())
        val oldResult = attention.result
        attention.result = request.result
        attention.attendedOn = data.attendedOn
        attention.description = data.description
        attentions.saveAndFlush(attention)
        record(professional, appointment, LogAction.UPDATE, oldResult, request.result)
        return attention.toResponse()
    }

    private fun professional(permission: String): User {
        val principal = currentUser.principal()
        return appointmentValidator.validateProfessional(users.findByIdWithRoles(principal.id), principal, permission)
    }

    private fun ownAppointment(id: UUID, professional: User): Appointment {
        val appointment = appointments.findDetailById(id) ?: throw AppointmentErrors.resourceNotFound()
        if (appointment.professionalAssignment.professional.id != professional.id) throw AppointmentErrors.resourceNotFound()
        return appointment
    }

    private fun lockedOwnAppointment(id: UUID, professional: User): Appointment {
        val appointment = appointments.findByIdForUpdate(id) ?: throw AppointmentErrors.resourceNotFound()
        if (appointment.professionalAssignment.professional.id != professional.id) throw AppointmentErrors.resourceNotFound()
        return appointment
    }

    private fun requireRegistrable(appointment: Appointment, now: OffsetDateTime) {
        if (appointment.status != AppointmentStatus.CONFIRMED || appointment.startsAt.toInstant().isAfter(now.toInstant())) {
            throw AttentionErrors.notRegistrable()
        }
    }

    private fun Appointment.toProfessionalResponse(attention: CommunityAttention?): ProfessionalAppointmentResponse =
        ProfessionalAppointmentResponse(
            appointment = toResponse(properties.zone()),
            citizenId = requireNotNull(citizen.id),
            citizenName = citizen.name,
            attention = attention?.toResponse(),
        )

    // Only metadata is audited: descriptions (including corrected text) never enter the audit log.
    private fun record(actor: User, appointment: Appointment, action: LogAction, old: AttentionResult?, new: AttentionResult) {
        logs.record(
            user = actor,
            action = action,
            entityType = LogEntityType.APPOINTMENT,
            entityId = requireNotNull(appointment.id).toString(),
            oldValues = old?.let { json.writeValueAsString(mapOf("attentionResult" to it.name)) },
            newValues = json.writeValueAsString(mapOf("operation" to "ATTENTION", "attentionResult" to new.name)),
        )
    }
}
