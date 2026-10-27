package com.uade.dda2.server.feature.application

import com.uade.dda2.server.feature.application.dto.response.ApplicationResolutionResponse
import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.application.repository.*
import com.uade.dda2.server.feature.auth.entity.*
import com.uade.dda2.server.feature.auth.repository.*
import com.uade.dda2.server.feature.document.entity.Document
import com.uade.dda2.server.feature.document.repository.DocumentRepository
import com.uade.dda2.server.feature.enrollmentperiod.entity.*
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.program.entity.*
import com.uade.dda2.server.feature.program.entity.enums.*
import com.uade.dda2.server.feature.program.repository.*
import com.uade.dda2.server.security.JwtService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.*
import java.util.UUID
import java.util.concurrent.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

@SpringBootTest(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
    "app.enrollment-period.expiration.cron=-",
])
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ApplicationResolutionFlowTest {
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
    @Autowired lateinit var resolutions: ApplicationResolutionRepository
    @Autowired lateinit var requirements: ProgramRequirementRepository
    @Autowired lateinit var documentRequirements: ProgramDocumentRequirementRepository
    @Autowired lateinit var documents: DocumentRepository
    @Autowired lateinit var applicationDocuments: ApplicationDocumentRepository
    @Autowired lateinit var visits: ApplicationVisitRepository
    @Autowired lateinit var incompatibilities: ProgramIncompatibilityRepository
    @Autowired lateinit var transactions: PlatformTransactionManager
    @Autowired lateinit var jdbc: JdbcTemplate

    companion object {
        @JvmStatic @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) {
            val postgres = System.getProperty("resolution.test.postgres-url")
            registry.add("spring.datasource.url") { postgres ?: "jdbc:h2:mem:application-resolutions;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000" }
            registry.add("spring.datasource.driver-class-name") { if (postgres == null) "org.h2.Driver" else "org.postgresql.Driver" }
            registry.add("spring.jpa.properties.hibernate.dialect") { if (postgres == null) "org.hibernate.dialect.H2Dialect" else "org.hibernate.dialect.PostgreSQLDialect" }
            registry.add("spring.datasource.username") { if (postgres == null) "sa" else System.getProperty("resolution.test.postgres-user", "application_test") }
            registry.add("spring.datasource.password") { System.getProperty("resolution.test.postgres-password", "") }
        }
    }

    data class Fixture(val id: UUID, val actorId: Long, val roleId: Long, val token: String,
        val applicantId: Long, val citizenToken: String, val workerId: Long, val editionId: UUID,
        val programId: UUID, val requirementId: UUID)
    private lateinit var f: Fixture
    private val today get() = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
    private fun <T : Any> tx(action: () -> T): T = TransactionTemplate(transactions).execute { action() }!!

    @BeforeEach fun setup() { f = fixture() }

    private fun permission(name: String) = permissions.findByNameIn(setOf(name)).firstOrNull()
        ?: permissions.save(Permission(name = name))

    private fun fixture(applicantId: Long? = null, editionId: UUID? = null): Fixture = tx {
        val role = roles.save(Role(name = "COORDINADOR_${UUID.randomUUID()}", permissions = mutableSetOf(
            permission("applications:management:resolve"), permission("applications:management:view"), permission("users:delete"))))
        val actor = users.saveAndFlush(User(name = "Coordinador", email = "coordinator@example.com", roles = mutableSetOf(role)))
        val citizenRole = roles.save(Role(name = "CIUDADANO_${UUID.randomUUID()}", permissions = mutableSetOf(permission("applications:own:view"))))
        val applicant = if (applicantId == null) users.saveAndFlush(User(name = "Titular", email = "applicant@example.com", roles = mutableSetOf(citizenRole)))
            else users.findById(applicantId).orElseThrow()
        val worker = users.saveAndFlush(User(name = "Evaluador", email = "worker@example.com"))
        val program = if (editionId == null) programs.saveAndFlush(Program(name = "Programa ${UUID.randomUUID()}", createdBy = worker))
            else editions.findById(editionId).orElseThrow().program
        val edition = if (editionId == null) editions.saveAndFlush(ProgramEdition(program = program, name = "Edicion ${UUID.randomUUID()}",
            startDate = today.minusDays(20), endDate = today.plusDays(20), maxCapacity = 1,
            status = ProgramEditionStatus.ACTIVE, createdBy = worker)) else editions.findById(editionId).orElseThrow()
        val period = periods.saveAndFlush(EnrollmentPeriod(programEdition = edition, openDate = today.minusDays(15),
            closeDate = today.minusDays(1), status = EnrollmentPeriodStatus.CLOSED))
        val application = applications.saveAndFlush(Application(user = applicant, registeredBy = applicant,
            programEdition = edition, enrollmentPeriod = period, submittedAt = LocalDateTime.now().minusDays(2),
            status = ApplicationStatus.IN_EVALUATION, assignedWorker = worker))
        evaluations.saveAndFlush(ApplicationEvaluation(application = application, vulnerability = VulnerabilityLevel.HIGH,
            priority = EvaluationPriority.HIGH, recommendation = "Evaluacion registrada.", professional = worker,
            evaluatedAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)))
        val requirement = if (editionId == null) requirements.saveAndFlush(ProgramRequirement(programEdition = edition,
            type = ProgramRequirementType.MIN_AGE, value = "18", description = "Mayor de edad"))
            else requirements.findAllByProgramEditionId(editionId).first()
        Fixture(application.id!!, actor.id!!, role.id!!, jwt.createToken(actor, role), applicant.id!!,
            if (applicantId == null) jwt.createToken(applicant, citizenRole) else "", worker.id!!, edition.id!!, program.id!!, requirement.id!!)
    }

    private fun body(decision: String = "APPROVED", fixture: Fixture = f, fulfilled: Boolean = true, reason: String = " Motivo reservado ") =
        json.writeValueAsString(mapOf("decision" to decision, "reason" to reason,
            "requirementChecks" to listOf(mapOf("requirementId" to fixture.requirementId, "fulfilled" to fulfilled))))

    private fun resolve(fixture: Fixture = f, decision: String = "APPROVED", token: String? = fixture.token,
        content: String = body(decision, fixture)): MvcResult {
        val request = post("/api/admin/applications/${fixture.id}/resolutions").contentType(MediaType.APPLICATION_JSON).content(content)
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }
    private fun history(token: String = f.token, id: UUID = f.id) = mvc.perform(
        get("/api/admin/applications/$id/resolutions").header("Authorization", "Bearer $token")).andReturn()
    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").stringValue())
    }
    private fun response(result: MvcResult) = json.readValue(result.response.contentAsString, ApplicationResolutionResponse::class.java)
    private fun unchanged() = tx {
        val application = applications.findById(f.id).orElseThrow()
        assertEquals(ApplicationStatus.IN_EVALUATION, application.status)
        assertNull(application.resolvedBy)
        assertNull(application.resolutionReason)
        assertTrue(resolutions.findAllByApplicationIdOrderByResolvedAtAscIdAsc(f.id).isEmpty())
        true
    }

    @Test fun `aprueba con responsable distinto cupo auditoria y privacidad`() {
        val result = resolve()
        expect(result, 201)
        val resolution = response(result)
        assertEquals("Motivo reservado", resolution.reason)
        assertEquals(f.actorId, resolution.resolvedByUserId)
        assertEquals(ZoneOffset.UTC, resolution.resolvedAt.offset)
        assertEquals("18", resolution.requirementChecks.single().value)
        tx {
            val application = applications.findDetailById(f.id)!!
            assertEquals(ApplicationStatus.APPROVED, application.status)
            assertEquals(f.workerId, application.assignedWorker?.id)
            assertEquals(f.actorId, application.resolvedBy?.id)
            assertNotNull(application.resolvedAt)
            assertEquals(application.resolvedAt, application.updatedAt)
            assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
            val audit = jdbc.queryForObject("select new_values::text from logs where entity_type = 'application_resolution' and entity_id = ?", String::class.java, resolution.id.toString())!!
            assertFalse(audit.contains("Motivo reservado"))
            assertTrue(audit.contains("resolvedByUserId"))
        }
        val admin = mvc.perform(get("/api/admin/applications/${f.id}").header("Authorization", "Bearer ${f.token}")).andReturn()
        expect(admin, 200)
        assertEquals(f.actorId, json.readTree(admin.response.contentAsString).get("resolvedByUserId").longValue())
        val own = mvc.perform(get("/api/applications/${f.id}").header("Authorization", "Bearer ${f.citizenToken}")).andReturn()
        expect(own, 200)
        val ownJson = json.readTree(own.response.contentAsString)
        assertEquals("APPROVED", ownJson.get("status").stringValue())
        listOf("reason", "resolutionReason", "resolvedAt", "resolvedByUserId", "resolvedByUserName", "resolvedByName", "resolutions")
            .forEach { assertFalse(ownJson.has(it), "La respuesta ciudadana no debe exponer $it") }
        expect(history(f.citizenToken), 403)
        expect(resolve(), 409, "APPLICATION_RESOLUTION_STATUS_NOT_ALLOWED")
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    @ParameterizedTest @ValueSource(strings = ["APPROVED", "REJECTED"])
    fun `resuelve la espera manualmente y conserva ambos registros`(decision: String) {
        tx { editions.findById(f.editionId).orElseThrow().currentEnrollment = 1 }
        expect(resolve(decision = "WAITLISTED"), 201)
        expect(resolve(decision = "WAITLISTED"), 409, "APPLICATION_RESOLUTION_STATUS_NOT_ALLOWED")
        tx { editions.findById(f.editionId).orElseThrow().maxCapacity = 2 }
        expect(resolve(decision = decision), 201)
        val entries = json.readTree(history().response.contentAsString)
        assertEquals(2, entries.size())
        assertEquals("WAITLISTED", entries[0].get("decision").stringValue())
        assertEquals("WAITLISTED", entries[1].get("previousStatus").stringValue())
        assertEquals(decision, entries[1].get("decision").stringValue())
        assertEquals(if (decision == "APPROVED") 2 else 1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    @Test fun `rechaza sin exigir cumplimiento documentos o cupo`() {
        addDocument(null)
        tx { editions.findById(f.editionId).orElseThrow().status = ProgramEditionStatus.SUSPENDED }
        expect(resolve(decision = "REJECTED", content = body("REJECTED", fulfilled = false)), 201)
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        assertFalse(responseFromHistory().requirementChecks.single().fulfilled)
    }

    private fun responseFromHistory(): ApplicationResolutionResponse = json.readValue(
        json.readTree(history().response.contentAsString)[0].toString(), ApplicationResolutionResponse::class.java)

    @ParameterizedTest @ValueSource(strings = ["", " ", "LONG"])
    fun `rechaza motivo invalido`(reason: String) {
        expect(resolve(content = body(reason = if (reason == "LONG") "a".repeat(1001) else reason)), 400)
        unchanged()
    }

    @Test fun `rechaza cuerpos incompletos campos desconocidos y checklist invalido`() {
        val check = """{"requirementId":"${f.requirementId}","fulfilled":true}"""
        val invalid = listOf("{}", """{"decision":"CLOSED","reason":"x","requirementChecks":[]}""",
            """{"decision":"APPROVED","reason":"x","requirementChecks":[null]}""",
            """{"decision":"APPROVED","reason":"x"}""",
            """{"decision":"APPROVED","reason":"x","requirementChecks":[]}""",
            """{"decision":"APPROVED","reason":"x","requirementChecks":[$check,$check]}""",
            """{"decision":"APPROVED","reason":"x","requirementChecks":[{"requirementId":"${UUID.randomUUID()}","fulfilled":true}]}""",
            """{"decision":"APPROVED","reason":"x","requirementChecks":[{"requirementId":"${f.requirementId}"}]}""",
            body().dropLast(1) + ",\"resolvedByUserId\":${f.workerId}}",
            body().dropLast(1) + ",\"resolvedAt\":\"2026-01-01T00:00:00Z\"}",
            """{"decision":"APPROVED","reason":"x","requirementChecks":[{"requirementId":"${f.requirementId}","fulfilled":true,"extra":1}]}""")
        invalid.forEach { expect(resolve(content = it), 400) }
        unchanged()
    }

    @Test fun `edicion sin requisitos acepta lista vacia`() {
        tx { requirements.deleteById(f.requirementId); true }
        expect(resolve(content = """{"decision":"APPROVED","reason":"Sin requisitos","requirementChecks":[]}"""), 201)
    }

    @ParameterizedTest @EnumSource(ApplicationStatus::class, names = ["IN_EVALUATION", "WAITLISTED"], mode = EnumSource.Mode.EXCLUDE)
    fun `bloquea estados que no admiten resolucion`(status: ApplicationStatus) {
        tx { applications.findById(f.id).orElseThrow().status = status }
        expect(resolve(), 409, "APPLICATION_RESOLUTION_STATUS_NOT_ALLOWED")
    }

    @Test fun `exige evaluacion y ningun turno de visita pendiente`() {
        tx { evaluations.delete(evaluations.findByApplicationId(f.id)!!); true }
        expect(resolve(decision = "REJECTED"), 409, "APPLICATION_RESOLUTION_EVALUATION_REQUIRED")
        unchanged()
    }

    @ParameterizedTest @EnumSource(ApplicationVisitType::class)
    fun `visitas y entrevistas pendientes bloquean incluso rechazo`(type: ApplicationVisitType) {
        tx {
            visits.saveAndFlush(ApplicationVisit(application = applications.findById(f.id).orElseThrow(), type = type,
                startsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), endsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).plusHours(1),
                location = "Centro", responsible = users.findById(f.workerId).orElseThrow(), scheduledAt = OffsetDateTime.now(ZoneOffset.UTC)))
        }
        expect(resolve(decision = "REJECTED"), 409, "APPLICATION_RESOLUTION_PENDING_VISITS")
        unchanged()
    }

    private fun addDocument(status: ApplicationDocumentStatus?) = tx {
        val application = applications.findById(f.id).orElseThrow()
        val requirement = documentRequirements.saveAndFlush(ProgramDocumentRequirement(programEdition = application.programEdition,
            code = "DNI", name = "DNI", required = true))
        if (status != null) {
            val now = LocalDateTime.now()
            val document = documents.saveAndFlush(Document(originalName = "dni.pdf", contentType = "application/pdf", sizeBytes = 4,
                content = byteArrayOf(1, 2, 3, 4), uploadedBy = application.user, createdAt = now))
            applicationDocuments.saveAndFlush(ApplicationDocument(application = application, requirement = requirement, document = document,
                status = status, uploadedAt = now, observation = if (status == ApplicationDocumentStatus.OBSERVED) "Ilegible" else null,
                reviewedBy = if (status == ApplicationDocumentStatus.PENDING) null else users.findById(f.workerId).orElseThrow(),
                reviewedAt = if (status == ApplicationDocumentStatus.PENDING) null else now))
        }
        true
    }

    @ParameterizedTest @ValueSource(strings = ["MISSING", "PENDING", "OBSERVED", "VALID"])
    fun `solo documentos obligatorios VALID habilitan aprobar y esperar`(status: String) {
        addDocument(if (status == "MISSING") null else ApplicationDocumentStatus.valueOf(status))
        if (status == "VALID") expect(resolve(), 201) else {
            expect(resolve(), 409, "APPLICATION_RESOLUTION_DOCUMENTS_NOT_VALID")
            tx { editions.findById(f.editionId).orElseThrow().currentEnrollment = 1 }
            expect(resolve(decision = "WAITLISTED"), 409, "APPLICATION_RESOLUTION_DOCUMENTS_NOT_VALID")
            unchanged()
        }
    }

    @Test fun `incumplimiento bloquea aprobar y espera sin resolucion automatica`() {
        expect(resolve(content = body(fulfilled = false)), 409, "APPLICATION_RESOLUTION_REQUIREMENTS_NOT_MET")
        tx { editions.findById(f.editionId).orElseThrow().currentEnrollment = 1 }
        expect(resolve(decision = "WAITLISTED", content = body("WAITLISTED", fulfilled = false)), 409, "APPLICATION_RESOLUTION_REQUIREMENTS_NOT_MET")
        unchanged()
    }

    @ParameterizedTest @EnumSource(ProgramEditionStatus::class, names = ["ACTIVE"], mode = EnumSource.Mode.EXCLUDE)
    fun `bloquea edicion no activa`(status: ProgramEditionStatus) {
        tx { editions.findById(f.editionId).orElseThrow().status = status }
        expect(resolve(), 409, "APPLICATION_RESOLUTION_EDITION_NOT_ELIGIBLE")
        unchanged()
    }

    @Test fun `edicion vencida bloquea y fecha final inclusiva permite`() {
        tx { editions.findById(f.editionId).orElseThrow().endDate = today.minusDays(1) }
        expect(resolve(), 409, "APPLICATION_RESOLUTION_EDITION_NOT_ELIGIBLE")
        tx { editions.findById(f.editionId).orElseThrow().endDate = today }
        expect(resolve(), 201)
    }

    @Test fun `cupo determina aprobar o esperar sin fallback automatico`() {
        expect(resolve(decision = "WAITLISTED"), 409, "APPLICATION_RESOLUTION_CAPACITY_AVAILABLE")
        tx { editions.findById(f.editionId).orElseThrow().currentEnrollment = 1 }
        expect(resolve(), 409, "APPLICATION_RESOLUTION_NO_CAPACITY")
        unchanged()
        expect(resolve(decision = "WAITLISTED"), 201)
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    private fun incompatible(other: Fixture, reverse: Boolean = false) = tx {
        val first = programs.findById(if (reverse) other.programId else f.programId).orElseThrow()
        val second = programs.findById(if (reverse) f.programId else other.programId).orElseThrow()
        incompatibilities.saveAndFlush(ProgramIncompatibility(id = ProgramIncompatibilityId(first.id, second.id), program = first, incompatibleWithProgram = second))
    }

    @ParameterizedTest @ValueSource(booleans = [false, true])
    fun `incompatibilidad funciona en ambos sentidos y no impide rechazo`(reverse: Boolean) {
        val other = fixture(applicantId = f.applicantId)
        incompatible(other, reverse)
        expect(resolve(other), 201)
        expect(resolve(), 409, "APPLICATION_RESOLUTION_INCOMPATIBLE_APPLICATION")
        tx { editions.findById(f.editionId).orElseThrow().currentEnrollment = 1 }
        expect(resolve(decision = "WAITLISTED"), 409, "APPLICATION_RESOLUTION_INCOMPATIBLE_APPLICATION")
        expect(resolve(decision = "REJECTED"), 201)
    }

    @ParameterizedTest @ValueSource(ints = [-1, 0, 1])
    fun `vigencias incompatibles incluyen los extremos pero no fechas separadas`(gap: Int) {
        val other = fixture(applicantId = f.applicantId)
        incompatible(other)
        tx {
            val edition = editions.findById(other.editionId).orElseThrow()
            edition.endDate = editions.findById(f.editionId).orElseThrow().startDate.plusDays(gap.toLong())
            edition.startDate = edition.endDate.minusDays(20)
            applications.findById(other.id).orElseThrow().status = ApplicationStatus.APPROVED
        }
        expect(resolve(), if (gap < 0) 201 else 409,
            if (gap < 0) null else "APPLICATION_RESOLUTION_INCOMPATIBLE_APPLICATION")
    }

    @Test fun `no bloquean otras personas ni solicitudes incompatibles sin aprobar`() {
        val other = fixture()
        incompatible(other)
        expect(resolve(other), 201)
        expect(resolve(), 201)
    }

    @Test fun `solicitud incompatible sin aprobar no bloquea al titular`() {
        val other = fixture(applicantId = f.applicantId)
        incompatible(other)
        expect(resolve(), 201)
    }

    @Test fun `migracion PostgreSQL puede repetirse sin fabricar historial o ajustar cupos`() {
        assumeTrue(System.getProperty("resolution.test.postgres-url") != null)
        val before = jdbc.queryForObject("select count(*) from application_resolution", Long::class.java)
        val migration = Files.readString(Path.of("docs/database/migrations/2026-10-18-application-resolutions.sql"))
        repeat(2) { jdbc.execute(migration) }
        assertEquals(before, jdbc.queryForObject("select count(*) from application_resolution", Long::class.java))
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        expect(resolve(), 201)
    }

    @Test fun `revalida requisitos y documentos al aprobar desde espera`() {
        tx { editions.findById(f.editionId).orElseThrow().currentEnrollment = 1 }
        expect(resolve(decision = "WAITLISTED"), 201)
        tx { editions.findById(f.editionId).orElseThrow().maxCapacity = 2 }
        addDocument(ApplicationDocumentStatus.PENDING)
        expect(resolve(), 409, "APPLICATION_RESOLUTION_DOCUMENTS_NOT_VALID")
        expect(resolve(content = body(fulfilled = false)), 409, "APPLICATION_RESOLUTION_REQUIREMENTS_NOT_MET")
        assertEquals(1, json.readTree(history().response.contentAsString).size())
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    @Test fun `historial conserva requisito modificado y eliminado y permite consulta sin resolver`() {
        expect(history(), 200)
        assertEquals(0, json.readTree(history().response.contentAsString).size())
        expect(resolve(), 201)
        tx { requirements.findById(f.requirementId).orElseThrow().value = "30" }
        assertEquals("18", responseFromHistory().requirementChecks.single().value)
        tx { requirements.deleteById(f.requirementId); true }
        assertEquals("18", responseFromHistory().requirementChecks.single().value)
        tx { roles.findById(f.roleId).orElseThrow().permissions.removeIf { it.name == "applications:management:resolve" }; true }
        expect(history(), 200)
    }

    @Test fun `seguridad exige token rol vigente usuario activo y permiso en base`() {
        expect(resolve(token = null), 401)
        expect(resolve(token = f.citizenToken), 403)
        expect(history(f.citizenToken), 403)
        tx { roles.findById(f.roleId).orElseThrow().permissions.removeIf { it.name == "applications:management:resolve" }; true }
        expect(resolve(), 403)
        tx { roles.findById(f.roleId).orElseThrow().permissions.add(permission("applications:management:resolve")) }
        tx { users.findById(f.actorId).orElseThrow().roles.clear(); true }
        expect(resolve(), 403)
        tx { users.findById(f.actorId).orElseThrow().roles.add(roles.findById(f.roleId).orElseThrow()) }
        tx { users.findById(f.actorId).orElseThrow().active = false }
        expect(resolve(), 401)
        expect(history(), 401)
        unchanged()
    }

    @Test fun `no admite permiso otorgado despues del token ni permiso en otro rol activo`() {
        val staleToken = tx {
            val role = roles.saveAndFlush(Role(name = "LECTOR_${UUID.randomUUID()}", permissions = mutableSetOf(permission("applications:management:view"))))
            val actor = users.findById(f.actorId).orElseThrow()
            actor.roles.add(role)
            jwt.createToken(actor, role)
        }
        expect(resolve(token = staleToken), 403)
        unchanged()
    }

    @Test fun `inexistente devuelve 404`() {
        expect(resolve(f.copy(id = UUID.randomUUID())), 404, "APPLICATION_NOT_FOUND")
        expect(history(id = UUID.randomUUID()), 404, "APPLICATION_NOT_FOUND")
    }

    @Test fun `fallo de auditoria revierte estado historial y cupo`() {
        jdbc.execute("alter table logs add constraint ck_resolution_test_audit_failure check (entity_type <> 'application_resolution' or user_id <> ${f.actorId})")
        try {
            expect(resolve(), 409, "DATA_INTEGRITY_VIOLATION")
            unchanged()
            assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        } finally { jdbc.execute("alter table logs drop constraint ck_resolution_test_audit_failure") }
    }

    @Test fun `no elimina responsable historico`() {
        expect(resolve(), 201)
        tx { applications.findById(f.id).orElseThrow().resolvedBy = null; true }
        val result = mvc.perform(delete("/users/${f.actorId}").header("Authorization", "Bearer ${fixture().token}")).andReturn()
        expect(result, 409, "USER_HAS_APPLICATION_REFERENCES")
        assertTrue(users.existsById(f.actorId))
    }

    private fun concurrently(first: () -> MvcResult, second: () -> MvcResult): List<MvcResult> {
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        return try {
            val tasks = listOf(first, second).map { action -> pool.submit<MvcResult> {
                ready.countDown(); assertTrue(start.await(5, TimeUnit.SECONDS)); action()
            } }
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown()
            tasks.map { it.get(25, TimeUnit.SECONDS) }
        } finally { pool.shutdownNow() }
    }

    @Test fun `dos resoluciones simultaneas solo resuelven una vez`() {
        val results = concurrently({ resolve() }, { resolve() })
        assertEquals(listOf(201, 409), results.map { it.response.status }.sorted())
        assertEquals(1, json.readTree(history().response.contentAsString).size())
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    @Test fun `dos titulares compiten por el ultimo cupo`() {
        val other = fixture(editionId = f.editionId)
        val results = concurrently({ resolve() }, { resolve(other) })
        assertEquals(listOf(201, 409), results.map { it.response.status }.sorted())
        expect(results.single { it.response.status == 409 }, 409, "APPLICATION_RESOLUTION_NO_CAPACITY")
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    @Test fun `dos programas incompatibles no aprueban al mismo titular simultaneamente`() {
        val other = fixture(applicantId = f.applicantId)
        incompatible(other)
        val results = concurrently({ resolve() }, { resolve(other) })
        assertEquals(listOf(201, 409), results.map { it.response.status }.sorted())
        expect(results.single { it.response.status == 409 }, 409, "APPLICATION_RESOLUTION_INCOMPATIBLE_APPLICATION")
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment + editions.findById(other.editionId).orElseThrow().currentEnrollment)
    }
}
