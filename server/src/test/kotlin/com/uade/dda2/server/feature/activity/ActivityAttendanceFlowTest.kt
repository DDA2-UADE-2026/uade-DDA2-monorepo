package com.uade.dda2.server.feature.activity

import com.uade.dda2.server.feature.activity.dto.response.ProfessionalActivityEnrollmentListResponse
import com.uade.dda2.server.feature.activity.dto.response.ProfessionalActivityEnrollmentResponse
import com.uade.dda2.server.feature.activity.dto.response.ProfessionalActivityListResponse
import com.uade.dda2.server.feature.activity.entity.Activity
import com.uade.dda2.server.feature.activity.entity.ActivityAttendanceStatus
import com.uade.dda2.server.feature.activity.entity.ActivityEnrollment
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
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "spring.datasource.url=jdbc:h2:mem:activity-attendance;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ActivityAttendanceFlowTest {
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

    private lateinit var professionalToken: String
    private lateinit var unauthorizedToken: String
    private lateinit var creator: User
    private lateinit var professional: User

    @BeforeEach
    fun setup() {
        val tokens = tx {
            val attendancePermissions = listOf(
                "activities:attendance:view",
                "activities:attendance:manage",
            ).map { name ->
                permissions.findByNameIn(listOf(name)).firstOrNull() ?: permissions.save(Permission(name = name))
            }.toMutableSet()
            val professionalRole = roles.save(
                Role(
                    name = "PROFESIONAL_CENTRO-${UUID.randomUUID().toString().take(8)}",
                    permissions = attendancePermissions,
                ),
            )
            val viewerRole = roles.save(Role(name = "VIEWER-${UUID.randomUUID()}"))
            creator = users.saveAndFlush(user("Activity Creator"))
            professional = users.saveAndFlush(user("Professional One", professionalRole))
            val viewer = users.saveAndFlush(user("Unauthorized Viewer", viewerRole))
            jwt.createToken(professional, professionalRole) to jwt.createToken(viewer, viewerRole)
        }
        professionalToken = tokens.first
        unauthorizedToken = tokens.second
    }

    @Test
    fun `lista actividades publicadas y cerradas pero no borradores`() {
        val open = saveActivity(ActivityStatus.OPEN)
        val closed = saveActivity(ActivityStatus.CLOSED)
        val draft = saveActivity(ActivityStatus.DRAFT)

        val result = performGet("/api/professional/activities?page=0&size=100")
        expect(result, 200)
        val page = json.readValue(result.response.contentAsString, ProfessionalActivityListResponse::class.java)
        val ids = page.content.map { it.id }
        assertTrue(open in ids)
        assertTrue(closed in ids)
        assertTrue(draft !in ids)
    }

    @Test
    fun `muestra inscriptos y registra presente con profesional fecha y auditoria`() {
        val activityId = saveActivity(ActivityStatus.OPEN)
        val enrollmentId = saveEnrollment(activityId, "Citizen Present")

        val listResult = performGet("/api/professional/activities/$activityId/enrollments")
        expect(listResult, 200)
        val page = json.readValue(
            listResult.response.contentAsString,
            ProfessionalActivityEnrollmentListResponse::class.java,
        )
        val pending = page.content.single { it.id == enrollmentId }
        assertEquals("Citizen Present", pending.citizenName)
        assertNull(pending.attendance)
        assertNull(pending.attendanceRecordedBy)

        val updateResult = updateAttendance(activityId, enrollmentId, "PRESENT")
        expect(updateResult, 200)
        val updated = json.readValue(
            updateResult.response.contentAsString,
            ProfessionalActivityEnrollmentResponse::class.java,
        )
        assertEquals(ActivityAttendanceStatus.PRESENT, updated.attendance)
        assertEquals(professional.id, updated.attendanceRecordedBy?.id)
        assertNotNull(updated.attendanceRecordedAt)

        tx {
            val stored = enrollments.findById(enrollmentId).orElseThrow()
            assertEquals(ActivityAttendanceStatus.PRESENT, stored.attendance)
            assertEquals(professional.id, stored.attendanceRecordedBy?.id)
            assertNotNull(stored.attendanceRecordedAt)
        }

        val log = jdbc.queryForMap(
            """
            select action, old_values, new_values from logs
            where entity_type = 'activity_enrollment' and entity_id = ? and action = 'UPDATE'
            """.trimIndent(),
            enrollmentId.toString(),
        )
        assertEquals("UPDATE", log["action"])
        assertTrue(databaseText(log["old_values"]).contains("attendance"))
        assertTrue(databaseText(log["new_values"]).contains("PRESENT"))
    }

    @Test
    fun `permite corregir una asistencia y conserva el ultimo valor`() {
        val activityId = saveActivity(ActivityStatus.CLOSED)
        val enrollmentId = saveEnrollment(activityId, "Citizen Correction")

        expect(updateAttendance(activityId, enrollmentId, "PRESENT"), 200)
        val corrected = updateAttendance(activityId, enrollmentId, "ABSENT")
        expect(corrected, 200)
        assertEquals(
            ActivityAttendanceStatus.ABSENT,
            json.readValue(corrected.response.contentAsString, ProfessionalActivityEnrollmentResponse::class.java).attendance,
        )
    }

    @Test
    fun `rechaza borradores inscripciones ajenas y valores invalidos`() {
        val draftId = saveActivity(ActivityStatus.DRAFT)
        val draftEnrollment = saveEnrollment(draftId, "Citizen Draft")
        expect(
            updateAttendance(draftId, draftEnrollment, "PRESENT"),
            409,
            "ACTIVITY_ATTENDANCE_NOT_ALLOWED",
        )

        val firstActivity = saveActivity(ActivityStatus.OPEN)
        val secondActivity = saveActivity(ActivityStatus.OPEN)
        val enrollmentId = saveEnrollment(firstActivity, "Citizen Other Activity")
        expect(
            updateAttendance(secondActivity, enrollmentId, "PRESENT"),
            404,
            "ACTIVITY_ENROLLMENT_NOT_FOUND",
        )
        expect(updateAttendance(firstActivity, enrollmentId, "UNKNOWN"), 400, "INVALID_REQUEST_BODY")
    }

    @Test
    fun `requiere autenticacion y permisos profesionales`() {
        val activityId = saveActivity(ActivityStatus.OPEN)
        val enrollmentId = saveEnrollment(activityId, "Citizen Secured")

        expect(performGet("/api/professional/activities", token = null), 401, "AUTH_UNAUTHENTICATED")
        expect(performGet("/api/professional/activities", unauthorizedToken), 403, "AUTH_FORBIDDEN")
        expect(updateAttendance(activityId, enrollmentId, "PRESENT", unauthorizedToken), 403, "AUTH_FORBIDDEN")
    }

    private fun saveActivity(status: ActivityStatus): UUID = tx {
        requireNotNull(
            activities.saveAndFlush(
                Activity(
                    name = "Activity ${UUID.randomUUID()}",
                    description = "Community activity for attendance tests.",
                    location = "Centro Municipal",
                    startDate = LocalDate.of(2026, 10, 10),
                    endDate = LocalDate.of(2026, 10, 10),
                    capacity = 20,
                    status = status,
                    createdBy = creator,
                ),
            ).id,
        )
    }

    private fun saveEnrollment(activityId: UUID, citizenName: String): UUID = tx {
        val citizen = users.saveAndFlush(user(citizenName))
        requireNotNull(
            enrollments.saveAndFlush(
                ActivityEnrollment(
                    activity = activities.getReferenceById(activityId),
                    citizen = citizen,
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

    private fun performGet(path: String, token: String? = professionalToken): MvcResult {
        val request = get(path)
        token?.let { request.header("Authorization", "Bearer $it") }
        return mvc.perform(request).andReturn()
    }

    private fun updateAttendance(
        activityId: UUID,
        enrollmentId: UUID,
        attendance: String,
        token: String? = professionalToken,
    ): MvcResult {
        val request = patch("/api/professional/activities/$activityId/enrollments/$enrollmentId/attendance")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"attendance":"$attendance"}""")
        token?.let { request.header("Authorization", "Bearer $it") }
        return mvc.perform(request).andReturn()
    }

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) {
            assertEquals(code, json.readTree(result.response.contentAsString).get("code").stringValue())
        }
    }

    private fun databaseText(value: Any?): String =
        when (value) {
            is ByteArray -> value.toString(Charsets.UTF_8)
            else -> value.toString()
        }

    private fun <T : Any> tx(action: () -> T): T =
        TransactionTemplate(transactions).execute { action() }
}
