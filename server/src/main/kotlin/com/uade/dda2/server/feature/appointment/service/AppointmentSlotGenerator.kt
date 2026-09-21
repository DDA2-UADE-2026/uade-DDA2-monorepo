package com.uade.dda2.server.feature.appointment.service

import com.uade.dda2.server.feature.appointment.dto.response.AvailableAppointmentSlotResponse
import com.uade.dda2.server.feature.appointment.entity.Appointment
import com.uade.dda2.server.feature.center.entity.ProfessionalAvailability
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

@Component
class AppointmentSlotGenerator {
    fun generate(
        date: LocalDate,
        durationMinutes: Int,
        availabilities: Collection<ProfessionalAvailability>,
        professionalAppointments: Collection<Appointment>,
        citizenAppointments: Collection<Appointment>,
        zone: ZoneId,
        now: OffsetDateTime,
    ): List<AvailableAppointmentSlotResponse> {
        if (durationMinutes <= 0) return emptyList()
        val appointmentsByProfessional = professionalAppointments.groupBy {
            requireNotNull(it.professionalAssignment.professional.id)
        }
        return availabilities.flatMap { availability ->
            val assignment = availability.assignment
            val professional = assignment.professional
            val professionalId = requireNotNull(professional.id)
            val result = mutableListOf<AvailableAppointmentSlotResponse>()
            var startsAt = date.atTime(availability.startTime).atZone(zone)
            val availabilityEnd = date.atTime(availability.endTime).atZone(zone)
            while (true) {
                val endsAt = startsAt.plusMinutes(durationMinutes.toLong())
                if (endsAt.isAfter(availabilityEnd)) break
                val inFuture = startsAt.toInstant().isAfter(now.toInstant())
                val professionalFree = appointmentsByProfessional[professionalId].orEmpty()
                    .none { overlaps(it.startsAt, it.endsAt, startsAt.toOffsetDateTime(), endsAt.toOffsetDateTime()) }
                val citizenFree = citizenAppointments.none {
                    overlaps(it.startsAt, it.endsAt, startsAt.toOffsetDateTime(), endsAt.toOffsetDateTime())
                }
                if (inFuture && professionalFree && citizenFree) {
                    result += AvailableAppointmentSlotResponse(
                        professionalAssignmentId = requireNotNull(assignment.id),
                        professionalId = professionalId,
                        professionalName = professional.name,
                        startsAt = startsAt.toOffsetDateTime(),
                        endsAt = endsAt.toOffsetDateTime(),
                    )
                }
                startsAt = endsAt
            }
            result
        }.distinctBy { Triple(it.professionalAssignmentId, it.startsAt.toInstant(), it.endsAt.toInstant()) }
            .sortedWith(compareBy(AvailableAppointmentSlotResponse::startsAt, AvailableAppointmentSlotResponse::professionalName))
    }

    private fun overlaps(
        existingStart: OffsetDateTime,
        existingEnd: OffsetDateTime,
        requestedStart: OffsetDateTime,
        requestedEnd: OffsetDateTime,
    ): Boolean = existingStart.isBefore(requestedEnd) && existingEnd.isAfter(requestedStart)
}
