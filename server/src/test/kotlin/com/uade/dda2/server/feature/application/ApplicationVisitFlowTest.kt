package com.uade.dda2.server.feature.application

import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitDetailResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitResponse
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationEvaluation
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.EvaluationPriority
import com.uade.dda2.server.feature.application.entity.VulnerabilityLevel
import com.uade.dda2.server.feature.application.repository.ApplicationEvaluationRepository
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.application.repository.ApplicationVisitRepository
import com.uade.dda2.server.feature.appointment.entity.Appointment
import com.uade.dda2.server.feature.appointment.repository.AppointmentRepository
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.center.entity.CenterService
import com.uade.dda2.server.feature.center.entity.MunicipalCenter
import com.uade.dda2.server.feature.center.entity.MunicipalService
import com.uade.dda2.server.feature.center.entity.ProfessionalAssignment
import com.uade.dda2.server.feature.center.entity.ProfessionalAvailability
import com.uade.dda2.server.feature.center.repository.CenterServiceRepository
import com.uade.dda2.server.feature.center.repository.MunicipalCenterRepository
import com.uade.dda2.server.feature.center.repository.MunicipalServiceRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAssignmentRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAvailabilityRepository
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import com.uade.dda2.server.security.JwtService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.dao.DataIntegrityViolationException
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
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZoneId
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:application-visits;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "app.enrollment-period.expiration.cron=-",
        "app.appointment.zone-id=America/Argentina/Buenos_Aires",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ApplicationVisitFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var programs: ProgramRepository
    @Autowired lateinit var editions: ProgramEditionRepository
    @Autowired lateinit var periods: EnrollmentPeriodRepository
    @Autowired lateinit var applications: ApplicationRepository
    @Autowired lateinit var evaluations: ApplicationEvaluationRepository
    @Autowired lateinit var visits: ApplicationVisitRepository
    @Autowired lateinit var appointments: AppointmentRepository
    @Autowired lateinit var centers: MunicipalCenterRepository
    @Autowired lateinit var municipalServices: MunicipalServiceRepository
    @Autowired lateinit var centerServices: CenterServiceRepository
    @Autowired lateinit var assignments: ProfessionalAssignmentRepository
    @Autowired lateinit var availabilities: ProfessionalAvailabilityRepository
    @Autowired lateinit var transactions: PlatformTransactionManager
    @Autowired lateinit var jdbc: JdbcTemplate

    data class Fixture(
        val applicationId: UUID,
        val actorId: Long,
        val actorRoleId: Long,
        val actorToken: String,
        val unauthorizedToken: String,
        val applicantId: Long,
        val periodId: UUID,
        val editionId: UUID,
        val programId: UUID,
    )

    private lateinit var fixture: Fixture

    @BeforeEach
    fun setup() {
        fixture = newFixture()
    }

    private fun <T : Any> tx(action: () -> T): T = TransactionTemplate(transactions).execute { action() }!!

    private fun permission(name: String): Permission =
        permissions.findAll().firstOrNull { it.name == name } ?: permissions.save(Permission(name = name))

    private fun newFixture(): Fixture = tx {
        val createPermission = permission("applications:visits:create")
        val viewPermission = permission("applications:visits:view")
        val managePermission = permission("applications:visits:manage")
        val resultPermission = permission("applications:visits:results:create")
        val actorRole = roles.save(
            Role(
                name = "TRABAJADOR_${UUID.randomUUID()}",
                permissions = mutableSetOf(createPermission, viewPermission, managePermission, resultPermission),
            ),
        )
        val unauthorizedRole = roles.save(Role(name = "SIN_VISITAS_${UUID.randomUUID()}"))
        val actor = users.saveAndFlush(
            User(name = "Trabajador Social", email = "worker-${UUID.randomUUID()}@example.com", roles = mutableSetOf(actorRole)),
        )
        val unauthorized = users.saveAndFlush(
            User(name = "Sin permiso", email = "forbidden-${UUID.randomUUID()}@example.com", roles = mutableSetOf(unauthorizedRole)),
        )
        val applicant = users.saveAndFlush(User(name = "Titular", email = "applicant-${UUID.randomUUID()}@example.com"))
        val program = programs.save(Program(name = "Programa ${UUID.randomUUID()}", createdBy = actor))
        val edition = editions.save(
            ProgramEdition(
                program = program,
                name = "Edicion ${UUID.randomUUID()}",
                startDate = LocalDate.now().minusDays(30),
                endDate = LocalDate.now().plusDays(30),
                maxCapacity = 100,
                status = ProgramEditionStatus.ACTIVE,
                createdBy = actor,
            ),
        )
        val period = periods.save(
            EnrollmentPeriod(
                programEdition = edition,
                openDate = LocalDate.now().minusDays(10),
                closeDate = LocalDate.now().plusDays(10),
                status = EnrollmentPeriodStatus.OPEN,
            ),
        )
        val application = applications.saveAndFlush(
            Application(
                user = applicant,
                registeredBy = applicant,
                programEdition = edition,
                enrollmentPeriod = period,
                status = ApplicationStatus.IN_EVALUATION,
                assignedWorker = actor,
                submittedAt = LocalDateTime.now().minusDays(1),
            ),
        )
        evaluations.saveAndFlush(
            ApplicationEvaluation(
                application = application,
                vulnerability = VulnerabilityLevel.HIGH,
                priority = EvaluationPriority.URGENT,
                recommendation = "Realizar visita.",
                professional = actor,
                evaluatedAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1),
            ),
        )
        Fixture(
            applicationId = requireNotNull(application.id),
            actorId = requireNotNull(actor.id),
            actorRoleId = requireNotNull(actorRole.id),
            actorToken = jwt.createToken(actor, actorRole),
            unauthorizedToken = jwt.createToken(unauthorized, unauthorizedRole),
            applicantId = requireNotNull(applicant.id),
            periodId = requireNotNull(period.id),
            editionId = requireNotNull(edition.id),
            programId = requireNotNull(program.id),
        )
    }

    private fun create(
        applicationId: UUID = fixture.applicationId,
        token: String? = fixture.actorToken,
        startsAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withNano(0),
        durationMinutes: Int = 60,
        type: String = "VISIT",
        location: String = "  Domicilio declarado  ",
        body: String? = null,
    ): MvcResult {
        val request = post("/api/applications/$applicationId/visits")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body ?: json.writeValueAsString(mapOf(
                "type" to type,
                "startsAt" to startsAt,
                "durationMinutes" to durationMinutes,
                "location" to location,
            )))
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").stringValue())
    }

    private fun reschedule(
        visitId: UUID,
        applicationId: UUID = fixture.applicationId,
        token: String? = fixture.actorToken,
        startsAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0),
        durationMinutes: Int = 90,
        location: String = "  Centro comunitario  ",
        body: String? = null,
    ): MvcResult {
        val request = put("/api/applications/$applicationId/visits/$visitId/schedule")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body ?: json.writeValueAsString(mapOf(
                "startsAt" to startsAt,
                "durationMinutes" to durationMinutes,
                "location" to location,
            )))
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }

    private fun cancel(
        visitId: UUID,
        applicationId: UUID = fixture.applicationId,
        token: String? = fixture.actorToken,
        reason: String = "  El ciudadano solicito cambiar la fecha.  ",
        body: String? = null,
    ): MvcResult {
        val request = patch("/api/applications/$applicationId/visits/$visitId/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body ?: json.writeValueAsString(mapOf("reason" to reason)))
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }

    private fun recordResult(
        visitId: UUID,
        applicationId: UUID = fixture.applicationId,
        token: String? = fixture.actorToken,
        status: String = "REALIZED",
        observations: String? = "  La familia recibio al profesional.  ",
        conclusions: String? = "  Se requiere continuar con la evaluacion.  ",
        body: String? = null,
    ): MvcResult {
        val request = post("/api/applications/$applicationId/visits/$visitId/result")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body ?: json.writeValueAsString(mapOf(
                "status" to status,
                "observations" to observations,
                "conclusions" to conclusions,
            )))
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }

    private fun markEnded(visitId: UUID) {
        tx {
            val visit = visits.findById(visitId).orElseThrow()
            visit.startsAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2)
            visit.endsAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1)
        }
    }

    @Test
    fun `programa multiples actuaciones y registra responsable estado y auditoria sin lugar`() {
        val firstStart = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withNano(0)
        val firstResult = create(startsAt = firstStart)
        expect(firstResult, 201)
        val first = json.readValue(firstResult.response.contentAsString, ApplicationVisitResponse::class.java)
        assertEquals(fixture.applicationId, first.applicationId)
        assertEquals(ApplicationStatus.IN_VISIT, first.applicationStatus)
        assertEquals("VISIT", first.type.name)
        assertEquals(60, first.durationMinutes)
        assertEquals("Domicilio declarado", first.location)
        assertEquals("SCHEDULED", first.status.name)
        assertEquals(fixture.actorId, first.responsibleId)
        assertEquals(first.startsAt.plusMinutes(60), first.endsAt)
        assertEquals(ZoneOffset.UTC, first.scheduledAt.offset)

        expect(create(startsAt = firstStart.plusHours(2), type = "INTERVIEW", location = "Oficina social"), 201)
        assertEquals(2L, visits.countByApplicationId(fixture.applicationId))
        tx { assertEquals(ApplicationStatus.IN_VISIT, applications.findById(fixture.applicationId).orElseThrow().status) }

        val log = jdbc.queryForMap(
            "select action, new_values from logs where entity_type = 'application_visit' and entity_id = ?",
            first.id.toString(),
        )
        assertEquals("CREATE", log["action"])
        val audit = jsonValue(log["new_values"])
        assertEquals("IN_EVALUATION", audit.get("previousApplicationStatus").stringValue())
        assertEquals("IN_VISIT", audit.get("applicationStatus").stringValue())
        assertFalse(audit.has("location"))
        assertFalse(log["new_values"].toString().contains(first.location))
    }

    @Test
    fun `lista con filtros ventana semiabierta y obtiene detalle`() {
        val start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0)
        val createdResult = create(startsAt = start, durationMinutes = 90, type = "INTERVIEW", location = "Centro barrial")
        expect(createdResult, 201)
        val created = json.readValue(createdResult.response.contentAsString, ApplicationVisitResponse::class.java)
        val applicationNumber = tx { requireNotNull(applications.findById(fixture.applicationId).orElseThrow().applicationNumber) }

        val listing = mvc.perform(
            get("/api/admin/application-visits")
                .header("Authorization", "Bearer ${fixture.actorToken}")
                .param("applicationId", fixture.applicationId.toString())
                .param("applicationNumber", applicationNumber.toString())
                .param("applicantUserId", fixture.applicantId.toString())
                .param("responsibleId", fixture.actorId.toString())
                .param("programId", fixture.programId.toString())
                .param("programEditionId", fixture.editionId.toString())
                .param("type", "INTERVIEW")
                .param("windowStart", created.startsAt.plusMinutes(30).toString())
                .param("windowEnd", created.endsAt.plusMinutes(30).toString()),
        ).andReturn()
        expect(listing, 200)
        val page = json.readTree(listing.response.contentAsString)
        assertEquals(1L, page.get("totalElements").longValue())
        assertEquals(created.id.toString(), page.get("content").get(0).get("id").stringValue())

        val touchingWindow = mvc.perform(
            get("/api/admin/application-visits")
                .header("Authorization", "Bearer ${fixture.actorToken}")
                .param("windowStart", created.endsAt.toString()),
        ).andReturn()
        expect(touchingWindow, 200)
        assertEquals(0L, json.readTree(touchingWindow.response.contentAsString).get("totalElements").longValue())

        val detailResult = mvc.perform(
            get("/api/admin/application-visits/${created.id}")
                .header("Authorization", "Bearer ${fixture.actorToken}"),
        ).andReturn()
        expect(detailResult, 200)
        val detail = json.readValue(detailResult.response.contentAsString, ApplicationVisitDetailResponse::class.java)
        assertEquals(fixture.applicantId, detail.applicantUserId)
        assertEquals(fixture.programId, detail.programId)
        assertEquals(fixture.editionId, detail.programEditionId)
        assertEquals(fixture.periodId, detail.enrollmentPeriodId)
        assertEquals("Centro barrial", detail.location)
    }

    @Test
    fun `reprograma horario duracion y lugar conservando identidad y trazabilidad sensible`() {
        val originalStart = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withNano(0)
        val createdResult = create(startsAt = originalStart, type = "INTERVIEW", location = "Domicilio original")
        expect(createdResult, 201)
        val created = json.readValue(createdResult.response.contentAsString, ApplicationVisitResponse::class.java)
        val newStart = originalStart.plusDays(1).plusMinutes(30)

        val result = reschedule(
            visitId = created.id,
            startsAt = newStart,
            durationMinutes = 90,
            location = "  Centro comunitario  ",
        )
        expect(result, 200)
        val updated = json.readValue(result.response.contentAsString, ApplicationVisitResponse::class.java)
        assertEquals(created.id, updated.id)
        assertEquals(created.type, updated.type)
        assertEquals(created.responsibleId, updated.responsibleId)
        assertEquals(created.scheduledAt.toInstant(), updated.scheduledAt.toInstant())
        assertEquals(newStart.toInstant(), updated.startsAt.toInstant())
        assertEquals(newStart.plusMinutes(90).toInstant(), updated.endsAt.toInstant())
        assertEquals(90, updated.durationMinutes)
        assertEquals("Centro comunitario", updated.location)
        assertEquals("SCHEDULED", updated.status.name)
        assertEquals(ApplicationStatus.IN_VISIT, updated.applicationStatus)

        val log = jdbc.queryForMap(
            "select action, old_values, new_values from logs " +
                "where entity_type = 'application_visit' and entity_id = ? and action = 'UPDATE'",
            created.id.toString(),
        )
        assertEquals("UPDATE", log["action"])
        val oldAudit = jsonValue(log["old_values"])
        val newAudit = jsonValue(log["new_values"])
        assertEquals(originalStart.toInstant(), OffsetDateTime.parse(oldAudit.get("startsAt").stringValue()).toInstant())
        assertEquals(newStart.toInstant(), OffsetDateTime.parse(newAudit.get("startsAt").stringValue()).toInstant())
        assertEquals("RESCHEDULE", newAudit.get("operation").stringValue())
        assertTrue(newAudit.get("locationChanged").booleanValue())
        assertFalse(log["old_values"].toString().contains("Domicilio original"))
        assertFalse(log["new_values"].toString().contains("Centro comunitario"))
    }

    @Test
    fun `cancela con motivo libera agenda y devuelve la ultima actuacion a evaluacion`() {
        val start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0)
        val createdResult = create(startsAt = start, location = "Domicilio reservado")
        expect(createdResult, 201)
        val created = json.readValue(createdResult.response.contentAsString, ApplicationVisitResponse::class.java)

        val result = cancel(created.id, reason = "  El ciudadano solicito cambiar la fecha.  ")
        expect(result, 200)
        val cancelled = json.readValue(result.response.contentAsString, ApplicationVisitResponse::class.java)
        assertEquals("CANCELLED", cancelled.status.name)
        assertEquals("El ciudadano solicito cambiar la fecha.", cancelled.cancellationReason)
        assertEquals(fixture.actorId, cancelled.cancelledById)
        assertEquals("Trabajador Social", cancelled.cancelledByName)
        assertNotNull(cancelled.cancelledAt)
        assertEquals(ApplicationStatus.IN_EVALUATION, cancelled.applicationStatus)
        tx { assertEquals(ApplicationStatus.IN_EVALUATION, applications.findById(fixture.applicationId).orElseThrow().status) }

        val listing = mvc.perform(
            get("/api/admin/application-visits")
                .header("Authorization", "Bearer ${fixture.actorToken}")
                .param("applicationId", fixture.applicationId.toString())
                .param("status", "CANCELLED"),
        ).andReturn()
        expect(listing, 200)
        val item = json.readTree(listing.response.contentAsString).get("content").get(0)
        assertEquals("CANCELLED", item.get("status").stringValue())
        assertFalse(item.has("cancellationReason"))

        val detailResult = mvc.perform(
            get("/api/admin/application-visits/${created.id}")
                .header("Authorization", "Bearer ${fixture.actorToken}"),
        ).andReturn()
        expect(detailResult, 200)
        assertEquals(
            "El ciudadano solicito cambiar la fecha.",
            json.readTree(detailResult.response.contentAsString).get("cancellationReason").stringValue(),
        )

        expect(cancel(created.id), 409, "APPLICATION_VISIT_NOT_MODIFIABLE")
        expect(create(startsAt = start, location = "Nueva reserva"), 201)

        val log = jdbc.queryForMap(
            "select old_values, new_values from logs " +
                "where entity_type = 'application_visit' and entity_id = ? and action = 'UPDATE'",
            created.id.toString(),
        )
        val audit = jsonValue(log["new_values"])
        assertEquals("CANCEL", audit.get("operation").stringValue())
        assertTrue(audit.get("cancellationReasonRecorded").booleanValue())
        assertEquals("IN_EVALUATION", audit.get("applicationStatus").stringValue())
        assertFalse(log["new_values"].toString().contains("El ciudadano solicito"))
        assertFalse(log["new_values"].toString().contains("Domicilio reservado"))
    }

    @Test
    fun `registra resultado realizado con profesional fecha textos sensibles y trazabilidad`() {
        val created = json.readValue(create().response.contentAsString, ApplicationVisitResponse::class.java)
        markEnded(created.id)

        val result = recordResult(created.id)
        expect(result, 201)
        val completed = json.readValue(result.response.contentAsString, ApplicationVisitResponse::class.java)
        assertEquals("REALIZED", completed.status.name)
        assertEquals("La familia recibio al profesional.", completed.observations)
        assertEquals("Se requiere continuar con la evaluacion.", completed.conclusions)
        assertEquals(fixture.actorId, completed.resultRecordedById)
        assertEquals("Trabajador Social", completed.resultRecordedByName)
        assertNotNull(completed.resultRecordedAt)
        assertEquals(ZoneOffset.UTC, completed.resultRecordedAt.offset)
        assertEquals(ApplicationStatus.IN_EVALUATION, completed.applicationStatus)

        val listing = mvc.perform(
            get("/api/admin/application-visits")
                .header("Authorization", "Bearer ${fixture.actorToken}")
                .param("applicationId", fixture.applicationId.toString())
                .param("status", "REALIZED"),
        ).andReturn()
        expect(listing, 200)
        val item = json.readTree(listing.response.contentAsString).get("content").get(0)
        assertEquals("REALIZED", item.get("status").stringValue())
        assertEquals(fixture.actorId, item.get("resultRecordedById").longValue())
        assertTrue(item.hasNonNull("resultRecordedAt"))
        assertFalse(item.has("observations"))
        assertFalse(item.has("conclusions"))

        val detailResult = mvc.perform(
            get("/api/admin/application-visits/${created.id}")
                .header("Authorization", "Bearer ${fixture.actorToken}"),
        ).andReturn()
        expect(detailResult, 200)
        val detail = json.readTree(detailResult.response.contentAsString)
        assertEquals("La familia recibio al profesional.", detail.get("observations").stringValue())
        assertEquals("Se requiere continuar con la evaluacion.", detail.get("conclusions").stringValue())
        assertEquals(fixture.actorId, detail.get("resultRecordedById").longValue())

        val log = jdbc.queryForMap(
            "select old_values, new_values from logs " +
                "where entity_type = 'application_visit' and entity_id = ? and action = 'UPDATE'",
            created.id.toString(),
        )
        val audit = jsonValue(log["new_values"])
        assertEquals("RECORD_RESULT", audit.get("operation").stringValue())
        assertEquals("REALIZED", audit.get("status").stringValue())
        assertTrue(audit.get("observationsRecorded").booleanValue())
        assertTrue(audit.get("conclusionsRecorded").booleanValue())
        assertEquals("IN_EVALUATION", audit.get("applicationStatus").stringValue())
        assertFalse(log["old_values"].toString().contains("La familia recibio"))
        assertFalse(log["new_values"].toString().contains("La familia recibio"))
        assertFalse(log["new_values"].toString().contains("continuar con la evaluacion"))
    }

    @Test
    fun `registra no realizada sin textos y conserva visita cuando queda otra pendiente`() {
        val first = json.readValue(create().response.contentAsString, ApplicationVisitResponse::class.java)
        expect(create(startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0)), 201)
        markEnded(first.id)

        val result = recordResult(
            first.id,
            status = "NOT_REALIZED",
            observations = null,
            conclusions = null,
        )
        expect(result, 201)
        val completed = json.readValue(result.response.contentAsString, ApplicationVisitResponse::class.java)
        assertEquals("NOT_REALIZED", completed.status.name)
        assertEquals(null, completed.observations)
        assertEquals(null, completed.conclusions)
        assertEquals(ApplicationStatus.IN_VISIT, completed.applicationStatus)
        tx { assertEquals(ApplicationStatus.IN_VISIT, applications.findById(fixture.applicationId).orElseThrow().status) }
    }

    @Test
    fun `rechaza cuerpos de resultado invalidos y propiedades administradas por el servidor`() {
        val created = json.readValue(create().response.contentAsString, ApplicationVisitResponse::class.java)
        markEnded(created.id)
        val bodies = listOf(
            "{}",
            """{"status":null}""",
            """{"status":"SCHEDULED"}""",
            """{"status":"UNKNOWN"}""",
            """{"status":"REALIZED","observations":"   "}""",
            """{"status":"REALIZED","conclusions":"   "}""",
            json.writeValueAsString(mapOf("status" to "REALIZED", "observations" to "x".repeat(2001))),
            json.writeValueAsString(mapOf("status" to "REALIZED", "conclusions" to "x".repeat(2001))),
            """{"status":"REALIZED","resultRecordedById":${fixture.actorId}}""",
            "{\"status\":",
        )
        bodies.forEach {
            expect(recordResult(created.id, body = it), 400, "APPLICATION_VISIT_RESULT_INVALID_DATA")
        }
        assertEquals("SCHEDULED", tx { visits.findById(created.id).orElseThrow().status.name })
    }

    @Test
    fun `la base exige que el resultado lo registre el responsable y que sus textos sean validos`() {
        val created = json.readValue(create().response.contentAsString, ApplicationVisitResponse::class.java)
        markEnded(created.id)
        assertFailsWith<DataIntegrityViolationException> {
            jdbc.update(
                "update application_visit set status = 'REALIZED', result_recorded_at = ?, " +
                    "result_recorded_by_user_id = ? where id = ?",
                OffsetDateTime.now(ZoneOffset.UTC),
                fixture.applicantId,
                created.id,
            )
        }
        assertFailsWith<DataIntegrityViolationException> {
            jdbc.update(
                "update application_visit set status = 'REALIZED', result_recorded_at = ?, " +
                    "result_recorded_by_user_id = ?, result_observations = '   ' where id = ?",
                OffsetDateTime.now(ZoneOffset.UTC),
                fixture.actorId,
                created.id,
            )
        }
        assertEquals("SCHEDULED", tx { visits.findById(created.id).orElseThrow().status.name })
    }

    @Test
    fun `cancelar una de varias actuaciones conserva la solicitud en visita`() {
        val start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5).withNano(0)
        val first = json.readValue(
            create(startsAt = start).response.contentAsString,
            ApplicationVisitResponse::class.java,
        )
        expect(create(startsAt = start.plusHours(2)), 201)

        val result = cancel(first.id)
        expect(result, 200)
        assertEquals(
            ApplicationStatus.IN_VISIT,
            json.readValue(result.response.contentAsString, ApplicationVisitResponse::class.java).applicationStatus,
        )
        tx { assertEquals(ApplicationStatus.IN_VISIT, applications.findById(fixture.applicationId).orElseThrow().status) }
    }

    @Test
    fun `rechaza datos de gestion invalidos actuaciones no pendientes y rutas inconsistentes`() {
        val start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(6).withNano(0)
        val created = json.readValue(create(startsAt = start).response.contentAsString, ApplicationVisitResponse::class.java)
        val validSchedule = """{"startsAt":"${created.startsAt}","durationMinutes":60,"location":"Domicilio declarado""""
        val scheduleBodies = listOf(
            "{}",
            """{"startsAt":null,"durationMinutes":60,"location":"Lugar"}""",
            """{"startsAt":"${start.plusDays(1)}","durationMinutes":0,"location":"Lugar"}""",
            """{"startsAt":"${start.plusDays(1)}","durationMinutes":60,"location":"   "}""",
            json.writeValueAsString(mapOf("startsAt" to start.plusDays(1), "durationMinutes" to 60, "location" to "x".repeat(301))),
            "$validSchedule,\"type\":\"VISIT\"}",
        )
        scheduleBodies.forEach { expect(reschedule(created.id, body = it), 400, "APPLICATION_VISIT_INVALID_DATA") }
        expect(
            reschedule(created.id, startsAt = created.startsAt, durationMinutes = 60, location = "Domicilio declarado"),
            400,
            "APPLICATION_VISIT_INVALID_DATA",
        )
        expect(reschedule(created.id, startsAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)), 400, "APPLICATION_VISIT_INVALID_DATA")
        expect(
            reschedule(
                created.id,
                startsAt = OffsetDateTime.MAX.minusMinutes(1),
                durationMinutes = Int.MAX_VALUE,
            ),
            400,
            "APPLICATION_VISIT_INVALID_DATA",
        )

        listOf(
            "{}",
            """{"reason":null}""",
            """{"reason":"   "}""",
            json.writeValueAsString(mapOf("reason" to "x".repeat(1001))),
            """{"reason":"Motivo","status":"CANCELLED"}""",
        ).forEach { expect(cancel(created.id, body = it), 400, "APPLICATION_VISIT_INVALID_DATA") }

        expect(reschedule(created.id, applicationId = UUID.randomUUID()), 404, "APPLICATION_VISIT_NOT_FOUND")
        expect(reschedule(UUID.randomUUID()), 404, "APPLICATION_VISIT_NOT_FOUND")
        expect(
            reschedule(UUID.randomUUID(), applicationId = UUID.randomUUID()),
            404,
            "APPLICATION_NOT_FOUND",
        )

        tx {
            val stored = visits.findById(created.id).orElseThrow()
            stored.startsAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2)
            stored.endsAt = stored.startsAt.plusHours(1)
        }
        expect(cancel(created.id), 409, "APPLICATION_VISIT_NOT_MODIFIABLE")
    }

    @Test
    fun `gestion exige autenticacion permiso vigente usuario activo rol y asignacion`() {
        val created = json.readValue(create().response.contentAsString, ApplicationVisitResponse::class.java)
        expect(reschedule(created.id, token = null), 401, "AUTH_UNAUTHENTICATED")
        expect(reschedule(created.id, token = "invalid"), 401, "AUTH_INVALID_TOKEN")
        expect(reschedule(created.id, token = fixture.unauthorizedToken), 403, "AUTH_FORBIDDEN")

        tx {
            roles.findById(fixture.actorRoleId).orElseThrow().permissions.removeIf {
                it.name == "applications:visits:manage"
            }
        }
        expect(reschedule(created.id), 403, "AUTH_FORBIDDEN")
        tx { roles.findById(fixture.actorRoleId).orElseThrow().permissions.add(permission("applications:visits:manage")) }

        tx { users.findById(fixture.actorId).orElseThrow().active = false }
        expect(cancel(created.id), 401, "AUTH_UNAUTHENTICATED")
        tx { users.findById(fixture.actorId).orElseThrow().active = true }

        tx { users.findById(fixture.actorId).orElseThrow().roles.clear() }
        expect(cancel(created.id), 403, "AUTH_FORBIDDEN")
        tx {
            users.findById(fixture.actorId).orElseThrow().roles.add(roles.findById(fixture.actorRoleId).orElseThrow())
        }

        val other = tx { users.saveAndFlush(User(name = "Otro", email = "other-${UUID.randomUUID()}@example.com")) }
        tx { applications.findById(fixture.applicationId).orElseThrow().assignedWorker = other }
        expect(cancel(created.id), 409, "APPLICATION_VISIT_ASSIGNED_TO_ANOTHER_WORKER")
    }

    @Test
    fun `reprogramacion detecta conflictos excluye la propia actuacion y permite limites`() {
        val start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7).withNano(0)
        val first = json.readValue(create(startsAt = start).response.contentAsString, ApplicationVisitResponse::class.java)
        expect(create(startsAt = start.plusHours(2)), 201)

        expect(
            reschedule(first.id, startsAt = start.plusMinutes(90), durationMinutes = 60),
            409,
            "APPLICATION_VISIT_RESPONSIBLE_CONFLICT",
        )
        expect(
            reschedule(first.id, startsAt = start, durationMinutes = 60, location = "Lugar actualizado"),
            200,
        )
        expect(
            reschedule(first.id, startsAt = start.plusHours(1), durationMinutes = 60, location = "Limite contiguo"),
            200,
        )

        val secondWorker = worker("Segundo trabajador")
        val otherApplication = newApplication(
            status = ApplicationStatus.IN_EVALUATION,
            actorId = secondWorker.first,
            applicantId = fixture.applicantId,
            withEvaluation = true,
        )
        val otherVisit = json.readValue(
            create(
                applicationId = otherApplication,
                token = secondWorker.second,
                startsAt = start.plusHours(4),
            ).response.contentAsString,
            ApplicationVisitResponse::class.java,
        )
        expect(
            reschedule(
                visitId = otherVisit.id,
                applicationId = otherApplication,
                token = secondWorker.second,
                startsAt = start.plusHours(1).plusMinutes(30),
                durationMinutes = 30,
            ),
            409,
            "APPLICATION_VISIT_APPLICANT_CONFLICT",
        )
    }

    @Test
    fun `rechaza cuerpos invalidos y campos administrados por el servidor`() {
        val future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).withNano(0)
        val valid = """{"type":"VISIT","startsAt":"$future","durationMinutes":60,"location":"Lugar""""
        val bodies = listOf(
            "{}",
            """{"type":null,"startsAt":"$future","durationMinutes":60,"location":"Lugar"}""",
            """{"type":"UNKNOWN","startsAt":"$future","durationMinutes":60,"location":"Lugar"}""",
            """{"type":"VISIT","startsAt":null,"durationMinutes":60,"location":"Lugar"}""",
            """{"type":"VISIT","startsAt":"$future","durationMinutes":0,"location":"Lugar"}""",
            """{"type":"VISIT","startsAt":"$future","durationMinutes":60,"location":"   "}""",
            json.writeValueAsString(mapOf("type" to "VISIT", "startsAt" to future, "durationMinutes" to 60, "location" to "x".repeat(301))),
            "$valid,\"responsibleId\":${fixture.actorId}}",
            "$valid,\"status\":\"SCHEDULED\"}",
            "$valid,\"endsAt\":\"${future.plusHours(1)}\"}",
        )
        bodies.forEach { expect(create(body = it), 400, "APPLICATION_VISIT_INVALID_DATA") }
        expect(create(startsAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)), 400, "APPLICATION_VISIT_INVALID_DATA")
        assertEquals(0L, visits.countByApplicationId(fixture.applicationId))
    }

    @Test
    fun `exige autenticacion permiso vigente usuario activo y rol asignado`() {
        expect(create(token = null), 401, "AUTH_UNAUTHENTICATED")
        expect(create(token = "invalid"), 401, "AUTH_INVALID_TOKEN")
        expect(create(token = fixture.unauthorizedToken), 403, "AUTH_FORBIDDEN")

        tx { users.findById(fixture.actorId).orElseThrow().active = false }
        expect(create(), 401, "AUTH_UNAUTHENTICATED")
        tx { users.findById(fixture.actorId).orElseThrow().active = true }

        tx {
            roles.findById(fixture.actorRoleId).orElseThrow().permissions.removeIf {
                it.name == "applications:visits:create"
            }
        }
        expect(create(), 403, "AUTH_FORBIDDEN")

        tx { users.findById(fixture.actorId).orElseThrow().roles.clear() }
        expect(create(), 403, "AUTH_FORBIDDEN")
    }

    @Test
    fun `exige evaluacion etapa permitida y trabajador asignado`() {
        val withoutEvaluation = newApplication(ApplicationStatus.IN_EVALUATION, actorId = fixture.actorId, withEvaluation = false)
        expect(create(applicationId = withoutEvaluation), 409, "APPLICATION_VISIT_EVALUATION_REQUIRED")

        val submitted = newApplication(ApplicationStatus.SUBMITTED, actorId = fixture.actorId, withEvaluation = true)
        expect(create(applicationId = submitted), 409, "APPLICATION_VISIT_STATUS_NOT_ALLOWED")

        val other = tx { users.saveAndFlush(User(name = "Otro", email = "other-${UUID.randomUUID()}@example.com")) }
        val assignedToOther = newApplication(ApplicationStatus.IN_EVALUATION, actorId = requireNotNull(other.id), withEvaluation = true)
        expect(create(applicationId = assignedToOther), 409, "APPLICATION_VISIT_ASSIGNED_TO_ANOTHER_WORKER")

        expect(create(applicationId = UUID.randomUUID()), 404, "APPLICATION_NOT_FOUND")
    }

    @Test
    fun `detecta conflictos del responsable y del ciudadano y permite limites contiguos`() {
        val start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5).withNano(0)
        expect(create(startsAt = start, durationMinutes = 60), 201)
        expect(create(startsAt = start.plusMinutes(30), durationMinutes = 30), 409, "APPLICATION_VISIT_RESPONSIBLE_CONFLICT")
        expect(create(startsAt = start.plusMinutes(60), durationMinutes = 30), 201)

        val secondWorker = worker("Segundo trabajador")
        val sameApplicantApplication = newApplication(
            status = ApplicationStatus.IN_EVALUATION,
            actorId = secondWorker.first,
            applicantId = fixture.applicantId,
            withEvaluation = true,
        )
        expect(
            create(
                applicationId = sameApplicantApplication,
                token = secondWorker.second,
                startsAt = start.plusMinutes(15),
                durationMinutes = 30,
            ),
            409,
            "APPLICATION_VISIT_APPLICANT_CONFLICT",
        )
    }

    @Test
    fun `visitas y turnos comparten ocupacion de agenda en ambos sentidos`() {
        val agenda = appointmentAgenda()
        val appointmentStart = agenda.date.atTime(9, 0).atZone(agenda.zone).toOffsetDateTime()
        val appointmentEnd = appointmentStart.plusHours(1)
        val appointmentId = tx {
            requireNotNull(appointments.saveAndFlush(
                Appointment(
                    citizen = users.getReferenceById(fixture.applicantId),
                    professionalAssignment = assignments.getReferenceById(agenda.assignmentId),
                    startsAt = appointmentStart,
                    endsAt = appointmentEnd,
                    idempotencyKey = "fixture-${UUID.randomUUID()}",
                    requestHash = "0".repeat(64),
                ),
            ).id)
        }

        expect(
            create(startsAt = appointmentStart.plusMinutes(30), durationMinutes = 30),
            409,
            "APPLICATION_VISIT_RESPONSIBLE_CONFLICT",
        )

        val visitStart = appointmentEnd
        val visitResult = create(startsAt = visitStart, durationMinutes = 60)
        expect(visitResult, 201)
        val visit = json.readValue(visitResult.response.contentAsString, ApplicationVisitResponse::class.java)

        val slotsResult = mvc.perform(
            get("/api/citizen/appointment-slots")
                .header("Authorization", "Bearer ${agenda.citizenToken}")
                .param("centerServiceId", agenda.centerServiceId.toString())
                .param("date", agenda.date.toString()),
        ).andReturn()
        expect(slotsResult, 200)
        val slots = json.readTree(slotsResult.response.contentAsString)
        assertTrue(slots.none { it.get("startsAt").textValue() == visitStart.toString() })

        val adminAvailability = mvc.perform(
            get("/api/admin/appointments/day-availability")
                .header("Authorization", "Bearer ${agenda.adminToken}")
                .param("centerId", agenda.centerId.toString())
                .param("date", agenda.date.toString()),
        ).andReturn()
        expect(adminAvailability, 200)
        assertTrue(json.readTree(adminAvailability.response.contentAsString).none {
            it.get("startsAt").textValue() == visitStart.toString()
        })

        val appointmentResult = mvc.perform(
            post("/api/citizen/appointments")
                .header("Authorization", "Bearer ${agenda.citizenToken}")
                .header("Idempotency-Key", "visit-conflict-${UUID.randomUUID()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(mapOf(
                    "professionalAssignmentId" to agenda.assignmentId,
                    "startsAt" to visitStart,
                    "endsAt" to visitStart.plusHours(1),
                ))),
        ).andReturn()
        expect(appointmentResult, 409, "APPOINTMENT_CITIZEN_OVERLAP")

        expect(
            reschedule(
                visit.id,
                startsAt = appointmentStart.plusMinutes(30),
                durationMinutes = 30,
            ),
            409,
            "APPLICATION_VISIT_RESPONSIBLE_CONFLICT",
        )

        val rescheduleResult = mvc.perform(
            put("/api/admin/appointments/$appointmentId/schedule")
                .header("Authorization", "Bearer ${agenda.adminToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(mapOf(
                    "professionalAssignmentId" to agenda.assignmentId,
                    "startsAt" to visitStart,
                    "endsAt" to visitStart.plusHours(1),
                ))),
        ).andReturn()
        expect(rescheduleResult, 409, "APPOINTMENT_CITIZEN_OVERLAP")

        expect(cancel(visit.id), 200)
        val appointmentAfterCancellation = mvc.perform(
            post("/api/citizen/appointments")
                .header("Authorization", "Bearer ${agenda.citizenToken}")
                .header("Idempotency-Key", "released-visit-${UUID.randomUUID()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(mapOf(
                    "professionalAssignmentId" to agenda.assignmentId,
                    "startsAt" to visitStart,
                    "endsAt" to visitStart.plusHours(1),
                ))),
        ).andReturn()
        expect(appointmentAfterCancellation, 201)
    }

    @Test
    fun `dos altas concurrentes dejan una sola actuacion en el horario`() {
        val start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(6).withNano(0)
        val ready = CountDownLatch(2)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures = (1..2).map {
                pool.submit<MvcResult> {
                    ready.countDown()
                    assertTrue(release.await(5, TimeUnit.SECONDS))
                    create(startsAt = start)
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            release.countDown()
            val results = futures.map { it.get(15, TimeUnit.SECONDS) }
            assertEquals(listOf(201, 409), results.map { it.response.status }.sorted())
            assertEquals(1L, visits.countByApplicationId(fixture.applicationId))
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `un fallo de auditoria revierte actuacion y cambio de estado`() {
        jdbc.execute(
            "alter table logs add constraint ck_visit_test_audit_failure " +
                "check (entity_type <> 'application_visit' or user_id <> ${fixture.actorId})",
        )
        try {
            expect(create(), 409, "DATA_INTEGRITY_VIOLATION")
            assertEquals(0L, visits.countByApplicationId(fixture.applicationId))
            tx { assertEquals(ApplicationStatus.IN_EVALUATION, applications.findById(fixture.applicationId).orElseThrow().status) }
        } finally {
            jdbc.execute("alter table logs drop constraint ck_visit_test_audit_failure")
        }
    }

    @Test
    fun `un fallo de auditoria revierte reprogramacion cancelacion y estado`() {
        val start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(8).withNano(0)
        val created = json.readValue(create(startsAt = start).response.contentAsString, ApplicationVisitResponse::class.java)
        jdbc.execute(
            "alter table logs add constraint ck_visit_management_audit_failure " +
                "check (entity_type <> 'application_visit' or action <> 'UPDATE' or user_id <> ${fixture.actorId})",
        )
        try {
            expect(
                reschedule(created.id, startsAt = start.plusHours(2), location = "Otro lugar"),
                409,
                "DATA_INTEGRITY_VIOLATION",
            )
            tx {
                val stored = visits.findById(created.id).orElseThrow()
                assertEquals(start.toInstant(), stored.startsAt.toInstant())
                assertEquals("Domicilio declarado", stored.location)
            }

            expect(cancel(created.id, reason = "Motivo que debe revertirse"), 409, "DATA_INTEGRITY_VIOLATION")
            tx {
                val stored = visits.findById(created.id).orElseThrow()
                assertEquals("SCHEDULED", stored.status.name)
                assertEquals(null, stored.cancellationReason)
                assertEquals(ApplicationStatus.IN_VISIT, applications.findById(fixture.applicationId).orElseThrow().status)
            }
        } finally {
            jdbc.execute("alter table logs drop constraint ck_visit_management_audit_failure")
        }
    }

    @Test
    fun `dos cancelaciones concurrentes aplican el cambio una sola vez`() {
        val created = json.readValue(create().response.contentAsString, ApplicationVisitResponse::class.java)
        val ready = CountDownLatch(2)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures = (1..2).map {
                pool.submit<MvcResult> {
                    ready.countDown()
                    assertTrue(release.await(5, TimeUnit.SECONDS))
                    cancel(created.id)
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            release.countDown()
            val results = futures.map { it.get(15, TimeUnit.SECONDS) }
            assertEquals(listOf(200, 409), results.map { it.response.status }.sorted())
            val conflictCode = results.first { it.response.status == 409 }.let {
                json.readTree(it.response.contentAsString).get("code").stringValue()
            }
            assertTrue(
                conflictCode in setOf(
                    "APPLICATION_VISIT_NOT_MODIFIABLE",
                    "APPLICATION_VISIT_CONCURRENT_MODIFICATION",
                ),
            )
            assertEquals(
                1,
                jdbc.queryForObject(
                    "select count(*) from logs where entity_type = 'application_visit' and entity_id = ? and action = 'UPDATE'",
                    Int::class.java,
                    created.id.toString(),
                ),
            )
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `resultado exige horario finalizado estado solicitud responsable y ruta consistente`() {
        val future = json.readValue(create().response.contentAsString, ApplicationVisitResponse::class.java)
        expect(recordResult(future.id), 409, "APPLICATION_VISIT_RESULT_NOT_REGISTRABLE")

        markEnded(future.id)
        expect(recordResult(future.id, applicationId = UUID.randomUUID()), 404, "APPLICATION_VISIT_NOT_FOUND")
        expect(recordResult(UUID.randomUUID()), 404, "APPLICATION_VISIT_NOT_FOUND")
        expect(recordResult(UUID.randomUUID(), applicationId = UUID.randomUUID()), 404, "APPLICATION_NOT_FOUND")

        val other = tx { users.saveAndFlush(User(name = "Otro", email = "other-${UUID.randomUUID()}@example.com")) }
        tx { applications.findById(fixture.applicationId).orElseThrow().assignedWorker = other }
        expect(recordResult(future.id), 409, "APPLICATION_VISIT_ASSIGNED_TO_ANOTHER_WORKER")
        tx { applications.findById(fixture.applicationId).orElseThrow().assignedWorker = users.getReferenceById(fixture.actorId) }

        expect(recordResult(future.id), 201)
        expect(recordResult(future.id), 409, "APPLICATION_VISIT_RESULT_NOT_REGISTRABLE")

        val cancelledApplication = newApplication(
            status = ApplicationStatus.IN_EVALUATION,
            actorId = fixture.actorId,
            withEvaluation = true,
        )
        val cancelled = json.readValue(
            create(applicationId = cancelledApplication).response.contentAsString,
            ApplicationVisitResponse::class.java,
        )
        expect(cancel(cancelled.id, applicationId = cancelledApplication), 200)
        expect(
            recordResult(cancelled.id, applicationId = cancelledApplication),
            409,
            "APPLICATION_VISIT_RESULT_NOT_REGISTRABLE",
        )
    }

    @Test
    fun `resultado exige autenticacion permiso vigente usuario activo rol y asignacion`() {
        val created = json.readValue(create().response.contentAsString, ApplicationVisitResponse::class.java)
        markEnded(created.id)
        expect(recordResult(created.id, token = null), 401, "AUTH_UNAUTHENTICATED")
        expect(recordResult(created.id, token = "invalid"), 401, "AUTH_INVALID_TOKEN")
        expect(recordResult(created.id, token = fixture.unauthorizedToken), 403, "AUTH_FORBIDDEN")

        tx {
            roles.findById(fixture.actorRoleId).orElseThrow().permissions.removeIf {
                it.name == "applications:visits:results:create"
            }
        }
        expect(recordResult(created.id), 403, "AUTH_FORBIDDEN")
        tx {
            roles.findById(fixture.actorRoleId).orElseThrow().permissions.add(
                permission("applications:visits:results:create"),
            )
        }

        tx { users.findById(fixture.actorId).orElseThrow().active = false }
        expect(recordResult(created.id), 401, "AUTH_UNAUTHENTICATED")
        tx { users.findById(fixture.actorId).orElseThrow().active = true }

        tx { users.findById(fixture.actorId).orElseThrow().roles.clear() }
        expect(recordResult(created.id), 403, "AUTH_FORBIDDEN")
    }

    @Test
    fun `dos resultados concurrentes finalizan la actuacion una sola vez`() {
        val created = json.readValue(create().response.contentAsString, ApplicationVisitResponse::class.java)
        markEnded(created.id)
        val ready = CountDownLatch(2)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures = (1..2).map {
                pool.submit<MvcResult> {
                    ready.countDown()
                    assertTrue(release.await(5, TimeUnit.SECONDS))
                    recordResult(created.id)
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            release.countDown()
            val results = futures.map { it.get(15, TimeUnit.SECONDS) }
            assertEquals(listOf(201, 409), results.map { it.response.status }.sorted())
            val conflictCode = results.first { it.response.status == 409 }.let {
                json.readTree(it.response.contentAsString).get("code").stringValue()
            }
            assertTrue(
                conflictCode in setOf(
                    "APPLICATION_VISIT_RESULT_NOT_REGISTRABLE",
                    "APPLICATION_VISIT_CONCURRENT_MODIFICATION",
                ),
            )
            assertEquals(
                1,
                jdbc.queryForObject(
                    "select count(*) from logs where entity_type = 'application_visit' " +
                        "and entity_id = ? and action = 'UPDATE'",
                    Int::class.java,
                    created.id.toString(),
                ),
            )
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `un fallo de auditoria revierte resultado y transicion de solicitud`() {
        val created = json.readValue(create().response.contentAsString, ApplicationVisitResponse::class.java)
        markEnded(created.id)
        jdbc.execute(
            "alter table logs add constraint ck_visit_result_audit_failure " +
                "check (entity_type <> 'application_visit' or action <> 'UPDATE' or user_id <> ${fixture.actorId})",
        )
        try {
            expect(recordResult(created.id), 409, "DATA_INTEGRITY_VIOLATION")
            tx {
                val stored = visits.findById(created.id).orElseThrow()
                assertEquals("SCHEDULED", stored.status.name)
                assertEquals(null, stored.resultObservations)
                assertEquals(null, stored.resultConclusions)
                assertEquals(null, stored.resultRecordedAt)
                assertEquals(null, stored.resultRecordedBy)
                assertEquals(ApplicationStatus.IN_VISIT, applications.findById(fixture.applicationId).orElseThrow().status)
            }
        } finally {
            jdbc.execute("alter table logs drop constraint ck_visit_result_audit_failure")
        }
    }

    @Test
    fun `rechaza ventana invertida y detalle inexistente`() {
        val invalidRange = mvc.perform(
            get("/api/admin/application-visits")
                .header("Authorization", "Bearer ${fixture.actorToken}")
                .param("windowStart", "2026-10-21T00:00:00Z")
                .param("windowEnd", "2026-10-20T00:00:00Z"),
        ).andReturn()
        expect(invalidRange, 400, "APPLICATION_VISIT_INVALID_FILTER")
        val invalidPage = mvc.perform(
            get("/api/admin/application-visits")
                .header("Authorization", "Bearer ${fixture.actorToken}")
                .param("page", "-1"),
        ).andReturn()
        expect(invalidPage, 400, "APPLICATION_VISIT_INVALID_FILTER")
        val invalidType = mvc.perform(
            get("/api/admin/application-visits")
                .header("Authorization", "Bearer ${fixture.actorToken}")
                .param("type", "UNKNOWN"),
        ).andReturn()
        expect(invalidType, 400, "APPLICATION_VISIT_INVALID_FILTER")
        val invalidStatus = mvc.perform(
            get("/api/admin/application-visits")
                .header("Authorization", "Bearer ${fixture.actorToken}")
                .param("status", "UNKNOWN"),
        ).andReturn()
        expect(invalidStatus, 400, "APPLICATION_VISIT_INVALID_FILTER")
        val missing = mvc.perform(
            get("/api/admin/application-visits/${UUID.randomUUID()}")
                .header("Authorization", "Bearer ${fixture.actorToken}"),
        ).andReturn()
        expect(missing, 404, "APPLICATION_VISIT_NOT_FOUND")
    }

    private fun worker(name: String): Pair<Long, String> = tx {
        val role = roles.save(
            Role(
                name = "WORKER_${UUID.randomUUID()}",
                permissions = mutableSetOf(
                    permission("applications:visits:create"),
                    permission("applications:visits:view"),
                    permission("applications:visits:manage"),
                    permission("applications:visits:results:create"),
                ),
            ),
        )
        val user = users.saveAndFlush(
            User(name = name, email = "worker-${UUID.randomUUID()}@example.com", roles = mutableSetOf(role)),
        )
        requireNotNull(user.id) to jwt.createToken(user, role)
    }

    data class AppointmentAgenda(
        val assignmentId: UUID,
        val centerId: UUID,
        val centerServiceId: UUID,
        val citizenToken: String,
        val adminToken: String,
        val date: LocalDate,
        val zone: ZoneId,
    )

    private fun appointmentAgenda(): AppointmentAgenda = tx {
        val citizenRole = roles.findAll().firstOrNull { it.name == "CIUDADANO" } ?: roles.save(
            Role(
                name = "CIUDADANO",
                permissions = mutableSetOf(
                    permission("appointments:own:view"),
                    permission("appointments:own:create"),
                ),
            ),
        )
        citizenRole.permissions.add(permission("appointments:own:view"))
        citizenRole.permissions.add(permission("appointments:own:create"))
        val professionalRole = roles.findAll().firstOrNull { it.name == "PROFESIONAL_CENTRO" }
            ?: roles.save(Role(name = "PROFESIONAL_CENTRO"))
        val adminRole = roles.findAll().firstOrNull { it.name == "ADMIN" }
            ?: roles.save(Role(name = "ADMIN"))
        adminRole.permissions.add(permission("appointments:management:view"))
        adminRole.permissions.add(permission("appointments:management:manage"))
        val citizen = users.findById(fixture.applicantId).orElseThrow().also { it.roles.add(citizenRole) }
        val professional = users.findById(fixture.actorId).orElseThrow().also { it.roles.add(professionalRole) }
        val admin = users.saveAndFlush(
            User(name = "Administrador", email = "admin-${UUID.randomUUID()}@example.com", roles = mutableSetOf(adminRole)),
        )
        roles.saveAll(listOf(citizenRole, adminRole))
        users.saveAllAndFlush(listOf(citizen, professional))

        val suffix = UUID.randomUUID()
        val center = centers.saveAndFlush(
            MunicipalCenter(name = "Centro $suffix", address = "Calle 123"),
        )
        val service = municipalServices.saveAndFlush(
            MunicipalService(name = "Servicio $suffix", description = "Atencion", durationMinutes = 60),
        )
        val centerService = centerServices.saveAndFlush(CenterService(center = center, service = service))
        val assignment = assignments.saveAndFlush(
            ProfessionalAssignment(professional = professional, centerService = centerService),
        )
        val zone = ZoneId.of("America/Argentina/Buenos_Aires")
        val date = LocalDate.now(zone).plusDays(8)
        availabilities.saveAndFlush(
            ProfessionalAvailability(
                assignment = assignment,
                dayOfWeek = date.dayOfWeek,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(12, 0),
            ),
        )
        AppointmentAgenda(
            assignmentId = requireNotNull(assignment.id),
            centerId = requireNotNull(center.id),
            centerServiceId = requireNotNull(centerService.id),
            citizenToken = jwt.createToken(citizen, citizenRole),
            adminToken = jwt.createToken(admin, adminRole),
            date = date,
            zone = zone,
        )
    }

    private fun newApplication(
        status: ApplicationStatus,
        actorId: Long,
        applicantId: Long? = null,
        withEvaluation: Boolean,
    ): UUID = tx {
        val actor = users.findById(actorId).orElseThrow()
        val applicant = applicantId?.let { users.findById(it).orElseThrow() }
            ?: users.saveAndFlush(User(name = "Titular", email = "applicant-${UUID.randomUUID()}@example.com"))
        val period = periods.saveAndFlush(
            EnrollmentPeriod(
                programEdition = editions.getReferenceById(fixture.editionId),
                openDate = LocalDate.now().minusDays(10),
                closeDate = LocalDate.now().plusDays(10),
                status = EnrollmentPeriodStatus.OPEN,
            ),
        )
        val application = applications.saveAndFlush(
            Application(
                user = applicant,
                registeredBy = applicant,
                programEdition = editions.getReferenceById(fixture.editionId),
                enrollmentPeriod = period,
                status = status,
                assignedWorker = actor,
                submittedAt = LocalDateTime.now().minusDays(1),
            ),
        )
        if (withEvaluation) {
            evaluations.saveAndFlush(
                ApplicationEvaluation(
                    application = application,
                    vulnerability = VulnerabilityLevel.MEDIUM,
                    priority = EvaluationPriority.MEDIUM,
                    recommendation = "Programar actuacion.",
                    professional = actor,
                    evaluatedAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1),
                ),
            )
        }
        requireNotNull(application.id)
    }

    private fun jsonValue(raw: Any?): JsonNode {
        val first = json.readTree(if (raw is ByteArray) raw.toString(Charsets.UTF_8) else raw.toString())
        return if (first.isTextual) json.readTree(first.stringValue()) else first
    }
}
