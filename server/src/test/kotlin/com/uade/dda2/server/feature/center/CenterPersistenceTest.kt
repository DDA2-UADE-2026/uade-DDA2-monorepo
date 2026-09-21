package com.uade.dda2.server.feature.center

import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.center.entity.CenterOpeningHour
import com.uade.dda2.server.feature.center.entity.CenterService
import com.uade.dda2.server.feature.center.entity.MunicipalCenter
import com.uade.dda2.server.feature.center.entity.MunicipalService
import com.uade.dda2.server.feature.center.entity.ProfessionalAssignment
import com.uade.dda2.server.feature.center.entity.ProfessionalAvailability
import com.uade.dda2.server.feature.center.repository.CenterOpeningHourRepository
import com.uade.dda2.server.feature.center.repository.CenterServiceRepository
import com.uade.dda2.server.feature.center.repository.MunicipalCenterRepository
import com.uade.dda2.server.feature.center.repository.MunicipalServiceRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAssignmentRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAvailabilityRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest(
    properties = [
        "spring.test.database.replace=NONE",
        "spring.datasource.url=jdbc:h2:mem:center-persistence;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
    ],
)
@ActiveProfiles("docs")
class CenterPersistenceTest {
    @Autowired lateinit var centers: MunicipalCenterRepository
    @Autowired lateinit var services: MunicipalServiceRepository
    @Autowired lateinit var centerServices: CenterServiceRepository
    @Autowired lateinit var openingHours: CenterOpeningHourRepository
    @Autowired lateinit var assignments: ProfessionalAssignmentRepository
    @Autowired lateinit var availabilities: ProfessionalAvailabilityRepository
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var entityManager: EntityManager

    @Test
    fun `persiste y actualiza nombres normalizados y campos recortados`() {
        val center = centers.saveAndFlush(
            MunicipalCenter(
                name = "  Centro   Ágil  ",
                address = "  Calle 123  ",
                phone = "   ",
                email = "  centro@example.com  ",
            ),
        )
        val service = services.saveAndFlush(
            MunicipalService(
                name = "  Orientación   Familiar ",
                description = "  Acompañamiento integral.  ",
                durationMinutes = 45,
            ),
        )

        assertEquals("Centro   Ágil", center.name)
        assertEquals("centro ágil", center.normalizedName)
        assertEquals("Calle 123", center.address)
        assertNull(center.phone)
        assertEquals("centro@example.com", center.email)
        assertEquals("orientación familiar", service.normalizedName)
        assertEquals("Acompañamiento integral.", service.description)
        assertNotNull(center.id)
        assertNotNull(center.createdAt)
        assertNotNull(center.updatedAt)

        center.name = "  CENTRO\tÁGIL  "
        centers.saveAndFlush(center)

        assertEquals("centro ágil", center.normalizedName)
        assertTrue(centers.existsByNormalizedName("centro ágil"))
        assertFalse(centers.existsByNormalizedNameAndIdNot("centro ágil", center.id!!))
        assertTrue(services.existsByNormalizedName("orientación familiar"))
    }

    @Test
    fun `rechaza nombres normalizados duplicados`() {
        centers.saveAndFlush(center("Centro Atención"))

        assertFailsWith<DataIntegrityViolationException> {
            centers.saveAndFlush(center("  CENTRO   ATENCIÓN "))
        }
    }

    @Test
    fun `rechaza duracion de servicio no positiva`() {
        assertFailsWith<DataIntegrityViolationException> {
            services.saveAndFlush(service("Servicio inválido", durationMinutes = 0))
        }
    }

    @Test
    fun `persiste relaciones unicas y permite listarlas por padre y estado`() {
        val center = centers.saveAndFlush(center("Centro Norte"))
        val service = services.saveAndFlush(service("Asesoramiento"))
        val relation = centerServices.saveAndFlush(CenterService(center = center, service = service))

        assertEquals(
            relation.id,
            centerServices.findByCenterIdAndServiceId(center.id!!, service.id!!)?.id,
        )
        assertEquals(listOf(relation.id), centerServices.findAllByCenterIdAndActive(center.id!!, true).map { it.id })
        assertEquals(relation.id, centerServices.findByIdForUpdate(relation.id!!)?.id)

        assertFailsWith<DataIntegrityViolationException> {
            centerServices.saveAndFlush(CenterService(center = center, service = service))
        }
    }

    @Test
    fun `rechaza asignacion profesional duplicada`() {
        val relation = persistedCenterService("Centro Sur", "Orientación social")
        val professional = users.saveAndFlush(user("professional@example.com"))
        assignments.saveAndFlush(
            ProfessionalAssignment(professional = professional, centerService = relation),
        )

        assertFailsWith<DataIntegrityViolationException> {
            assignments.saveAndFlush(
                ProfessionalAssignment(professional = professional, centerService = relation),
            )
        }
    }

    @Test
    fun `rechaza rangos invalidos de horarios de centro`() {
        val center = centers.saveAndFlush(center("Centro Horarios"))

        assertFailsWith<DataIntegrityViolationException> {
            openingHours.saveAndFlush(
                CenterOpeningHour(
                    center = center,
                    dayOfWeek = DayOfWeek.MONDAY,
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(10, 0),
                ),
            )
        }
    }

