package com.uade.dda2.server.feature.center

import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.center.dto.response.CenterServiceResponse
import com.uade.dda2.server.feature.center.entity.MunicipalCenter
import com.uade.dda2.server.feature.center.entity.MunicipalService
import com.uade.dda2.server.feature.center.repository.MunicipalCenterRepository
import com.uade.dda2.server.feature.center.repository.MunicipalServiceRepository
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
        "spring.datasource.url=jdbc:h2:mem:center-services;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class CenterServiceFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var centers: MunicipalCenterRepository
    @Autowired lateinit var services: MunicipalServiceRepository
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var transactions: PlatformTransactionManager

    private lateinit var token: String

    @BeforeEach
    fun setup() {
        token = tx {
            val granted = listOf("centers:management:view", "centers:management:edit").map { name ->
                permissions.findByNameIn(listOf(name)).firstOrNull() ?: permissions.save(Permission(name = name))
            }.toMutableSet()
            val role = roles.findByNameIn(listOf("ADMIN")).firstOrNull()
                ?.also { it.permissions.addAll(granted) }
                ?: Role(name = "ADMIN", permissions = granted)
            roles.saveAndFlush(role)
            val user = users.saveAndFlush(
                User(
                    name = "Center Service Admin",
                    email = "center-service-${UUID.randomUUID()}@example.com",
                    roles = mutableSetOf(role),
                ),
            )
            jwt.createToken(user, role)
        }
    }

    @Test
    fun `asigna lista desactiva y reactiva el mismo servicio del centro`() {
        val fixtures = fixtures()
        val created = response(assign(fixtures.centerId, fixtures.serviceId).also { expect(it, 201) })
        assertTrue(created.active)
        assertEquals(fixtures.centerId, created.centerId)
        assertEquals(fixtures.serviceId, created.service.id)

        expect(assign(fixtures.centerId, fixtures.serviceId), 409, "CENTER_SERVICE_ALREADY_ACTIVE")

        val listResult = mvc.perform(
            get("/api/admin/municipal-centers/${fixtures.centerId}/services")
                .header("Authorization", "Bearer $token"),
        ).andReturn()
        expect(listResult, 200)
        val listed = json.readValue(
            listResult.response.contentAsString,
            json.typeFactory.constructCollectionType(List::class.java, CenterServiceResponse::class.java),
        ) as List<CenterServiceResponse>
        assertEquals(listOf(created.id), listed.map { it.id })

        val inactive = response(changeStatus(created.id, "deactivate").also { expect(it, 200) })
        assertFalse(inactive.active)
        expect(changeStatus(created.id, "deactivate"), 409, "CENTER_SERVICE_ALREADY_INACTIVE")

        val reactivated = response(assign(fixtures.centerId, fixtures.serviceId).also { expect(it, 201) })
        assertTrue(reactivated.active)
        assertEquals(created.id, reactivated.id)
        assertEquals(3, auditCount(created.id))
    }

    @Test
    fun `rechaza asignar o reactivar con antecedentes inactivos`() {
        val fixtures = fixtures()
        tx {
            val service = services.findById(fixtures.serviceId).orElseThrow()
            service.active = false
            services.saveAndFlush(service)
        }
        expect(assign(fixtures.centerId, fixtures.serviceId), 409, "CENTER_DEPENDENCY_INACTIVE")

        tx {
            val service = services.findById(fixtures.serviceId).orElseThrow()
            service.active = true
            services.saveAndFlush(service)
        }
        val relation = response(assign(fixtures.centerId, fixtures.serviceId))
        expect(changeStatus(relation.id, "deactivate"), 200)
        tx {
            val center = centers.findById(fixtures.centerId).orElseThrow()
            center.active = false
            centers.saveAndFlush(center)
        }
        expect(changeStatus(relation.id, "activate"), 409, "CENTER_DEPENDENCY_INACTIVE")
    }

    @Test
    fun `requiere permisos y recursos existentes`() {
        val fixtures = fixtures()
        val noToken = post("/api/admin/municipal-centers/${fixtures.centerId}/services")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(mapOf("serviceId" to fixtures.serviceId)))
        expect(mvc.perform(noToken).andReturn(), 401)

        expect(assign(UUID.randomUUID(), fixtures.serviceId), 404, "MUNICIPAL_CENTER_NOT_FOUND")
        expect(assign(fixtures.centerId, UUID.randomUUID()), 404, "MUNICIPAL_SERVICE_NOT_FOUND")
    }

    private fun fixtures(): Fixtures = tx {
        val suffix = UUID.randomUUID().toString().take(8)
        val center = centers.saveAndFlush(MunicipalCenter(name = "Centro $suffix", address = "Calle 123"))
        val service = services.saveAndFlush(
            MunicipalService(name = "Servicio $suffix", description = "Descripción", durationMinutes = 30),
        )
        Fixtures(requireNotNull(center.id), requireNotNull(service.id))
    }

    private fun assign(centerId: UUID, serviceId: UUID): MvcResult =
        mvc.perform(
            post("/api/admin/municipal-centers/$centerId/services")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(mapOf("serviceId" to serviceId))),
        ).andReturn()

    private fun changeStatus(id: UUID, action: String): MvcResult =
        mvc.perform(
            patch("/api/admin/center-services/$id/$action").header("Authorization", "Bearer $token"),
        ).andReturn()

    private fun response(result: MvcResult): CenterServiceResponse =
        json.readValue(result.response.contentAsString, CenterServiceResponse::class.java)

    private fun auditCount(id: UUID): Int =
        jdbc.queryForObject(
            "select count(*) from logs where entity_type = 'center_service' and entity_id = ?",
            Int::class.java,
            id.toString(),
        ) ?: 0

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").asText())
    }

    private fun <T> tx(block: () -> T): T = TransactionTemplate(transactions).execute { block() }!!

    private data class Fixtures(val centerId: UUID, val serviceId: UUID)
}
