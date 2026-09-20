package com.uade.dda2.server.feature.center

import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
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
import com.uade.dda2.server.security.JwtService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete as deleteRequest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch as patchRequest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@SpringBootTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:center-lifecycle;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class CenterLifecycleFlowTest {
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
    @Autowired lateinit var availabilities: ProfessionalAvailabilityRepository
    @Autowired lateinit var openingHours: CenterOpeningHourRepository
    @Autowired lateinit var transactions: PlatformTransactionManager

    private lateinit var token: String
    private lateinit var professionalRole: Role

    @BeforeEach
    fun setup() {
        token = tx {
            val granted = listOf(
                "centers:management:edit",
                "centers:management:change-status",
                "services:management:change-status",
                "users:edit",
                "users:delete",
                "roles:edit",
                "roles:delete",
            ).map { name ->
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
                    name = "Lifecycle Admin",
                    email = "lifecycle-admin-${UUID.randomUUID()}@example.com",
                    roles = mutableSetOf(adminRole),
                ),
            )
            jwt.createToken(admin, adminRole)
        }
    }

    @Test
    fun `revalida solapamientos al activar cada dependencia`() {
        ActivationTarget.entries.forEach { target ->
            val fixture = fixtureWithLatentOverlap(target)
            expect(activate(target, fixture), 409, "PROFESSIONAL_AVAILABILITY_OVERLAP")
            assertTargetRemainsInactive(target, fixture)
        }
    }

    @Test
    fun `protege usuario con asignaciones profesionales`() {
        val fixture = fixtureWithAssignment(active = true)

        expect(updateProfessional(fixture, includeRole = false), 409, "PROFESSIONAL_ROLE_HAS_ACTIVE_ASSIGNMENTS")
        expect(delete("/users/${fixture.professionalId}"), 409, "USER_HAS_PROFESSIONAL_ASSIGNMENTS")
    }

    @Test
    fun `protege rol profesional mientras existen asignaciones activas`() {
        fixtureWithAssignment(active = true)
        val roleId = requireNotNull(professionalRole.id)

        val rename = mvc.perform(
            put("/roles/$roleId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(mapOf("name" to "OTRO_ROL", "permissions" to emptyList<String>()))),
        ).andReturn()
        expect(rename, 409, "PROFESSIONAL_ROLE_HAS_ACTIVE_ASSIGNMENTS")
        expect(delete("/roles/$roleId"), 409, "PROFESSIONAL_ROLE_HAS_ACTIVE_ASSIGNMENTS")
    }

    @Test
    fun `impide eliminar profesional aun con asignaciones inactivas`() {
        val fixture = fixtureWithAssignment(active = false)

        expect(delete("/users/${fixture.professionalId}"), 409, "USER_HAS_PROFESSIONAL_ASSIGNMENTS")
    }

    private fun fixtureWithLatentOverlap(target: ActivationTarget): Fixture = tx {
        val suffix = UUID.randomUUID().toString().take(8)
        val baseCenter = centers.saveAndFlush(MunicipalCenter(name = "Base $suffix", address = "Base 123"))
        val targetCenter = centers.saveAndFlush(
            MunicipalCenter(
                name = "Target $suffix",
                address = "Target 123",
                active = target != ActivationTarget.CENTER,
            ),
        )
        val baseService = services.saveAndFlush(
            MunicipalService(name = "Base $suffix", description = "Base", durationMinutes = 30),
        )
        val targetService = services.saveAndFlush(
            MunicipalService(
                name = "Target $suffix",
                description = "Target",
                durationMinutes = 30,
                active = target != ActivationTarget.SERVICE,
            ),
        )
        val baseRelation = centerServices.saveAndFlush(CenterService(center = baseCenter, service = baseService))
        val targetRelation = centerServices.saveAndFlush(
            CenterService(
                center = targetCenter,
                service = targetService,
                active = target != ActivationTarget.CENTER_SERVICE,
            ),
        )
        val professional = users.saveAndFlush(
            User(
                name = "Profesional $suffix",
                email = "professional-$suffix-${UUID.randomUUID()}@example.com",
                active = target != ActivationTarget.PROFESSIONAL,
                roles = mutableSetOf(professionalRole),
            ),
        )
        val baseAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = professional, centerService = baseRelation),
        )
        val targetAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(
                professional = professional,
                centerService = targetRelation,
                active = target != ActivationTarget.ASSIGNMENT,
            ),
        )
        listOf(baseCenter, targetCenter).forEach { center ->
            openingHours.saveAndFlush(
                CenterOpeningHour(
                    center = center,
                    dayOfWeek = DayOfWeek.MONDAY,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(13, 0),
                ),
            )
        }
        availabilities.saveAndFlush(
            ProfessionalAvailability(
                assignment = baseAssignment,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(11, 0),
            ),
        )
        availabilities.saveAndFlush(
            ProfessionalAvailability(
                assignment = targetAssignment,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(12, 0),
            ),
        )
        Fixture(
            centerId = requireNotNull(targetCenter.id),
            serviceId = requireNotNull(targetService.id),
            centerServiceId = requireNotNull(targetRelation.id),
            assignmentId = requireNotNull(targetAssignment.id),
            professionalId = requireNotNull(professional.id),
            professionalName = professional.name,
            professionalEmail = professional.email,
        )
    }

    private fun fixtureWithAssignment(active: Boolean): Fixture = tx {
        val suffix = UUID.randomUUID().toString().take(8)
        val center = centers.saveAndFlush(MunicipalCenter(name = "Centro $suffix", address = "Calle 123"))
        val service = services.saveAndFlush(
            MunicipalService(name = "Servicio $suffix", description = "Servicio", durationMinutes = 30),
        )
        val relation = centerServices.saveAndFlush(CenterService(center = center, service = service))
        val professional = users.saveAndFlush(
            User(
                name = "Profesional $suffix",
                email = "professional-$suffix-${UUID.randomUUID()}@example.com",
                roles = mutableSetOf(professionalRole),
            ),
        )
        val assignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = professional, centerService = relation, active = active),
        )
        Fixture(
            centerId = requireNotNull(center.id),
            serviceId = requireNotNull(service.id),
            centerServiceId = requireNotNull(relation.id),
            assignmentId = requireNotNull(assignment.id),
            professionalId = requireNotNull(professional.id),
            professionalName = professional.name,
            professionalEmail = professional.email,
        )
    }

    private fun activate(target: ActivationTarget, fixture: Fixture): MvcResult =
        when (target) {
            ActivationTarget.CENTER -> patch("/api/admin/municipal-centers/${fixture.centerId}/activate")
            ActivationTarget.SERVICE -> patch("/api/admin/municipal-services/${fixture.serviceId}/activate")
            ActivationTarget.CENTER_SERVICE -> patch("/api/admin/center-services/${fixture.centerServiceId}/activate")
            ActivationTarget.ASSIGNMENT -> patch("/api/admin/professional-assignments/${fixture.assignmentId}/activate")
            ActivationTarget.PROFESSIONAL -> updateProfessional(fixture, active = true)
        }

    private fun updateProfessional(
        fixture: Fixture,
        active: Boolean = true,
        includeRole: Boolean = true,
    ): MvcResult =
        mvc.perform(
            put("/users/${fixture.professionalId}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        mapOf(
                            "name" to fixture.professionalName,
                            "email" to fixture.professionalEmail,
                            "active" to active,
                            "roles" to if (includeRole) listOf("PROFESIONAL_CENTRO") else emptyList<String>(),
                        ),
                    ),
                ),
        ).andReturn()

    private fun patch(path: String): MvcResult =
        mvc.perform(
            patchRequest(path).header("Authorization", "Bearer $token"),
        ).andReturn()

    private fun delete(path: String): MvcResult =
        mvc.perform(
            deleteRequest(path).header("Authorization", "Bearer $token"),
        ).andReturn()

    private fun assertTargetRemainsInactive(target: ActivationTarget, fixture: Fixture) = tx {
        val active = when (target) {
            ActivationTarget.CENTER -> centers.findById(fixture.centerId).orElseThrow().active
            ActivationTarget.SERVICE -> services.findById(fixture.serviceId).orElseThrow().active
            ActivationTarget.CENTER_SERVICE -> centerServices.findById(fixture.centerServiceId).orElseThrow().active
            ActivationTarget.ASSIGNMENT -> assignments.findById(fixture.assignmentId).orElseThrow().active
            ActivationTarget.PROFESSIONAL -> users.findById(fixture.professionalId).orElseThrow().active
        }
        assertFalse(active)
    }

    private fun expect(result: MvcResult, status: Int, code: String) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        assertEquals(code, json.readTree(result.response.contentAsString).get("code").asText())
    }

    private fun <T> tx(block: () -> T): T = TransactionTemplate(transactions).execute { block() }!!

    private enum class ActivationTarget {
        CENTER,
        SERVICE,
        CENTER_SERVICE,
        ASSIGNMENT,
        PROFESSIONAL,
    }

    private data class Fixture(
        val centerId: UUID,
        val serviceId: UUID,
        val centerServiceId: UUID,
        val assignmentId: UUID,
        val professionalId: Long,
        val professionalName: String,
        val professionalEmail: String,
    )
}
