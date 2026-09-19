package com.uade.dda2.server.feature.center

import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.center.dto.response.ProfessionalAssignmentResponse
import com.uade.dda2.server.feature.center.entity.CenterService
import com.uade.dda2.server.feature.center.entity.MunicipalCenter
import com.uade.dda2.server.feature.center.entity.MunicipalService
import com.uade.dda2.server.feature.center.repository.CenterServiceRepository
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
        "spring.datasource.url=jdbc:h2:mem:professional-assignments;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ProfessionalAssignmentFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var centers: MunicipalCenterRepository
    @Autowired lateinit var services: MunicipalServiceRepository
    @Autowired lateinit var centerServices: CenterServiceRepository
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var transactions: PlatformTransactionManager

    private lateinit var token: String
    private lateinit var professionalRole: Role

    @BeforeEach
    fun setup() {
        token = tx {
            val granted = listOf("centers:management:view", "centers:management:edit").map { name ->
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
                    name = "Assignment Admin",
                    email = "assignment-admin-${UUID.randomUUID()}@example.com",
                    roles = mutableSetOf(adminRole),
                ),
            )
            jwt.createToken(admin, adminRole)
        }
    }

    @Test
    fun `asigna lista desactiva y reactiva profesional en la misma fila`() {
        val fixture = fixture()
        val professional = professional(active = true, withRole = true)
        val created = response(assign(fixture.centerServiceId, professional.id!!).also { expect(it, 201) })
        assertTrue(created.active)
        assertEquals(professional.id, created.professionalId)
        assertEquals(fixture.serviceId, created.serviceId)

        expect(assign(fixture.centerServiceId, professional.id!!), 409, "PROFESSIONAL_ASSIGNMENT_ALREADY_ACTIVE")

        val listResult = mvc.perform(
            get("/api/admin/center-services/${fixture.centerServiceId}/professionals")
                .header("Authorization", "Bearer $token"),
        ).andReturn()
        expect(listResult, 200)
        assertTrue(listResult.response.contentAsString.contains(professional.email))

        val inactive = response(changeStatus(created.id, "deactivate").also { expect(it, 200) })
        assertFalse(inactive.active)
        val reactivated = response(assign(fixture.centerServiceId, professional.id!!).also { expect(it, 201) })
        assertEquals(created.id, reactivated.id)
        assertTrue(reactivated.active)
        assertEquals(3, auditCount(created.id))
    }

    @Test
    fun `rechaza usuarios inactivos o sin rol profesional`() {
        val fixture = fixture()
        val inactive = professional(active = false, withRole = true)
        expect(assign(fixture.centerServiceId, inactive.id!!), 409, "PROFESSIONAL_NOT_ELIGIBLE")

        val withoutRole = professional(active = true, withRole = false)
        expect(assign(fixture.centerServiceId, withoutRole.id!!), 409, "PROFESSIONAL_NOT_ELIGIBLE")
        expect(assign(fixture.centerServiceId, Long.MAX_VALUE), 404, "PROFESSIONAL_NOT_FOUND")
    }

    @Test
    fun `rechaza asignar cuando alguna dependencia esta inactiva`() {
        val fixture = fixture()
        val professional = professional(active = true, withRole = true)
        tx {
            val relation = centerServices.findById(fixture.centerServiceId).orElseThrow()
            relation.active = false
            centerServices.saveAndFlush(relation)
        }
        expect(assign(fixture.centerServiceId, professional.id!!), 409, "CENTER_DEPENDENCY_INACTIVE")
    }

    private fun fixture(): Fixture = tx {
        val suffix = UUID.randomUUID().toString().take(8)
        val center = centers.saveAndFlush(MunicipalCenter(name = "Centro $suffix", address = "Calle 123"))
        val service = services.saveAndFlush(
            MunicipalService(name = "Servicio $suffix", description = "Descripción", durationMinutes = 30),
        )
        val relation = centerServices.saveAndFlush(CenterService(center = center, service = service))
        Fixture(requireNotNull(relation.id), requireNotNull(service.id))
    }

    private fun professional(active: Boolean, withRole: Boolean): User = tx {
        users.saveAndFlush(
            User(
                name = "Profesional ${UUID.randomUUID().toString().take(8)}",
                email = "professional-${UUID.randomUUID()}@example.com",
                active = active,
                roles = if (withRole) mutableSetOf(professionalRole) else mutableSetOf(),
            ),
        )
    }

    private fun assign(centerServiceId: UUID, professionalId: Long): MvcResult =
        mvc.perform(
            post("/api/admin/center-services/$centerServiceId/professionals")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(mapOf("professionalId" to professionalId))),
        ).andReturn()

    private fun changeStatus(id: UUID, action: String): MvcResult =
        mvc.perform(
            patch("/api/admin/professional-assignments/$id/$action")
                .header("Authorization", "Bearer $token"),
        ).andReturn()

    private fun response(result: MvcResult): ProfessionalAssignmentResponse =
        json.readValue(result.response.contentAsString, ProfessionalAssignmentResponse::class.java)

    private fun auditCount(id: UUID): Int =
        jdbc.queryForObject(
            "select count(*) from logs where entity_type = 'professional_assignment' and entity_id = ?",
            Int::class.java,
            id.toString(),
        ) ?: 0

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").asText())
    }

    private fun <T> tx(block: () -> T): T = TransactionTemplate(transactions).execute { block() }!!

    private data class Fixture(val centerServiceId: UUID, val serviceId: UUID)
}
