package com.uade.dda2.server.feature.activity

import com.uade.dda2.server.feature.activity.dto.response.ActivityEnrollmentResponse
import com.uade.dda2.server.feature.activity.dto.response.CitizenActivityListResponse
import com.uade.dda2.server.feature.activity.dto.response.CitizenActivityResponse
import com.uade.dda2.server.feature.activity.dto.response.ActivityResponse
import com.uade.dda2.server.feature.activity.entity.ActivityAttendanceStatus
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
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

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

    @Test
    fun `recupera la inscripcion propia y no revela la de otro ciudadano`() {
        val id = saveActivity(ActivityStatus.OPEN)
        val enrollment = json.readValue(enroll(id).response.contentAsString, ActivityEnrollmentResponse::class.java)
        val own = json.readValue(performGet("/api/activities/$id").response.contentAsString, CitizenActivityResponse::class.java)
        assertEquals(enrollment.id, own.myEnrollment?.id)
        val other = json.readValue(performGet("/api/activities/$id", secondCitizenToken).response.contentAsString, CitizenActivityResponse::class.java)
        assertNull(other.myEnrollment)
        val ownList = json.readValue(performGet("/api/activities?enrolledOnly=true").response.contentAsString, CitizenActivityListResponse::class.java)
        assertEquals(listOf(id), ownList.content.map { it.id })
        assertEquals(enrollment.id, ownList.content.single().myEnrollment?.id)
        val otherList = json.readValue(performGet("/api/activities?enrolledOnly=true", secondCitizenToken).response.contentAsString, CitizenActivityListResponse::class.java)
        assertEquals(0, otherList.totalElements)
    }

    @Test
    fun `conserva el historial cerrado solo para su titular y pagina sus inscripciones`() {
        val first = saveActivity(ActivityStatus.OPEN)
        val second = saveActivity(ActivityStatus.OPEN)
        expect(enroll(first), 201)
        expect(enroll(second), 201)
        tx { activities.findById(first).orElseThrow().also { it.status = ActivityStatus.CLOSED } }
        val detail = performGet("/api/activities/$first")
        expect(detail, 200)
        assertEquals(ActivityStatus.CLOSED, json.readValue(detail.response.contentAsString, CitizenActivityResponse::class.java).status)
        expect(performGet("/api/activities/$first", secondCitizenToken), 404, "ACTIVITY_NOT_AVAILABLE")
        val page0 = json.readValue(performGet("/api/activities?enrolledOnly=true&page=0&size=1").response.contentAsString, CitizenActivityListResponse::class.java)
        val page1 = json.readValue(performGet("/api/activities?enrolledOnly=true&page=1&size=1").response.contentAsString, CitizenActivityListResponse::class.java)
        assertEquals(2, page0.totalElements)
        assertEquals(setOf(first, second), (page0.content + page1.content).map { it.id }.toSet())
        expect(performGet("/api/activities?enrolledOnly=true&page=-1"), 400)
        expect(performGet("/api/activities?enrolledOnly=true", unauthorizedToken), 403)
        expect(mvc.perform(get("/api/activities?enrolledOnly=true")).andReturn(), 401)
    }

    @Test
    fun `recorre alta edicion publicacion inscripcion asistencia correccion y cierre`() {
        val adminToken = tx {
            val names = listOf("activities:management:create", "activities:management:edit", "activities:management:change-status", "activities:attendance:view", "activities:attendance:manage")
            val granted = names.map { name -> permissions.findByNameIn(listOf(name)).firstOrNull() ?: permissions.save(Permission(name = name)) }.toMutableSet()
            val role = roles.save(Role(name = "GESTOR-${UUID.randomUUID()}", permissions = granted))
            jwt.createToken(users.saveAndFlush(user("Gestor del recorrido", role)), role)
        }
        val body = """{"name":"Taller recorrido","description":"Encuentro comunitario","location":"Centro Norte","startDate":"2026-10-20","endDate":"2026-10-20","capacity":2}"""
        val created = mvc.perform(post("/api/admin/activities").header("Authorization", "Bearer $adminToken").contentType(MediaType.APPLICATION_JSON).content(body)).andReturn()
        expect(created, 201)
        val activity = json.readValue(created.response.contentAsString, ActivityResponse::class.java)
        assertEquals(ActivityStatus.DRAFT, activity.status)
        val edited = mvc.perform(put("/api/admin/activities/${activity.id}").header("Authorization", "Bearer $adminToken").contentType(MediaType.APPLICATION_JSON).content(body.replace("Centro Norte", "Centro Sur"))).andReturn()
        expect(edited, 200)
        assertEquals("Centro Sur", json.readValue(edited.response.contentAsString, ActivityResponse::class.java).location)
        expect(mvc.perform(patch("/api/admin/activities/${activity.id}/publish").header("Authorization", "Bearer $adminToken")).andReturn(), 200)
        val enrolled = enroll(activity.id)
        expect(enrolled, 201)
        val enrollment = json.readValue(enrolled.response.contentAsString, ActivityEnrollmentResponse::class.java)
        expect(performGet("/api/professional/activities/${activity.id}/enrollments", adminToken), 200)
        for (attendance in listOf("PRESENT", "ABSENT")) {
            expect(mvc.perform(patch("/api/professional/activities/${activity.id}/enrollments/${enrollment.id}/attendance")
                .header("Authorization", "Bearer $adminToken").contentType(MediaType.APPLICATION_JSON)
                .content("""{"attendance":"$attendance"}""")).andReturn(), 200)
        }
        expect(mvc.perform(patch("/api/admin/activities/${activity.id}/close").header("Authorization", "Bearer $adminToken")).andReturn(), 200)
        val detail = json.readValue(performGet("/api/activities/${activity.id}").response.contentAsString, CitizenActivityResponse::class.java)
        assertEquals(ActivityStatus.CLOSED, detail.status)
        assertEquals(ActivityAttendanceStatus.ABSENT, detail.myEnrollment?.attendance)
        expect(enroll(activity.id, secondCitizenToken), 409, "ACTIVITY_NOT_OPEN")
        val history = json.readValue(performGet("/api/activities?enrolledOnly=true").response.contentAsString, CitizenActivityListResponse::class.java)
        assertEquals(activity.id, history.content.single().id)
        assertEquals(ActivityAttendanceStatus.ABSENT, history.content.single().myEnrollment?.attendance)
    }

    @Test
    fun `dos inscripciones concurrentes no superan el ultimo cupo`() {
        val activityId = saveActivity(ActivityStatus.OPEN, capacity = 1)
        val start = CountDownLatch(1)
        val attempts = listOf(citizenToken, secondCitizenToken).map { token ->
            CompletableFuture.supplyAsync {
                check(start.await(10, TimeUnit.SECONDS))
                enroll(activityId, token).response.status
            }
        }
        start.countDown()
        assertEquals(listOf(201, 409), attempts.map { it.get(30, TimeUnit.SECONDS) }.sorted())
        assertEquals(1, enrollments.countByActivityId(activityId))
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
