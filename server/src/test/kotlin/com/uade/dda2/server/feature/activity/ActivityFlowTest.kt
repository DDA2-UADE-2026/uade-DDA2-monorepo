package com.uade.dda2.server.feature.activity

import com.uade.dda2.server.feature.activity.dto.response.ActivityListResponse
import com.uade.dda2.server.feature.activity.dto.response.ActivityResponse
import com.uade.dda2.server.feature.activity.entity.Activity
import com.uade.dda2.server.feature.activity.entity.ActivityStatus
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "spring.datasource.url=jdbc:h2:mem:activities;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ActivityFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var activities: ActivityRepository
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var transactions: PlatformTransactionManager

    private lateinit var adminToken: String
    private lateinit var unauthorizedToken: String

    private val requiredPermissions = listOf(
        "activities:management:view",
        "activities:management:create",
        "activities:management:edit",
        "activities:management:change-status",
        "users:delete",
    )

    @BeforeEach
    fun setup() {
        val tokens = tx {
            val granted = requiredPermissions.map { name ->
                permissions.findByNameIn(listOf(name)).firstOrNull() ?: permissions.save(Permission(name = name))
            }.toMutableSet()
            val adminRole = roles.save(Role(name = "ADMIN-${UUID.randomUUID()}", permissions = granted))
            val viewerRole = roles.save(Role(name = "VIEWER-${UUID.randomUUID()}"))
            val admin = users.saveAndFlush(
                User(
                    name = "Activity Admin",
                    email = "activity-admin-${UUID.randomUUID()}@example.com",
                    roles = mutableSetOf(adminRole),
                ),
            )
            val viewer = users.saveAndFlush(
                User(
                    name = "Activity Viewer",
                    email = "activity-viewer-${UUID.randomUUID()}@example.com",
                    roles = mutableSetOf(viewerRole),
                ),
            )
            jwt.createToken(admin, adminRole) to jwt.createToken(viewer, viewerRole)
        }
        adminToken = tokens.first
        unauthorizedToken = tokens.second
    }

    @Test
    fun `crea actividad en borrador con actor y auditoria`() {
        val result = create()
        expect(result, 201)
        val response = response(result)

        assertEquals(ActivityStatus.DRAFT, response.status)
        assertEquals("Taller comunitario de RCP", response.name)
        assertEquals(30, response.capacity)
        assertEquals("Activity Admin", response.createdBy.name)
        assertNotNull(response.createdAt)

        tx {
            val stored = activities.findById(response.id).orElseThrow()
            assertEquals(ActivityStatus.DRAFT, stored.status)
            assertEquals("Centro Municipal Norte", stored.location)
        }

        val log = jdbc.queryForMap(
            "select action, entity_type, entity_id from logs where entity_type = 'activity' and entity_id = ?",
            response.id.toString(),
        )
        assertEquals("CREATE", log["action"])
        assertEquals(response.id.toString(), log["entity_id"])
    }

    @Test
    fun `requiere autenticacion y permisos especificos`() {
        expect(create(token = null), 401, "AUTH_UNAUTHENTICATED")
        expect(create(token = unauthorizedToken), 403, "AUTH_FORBIDDEN")
        expect(create(), 201)
    }

    @Test
    fun `valida campos capacidad y rango de fechas`() {
        for (requiredValue in listOf(
            "Taller comunitario de RCP",
            "Capacitación abierta sobre técnicas básicas de reanimación.",
            "Centro Municipal Norte",
        )) {
            expect(create(body = validBody().replace(requiredValue, "   ")), 400, "VALIDATION_ERROR")
        }
        expect(create(body = """{"name":"Actividad incompleta"}"""), 400)
        expect(create(body = validBody().replace("\"capacity\":30", "\"capacity\":0")), 400, "VALIDATION_ERROR")
        expect(
            create(body = validBody().replace("\"endDate\":\"2026-10-10\"", "\"endDate\":\"2026-10-09\"")),
            400,
            "ACTIVITY_INVALID_DATE_RANGE",
        )
    }

    @Test
    fun `lista consulta y actualiza una actividad en borrador`() {
        val created = response(create())
        val updatedBody = validBody()
            .replace("Taller comunitario de RCP", "Taller actualizado")
            .replace("Centro Municipal Norte", "Centro Municipal Sur")
            .replace("\"capacity\":30", "\"capacity\":40")

        val updatedResult = performPut("/api/admin/activities/${created.id}", updatedBody)
        expect(updatedResult, 200)
        val updated = response(updatedResult)
        assertEquals("Taller actualizado", updated.name)
        assertEquals("Centro Municipal Sur", updated.location)
        assertEquals(40, updated.capacity)

        val detail = mvc.perform(
            get("/api/admin/activities/${created.id}").header("Authorization", "Bearer $adminToken"),
        ).andReturn()
        expect(detail, 200)
        assertEquals("Taller actualizado", response(detail).name)

        val list = mvc.perform(
            get("/api/admin/activities?page=0&size=20").header("Authorization", "Bearer $adminToken"),
        ).andReturn()
        expect(list, 200)
        val page = json.readValue(list.response.contentAsString, ActivityListResponse::class.java)
        assertTrue(page.content.any { it.id == created.id })
    }

    @Test
    fun `publica y cierra siguiendo transiciones unidireccionales`() {
        val created = response(create())

        val published = performPatch("/api/admin/activities/${created.id}/publish")
        expect(published, 200)
        assertEquals(ActivityStatus.OPEN, response(published).status)

        expect(performPatch("/api/admin/activities/${created.id}/publish"), 409, "ACTIVITY_INVALID_STATUS_TRANSITION")
        expect(performPut("/api/admin/activities/${created.id}", validBody()), 409, "ACTIVITY_CANNOT_BE_EDITED")

        val closed = performPatch("/api/admin/activities/${created.id}/close")
        expect(closed, 200)
        assertEquals(ActivityStatus.CLOSED, response(closed).status)
        expect(performPatch("/api/admin/activities/${created.id}/close"), 409, "ACTIVITY_INVALID_STATUS_TRANSITION")
    }

    @Test
    fun `rechaza cierre de borrador y recursos inexistentes`() {
        val created = response(create())
        expect(performPatch("/api/admin/activities/${created.id}/close"), 409, "ACTIVITY_INVALID_STATUS_TRANSITION")

        val missing = UUID.randomUUID()
        val result = mvc.perform(
            get("/api/admin/activities/$missing").header("Authorization", "Bearer $adminToken"),
        ).andReturn()
        expect(result, 404, "ACTIVITY_NOT_FOUND")
    }

    @Test
    fun `valida parametros de paginacion`() {
        val result = mvc.perform(
            get("/api/admin/activities?page=-1&size=101").header("Authorization", "Bearer $adminToken"),
        ).andReturn()
        expect(result, 400, "VALIDATION_ERROR")
    }

    @Test
    fun `impide eliminar al usuario que creo una actividad`() {
        val creatorId = tx {
            val creator = users.saveAndFlush(
                User(
                    name = "Activity Creator",
                    email = "activity-creator-${UUID.randomUUID()}@example.com",
                ),
            )
            activities.saveAndFlush(
                Activity(
                    name = "Actividad vinculada",
                    description = "Actividad que conserva a su creador.",
                    location = "Centro Municipal",
                    startDate = LocalDate.of(2026, 10, 10),
                    endDate = LocalDate.of(2026, 10, 10),
                    capacity = 10,
                    createdBy = creator,
                ),
            )
            requireNotNull(creator.id)
        }

        val result = mvc.perform(
            delete("/users/$creatorId").header("Authorization", "Bearer $adminToken"),
        ).andReturn()
        expect(result, 409, "USER_HAS_ACTIVITY_REFERENCES")
    }

    private fun create(
        body: String = validBody(),
        token: String? = adminToken,
    ): MvcResult {
        val request = post("/api/admin/activities")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
        token?.let { request.header("Authorization", "Bearer $it") }
        return mvc.perform(request).andReturn()
    }

    private fun performPut(path: String, body: String): MvcResult =
        mvc.perform(
            put(path)
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        ).andReturn()

    private fun performPatch(path: String): MvcResult =
        mvc.perform(patch(path).header("Authorization", "Bearer $adminToken")).andReturn()

    private fun response(result: MvcResult): ActivityResponse =
        json.readValue(result.response.contentAsString, ActivityResponse::class.java)

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) {
            assertEquals(code, json.readTree(result.response.contentAsString).get("code").stringValue())
        }
    }

    private fun validBody(): String =
        """{
          "name":"Taller comunitario de RCP",
          "description":"Capacitación abierta sobre técnicas básicas de reanimación.",
          "location":"Centro Municipal Norte",
          "startDate":"2026-10-10",
          "endDate":"2026-10-10",
          "capacity":30
        }""".trimIndent()

    private fun <T : Any> tx(action: () -> T): T =
        TransactionTemplate(transactions).execute { action() }
}
