package com.uade.dda2.server.feature.appointment

import com.uade.dda2.server.feature.appointment.repository.AppointmentRepository
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
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
        "app.appointment.zone-id=America/Argentina/Buenos_Aires",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class AppointmentConcurrencyFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var centers: MunicipalCenterRepository
    @Autowired lateinit var services: MunicipalServiceRepository
    @Autowired lateinit var centerServices: CenterServiceRepository
    @Autowired lateinit var openingHours: CenterOpeningHourRepository
    @Autowired lateinit var assignments: ProfessionalAssignmentRepository
    @Autowired lateinit var availabilities: ProfessionalAvailabilityRepository
    @Autowired lateinit var appointments: AppointmentRepository
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var transactions: PlatformTransactionManager

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    companion object {
        private val SAFE_POSTGRES_URLS = setOf(
            "jdbc:postgresql://127.0.0.1:55439/appointment_test",
        )

        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) {
            val postgresUrl = System.getProperty("appointment.test.postgres-url")
            require(postgresUrl == null || postgresUrl in SAFE_POSTGRES_URLS) {
                "Las pruebas de turnos solo pueden usar bases locales descartables: $SAFE_POSTGRES_URLS."
            }
            registry.add("spring.datasource.url") {
                postgresUrl ?: "jdbc:h2:mem:appointment-concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000"
            }
            registry.add("spring.datasource.driver-class-name") {
                if (postgresUrl == null) "org.h2.Driver" else "org.postgresql.Driver"
            }
            registry.add("spring.jpa.properties.hibernate.dialect") {
                if (postgresUrl == null) "org.hibernate.dialect.H2Dialect" else "org.hibernate.dialect.PostgreSQLDialect"
            }
            registry.add("spring.datasource.username") {
                if (postgresUrl == null) "sa" else System.getProperty("appointment.test.postgres-user", "appointment_test")
            }
            registry.add("spring.datasource.password") {
                if (postgresUrl == null) "" else System.getProperty("appointment.test.postgres-password", "")
            }
        }
    }

    private lateinit var fixture: Fixture

    data class Fixture(
        val firstCitizenToken: String,
        val secondCitizenToken: String,
        val firstAssignmentId: UUID,
        val secondAssignmentId: UUID,
        val sameProfessionalAssignmentId: UUID,
        val date: LocalDate,
    )

    @BeforeEach
    fun setup() {
        fixture = createFixture()
    }

    @Test
    fun `dos ciudadanos no obtienen el mismo horario del mismo profesional`() {
        val results = concurrently(
            { create(fixture.firstAssignmentId, 9, 10, UUID.randomUUID().toString(), fixture.firstCitizenToken) },
            { create(fixture.firstAssignmentId, 9, 10, UUID.randomUUID().toString(), fixture.secondCitizenToken) },
        )

        assertStatuses(results, 201, "APPOINTMENT_SLOT_UNAVAILABLE")
        assertEquals(
            1,
            tx { appointments.findOverlapsByProfessionalId(professionalId(fixture.firstAssignmentId), at(9), at(10)).size },
        )
    }

    @Test
    fun `el mismo profesional no queda doble reservado por asignaciones distintas`() {
        val results = concurrently(
            { create(fixture.firstAssignmentId, 9, 10, UUID.randomUUID().toString(), fixture.firstCitizenToken) },
            { create(fixture.sameProfessionalAssignmentId, 9, 10, UUID.randomUUID().toString(), fixture.secondCitizenToken) },
        )

        assertStatuses(results, 201, "APPOINTMENT_SLOT_UNAVAILABLE")
    }

    @Test
    fun `el mismo ciudadano no confirma dos turnos superpuestos`() {
        val results = concurrently(
            { create(fixture.firstAssignmentId, 10, 11, UUID.randomUUID().toString(), fixture.firstCitizenToken) },
            { create(fixture.secondAssignmentId, 10, 11, UUID.randomUUID().toString(), fixture.firstCitizenToken) },
        )

        val statuses = results.map { it.response.status }.sorted()
        assertEquals(listOf(201, 409), statuses)
        val conflict = results.single { it.response.status == 409 }
        assertEquals(
            "APPOINTMENT_CITIZEN_OVERLAP",
            json.readTree(conflict.response.contentAsString).get("code").asText(),
        )
    }

    @Test
    fun `reintentos concurrentes con la misma clave no duplican`() {
        val key = UUID.randomUUID().toString()
        val results = concurrently(
            { create(fixture.firstAssignmentId, 9, 10, key, fixture.firstCitizenToken) },
            { create(fixture.firstAssignmentId, 9, 10, key, fixture.firstCitizenToken) },
            { create(fixture.firstAssignmentId, 9, 10, key, fixture.firstCitizenToken) },
        )

        val statuses = results.map { it.response.status }.sorted()
        assertEquals(listOf(200, 200, 201), statuses)
        val ids = results.map { json.readTree(it.response.contentAsString).get("id").asText() }.toSet()
        assertEquals(1, ids.size)
        assertEquals(
            1,
            jdbc.queryForObject(
                "select count(*) from logs where entity_type = 'appointment' and entity_id = ?",
                Long::class.java,
                ids.single(),
            ),
        )
    }

    private fun createFixture(): Fixture = tx {
        val view = permissions.findByNameIn(listOf("appointments:own:view")).firstOrNull()
            ?: permissions.save(Permission(name = "appointments:own:view"))
        val create = permissions.findByNameIn(listOf("appointments:own:create")).firstOrNull()
            ?: permissions.save(Permission(name = "appointments:own:create"))
        val citizenRole = roles.findByNameIn(listOf("CIUDADANO")).firstOrNull()
            ?.also { it.permissions.addAll(setOf(view, create)); roles.save(it) }
            ?: roles.save(Role(name = "CIUDADANO", permissions = mutableSetOf(view, create)))
        val professionalRole = roles.findByNameIn(listOf("PROFESIONAL_CENTRO")).firstOrNull()
            ?: roles.save(Role(name = "PROFESIONAL_CENTRO"))

        val firstCitizen = users.saveAndFlush(
            User(name = "Ciudadano Uno", email = "c1-${UUID.randomUUID()}@example.com", roles = mutableSetOf(citizenRole)),
        )
        val secondCitizen = users.saveAndFlush(
            User(name = "Ciudadano Dos", email = "c2-${UUID.randomUUID()}@example.com", roles = mutableSetOf(citizenRole)),
        )
        val firstProfessional = users.saveAndFlush(
            User(name = "Profesional Uno", email = "p1-${UUID.randomUUID()}@example.com", roles = mutableSetOf(professionalRole)),
        )
        val secondProfessional = users.saveAndFlush(
            User(name = "Profesional Dos", email = "p2-${UUID.randomUUID()}@example.com", roles = mutableSetOf(professionalRole)),
        )

        val date = LocalDate.now(zone).plusDays(7)
        val suffix = UUID.randomUUID().toString().take(8)
        val center = centers.saveAndFlush(MunicipalCenter(name = "Centro Turnos $suffix", address = "Calle 123"))
        openingHours.saveAndFlush(
            CenterOpeningHour(
                center = center,
                dayOfWeek = date.dayOfWeek,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(12, 0),
            ),
        )
        val firstService = services.saveAndFlush(
            MunicipalService(name = "Clinica A $suffix", description = "Consulta", durationMinutes = 60),
        )
        val secondService = services.saveAndFlush(
            MunicipalService(name = "Clinica B $suffix", description = "Consulta", durationMinutes = 60),
        )
        val firstCenterService = centerServices.saveAndFlush(CenterService(center = center, service = firstService))
        val secondCenterService = centerServices.saveAndFlush(CenterService(center = center, service = secondService))
        val firstAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = firstProfessional, centerService = firstCenterService),
        )
        val secondAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = secondProfessional, centerService = firstCenterService),
        )
        val sameProfessionalAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = firstProfessional, centerService = secondCenterService),
        )
        listOf(firstAssignment, secondAssignment, sameProfessionalAssignment).forEach { assignment ->
            availabilities.saveAndFlush(
                com.uade.dda2.server.feature.center.entity.ProfessionalAvailability(
                    assignment = assignment,
                    dayOfWeek = date.dayOfWeek,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(12, 0),
                ),
            )
        }
        Fixture(
            firstCitizenToken = jwt.createToken(firstCitizen, citizenRole),
            secondCitizenToken = jwt.createToken(secondCitizen, citizenRole),
            firstAssignmentId = requireNotNull(firstAssignment.id),
            secondAssignmentId = requireNotNull(secondAssignment.id),
            sameProfessionalAssignmentId = requireNotNull(sameProfessionalAssignment.id),
            date = date,
        )
    }

    private fun create(assignmentId: UUID, startHour: Int, endHour: Int, key: String, token: String): MvcResult =
        mvc.perform(
            post("/api/citizen/appointments")
                .header("Authorization", "Bearer $token")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"professionalAssignmentId":"$assignmentId","startsAt":"${at(startHour)}","endsAt":"${at(endHour)}"}""",
                ),
        ).andReturn()

    private fun at(hour: Int): OffsetDateTime = fixture.date.atTime(hour, 0).atZone(zone).toOffsetDateTime()

    private fun professionalId(assignmentId: UUID): Long = tx {
        requireNotNull(assignments.findById(assignmentId).orElseThrow().professional.id)
    }

    private fun assertStatuses(results: List<MvcResult>, successStatus: Int, conflictCode: String) {
        val statuses = results.map { it.response.status }
        assertEquals(1, statuses.count { it == successStatus }, responses(results))
        assertEquals(1, statuses.count { it == 409 }, responses(results))
        val conflict = results.single { it.response.status == 409 }
        assertEquals(conflictCode, json.readTree(conflict.response.contentAsString).get("code").asText())
    }

    private fun responses(results: List<MvcResult>): String =
        results.joinToString(separator = "\n") { "${it.response.status}: ${it.response.contentAsString}" }

    private fun concurrently(vararg actions: () -> MvcResult): List<MvcResult> {
        val ready = CountDownLatch(actions.size)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(actions.size)
        return try {
            val futures = actions.map { action ->
                executor.submit<MvcResult> {
                    ready.countDown()
                    start.await(10, TimeUnit.SECONDS)
                    action()
                }
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun <T : Any> tx(action: () -> T): T = TransactionTemplate(transactions).execute { action() }!!
}
