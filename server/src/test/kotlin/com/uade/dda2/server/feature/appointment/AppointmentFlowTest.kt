package com.uade.dda2.server.feature.appointment

import com.uade.dda2.server.feature.appointment.dto.response.AppointmentResponse
import com.uade.dda2.server.feature.appointment.entity.Appointment
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:appointments;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000",
        "app.enrollment-period.expiration.cron=-",
        "app.appointment.zone-id=America/Argentina/Buenos_Aires",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class AppointmentFlowTest {
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
    private lateinit var fixture: Fixture

    data class Fixture(
        val citizenId: Long,
        val token: String,
        val wrongRoleToken: String,
        val secondCitizenToken: String,
        val serviceId: UUID,
        val centerServiceId: UUID,
        val firstAssignmentId: UUID,
        val secondAssignmentId: UUID,
        val date: LocalDate,
    )

    @BeforeEach
    fun setup() {
        fixture = createFixture()
    }

    @Test
    fun `consulta opciones confirma obtiene y reintenta sin duplicar`() {
        val servicesResult = authorizedGet("/api/citizen/appointment-services")
        expect(servicesResult, 200)
        assertTrue(servicesResult.response.contentAsString.contains(fixture.serviceId.toString()))

        val centersResult = authorizedGet("/api/citizen/appointment-services/${fixture.serviceId}/centers")
        expect(centersResult, 200)
        assertTrue(centersResult.response.contentAsString.contains(fixture.centerServiceId.toString()))

        val slotsResult = slots(fixture.token)
        expect(slotsResult, 200)
        val slots = json.readTree(slotsResult.response.contentAsString)
        assertEquals(6, slots.size())
        assertTrue(slots.any { it.get("professionalName").asText() == "Profesional Uno" })

        val key = UUID.randomUUID().toString()
        val createdResult = create(fixture.firstAssignmentId, 9, 10, key)
        expect(createdResult, 201)
        assertEquals("false", createdResult.response.getHeader("Idempotency-Replayed"))
        val created = response(createdResult)
        assertEquals("CONFIRMED", created.status.name)
        assertEquals("Profesional Uno", created.professionalName)
        assertTrue(created.centerName.startsWith("Centro Salud "))
        assertEquals("Calle 123", created.centerAddress)

        val replay = create(fixture.firstAssignmentId, 9, 10, key)
        expect(replay, 200)
        assertEquals(created.id, response(replay).id)
        assertEquals("true", replay.response.getHeader("Idempotency-Replayed"))

        val detail = authorizedGet("/api/citizen/appointments/${created.id}")
        expect(detail, 200)
        assertEquals(created.id, response(detail).id)
        assertTrue(appointments.existsById(created.id))
        assertEquals(
            1,
            jdbc.queryForObject(
                "select count(*) from logs where entity_type = 'appointment' and entity_id = ?",
                Long::class.java,
                created.id.toString(),
            ),
        )

        val refreshedSlots = json.readTree(slots(fixture.token).response.contentAsString)
        assertFalse(refreshedSlots.any {
            it.get("startsAt").asText().contains("T09:00")
        })

        expect(create(fixture.firstAssignmentId, 10, 11, UUID.randomUUID().toString()), 201)
    }

    @Test
    fun `rechaza superposicion ciudadana horario ocupado clave reutilizada y rol incorrecto`() {
        val key = UUID.randomUUID().toString()
        expect(create(fixture.firstAssignmentId, 9, 10, key), 201)

        expect(
            create(fixture.secondAssignmentId, 9, 10, UUID.randomUUID().toString()),
            409,
            "APPOINTMENT_CITIZEN_OVERLAP",
        )
        expect(create(fixture.firstAssignmentId, 10, 11, key), 409, "IDEMPOTENCY_KEY_REUSED")

        expect(
            create(
                fixture.firstAssignmentId,
                9,
                10,
                UUID.randomUUID().toString(),
                token = fixture.secondCitizenToken,
            ),
            409,
            "APPOINTMENT_SLOT_UNAVAILABLE",
        )
        expect(slots(fixture.wrongRoleToken), 403)
    }

    @Test
    fun `protege turno ajeno y rechaza campos inesperados`() {
        val created = response(create(fixture.firstAssignmentId, 9, 10, UUID.randomUUID().toString()))
        val foreignDetail = mvc.perform(
            get("/api/citizen/appointments/${created.id}")
                .header("Authorization", "Bearer ${fixture.secondCitizenToken}"),
        ).andReturn()
        expect(foreignDetail, 404, "APPOINTMENT_RESOURCE_NOT_FOUND")

        val start = at(9)
        val end = at(10)
        val unexpected = mvc.perform(
            post("/api/citizen/appointments")
                .header("Authorization", "Bearer ${fixture.token}")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"professionalAssignmentId":"${fixture.secondAssignmentId}","startsAt":"$start","endsAt":"$end","citizenId":999}""",
                ),
        ).andReturn()
        expect(unexpected, 400, "APPOINTMENT_INVALID_REQUEST")
    }

    @Test
    fun `serializa reservas concurrentes por profesional y ciudadano`() {
        val sameProfessional = concurrently(
            {
                create(
                    fixture.firstAssignmentId,
                    9,
                    10,
                    UUID.randomUUID().toString(),
                    token = fixture.token,
                )
            },
            {
                create(
                    fixture.firstAssignmentId,
                    9,
                    10,
                    UUID.randomUUID().toString(),
                    token = fixture.secondCitizenToken,
                )
            },
        )
        assertEquals(listOf(201, 409), sameProfessional.map { it.response.status }.sorted())
        assertEquals(
            1,
            tx {
                appointments.findOverlapsByProfessionalId(
                    professionalId(fixture.firstAssignmentId),
                    at(9),
                    at(10),
                ).size
            },
        )

        val sameCitizen = concurrently(
            { create(fixture.firstAssignmentId, 10, 11, UUID.randomUUID().toString()) },
            { create(fixture.secondAssignmentId, 10, 11, UUID.randomUUID().toString()) },
        )
        assertEquals(listOf(201, 409), sameCitizen.map { it.response.status }.sorted())
        assertEquals(
            1,
            tx { appointments.findOverlapsByCitizenId(fixture.citizenId, at(10), at(11)).size },
        )
    }

    @Test
    fun `reproduce un turno aunque su horario ya haya pasado`() {
        val key = UUID.randomUUID().toString()
        val startsAt = LocalDate.now(zone).minusDays(1).atTime(9, 0).atZone(zone).toOffsetDateTime()
            .toInstant().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC)
        val endsAt = startsAt.plusHours(1)
        val storedId = tx {
            val assignment = assignments.findById(fixture.firstAssignmentId).orElseThrow()
            val citizen = users.findById(fixture.citizenId).orElseThrow()
            appointments.saveAndFlush(
                Appointment(
                    citizen = citizen,
                    professionalAssignment = assignment,
                    startsAt = startsAt,
                    endsAt = endsAt,
                    idempotencyKey = key,
                    requestHash = requestHash(fixture.firstAssignmentId, startsAt, endsAt),
                ),
            ).id!!
        }

        val replay = mvc.perform(
            post("/api/citizen/appointments")
                .header("Authorization", "Bearer ${fixture.token}")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"professionalAssignmentId":"${fixture.firstAssignmentId}","startsAt":"$startsAt","endsAt":"$endsAt"}""",
                ),
        ).andReturn()

        expect(replay, 200)
        assertEquals(storedId, response(replay).id)
        assertEquals("true", replay.response.getHeader("Idempotency-Replayed"))
    }

    private fun createFixture(): Fixture = tx {
        val view = permission("appointments:own:view")
        val create = permission("appointments:own:create")
        val citizenRole = role("CIUDADANO", mutableSetOf(view, create))
        val viewerRole = role("VIEWER_APPOINTMENTS", mutableSetOf(view, create))
        val professionalRole = role("PROFESIONAL_CENTRO")
        val citizen = users.saveAndFlush(
            User(
                name = "Ciudadano Uno",
                email = "citizen-${UUID.randomUUID()}@example.com",
                roles = mutableSetOf(citizenRole, viewerRole),
            ),
        )
        val secondCitizen = users.saveAndFlush(
            User(
                name = "Ciudadano Dos",
                email = "citizen2-${UUID.randomUUID()}@example.com",
                roles = mutableSetOf(citizenRole),
            ),
        )
        val firstProfessional = users.saveAndFlush(
            User(
                name = "Profesional Uno",
                email = "professional1-${UUID.randomUUID()}@example.com",
                roles = mutableSetOf(professionalRole),
            ),
        )
        val secondProfessional = users.saveAndFlush(
            User(
                name = "Profesional Dos",
                email = "professional2-${UUID.randomUUID()}@example.com",
                roles = mutableSetOf(professionalRole),
            ),
        )
        val date = LocalDate.now(zone).plusDays(7)
        val suffix = UUID.randomUUID().toString().take(8)
        val center = centers.saveAndFlush(MunicipalCenter(name = "Centro Salud $suffix", address = "Calle 123"))
        val service = services.saveAndFlush(
            MunicipalService(name = "Clinica medica $suffix", description = "Consulta general", durationMinutes = 60),
        )
        openingHours.saveAndFlush(
            CenterOpeningHour(
                center = center,
                dayOfWeek = date.dayOfWeek,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(12, 0),
            ),
        )
        val centerService = centerServices.saveAndFlush(CenterService(center = center, service = service))
        val firstAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = firstProfessional, centerService = centerService),
        )
        val secondAssignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = secondProfessional, centerService = centerService),
        )
        listOf(firstAssignment, secondAssignment).forEach { assignment ->
            availabilities.saveAndFlush(
                ProfessionalAvailability(
                    assignment = assignment,
                    dayOfWeek = date.dayOfWeek,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(12, 0),
                ),
            )
        }
        Fixture(
            citizenId = requireNotNull(citizen.id),
            token = jwt.createToken(citizen, citizenRole),
            wrongRoleToken = jwt.createToken(citizen, viewerRole),
            secondCitizenToken = jwt.createToken(secondCitizen, citizenRole),
            serviceId = requireNotNull(service.id),
            centerServiceId = requireNotNull(centerService.id),
            firstAssignmentId = requireNotNull(firstAssignment.id),
            secondAssignmentId = requireNotNull(secondAssignment.id),
            date = date,
        )
    }

    private fun permission(name: String): Permission =
        permissions.findByNameIn(listOf(name)).firstOrNull() ?: permissions.save(Permission(name = name))

    private fun role(name: String, granted: MutableSet<Permission> = mutableSetOf()): Role {
        val existing = roles.findByNameIn(listOf(name)).firstOrNull()
        if (existing != null) {
            existing.permissions.addAll(granted)
            return roles.save(existing)
        }
        return roles.save(Role(name = name, permissions = granted))
    }

    private fun authorizedGet(path: String): MvcResult = mvc.perform(
        get(path).header("Authorization", "Bearer ${fixture.token}"),
    ).andReturn()

    private fun slots(token: String): MvcResult = mvc.perform(
        get("/api/citizen/appointment-slots")
            .header("Authorization", "Bearer $token")
            .queryParam("centerServiceId", fixture.centerServiceId.toString())
            .queryParam("date", fixture.date.toString()),
    ).andReturn()

    private fun create(
        assignmentId: UUID,
        startHour: Int,
        endHour: Int,
        key: String,
        token: String = fixture.token,
    ): MvcResult = mvc.perform(
        post("/api/citizen/appointments")
            .header("Authorization", "Bearer $token")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """{"professionalAssignmentId":"$assignmentId","startsAt":"${at(startHour)}","endsAt":"${at(endHour)}"}""",
            ),
    ).andReturn()

    private fun at(hour: Int): OffsetDateTime = fixture.date.atTime(hour, 0).atZone(zone).toOffsetDateTime()

    private fun response(result: MvcResult): AppointmentResponse =
        json.readValue(result.response.contentAsString, AppointmentResponse::class.java)

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").asText())
    }

    private fun professionalId(assignmentId: UUID): Long = tx {
        requireNotNull(assignments.findById(assignmentId).orElseThrow().professional.id)
    }

    private fun requestHash(assignmentId: UUID, startsAt: OffsetDateTime, endsAt: OffsetDateTime): String =
        HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(
                "appointment:v1:$assignmentId:${startsAt.toInstant()}:${endsAt.toInstant()}".toByteArray(Charsets.UTF_8),
            ),
        )

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
            futures.map { it.get(20, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun <T : Any> tx(action: () -> T): T = TransactionTemplate(transactions).execute { action() }!!
}
