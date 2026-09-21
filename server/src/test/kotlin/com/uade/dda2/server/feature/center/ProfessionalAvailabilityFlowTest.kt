package com.uade.dda2.server.feature.center

import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.center.dto.response.ProfessionalAvailabilityResponse
import com.uade.dda2.server.feature.center.entity.CenterOpeningHour
import com.uade.dda2.server.feature.center.entity.CenterService
import com.uade.dda2.server.feature.center.entity.MunicipalCenter
import com.uade.dda2.server.feature.center.entity.MunicipalService
import com.uade.dda2.server.feature.center.entity.ProfessionalAssignment
import com.uade.dda2.server.feature.center.repository.CenterOpeningHourRepository
import com.uade.dda2.server.feature.center.repository.CenterServiceRepository
import com.uade.dda2.server.feature.center.repository.MunicipalCenterRepository
import com.uade.dda2.server.feature.center.repository.MunicipalServiceRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAssignmentRepository
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
        "spring.datasource.url=jdbc:h2:mem:professional-availability;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ProfessionalAvailabilityFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var centers: MunicipalCenterRepository
    @Autowired lateinit var services: MunicipalServiceRepository
    @Autowired lateinit var centerServices: CenterServiceRepository
    @Autowired lateinit var assignments: ProfessionalAssignmentRepository
    @Autowired lateinit var openingHours: CenterOpeningHourRepository
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
            val adminRole = roles.findByNameIn(listOf("ADMIN")).firstOrNull()
                ?.also { it.permissions.addAll(granted) }
                ?: Role(name = "ADMIN", permissions = granted)
            roles.saveAndFlush(adminRole)
            professionalRole = roles.findByNameIn(listOf("PROFESIONAL_CENTRO")).firstOrNull()
                ?: roles.saveAndFlush(Role(name = "PROFESIONAL_CENTRO"))
            val admin = users.saveAndFlush(
                User(
                    name = "Availability Admin",
                    email = "availability-admin-${UUID.randomUUID()}@example.com",
                    roles = mutableSetOf(adminRole),
                ),
            )
            jwt.createToken(admin, adminRole)
        }
    }

    @Test
    fun `crea disponibilidad por servicio y permite franjas adyacentes`() {
        val fixture = fixture()
        val first = response(create(fixture.firstAssignmentId, "09:00", "10:30").also { expect(it, 201) })
        assertEquals(fixture.firstServiceId, first.serviceId)
        assertTrue(first.active)

        expect(
            create(fixture.secondAssignmentId, "10:00", "11:00"),
            409,
            "PROFESSIONAL_AVAILABILITY_OVERLAP",
        )
        val adjacent = response(create(fixture.secondAssignmentId, "10:30", "11:30").also { expect(it, 201) })
        assertEquals(fixture.secondServiceId, adjacent.serviceId)

        expect(
            create(fixture.secondAssignmentId, "11:30", "12:30"),
            409,
            "PROFESSIONAL_AVAILABILITY_OUTSIDE_OPENING_HOURS",
        )
        expect(
            create(fixture.secondAssignmentId, "11:00", "11:00"),
            400,
            "PROFESSIONAL_AVAILABILITY_INVALID_RANGE",
        )

        val list = mvc.perform(
            get("/api/admin/professional-assignments/${fixture.firstAssignmentId}/availability")
                .header("Authorization", "Bearer $token"),
        ).andReturn()
        expect(list, 200)
        assertTrue(list.response.contentAsString.contains(first.id.toString()))
    }

    @Test
    fun `edita desactiva y revalida al reactivar`() {
        val fixture = fixture()
        val first = response(create(fixture.firstAssignmentId, "09:00", "10:00"))
        response(create(fixture.secondAssignmentId, "10:00", "11:30"))

        val inactive = response(changeStatus(first.id, "deactivate").also { expect(it, 200) })
        assertFalse(inactive.active)
        val edited = response(update(first.id, "10:30", "11:00").also { expect(it, 200) })
        assertEquals(LocalTime.of(10, 30), edited.startTime)
        expect(changeStatus(first.id, "activate"), 409, "PROFESSIONAL_AVAILABILITY_OVERLAP")
        assertEquals(3, auditCount(first.id))
    }

    @Test
    fun `rechaza crear disponibilidad con asignacion inactiva`() {
        val fixture = fixture()
        tx {
            val assignment = assignments.findById(fixture.firstAssignmentId).orElseThrow()
            assignment.active = false
            assignments.saveAndFlush(assignment)
        }
        expect(
            create(fixture.firstAssignmentId, "09:00", "10:00"),
            409,
            "CENTER_DEPENDENCY_INACTIVE",
        )
    }

    private fun fixture(): Fixture = tx {
        val suffix = UUID.randomUUID().toString().take(8)
        val center = centers.saveAndFlush(MunicipalCenter(name = "Centro $suffix", address = "Calle 123"))
        openingHours.saveAndFlush(
            CenterOpeningHour(
                center = center,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(10, 0),
            ),
        )
        openingHours.saveAndFlush(
            CenterOpeningHour(
                center = center,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(12, 0),
            ),
        )
        openingHours.saveAndFlush(
            CenterOpeningHour(
                center = center,
                dayOfWeek = DayOfWeek.TUESDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(12, 0),
            ),
        )
        openingHours.saveAndFlush(
            CenterOpeningHour(
                center = center,
                dayOfWeek = DayOfWeek.WEDNESDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(12, 0),
            ),
        )
        val firstService = services.saveAndFlush(
            MunicipalService(name = "Servicio A $suffix", description = "Descripción", durationMinutes = 30),
        )
        val secondService = services.saveAndFlush(
            MunicipalService(name = "Servicio B $suffix", description = "Descripción", durationMinutes = 30),
        )
        val firstCenterService = centerServices.saveAndFlush(CenterService(center = center, service = firstService))
        val secondCenterService = centerServices.saveAndFlush(CenterService(center = center, service = secondService))
        val professional = users.saveAndFlush(
            User(
                name = "Profesional",
                email = "professional-${UUID.randomUUID()}@example.com",
                roles = mutableSetOf(professionalRole),
            ),
        )
        val firstAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = professional, centerService = firstCenterService),
        )
        val secondAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = professional, centerService = secondCenterService),
        )
        Fixture(
            requireNotNull(firstAssignment.id),
            requireNotNull(secondAssignment.id),
            requireNotNull(firstService.id),
            requireNotNull(secondService.id),
        )
    }

    @Test
    fun `crea la misma disponibilidad en varios días y revierte todo ante conflicto`() {
        val fixture = fixture()
        val created = createBulk(fixture.firstAssignmentId, listOf("TUESDAY", "WEDNESDAY"), "09:00", "11:00")
            .also { expect(it, 201) }.response.contentAsString
        assertTrue(created.contains("TUESDAY") && created.contains("WEDNESDAY"))

        val conflict = createBulk(fixture.secondAssignmentId, listOf("TUESDAY", "WEDNESDAY"), "09:30", "10:30")
        expect(conflict, 409, "PROFESSIONAL_AVAILABILITY_OVERLAP")
        assertTrue(conflict.response.contentAsString.contains("martes"))
        val wednesday = mvc.perform(
            get("/api/admin/professional-assignments/${fixture.secondAssignmentId}/availability")
                .header("Authorization", "Bearer $token"),
        ).andReturn()
        expect(wednesday, 200)
        assertEquals("[]", wednesday.response.contentAsString)
    }

    @Test
    fun `rechaza bulk sin cobertura o con días inválidos`() {
        val fixture = fixture()
        val noCoverage = createBulk(fixture.firstAssignmentId, listOf("THURSDAY"), "09:00", "11:00")
        expect(noCoverage, 409, "PROFESSIONAL_AVAILABILITY_OUTSIDE_OPENING_HOURS")
        assertTrue(noCoverage.response.contentAsString.contains("jueves"))

        expect(createBulk(fixture.firstAssignmentId, emptyList(), "09:00", "11:00"), 400, "PROFESSIONAL_AVAILABILITY_INVALID_DAYS")
        expect(
            createBulk(fixture.firstAssignmentId, listOf("TUESDAY", "TUESDAY"), "09:00", "11:00"),
            400,
            "PROFESSIONAL_AVAILABILITY_INVALID_DAYS",
        )
        expect(
            createBulk(fixture.firstAssignmentId, listOf("TUESDAY"), "11:00", "11:00"),
            400,
            "PROFESSIONAL_AVAILABILITY_INVALID_RANGE",
        )
    }

    private fun create(assignmentId: UUID, start: String, end: String): MvcResult =
        mvc.perform(
            post("/api/admin/professional-assignments/$assignmentId/availability")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(start, end)),
        ).andReturn()

    private fun createBulk(assignmentId: UUID, days: List<String>, start: String, end: String): MvcResult =
        mvc.perform(
            post("/api/admin/professional-assignments/$assignmentId/availability/batch")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(mapOf("days" to days, "startTime" to start, "endTime" to end))),
        ).andReturn()

    private fun update(id: UUID, start: String, end: String): MvcResult =
        mvc.perform(
            put("/api/admin/professional-availability/$id")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(start, end)),
        ).andReturn()

    private fun changeStatus(id: UUID, action: String): MvcResult =
        mvc.perform(
            patch("/api/admin/professional-availability/$id/$action")
                .header("Authorization", "Bearer $token"),
        ).andReturn()

    private fun scheduleBody(start: String, end: String): String =
        json.writeValueAsString(mapOf("dayOfWeek" to "MONDAY", "startTime" to start, "endTime" to end))

    private fun response(result: MvcResult): ProfessionalAvailabilityResponse =
        json.readValue(result.response.contentAsString, ProfessionalAvailabilityResponse::class.java)

    private fun auditCount(id: UUID): Int =
        jdbc.queryForObject(
            "select count(*) from logs where entity_type = 'professional_availability' and entity_id = ?",
            Int::class.java,
            id.toString(),
        ) ?: 0

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").asText())
    }

    private fun <T> tx(block: () -> T): T = TransactionTemplate(transactions).execute { block() }!!

    private data class Fixture(
        val firstAssignmentId: UUID,
        val secondAssignmentId: UUID,
        val firstServiceId: UUID,
        val secondServiceId: UUID,
    )
}
