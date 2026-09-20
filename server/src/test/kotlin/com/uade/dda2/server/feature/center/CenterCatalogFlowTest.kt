package com.uade.dda2.server.feature.center

import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.center.dto.response.MunicipalCenterListResponse
import com.uade.dda2.server.feature.center.dto.response.MunicipalCenterResponse
import com.uade.dda2.server.feature.center.dto.response.MunicipalServiceListResponse
import com.uade.dda2.server.feature.center.dto.response.MunicipalServiceResponse
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
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "spring.datasource.url=jdbc:h2:mem:center-catalog;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class CenterCatalogFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var transactions: PlatformTransactionManager

    private lateinit var adminToken: String
    private lateinit var wrongRoleToken: String

    private val requiredPermissions = listOf(
        "centers:management:view",
        "centers:management:create",
        "centers:management:edit",
        "centers:management:change-status",
        "services:management:view",
        "services:management:create",
        "services:management:edit",
        "services:management:change-status",
    )

    @BeforeEach
    fun setup() {
        val tokens = tx {
            val granted = requiredPermissions.map { name ->
                permissions.findByNameIn(listOf(name)).firstOrNull() ?: permissions.save(Permission(name = name))
            }.toMutableSet()
            val adminRole = roles.findByNameIn(listOf("ADMIN")).firstOrNull()
                ?.also { it.permissions.addAll(granted) }
                ?: Role(name = "ADMIN", permissions = granted)
            roles.saveAndFlush(adminRole)
            val wrongRole = roles.saveAndFlush(Role(name = "CENTER-OTHER-${UUID.randomUUID()}", permissions = granted))
            val admin = users.saveAndFlush(
                User(
                    name = "Center Admin",
                    email = "center-admin-${UUID.randomUUID()}@example.com",
                    roles = mutableSetOf(adminRole),
                ),
            )
            val other = users.saveAndFlush(
                User(
                    name = "Center Other",
                    email = "center-other-${UUID.randomUUID()}@example.com",
                    roles = mutableSetOf(wrongRole),
                ),
            )
            jwt.createToken(admin, adminRole) to jwt.createToken(other, wrongRole)
        }
        adminToken = tokens.first
        wrongRoleToken = tokens.second
    }

    @Test
    fun `administra centros con busqueda estados y auditoria`() {
        val suffix = UUID.randomUUID().toString().take(8)
        val created = centerResponse(
            performPost(
                "/api/admin/municipal-centers",
                centerBody("Centro Norte $suffix"),
            ).also { expect(it, 201) },
        )
        assertTrue(created.active)
        assertEquals("Calle 123", created.address)

        val updated = centerResponse(
            performPut(
                "/api/admin/municipal-centers/${created.id}",
                centerBody("Centro Comunitario $suffix", email = "norte@example.com"),
            ).also { expect(it, 200) },
        )
        assertEquals("Centro Comunitario $suffix", updated.name)
        assertEquals("norte@example.com", updated.email)

        val inactive = centerResponse(performPatch("/api/admin/municipal-centers/${created.id}/deactivate"))
        assertFalse(inactive.active)
        expect(performPatch("/api/admin/municipal-centers/${created.id}/deactivate"), 409, "MUNICIPAL_CENTER_ALREADY_INACTIVE")

        val listResult = mvc.perform(
            get("/api/admin/municipal-centers")
                .param("search", "comunitario $suffix")
                .param("active", "false")
                .header("Authorization", "Bearer $adminToken"),
        ).andReturn()
        expect(listResult, 200)
        val page = json.readValue(listResult.response.contentAsString, MunicipalCenterListResponse::class.java)
        assertEquals(listOf(created.id), page.content.map { it.id })

        val active = centerResponse(performPatch("/api/admin/municipal-centers/${created.id}/activate"))
        assertTrue(active.active)
        assertEquals(4, auditCount("municipal_center", created.id))
    }

    @Test
    fun `administra catalogo central de servicios`() {
        val suffix = UUID.randomUUID().toString().take(8)
        val created = serviceResponse(
            performPost(
                "/api/admin/municipal-services",
                serviceBody("Orientación Familiar $suffix", 45),
            ).also { expect(it, 201) },
        )
        assertEquals(45, created.durationMinutes)

        val updated = serviceResponse(
            performPut(
                "/api/admin/municipal-services/${created.id}",
                serviceBody("Orientación Integral $suffix", 60),
            ).also { expect(it, 200) },
        )
        assertEquals(60, updated.durationMinutes)

        val inactive = serviceResponse(performPatch("/api/admin/municipal-services/${created.id}/deactivate"))
        assertFalse(inactive.active)

        val listResult = mvc.perform(
            get("/api/admin/municipal-services")
                .param("search", "integral $suffix")
                .param("active", "false")
                .header("Authorization", "Bearer $adminToken"),
        ).andReturn()
        expect(listResult, 200)
        val page = json.readValue(listResult.response.contentAsString, MunicipalServiceListResponse::class.java)
        assertEquals(listOf(created.id), page.content.map { it.id })

        expect(performPatch("/api/admin/municipal-services/${created.id}/activate"), 200)
        assertEquals(4, auditCount("municipal_service", created.id))
    }

    @Test
    fun `rechaza nombres duplicados y datos invalidos`() {
        val suffix = UUID.randomUUID().toString().take(8)
        expect(performPost("/api/admin/municipal-centers", centerBody("Centro Único $suffix")), 201)
        expect(
            performPost("/api/admin/municipal-centers", centerBody("  CENTRO   ÚNICO $suffix  ")),
            409,
            "MUNICIPAL_CENTER_NAME_ALREADY_EXISTS",
        )

        expect(performPost("/api/admin/municipal-services", serviceBody("Servicio $suffix", 30)), 201)
        expect(
            performPost("/api/admin/municipal-services", serviceBody("servicio $suffix", 30)),
            409,
            "MUNICIPAL_SERVICE_NAME_ALREADY_EXISTS",
        )
        expect(
            performPost("/api/admin/municipal-services", serviceBody("Inválido $suffix", 0)),
            400,
            "VALIDATION_ERROR",
        )
        expect(
            performPost("/api/admin/municipal-centers", centerBody("Correo $suffix", email = "incorrecto")),
            400,
            "VALIDATION_ERROR",
        )
    }

    @Test
    fun `requiere rol admin ademas del permiso`() {
        expect(performPost("/api/admin/municipal-centers", centerBody("Sin token"), token = null), 401)
        expect(performPost("/api/admin/municipal-centers", centerBody("Otro rol"), token = wrongRoleToken), 403)
        expect(performPost("/api/admin/municipal-centers", centerBody("Con admin")), 201)
    }

    @Test
    fun `informa recursos inexistentes y valida paginacion`() {
        val missing = UUID.randomUUID()
        val result = mvc.perform(
            get("/api/admin/municipal-centers/$missing").header("Authorization", "Bearer $adminToken"),
        ).andReturn()
        expect(result, 404, "MUNICIPAL_CENTER_NOT_FOUND")

        val invalidPage = mvc.perform(
            get("/api/admin/municipal-services")
                .param("page", "-1")
                .param("size", "101")
                .header("Authorization", "Bearer $adminToken"),
        ).andReturn()
        expect(invalidPage, 400, "VALIDATION_ERROR")
    }

    private fun centerBody(name: String, email: String? = null): String =
        json.writeValueAsString(
            mapOf(
                "name" to name,
                "address" to "  Calle 123  ",
                "phone" to "+54 11 1234 5678",
                "email" to email,
            ),
        )

    private fun serviceBody(name: String, durationMinutes: Int): String =
        json.writeValueAsString(
            mapOf(
                "name" to name,
                "description" to "Servicio municipal de atención comunitaria.",
                "durationMinutes" to durationMinutes,
            ),
        )

    private fun performPost(path: String, body: String, token: String? = adminToken): MvcResult {
        val request = post(path).contentType(MediaType.APPLICATION_JSON).content(body)
        if (token != null) request.header("Authorization", "Bearer $token")
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

    private fun centerResponse(result: MvcResult): MunicipalCenterResponse =
        json.readValue(result.response.contentAsString, MunicipalCenterResponse::class.java)

    private fun serviceResponse(result: MvcResult): MunicipalServiceResponse =
        json.readValue(result.response.contentAsString, MunicipalServiceResponse::class.java)

    private fun auditCount(entityType: String, entityId: UUID): Int =
        jdbc.queryForObject(
            "select count(*) from logs where entity_type = ? and entity_id = ?",
            Int::class.java,
            entityType,
            entityId.toString(),
        ) ?: 0

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) {
            assertEquals(code, json.readTree(result.response.contentAsString).get("code").asText())
        }
    }

    private fun <T> tx(block: () -> T): T = TransactionTemplate(transactions).execute { block() }!!
}
