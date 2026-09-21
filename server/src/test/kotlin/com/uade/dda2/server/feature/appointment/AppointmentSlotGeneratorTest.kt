package com.uade.dda2.server.feature.appointment

import com.uade.dda2.server.feature.appointment.entity.Appointment
import com.uade.dda2.server.feature.appointment.service.AppointmentSlotGenerator
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.center.entity.CenterService
import com.uade.dda2.server.feature.center.entity.MunicipalCenter
import com.uade.dda2.server.feature.center.entity.MunicipalService
import com.uade.dda2.server.feature.center.entity.ProfessionalAssignment
import com.uade.dda2.server.feature.center.entity.ProfessionalAvailability
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals

class AppointmentSlotGeneratorTest {
    private val generator = AppointmentSlotGenerator()
    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")
    private val date = LocalDate.of(2030, 1, 7)
    private val professional = User(id = 10, name = "Profesional", email = "professional@example.com")
    private val citizen = User(id = 20, name = "Ciudadano", email = "citizen@example.com")
    private val assignment = ProfessionalAssignment(
        id = UUID.randomUUID(),
        professional = professional,
        centerService = CenterService(
            id = UUID.randomUUID(),
            center = MunicipalCenter(id = UUID.randomUUID(), name = "Centro", address = "Calle 1"),
            service = MunicipalService(
                id = UUID.randomUUID(),
                name = "Clinica",
                description = "Atencion",
                durationMinutes = 60,
            ),
        ),
    )

    @Test
    fun `genera bloques completos descarta remanente y permite adyacencia`() {
        val availability = availability(LocalTime.of(9, 0), LocalTime.of(11, 30))
        val adjacentAppointment = appointment(
            startsAt = at(LocalTime.of(8, 0)),
            endsAt = at(LocalTime.of(9, 0)),
        )

        val slots = generator.generate(
            date = date,
            durationMinutes = 60,
            availabilities = listOf(availability),
            professionalAppointments = listOf(adjacentAppointment),
            citizenAppointments = emptyList(),
            zone = zone,
            now = at(LocalTime.of(7, 0)),
        )

        assertEquals(listOf(LocalTime.of(9, 0), LocalTime.of(10, 0)), slots.map { it.startsAt.toLocalTime() })
        assertEquals(listOf(LocalTime.of(10, 0), LocalTime.of(11, 0)), slots.map { it.endsAt.toLocalTime() })
    }

    @Test
    fun `excluye ocupacion profesional y ciudadana y no fusiona franjas`() {
        val shortRanges = listOf(
            availability(LocalTime.of(9, 0), LocalTime.of(9, 30)),
            availability(LocalTime.of(9, 30), LocalTime.of(10, 0)),
        )
        assertEquals(
            emptyList(),
            generator.generate(
                date,
                60,
                shortRanges,
                emptyList(),
                emptyList(),
                zone,
                at(LocalTime.of(7, 0)),
            ),
        )

        val range = availability(LocalTime.of(9, 0), LocalTime.of(11, 0))
        val professionalBusy = appointment(at(LocalTime.of(9, 0)), at(LocalTime.of(10, 0)))
        val citizenBusy = appointment(at(LocalTime.of(10, 0)), at(LocalTime.of(11, 0)))
        assertEquals(
            emptyList(),
            generator.generate(
                date,
                60,
                listOf(range),
                listOf(professionalBusy),
                listOf(citizenBusy),
                zone,
                at(LocalTime.of(7, 0)),
            ),
        )
    }

    private fun availability(start: LocalTime, end: LocalTime): ProfessionalAvailability =
        ProfessionalAvailability(
            id = UUID.randomUUID(),
            assignment = assignment,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = start,
            endTime = end,
        )

    private fun appointment(startsAt: OffsetDateTime, endsAt: OffsetDateTime): Appointment =
        Appointment(
            id = UUID.randomUUID(),
            citizen = citizen,
            professionalAssignment = assignment,
            startsAt = startsAt,
            endsAt = endsAt,
            idempotencyKey = UUID.randomUUID().toString(),
            requestHash = "a".repeat(64),
        )

    private fun at(time: LocalTime): OffsetDateTime = date.atTime(time).atZone(zone).toOffsetDateTime()
}