    @Test
    fun `rechaza rangos invalidos de disponibilidad profesional`() {
        val relation = persistedCenterService("Centro Agenda", "Salud")
        val professional = users.saveAndFlush(user("agenda@example.com"))
        val assignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = professional, centerService = relation),
        )

        assertFailsWith<DataIntegrityViolationException> {
            availabilities.saveAndFlush(
                ProfessionalAvailability(
                    assignment = assignment,
                    dayOfWeek = DayOfWeek.FRIDAY,
                    startTime = LocalTime.of(12, 0),
                    endTime = LocalTime.of(11, 0),
                ),
            )
        }
    }

    @Test
    fun `consulta solapamientos activos de centro con intervalos semiabiertos`() {
        val center = centers.saveAndFlush(center("Centro Solapamientos"))
        val existing = openingHours.saveAndFlush(
            CenterOpeningHour(
                center = center,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
            ),
        )
        openingHours.saveAndFlush(
            CenterOpeningHour(
                center = center,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(11, 0),
                active = false,
            ),
        )

        val overlaps = openingHours.findActiveOverlaps(
            center.id!!,
            DayOfWeek.MONDAY,
            LocalTime.of(9, 30),
            LocalTime.of(10, 30),
        )
        val adjacent = openingHours.findActiveOverlaps(
            center.id!!,
            DayOfWeek.MONDAY,
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
        )
        val excludingSelf = openingHours.findActiveOverlapsExcludingId(
            center.id!!,
            DayOfWeek.MONDAY,
            LocalTime.of(9, 30),
            LocalTime.of(10, 30),
            existing.id!!,
        )

        assertEquals(listOf(existing.id), overlaps.map { it.id })
        assertTrue(adjacent.isEmpty())
        assertTrue(excludingSelf.isEmpty())
        assertEquals(
            listOf(existing.id),
            openingHours.findAllByCenterIdAndActiveOrderByDayOfWeekAscStartTimeAsc(center.id!!, true).map { it.id },
        )
    }

    @Test
    fun `consulta conflictos globales del profesional y filtra toda la cadena activa`() {
        val firstRelation = persistedCenterService("Centro Uno", "Servicio Uno")
        val secondRelation = persistedCenterService("Centro Dos", "Servicio Dos")
        val professionalRole = roles.saveAndFlush(Role(name = "PROFESIONAL_CENTRO"))
        val professional = users.saveAndFlush(
            user("global@example.com").also { it.roles.add(professionalRole) },
        )
        val firstAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = professional, centerService = firstRelation),
        )
        val secondAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = professional, centerService = secondRelation),
        )
        val existing = availabilities.saveAndFlush(
            ProfessionalAvailability(
                assignment = firstAssignment,
                dayOfWeek = DayOfWeek.WEDNESDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
            ),
        )

        assertEquals(listOf(existing.id), globalOverlaps(professional.id!!).map { it.id })
        assertTrue(
            availabilities.findEffectiveOverlapsByProfessionalId(
                professional.id!!,
                DayOfWeek.WEDNESDAY,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
            ).isEmpty(),
        )
        assertTrue(
            availabilities.findEffectiveOverlapsByProfessionalIdExcludingId(
                professional.id!!,
                DayOfWeek.WEDNESDAY,
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                existing.id!!,
            ).isEmpty(),
        )
        assertEquals(
            listOf(firstAssignment.id, secondAssignment.id),
            assignments.findAllByProfessionalIdAndActive(professional.id!!, true).map { it.id },
        )

        existing.active = false
        availabilities.saveAndFlush(existing)
        assertTrue(globalOverlaps(professional.id!!).isEmpty())
        existing.active = true
        availabilities.saveAndFlush(existing)

        firstAssignment.active = false
        assignments.saveAndFlush(firstAssignment)
        assertTrue(globalOverlaps(professional.id!!).isEmpty())
        firstAssignment.active = true
        assignments.saveAndFlush(firstAssignment)

        firstRelation.active = false
        centerServices.saveAndFlush(firstRelation)
        assertTrue(globalOverlaps(professional.id!!).isEmpty())
        firstRelation.active = true
        centerServices.saveAndFlush(firstRelation)

        firstRelation.center.active = false
        centers.saveAndFlush(firstRelation.center)
        assertTrue(globalOverlaps(professional.id!!).isEmpty())
        firstRelation.center.active = true
        centers.saveAndFlush(firstRelation.center)

        firstRelation.service.active = false
        services.saveAndFlush(firstRelation.service)
        assertTrue(globalOverlaps(professional.id!!).isEmpty())
        firstRelation.service.active = true
        services.saveAndFlush(firstRelation.service)

        val professionalWithoutRole = users.saveAndFlush(user("without-role@example.com"))
        val assignmentWithoutRole = assignments.saveAndFlush(
            ProfessionalAssignment(professional = professionalWithoutRole, centerService = firstRelation),
        )
        availabilities.saveAndFlush(
            ProfessionalAvailability(
                assignment = assignmentWithoutRole,
                dayOfWeek = DayOfWeek.WEDNESDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
            ),
        )
        assertTrue(globalOverlaps(professionalWithoutRole.id!!).isEmpty())

        professional.active = false
        users.saveAndFlush(professional)
        assertTrue(globalOverlaps(professional.id!!).isEmpty())
    }

    private fun globalOverlaps(professionalId: Long): List<ProfessionalAvailability> =
        availabilities.findEffectiveOverlapsByProfessionalId(
            professionalId,
            DayOfWeek.WEDNESDAY,
            LocalTime.of(9, 30),
            LocalTime.of(10, 30),
        )

    private fun persistedCenterService(centerName: String, serviceName: String): CenterService {
        val center = centers.saveAndFlush(center(centerName))
        val service = services.saveAndFlush(service(serviceName))
        return centerServices.saveAndFlush(CenterService(center = center, service = service))
    }

    private fun center(name: String): MunicipalCenter =
        MunicipalCenter(name = name, address = "Calle Municipal 123")

    private fun service(name: String, durationMinutes: Int = 30): MunicipalService =
        MunicipalService(
            name = name,
            description = "Descripción de $name",
            durationMinutes = durationMinutes,
        )

    private fun user(email: String): User = User(name = "Profesional", email = email)
}
