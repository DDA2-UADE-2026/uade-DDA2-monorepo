package com.uade.dda2.server.feature.activity

import com.uade.dda2.server.feature.activity.dto.response.ActivityEnrollmentResponse
import com.uade.dda2.server.feature.activity.dto.response.CitizenActivityListResponse
import com.uade.dda2.server.feature.activity.entity.Activity
import com.uade.dda2.server.feature.activity.entity.ActivityStatus
import com.uade.dda2.server.feature.activity.repository.ActivityEnrollmentRepository
import com.uade.dda2.server.feature.activity.repository.ActivityRepository
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.security.JwtService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "spring.datasource.url=jdbc:h2:mem:activity-enrollments;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ActivityEnrollmentFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var activities: ActivityRepository
    @Autowired lateinit var enrollments: ActivityEnrollmentRepository
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var transactions: PlatformTransactionManager

    private lateinit var citizenToken: String
    private lateinit var secondCitizenToken: String
    private lateinit var unauthorizedToken: String
    private lateinit var creator: User

    @BeforeEach
    fun setup() {
        val tokens = tx {
            val ownPermissions = listOf("activities:own:view", "activities:own:enroll").map { name ->
                permissions.findByNameIn(listOf(name)).firstOrNull() ?: permissions.save(Permission(name = name))
            }.toMutableSet()
            val citizenRole = roles.save(Role(name = "CIUDADANO-${UUID.randomUUID()}", permissions = ownPermissions))
            val viewerRole = roles.save(Role(name = "VIEWER-${UUID.randomUUID()}"))
            creator = users.saveAndFlush(user("Activity Creator"))
            val citizen = users.saveAndFlush(user("Citizen One", citizenRole))
            val secondCitizen = users.saveAndFlush(user("Citizen Two", citizenRole))
            val viewer = users.saveAndFlush(user("Unauthorized Viewer", viewerRole))
            Triple(
                jwt.createToken(citizen, citizenRole),
                jwt.createToken(secondCitizen, citizenRole),
                jwt.createToken(viewer, viewerRole),
            )
        }
        citizenToken = tokens.first
        secondCitizenToken = tokens.second
        unauthorizedToken = tokens.third
    }

    @Test
    fun `lista y consulta solamente actividades abiertas con cupos disponibles`() {
        val open = saveActivity(ActivityStatus.OPEN, capacity = 3)
        val draft = saveActivity(ActivityStatus.DRAFT)
        val closed = saveActivity(ActivityStatus.CLOSED)

        val listResult = performGet("/api/activities?page=0&size=20")
        expect(listResult, 200)
        val page = json.readValue(listResult.response.contentAsString, CitizenActivityListResponse::class.java)
        val listedIds = page.content.map { it.id }
        assertTrue(open in listedIds)
        assertTrue(draft !in listedIds)
        assertTrue(closed !in listedIds)
        val listedOpen = page.content.single { it.id == open }
        assertEquals(0, listedOpen.enrolledCount)
        assertEquals(3, listedOpen.availableCapacity)

        expect(performGet("/api/activities/$open"), 200)
        expect(performGet("/api/activities/${saveActivity(ActivityStatus.DRAFT)}"), 404, "ACTIVITY_NOT_AVAILABLE")
    }

    @Test
    fun `confirma la inscripcion y rechaza duplicados`() {
        val activityId = saveActivity(ActivityStatus.OPEN)

        val result = enroll(activityId)
        expect(result, 201)
        val response = json.readValue(result.response.contentAsString, ActivityEnrollmentResponse::class.java)
        assertEquals(activityId, response.activityId)
        assertEquals("CONFIRMED", response.confirmation)
        assertEquals("/api/activities/$activityId/enrollments/${response.id}", result.response.getHeader("Location"))
        assertTrue(enrollments.existsByActivityIdAndCitizenId(activityId, response.citizenId))

        val log = jdbc.queryForMap(
            "select action, entity_type from logs where entity_type = 'activity_enrollment' and entity_id = ?",
            response.id.toString(),
        )
        assertEquals("CREATE", log["action"])
        expect(enroll(activityId), 409, "ACTIVITY_ALREADY_ENROLLED")
    }

    @Test
    fun `exige actividad abierta y cupo disponible`() {
        expect(enroll(saveActivity(ActivityStatus.DRAFT)), 409, "ACTIVITY_NOT_OPEN")
        expect(enroll(saveActivity(ActivityStatus.CLOSED)), 409, "ACTIVITY_NOT_OPEN")

        val activityId = saveActivity(ActivityStatus.OPEN, capacity = 1)
        expect(enroll(activityId), 201)
        expect(enroll(activityId, secondCitizenToken), 409, "ACTIVITY_CAPACITY_FULL")
    }

    @Test
    fun `requiere autenticacion y permisos ciudadanos`() {
        val activityId = saveActivity(ActivityStatus.OPEN)
        expect(enroll(activityId, token = null), 401, "AUTH_UNAUTHENTICATED")
        expect(enroll(activityId, unauthorizedToken), 403, "AUTH_FORBIDDEN")
        expect(performGet("/api/activities", unauthorizedToken), 403, "AUTH_FORBIDDEN")
    }

    private fun saveActivity(status: ActivityStatus, capacity: Int = 10): UUID = tx {
        requireNotNull(
            activities.saveAndFlush(
                Activity(
                    name = "Actividad ${UUID.randomUUID()}",
                    description = "Actividad comunitaria de prueba.",
                    location = "Centro Municipal",
                    startDate = LocalDate.of(2026, 10, 10),
                    endDate = LocalDate.of(2026, 10, 10),
                    capacity = capacity,
                    status = status,
                    createdBy = creator,
                ),
            ).id,
        )
    }

    private fun user(name: String, role: Role? = null): User =
        User(
            name = name,
            email = "${UUID.randomUUID()}@example.com",
            roles = role?.let { mutableSetOf(it) } ?: mutableSetOf(),
        )

    private fun performGet(path: String, token: String = citizenToken): MvcResult =
        mvc.perform(get(path).header("Authorization", "Bearer $token")).andReturn()

    private fun enroll(activityId: UUID, token: String? = citizenToken): MvcResult {
        val request = post("/api/activities/$activityId/enrollments")
        token?.let { request.header("Authorization", "Bearer $it") }
        return mvc.perform(request).andReturn()
    }

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) {
            assertEquals(code, json.readTree(result.response.contentAsString).get("code").stringValue())
        }
    }

    private fun <T : Any> tx(action: () -> T): T =
        TransactionTemplate(transactions).execute { action() }
}
