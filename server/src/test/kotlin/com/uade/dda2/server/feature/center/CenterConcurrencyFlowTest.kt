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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class CenterConcurrencyFlowTest {
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

    companion object {
        private val SAFE_POSTGRES_URLS = setOf(
            "jdbc:postgresql://127.0.0.1:55439/center_test",
            "jdbc:postgresql://127.0.0.1:5433/center_test_real",
        )

        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) {
            val postgresUrl = System.getProperty("center.test.postgres-url")
            require(postgresUrl == null || postgresUrl in SAFE_POSTGRES_URLS) {
                "Las pruebas de centros solo pueden usar bases locales descartables: $SAFE_POSTGRES_URLS."
            }
            val isRealisticClone = postgresUrl?.contains("center_test_real") == true
            registry.add("spring.datasource.url") {
                postgresUrl ?: "jdbc:h2:mem:center-concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000"
            }
            registry.add("spring.datasource.driver-class-name") {
                if (postgresUrl == null) "org.h2.Driver" else "org.postgresql.Driver"
            }
            registry.add("spring.jpa.properties.hibernate.dialect") {
                if (postgresUrl == null) "org.hibernate.dialect.H2Dialect" else "org.hibernate.dialect.PostgreSQLDialect"
            }
            registry.add("spring.datasource.username") {
                if (postgresUrl == null) "sa" else System.getProperty("center.test.postgres-user", "postgres")
            }
            registry.add("spring.datasource.password") {
                if (postgresUrl == null) "" else System.getProperty("center.test.postgres-password", "")
            }
            registry.add("spring.jpa.hibernate.ddl-auto") {
                System.getProperty("center.test.ddl-auto") ?: if (isRealisticClone) "update" else "create-drop"
            }
        }
    }

    @BeforeEach
    fun setup() {
        token = tx {
            val granted = listOf("schedules:management:manage").map { name ->
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
                    name = "Concurrency Admin",
                    email = "concurrency-admin-${UUID.randomUUID()}@example.com",
                    roles = mutableSetOf(adminRole),
                ),
            )
            jwt.createToken(admin, adminRole)
        }
    }

    @Test
    fun `serializa altas concurrentes de horarios del mismo centro`() {
        val centerId = tx {
            requireNotNull(
                centers.saveAndFlush(
                    MunicipalCenter(name = "Centro ${UUID.randomUUID()}", address = "Calle 123"),
                ).id,
            )
        }

        val results = concurrently(
            { createOpeningHour(centerId, "09:00", "11:00") },
            { createOpeningHour(centerId, "10:00", "12:00") },
        )

        assertStatuses(results, successStatus = 201, conflictCode = "CENTER_OPENING_HOUR_OVERLAP")
        assertEquals(
            1,
            tx { openingHours.findAllByCenterIdAndActiveOrderByDayOfWeekAscStartTimeAsc(centerId, true).size },
        )
    }

    @Test
    fun `serializa disponibilidades concurrentes de asignaciones del mismo profesional`() {
        val fixture = availabilityFixture()

        val results = concurrently(
            { createAvailability(fixture.firstAssignmentId, "09:00", "11:00") },
            { createAvailability(fixture.secondAssignmentId, "10:00", "12:00") },
        )

        assertStatuses(results, successStatus = 201, conflictCode = "PROFESSIONAL_AVAILABILITY_OVERLAP")
        val activeCount = tx {
            listOf(fixture.firstAssignmentId, fixture.secondAssignmentId).sumOf { assignmentId ->
                availabilities.findAllByAssignmentIdAndActiveOrderByDayOfWeekAscStartTimeAsc(assignmentId, true).size
            }
        }
        assertEquals(1, activeCount)
    }

    @Test
    fun `serializa cambio de apertura contra alta de disponibilidad`() {
        val fixture = availabilityFixture(singleOpeningHour = true)

        val results = concurrently(
            { updateOpeningHour(fixture.openingHourId, "08:00", "10:00") },
            { createAvailability(fixture.firstAssignmentId, "10:00", "11:00") },
        )

        val statuses = results.map { it.response.status }
        assertEquals(1, statuses.count { it == 409 }, responses(results))
        assertEquals(1, statuses.count { it == 200 || it == 201 }, responses(results))
        val conflict = results.single { it.response.status == 409 }
        assertEquals(
            "PROFESSIONAL_AVAILABILITY_OUTSIDE_OPENING_HOURS",
            json.readTree(conflict.response.contentAsString).get("code").asString(),
        )

        tx {
            val openingHour = openingHours.findById(fixture.openingHourId).orElseThrow()
            val effectiveAvailability = availabilities
                .findAllByAssignmentIdAndActiveOrderByDayOfWeekAscStartTimeAsc(fixture.firstAssignmentId, true)
            if (effectiveAvailability.isEmpty()) {
                assertEquals(LocalTime.of(10, 0), openingHour.endTime)
            } else {
                assertEquals(LocalTime.of(12, 0), openingHour.endTime)
                assertEquals(LocalTime.of(10, 0), effectiveAvailability.single().startTime)
            }
        }
    }

    private fun availabilityFixture(singleOpeningHour: Boolean = false): AvailabilityFixture = tx {
        val suffix = UUID.randomUUID().toString().take(8)
        val center = centers.saveAndFlush(MunicipalCenter(name = "Centro $suffix", address = "Calle 123"))
        val openingHour = openingHours.saveAndFlush(
            CenterOpeningHour(
                center = center,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(12, 0),
            ),
        )
        if (!singleOpeningHour) {
            openingHours.saveAndFlush(
                CenterOpeningHour(
                    center = center,
                    dayOfWeek = DayOfWeek.MONDAY,
                    startTime = LocalTime.of(12, 0),
                    endTime = LocalTime.of(13, 0),
                ),
            )
        }
        val firstService = services.saveAndFlush(
            MunicipalService(name = "Servicio A $suffix", description = "Servicio A", durationMinutes = 30),
        )
        val secondService = services.saveAndFlush(
            MunicipalService(name = "Servicio B $suffix", description = "Servicio B", durationMinutes = 30),
        )
        val firstRelation = centerServices.saveAndFlush(CenterService(center = center, service = firstService))
        val secondRelation = centerServices.saveAndFlush(CenterService(center = center, service = secondService))
        val professional = users.saveAndFlush(
            User(
                name = "Profesional $suffix",
                email = "professional-$suffix-${UUID.randomUUID()}@example.com",
                roles = mutableSetOf(professionalRole),
            ),
        )
        val firstAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = professional, centerService = firstRelation),
        )
        val secondAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = professional, centerService = secondRelation),
        )
        AvailabilityFixture(
            openingHourId = requireNotNull(openingHour.id),
            firstAssignmentId = requireNotNull(firstAssignment.id),
            secondAssignmentId = requireNotNull(secondAssignment.id),
        )
    }

    private fun createOpeningHour(centerId: UUID, start: String, end: String): MvcResult =
        mvc.perform(
            post("/api/admin/municipal-centers/$centerId/opening-hours")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(start, end)),
        ).andReturn()

    private fun updateOpeningHour(id: UUID, start: String, end: String): MvcResult =
        mvc.perform(
            put("/api/admin/center-opening-hours/$id")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(start, end)),
        ).andReturn()

    private fun createAvailability(assignmentId: UUID, start: String, end: String): MvcResult =
        mvc.perform(
            post("/api/admin/professional-assignments/$assignmentId/availability")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(start, end)),
        ).andReturn()

    private fun scheduleBody(start: String, end: String): String =
        json.writeValueAsString(mapOf("dayOfWeek" to "MONDAY", "startTime" to start, "endTime" to end))

    private fun concurrently(vararg actions: () -> MvcResult): List<MvcResult> {
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(actions.size)
        return try {
            val futures = actions.map { action ->
                executor.submit<MvcResult> {
                    assertTrue(start.await(10, TimeUnit.SECONDS))
                    action()
                }
            }
            start.countDown()
            futures.map { it.get(20, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun assertStatuses(results: List<MvcResult>, successStatus: Int, conflictCode: String) {
        val statuses = results.map { it.response.status }
        assertEquals(1, statuses.count { it == successStatus }, responses(results))
        assertEquals(1, statuses.count { it == 409 }, responses(results))
        val conflict = results.single { it.response.status == 409 }
        assertEquals(conflictCode, json.readTree(conflict.response.contentAsString).get("code").asString())
    }

    private fun responses(results: List<MvcResult>): String =
        results.joinToString(separator = "\n") { result ->
            "${result.response.status}: ${result.response.contentAsString}"
        }

    private fun <T> tx(block: () -> T): T = TransactionTemplate(transactions).execute { block() }!!

    private data class AvailabilityFixture(
        val openingHourId: UUID,
        val firstAssignmentId: UUID,
        val secondAssignmentId: UUID,
    )
}
