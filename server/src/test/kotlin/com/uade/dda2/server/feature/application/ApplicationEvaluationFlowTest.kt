package com.uade.dda2.server.feature.application

import com.uade.dda2.server.feature.application.dto.response.ApplicationEvaluationResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationEvaluationDetailResponse
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.EvaluationPriority
import com.uade.dda2.server.feature.application.entity.VulnerabilityLevel
import com.uade.dda2.server.feature.application.repository.ApplicationEvaluationRepository
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:application-evaluations;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ApplicationEvaluationFlowTest {
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
    @Autowired lateinit var transactions: PlatformTransactionManager
    @Autowired lateinit var jdbc: JdbcTemplate

    data class Fixture(
        val applicationId: UUID,
        val actorId: Long,
        val actorRoleId: Long,
        val actorToken: String,
        val unauthorizedToken: String,
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

    private fun evaluationPermission(): Permission =
        permissions.findAll().firstOrNull { it.name == "applications:evaluations:create" }
            ?: permissions.save(Permission(name = "applications:evaluations:create"))

    private fun evaluationViewPermission(): Permission =
        permissions.findAll().firstOrNull { it.name == "applications:evaluations:view" }
            ?: permissions.save(Permission(name = "applications:evaluations:view"))

    private fun newFixture(): Fixture = tx {
        val permission = evaluationPermission()
        val viewPermission = evaluationViewPermission()
        val actorRole = roles.save(
            Role(
                name = "EVALUADOR_${UUID.randomUUID()}",
                permissions = mutableSetOf(permission, viewPermission),
            ),
        )
        val unauthorizedRole = roles.save(Role(name = "SIN_EVALUAR_${UUID.randomUUID()}"))
        val actor = users.saveAndFlush(
            User(name = "Profesional Social", email = "professional-${UUID.randomUUID()}@example.com", roles = mutableSetOf(actorRole)),
        )
        val unauthorized = users.saveAndFlush(
            User(name = "Sin permiso", email = "unauthorized-${UUID.randomUUID()}@example.com", roles = mutableSetOf(unauthorizedRole)),
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
                submittedAt = LocalDateTime.now().minusDays(1),
            ),
        )
        Fixture(
            applicationId = requireNotNull(application.id),
            actorId = requireNotNull(actor.id),
            actorRoleId = requireNotNull(actorRole.id),
            actorToken = jwt.createToken(actor, actorRole),
            unauthorizedToken = jwt.createToken(unauthorized, unauthorizedRole),
            periodId = requireNotNull(period.id),
            editionId = requireNotNull(edition.id),
            programId = requireNotNull(program.id),
        )
    }

    private fun newApplication(
        status: ApplicationStatus = ApplicationStatus.SUBMITTED,
        assignedWorker: User? = null,
    ): UUID = tx {
        val applicant = users.saveAndFlush(User(name = "Titular", email = "applicant-${UUID.randomUUID()}@example.com"))
        requireNotNull(
            applications.saveAndFlush(
                Application(
                    user = applicant,
                    registeredBy = applicant,
                    programEdition = editions.getReferenceById(fixture.editionId),
                    enrollmentPeriod = periods.getReferenceById(fixture.periodId),
                    status = status,
                    assignedWorker = assignedWorker,
                    submittedAt = LocalDateTime.now().minusDays(1),
                ),
            ).id,
        )
    }

    private fun create(
        applicationId: UUID = fixture.applicationId,
        token: String? = fixture.actorToken,
        body: String = validBody(),
    ): MvcResult {
        val request = post("/api/applications/$applicationId/evaluation")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }

    private fun list(token: String? = fixture.actorToken): MvcResult {
        val request = get("/api/admin/application-evaluations")
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }

    private fun detail(evaluationId: UUID, token: String? = fixture.actorToken): MvcResult {
        val request = get("/api/admin/application-evaluations/$evaluationId")
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }

    private fun validBody(recommendation: String = "Se recomienda intervencion prioritaria."): String =
        json.writeValueAsString(
            mapOf(
                "vulnerability" to "HIGH",
                "priority" to "URGENT",
                "recommendation" to recommendation,
            ),
        )

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").stringValue())
    }

    @Test
    fun `registra evaluacion responsable fecha asignacion estado y auditoria sin recomendacion`() {
        val previousUpdatedAt = tx { applications.findById(fixture.applicationId).orElseThrow().updatedAt }
        val result = create(body = validBody("  Se recomienda intervencion prioritaria.  "))
        expect(result, 201)
        val response = json.readValue(result.response.contentAsString, ApplicationEvaluationResponse::class.java)
        assertEquals(fixture.applicationId, response.applicationId)
        assertEquals(VulnerabilityLevel.HIGH, response.vulnerability)
        assertEquals(EvaluationPriority.URGENT, response.priority)
        assertEquals("Se recomienda intervencion prioritaria.", response.recommendation)
        assertEquals(fixture.actorId, response.professionalId)
        assertEquals("Profesional Social", response.professionalName)
        assertEquals(ZoneOffset.UTC, response.evaluatedAt.offset)

        tx {
            val stored = assertNotNull(evaluations.findByApplicationId(fixture.applicationId))
            assertEquals(response.id, stored.id)
            assertEquals(fixture.actorId, stored.professional.id)
            val application = applications.findById(fixture.applicationId).orElseThrow()
            assertEquals(ApplicationStatus.IN_EVALUATION, application.status)
            assertEquals(fixture.actorId, application.assignedWorker?.id)
            assertTrue(application.updatedAt.isAfter(previousUpdatedAt))
        }

        val log = jdbc.queryForMap(
            "select user_id, action, new_values from logs where entity_type = 'application_evaluation' and entity_id = ?",
            response.id.toString(),
        )
        assertEquals(fixture.actorId, (log["user_id"] as Number).toLong())
        assertEquals("CREATE", log["action"])
        val audit = jsonValue(log["new_values"])
        assertEquals("HIGH", audit.get("vulnerability").stringValue())
        assertEquals("URGENT", audit.get("priority").stringValue())
        assertEquals("SUBMITTED", audit.get("previousApplicationStatus").stringValue())
        assertEquals("IN_EVALUATION", audit.get("applicationStatus").stringValue())
        assertFalse(audit.has("recommendation"))
        assertFalse(log["new_values"].toString().contains(response.recommendation))
    }

    @Test
    fun `lista evaluaciones con filtros y obtiene el detalle completo`() {
        val createdResult = create()
        expect(createdResult, 201)
        val created = json.readValue(createdResult.response.contentAsString, ApplicationEvaluationResponse::class.java)
        val (applicationNumber, applicantUserId) = tx {
            val application = applications.findById(fixture.applicationId).orElseThrow()
            requireNotNull(application.applicationNumber) to requireNotNull(application.user.id)
        }

        val listing = mvc.perform(
            get("/api/admin/application-evaluations")
                .header("Authorization", "Bearer ${fixture.actorToken}")
                .param("applicationId", fixture.applicationId.toString())
                .param("applicationNumber", applicationNumber.toString())
                .param("applicantUserId", applicantUserId.toString())
                .param("professionalId", fixture.actorId.toString())
                .param("programId", fixture.programId.toString())
                .param("programEditionId", fixture.editionId.toString())
                .param("vulnerability", "HIGH")
                .param("priority", "URGENT")
                .param("evaluatedFrom", created.evaluatedAt.minusSeconds(1).toString())
                .param("evaluatedTo", created.evaluatedAt.plusSeconds(1).toString()),
        ).andReturn()
        expect(listing, 200)
        val page = json.readTree(listing.response.contentAsString)
        assertEquals(1L, page.get("totalElements").longValue())
        assertEquals(1, page.get("content").size())
        val item = page.get("content").get(0)
        assertEquals(created.id.toString(), item.get("id").stringValue())
        assertEquals(applicationNumber, item.get("applicationNumber").longValue())
        assertEquals("IN_EVALUATION", item.get("applicationStatus").stringValue())
        assertEquals("HIGH", item.get("vulnerability").stringValue())
        assertEquals("URGENT", item.get("priority").stringValue())
        assertFalse(item.has("recommendation"))

        val withoutMatches = mvc.perform(
            get("/api/admin/application-evaluations")
                .header("Authorization", "Bearer ${fixture.actorToken}")
                .param("priority", "LOW"),
        ).andReturn()
        expect(withoutMatches, 200)
        assertEquals(0L, json.readTree(withoutMatches.response.contentAsString).get("totalElements").longValue())

        val detailResult = detail(created.id)
        expect(detailResult, 200)
        val detail = json.readValue(detailResult.response.contentAsString, ApplicationEvaluationDetailResponse::class.java)
        assertEquals(created.id, detail.id)
        assertEquals(fixture.applicationId, detail.applicationId)
        assertEquals(applicationNumber, detail.applicationNumber)
        assertEquals(applicantUserId, detail.applicantUserId)
        assertEquals(fixture.programId, detail.programId)
        assertEquals(fixture.editionId, detail.programEditionId)
        assertEquals(fixture.periodId, detail.enrollmentPeriodId)
        assertEquals(created.recommendation, detail.recommendation)
        assertEquals(fixture.actorId, detail.professionalId)
        assertEquals(created.evaluatedAt, detail.evaluatedAt)
    }

    @Test
    fun `protege las consultas con permiso vigente usuario activo y rol asignado`() {
        expect(list(token = null), 401, "AUTH_UNAUTHENTICATED")
        expect(list(token = "invalid"), 401, "AUTH_INVALID_TOKEN")
        expect(list(token = fixture.unauthorizedToken), 403, "AUTH_FORBIDDEN")

        tx { users.findById(fixture.actorId).orElseThrow().active = false }
        expect(list(), 401, "AUTH_UNAUTHENTICATED")
        tx { users.findById(fixture.actorId).orElseThrow().active = true }

        tx {
            roles.findById(fixture.actorRoleId).orElseThrow().permissions.removeIf {
                it.name == "applications:evaluations:view"
            }
        }
        expect(list(), 403, "AUTH_FORBIDDEN")

        tx { users.findById(fixture.actorId).orElseThrow().roles.clear() }
        expect(list(), 403, "AUTH_FORBIDDEN")
    }

    @Test
    fun `rechaza rango de evaluacion invertido y detalle inexistente`() {
        val invalidRange = mvc.perform(
            get("/api/admin/application-evaluations")
                .header("Authorization", "Bearer ${fixture.actorToken}")
                .param("evaluatedFrom", "2026-10-11T00:00:00Z")
                .param("evaluatedTo", "2026-10-10T00:00:00Z"),
        ).andReturn()
        expect(invalidRange, 400, "APPLICATION_EVALUATION_INVALID_FILTER")
        expect(detail(UUID.randomUUID()), 404, "APPLICATION_EVALUATION_NOT_FOUND")
    }

    @Test
    fun `rechaza datos ausentes nulos desconocidos extras y recomendacion invalida`() {
        val bodies = listOf(
            "{}",
            """{"vulnerability":null,"priority":"HIGH","recommendation":"Texto"}""",
            """{"vulnerability":"HIGH","priority":null,"recommendation":"Texto"}""",
            """{"vulnerability":"HIGH","priority":"URGENT","recommendation":null}""",
            """{"vulnerability":"UNKNOWN","priority":"HIGH","recommendation":"Texto"}""",
            """{"vulnerability":"HIGH","priority":"UNKNOWN","recommendation":"Texto"}""",
            """{"vulnerability":"HIGH","priority":"URGENT","recommendation":""}""",
            """{"vulnerability":"HIGH","priority":"URGENT","recommendation":"   "}""",
            validBody("x".repeat(1001)),
            """{"vulnerability":"HIGH","priority":"URGENT","recommendation":"Texto","professionalId":${fixture.actorId}}""",
            """{"vulnerability":"HIGH","priority":"URGENT","recommendation":"Texto","evaluatedAt":"2026-10-10T18:30:00Z"}""",
            """{"vulnerability":"HIGH","priority":"URGENT","recommendation":"Texto","status":"APPROVED"}""",
            """{"vulnerability":"HIGH","priority":"URGENT","recommendation":"Texto","applicationId":"${fixture.applicationId}"}""",
        )
        bodies.forEach { body ->
            expect(create(body = body), 400, "APPLICATION_EVALUATION_INVALID_DATA")
        }
        assertFalse(evaluations.existsByApplicationId(fixture.applicationId))
    }

    @Test
    fun `exige usuario activo rol vigente y permiso presente en jwt y base`() {
        expect(create(token = null), 401, "AUTH_UNAUTHENTICATED")
        expect(create(token = "invalid"), 401, "AUTH_INVALID_TOKEN")
        expect(create(token = fixture.unauthorizedToken), 403, "AUTH_FORBIDDEN")

        tx { users.findById(fixture.actorId).orElseThrow().active = false }
        expect(create(), 401, "AUTH_UNAUTHENTICATED")
        tx { users.findById(fixture.actorId).orElseThrow().active = true }

        tx { roles.findById(fixture.actorRoleId).orElseThrow().permissions.clear() }
        expect(create(), 403, "AUTH_FORBIDDEN")

        tx {
            val actor = users.findById(fixture.actorId).orElseThrow()
            actor.roles.clear()
        }
        expect(create(), 403, "AUTH_FORBIDDEN")
        assertFalse(evaluations.existsByApplicationId(fixture.applicationId))
    }

    @Test
    fun `responde inexistente y no crea evaluacion`() {
        expect(create(applicationId = UUID.randomUUID()), 404, "APPLICATION_NOT_FOUND")
    }

    @ParameterizedTest
    @EnumSource(
        value = ApplicationStatus::class,
        names = ["SUBMITTED", "IN_VALIDATION", "PENDING_DOCUMENTATION", "IN_EVALUATION"],
    )
    fun `admite las etapas de analisis`(status: ApplicationStatus) {
        val id = newApplication(status)
        expect(create(applicationId = id), 201)
        tx { assertEquals(ApplicationStatus.IN_EVALUATION, applications.findById(id).orElseThrow().status) }
    }

    @ParameterizedTest
    @EnumSource(
        value = ApplicationStatus::class,
        names = ["DRAFT", "IN_VISIT", "APPROVED", "REJECTED", "WAITLISTED", "CLOSED"],
    )
    fun `rechaza estados fuera de evaluacion sin modificarlos`(status: ApplicationStatus) {
        val id = newApplication(status)
        expect(create(applicationId = id), 409, "APPLICATION_EVALUATION_STATUS_NOT_ALLOWED")
        tx {
            val application = applications.findById(id).orElseThrow()
            assertEquals(status, application.status)
            assertNull(application.assignedWorker)
        }
        assertFalse(evaluations.existsByApplicationId(id))
    }

    @Test
    fun `rechaza solicitud asignada a otro trabajador`() {
        val other = tx { users.saveAndFlush(User(name = "Otro", email = "other-${UUID.randomUUID()}@example.com")) }
        val id = newApplication(assignedWorker = other)
        expect(create(applicationId = id), 409, "APPLICATION_EVALUATION_ASSIGNED_TO_ANOTHER_WORKER")
        tx {
            val application = applications.findById(id).orElseThrow()
            assertEquals(other.id, application.assignedWorker?.id)
            assertEquals(ApplicationStatus.SUBMITTED, application.status)
        }
    }

    @Test
    fun `una segunda evaluacion responde conflicto y conserva la original`() {
        val first = create()
        expect(first, 201)
        val original = json.readValue(first.response.contentAsString, ApplicationEvaluationResponse::class.java)
        expect(create(body = validBody("Otra recomendacion")), 409, "APPLICATION_EVALUATION_ALREADY_EXISTS")
        tx {
            val stored = assertNotNull(evaluations.findByApplicationId(fixture.applicationId))
            assertEquals(original.id, stored.id)
            assertEquals(original.recommendation, stored.recommendation)
        }
    }

    @Test
    fun `dos altas concurrentes producen una evaluacion y un conflicto`() {
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures = (1..2).map {
                pool.submit<MvcResult> {
                    ready.countDown()
                    assertTrue(start.await(5, TimeUnit.SECONDS))
                    create()
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            val results = futures.map { it.get(15, TimeUnit.SECONDS) }
            assertEquals(listOf(201, 409), results.map { it.response.status }.sorted())
            assertEquals(
                "APPLICATION_EVALUATION_ALREADY_EXISTS",
                json.readTree(results.single { it.response.status == 409 }.response.contentAsString).get("code").stringValue(),
            )
            assertNotNull(evaluations.findByApplicationId(fixture.applicationId))
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `un fallo de auditoria revierte evaluacion asignacion y estado`() {
        jdbc.execute(
            "alter table logs add constraint ck_evaluation_test_audit_failure " +
                "check (entity_type <> 'application_evaluation')",
        )
        try {
            expect(create(), 409, "DATA_INTEGRITY_VIOLATION")
            assertFalse(evaluations.existsByApplicationId(fixture.applicationId))
            tx {
                val application = applications.findById(fixture.applicationId).orElseThrow()
                assertEquals(ApplicationStatus.SUBMITTED, application.status)
                assertNull(application.assignedWorker)
            }
        } finally {
            jdbc.execute("alter table logs drop constraint ck_evaluation_test_audit_failure")
        }
    }

    @Test
    fun `no permite eliminar al profesional referenciado por una evaluacion`() {
        expect(create(), 201)
        tx { applications.findById(fixture.applicationId).orElseThrow().assignedWorker = null }

        val manager = tx {
            val permission = permissions.findAll().firstOrNull { it.name == "users:delete" }
                ?: permissions.save(Permission(name = "users:delete"))
            val role = roles.save(
                Role(name = "GESTOR_${UUID.randomUUID()}", permissions = mutableSetOf(permission)),
            )
            val user = users.saveAndFlush(
                User(name = "Gestor", email = "manager-${UUID.randomUUID()}@example.com", roles = mutableSetOf(role)),
            )
            jwt.createToken(user, role)
        }
        val result = mvc.perform(
            delete("/users/${fixture.actorId}").header("Authorization", "Bearer $manager"),
        ).andReturn()
        expect(result, 409, "USER_HAS_APPLICATION_REFERENCES")
        assertTrue(users.existsById(fixture.actorId))
    }

    private fun jsonValue(raw: Any?): JsonNode {
        val first = json.readTree(if (raw is ByteArray) raw.toString(Charsets.UTF_8) else raw.toString())
        return if (first.isTextual) json.readTree(first.stringValue()) else first
    }
}
