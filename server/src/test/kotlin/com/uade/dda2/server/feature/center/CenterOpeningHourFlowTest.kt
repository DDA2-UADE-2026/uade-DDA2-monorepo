package com.uade.dda2.server.feature.center

import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.center.dto.response.CenterOpeningHourResponse
import com.uade.dda2.server.feature.center.entity.CenterOpeningHour
import com.uade.dda2.server.feature.center.entity.CenterService
import com.uade.dda2.server.feature.center.entity.MunicipalCenter
import com.uade.dda2.server.feature.center.entity.MunicipalService
import com.uade.dda2.server.feature.center.entity.ProfessionalAssignment
import com.uade.dda2.server.feature.center.entity.ProfessionalAvailability
import com.uade.dda2.server.feature.center.repository.CenterServiceRepository
import com.uade.dda2.server.feature.center.repository.CenterOpeningHourRepository
import com.uade.dda2.server.feature.center.repository.MunicipalCenterRepository
import com.uade.dda2.server.feature.center.repository.MunicipalServiceRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAssignmentRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAvailabilityRepository
import com.uade.dda2.server.security.JwtService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:center-opening-hours;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class CenterOpeningHourFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var centers: MunicipalCenterRepository
    @Autowired lateinit var services: MunicipalServiceRepository
    @Autowired lateinit var centerServices: CenterServiceRepository
    @Autowired lateinit var openingHours: CenterOpeningHourRepository
    @Autowired lateinit var assignments: ProfessionalAssignmentRepository
    @Autowired lateinit var availabilities: ProfessionalAvailabilityRepository
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var transactions: PlatformTransactionManager

    private lateinit var token: String
    private lateinit var professionalRole: Role

    @BeforeEach
    fun setup() {
        token = tx {
            val granted = listOf("schedules:management:view", "schedules:management:manage").map { name ->
                permissions.findByNameIn(listOf(name)).firstOrNull() ?: permissions.save(Permission(name = name))
            }.toMutableSet()
            val role = roles.findByNameIn(listOf("ADMIN")).firstOrNull()
                ?.also { it.permissions.addAll(granted) }
                ?: Role(name = "ADMIN", permissions = granted)
            roles.saveAndFlush(role)
            professionalRole = roles.findByNameIn(listOf("PROFESIONAL_CENTRO")).firstOrNull()
                ?: roles.saveAndFlush(Role(name = "PROFESIONAL_CENTRO"))
            val admin = users.saveAndFlush(
                User(
                    name = "Schedule Admin",
                    email = "schedule-admin-${UUID.randomUUID()}@example.com",
                    roles = mutableSetOf(role),
                ),
            )
            jwt.createToken(admin, role)
        }
    }

    @Test
    fun `crea lista edita y cambia estado de horarios`() {
        val centerId = center()
        val morning = response(create(centerId, "MONDAY", "08:00", "12:00").also { expect(it, 201) })
        val afternoon = response(create(centerId, "MONDAY", "12:00", "18:00").also { expect(it, 201) })
        assertTrue(morning.active)
        assertEquals(LocalTime.of(12, 0), afternoon.startTime)

        expect(create(centerId, "MONDAY", "11:00", "13:00"), 409, "CENTER_OPENING_HOUR_OVERLAP")
        expect(create(centerId, "TUESDAY", "10:00", "10:00"), 400, "CENTER_OPENING_HOUR_INVALID_RANGE")

        val updated = response(update(afternoon.id, "MONDAY", "13:00", "19:00").also { expect(it, 200) })
        assertEquals(LocalTime.of(13, 0), updated.startTime)

        val inactive = response(changeStatus(updated.id, "deactivate").also { expect(it, 200) })
        assertFalse(inactive.active)
        val active = response(changeStatus(updated.id, "activate").also { expect(it, 200) })
        assertTrue(active.active)

        val list = mvc.perform(
            get("/api/admin/municipal-centers/$centerId/opening-hours")
                .header("Authorization", "Bearer $token"),
        ).andReturn()
        expect(list, 200)
        assertTrue(list.response.contentAsString.contains("MONDAY"))
        assertEquals(4, auditCount(updated.id))
    }

    @Test
    fun `impide cambios que dejan disponibilidad sin cobertura`() {
        val fixture = fixtureWithAvailability()
        expect(
            update(fixture.openingHourId, "MONDAY", "10:00", "12:00"),
            409,
            "PROFESSIONAL_AVAILABILITY_OUTSIDE_OPENING_HOURS",
        )
        expect(
            changeStatus(fixture.openingHourId, "deactivate"),
            409,
            "PROFESSIONAL_AVAILABILITY_OUTSIDE_OPENING_HOURS",
        )
    }

    @Test
    fun `crea la misma franja en varios días y revierte todo ante conflicto`() {
        val centerId = center()
        val created = createBulk(centerId, listOf("MONDAY", "TUESDAY", "THURSDAY"), "09:00", "12:00")
            .also { expect(it, 201) }.response.contentAsString
        assertTrue(created.contains("MONDAY") && created.contains("TUESDAY") && created.contains("THURSDAY"))
        assertEquals(3, activeCount(centerId))

        val conflict = createBulk(centerId, listOf("MONDAY", "WEDNESDAY"), "10:00", "11:00")
        expect(conflict, 409, "CENTER_OPENING_HOUR_OVERLAP")
        assertTrue(conflict.response.contentAsString.contains("MONDAY"))
        assertEquals(3, activeCount(centerId))
        assertTrue(
            tx {
                openingHours.findAllByCenterIdAndActiveOrderByDayOfWeekAscStartTimeAsc(centerId, true)
            }.none { it.dayOfWeek == DayOfWeek.WEDNESDAY },
        )
    }

    @Test
    fun `rechaza bulk con días inválidos o centro inactivo`() {
        val centerId = center()
        expect(createBulk(centerId, emptyList(), "09:00", "12:00"), 400, "CENTER_OPENING_HOUR_INVALID_DAYS")
        expect(createBulk(centerId, listOf("MONDAY", "MONDAY"), "09:00", "12:00"), 400, "CENTER_OPENING_HOUR_INVALID_DAYS")
        expect(createBulk(centerId, listOf("MONDAY"), "12:00", "09:00"), 400, "CENTER_OPENING_HOUR_INVALID_RANGE")
        tx {
            val center = centers.findById(centerId).orElseThrow()
            center.active = false
            centers.saveAndFlush(center)
        }
        expect(createBulk(centerId, listOf("MONDAY"), "09:00", "12:00"), 409, "CENTER_DEPENDENCY_INACTIVE")
    }

    private fun activeCount(centerId: UUID): Int = tx {
        openingHours.findAllByCenterIdAndActiveOrderByDayOfWeekAscStartTimeAsc(centerId, true).size
    }

    private fun fixtureWithAvailability(): Fixture = tx {
        val suffix = UUID.randomUUID().toString().take(8)
        val center = centers.saveAndFlush(MunicipalCenter(name = "Centro $suffix", address = "Calle 123"))
        val service = services.saveAndFlush(
            MunicipalService(name = "Servicio $suffix", description = "Descripción", durationMinutes = 30),
        )
        val centerService = centerServices.saveAndFlush(CenterService(center = center, service = service))
        val professional = users.saveAndFlush(
            User(
                name = "Profesional",
                email = "professional-${UUID.randomUUID()}@example.com",
                roles = mutableSetOf(professionalRole),
            ),
        )
        val assignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = professional, centerService = centerService),
        )
        val openingHour = CenterOpeningHour(
            center = center,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(12, 0),
        )
        val savedOpening = openingHours.saveAndFlush(openingHour)
        availabilities.saveAndFlush(
            ProfessionalAvailability(
                assignment = assignment,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(11, 0),
            ),
        )
        Fixture(requireNotNull(savedOpening.id))
    }

    private fun center(): UUID = tx {
        requireNotNull(
            centers.saveAndFlush(
                MunicipalCenter(name = "Centro ${UUID.randomUUID()}", address = "Calle 123"),
            ).id,
        )
    }

    private fun create(centerId: UUID, day: String, start: String, end: String): MvcResult =
        mvc.perform(
            post("/api/admin/municipal-centers/$centerId/opening-hours")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(day, start, end)),
        ).andReturn()

    private fun update(id: UUID, day: String, start: String, end: String): MvcResult =
        mvc.perform(
            put("/api/admin/center-opening-hours/$id")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(day, start, end)),
        ).andReturn()

    private fun changeStatus(id: UUID, action: String): MvcResult =
        mvc.perform(
            patch("/api/admin/center-opening-hours/$id/$action")
                .header("Authorization", "Bearer $token"),
        ).andReturn()

    private fun scheduleBody(day: String, start: String, end: String): String =
        json.writeValueAsString(mapOf("dayOfWeek" to day, "startTime" to start, "endTime" to end))

    private fun createBulk(centerId: UUID, days: List<String>, start: String, end: String): MvcResult =
        mvc.perform(
            post("/api/admin/municipal-centers/$centerId/opening-hours/batch")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(mapOf("days" to days, "startTime" to start, "endTime" to end))),
        ).andReturn()

    private fun response(result: MvcResult): CenterOpeningHourResponse =
        json.readValue(result.response.contentAsString, CenterOpeningHourResponse::class.java)

    private fun auditCount(id: UUID): Int =
        jdbc.queryForObject(
            "select count(*) from logs where entity_type = 'center_opening_hour' and entity_id = ?",
            Int::class.java,
            id.toString(),
        ) ?: 0

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").asText())
    }

    private fun <T> tx(block: () -> T): T = TransactionTemplate(transactions).execute { block() }!!

    private data class Fixture(val openingHourId: UUID)
}
