package com.uade.dda2.server.feature.appointment.service

import com.uade.dda2.server.feature.application.repository.ApplicationVisitRepository
import com.uade.dda2.server.feature.appointment.entity.AppointmentStatus
import com.uade.dda2.server.feature.appointment.repository.AppointmentRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

data class AgendaBusyInterval(
    val userId: Long,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
)

@Service
class AgendaAvailabilityService(
    private val appointments: AppointmentRepository,
    private val visits: ApplicationVisitRepository,
) {
    fun hasConflict(
        userId: Long,
        startsAt: OffsetDateTime,
        endsAt: OffsetDateTime,
        excludeAppointmentId: UUID? = null,
        excludeVisitId: UUID? = null,
    ): Boolean = busyIntervals(
        userIds = setOf(userId),
        rangeStart = startsAt,
        rangeEnd = endsAt,
        excludeAppointmentId = excludeAppointmentId,
        excludeVisitId = excludeVisitId,
    ).isNotEmpty()

    fun busyIntervals(
        userIds: Collection<Long>,
        rangeStart: OffsetDateTime,
        rangeEnd: OffsetDateTime,
        excludeAppointmentId: UUID? = null,
        excludeVisitId: UUID? = null,
    ): List<AgendaBusyInterval> {
        if (userIds.isEmpty()) return emptyList()
        val relevantIds = userIds.toSet()
        val appointmentIntervals = appointments.findBlockingByParticipantIdsInRange(
            relevantIds,
            rangeStart,
            rangeEnd,
        ).filterNot { it.id == excludeAppointmentId }.flatMap { appointment ->
            buildList {
                val citizenId = requireNotNull(appointment.citizen.id)
                if (citizenId in relevantIds && appointment.status == AppointmentStatus.CONFIRMED) {
                    add(AgendaBusyInterval(citizenId, appointment.startsAt, appointment.endsAt))
                }
                val professionalId = requireNotNull(appointment.professionalAssignment.professional.id)
                val blocksProfessional = appointment.status == AppointmentStatus.CONFIRMED ||
                    (appointment.status == AppointmentStatus.CANCELLED && appointment.slotReleasedAt == null)
                if (professionalId in relevantIds && blocksProfessional) {
                    add(AgendaBusyInterval(professionalId, appointment.startsAt, appointment.endsAt))
                }
            }
        }
        val visitIntervals = visits.findByParticipantIdsInRange(relevantIds, rangeStart, rangeEnd)
            .filterNot { it.id == excludeVisitId }
            .flatMap { visit ->
                buildList {
                    val responsibleId = requireNotNull(visit.responsible.id)
                    if (responsibleId in relevantIds) {
                        add(AgendaBusyInterval(responsibleId, visit.startsAt, visit.endsAt))
                    }
                    val applicantId = requireNotNull(visit.application.user.id)
                    if (applicantId in relevantIds && applicantId != responsibleId) {
                        add(AgendaBusyInterval(applicantId, visit.startsAt, visit.endsAt))
                    }
                }
            }
        return appointmentIntervals + visitIntervals
    }
}
