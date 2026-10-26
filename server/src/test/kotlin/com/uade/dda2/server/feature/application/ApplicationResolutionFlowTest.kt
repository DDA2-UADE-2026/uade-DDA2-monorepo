package com.uade.dda2.server.feature.application

import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationEvaluation
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisit
import com.uade.dda2.server.feature.application.entity.ApplicationVisitType
import com.uade.dda2.server.feature.application.entity.EvaluationPriority
import com.uade.dda2.server.feature.application.entity.VulnerabilityLevel
import com.uade.dda2.server.feature.application.repository.ApplicationEvaluationRepository
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.application.repository.ApplicationVisitRepository
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.log.repository.LogRepository
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramDocumentRequirement
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import com.uade.dda2.server.feature.program.repository.ProgramDocumentRequirementRepository
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:application-resolution;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "app.enrollment-period.expiration.cron=-",
    ],
)
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
    @Autowired lateinit var visits: ApplicationVisitRepository
    @Autowired lateinit var documentRequirements: ProgramDocumentRequirementRepository
    @Autowired lateinit var logs: LogRepository
    @Autowired lateinit var transactions: PlatformTransactionManager

    data class Fixture(
        val workerId: Long,
        val workerToken: String,
        val otherWorkerToken: String,
        val noPermissionToken: String,
        val citizenToken: String,
        val editionId: UUID,
        val periodId: UUID,
    )

    private lateinit var fixture: Fixture

    @BeforeEach
    fun setup() {
        fixture = newFixture(maxCapacity = 1)
    }

    private fun <T : Any> tx(action: () -> T): T = TransactionTemplate(transactions).execute { action() }!!

    private fun permission(name: String): Permission =
        permissions.findAll().firstOrNull { it.name == name } ?: permissions.save(Permission(name = name))

    private fun newFixture(maxCapacity: Int): Fixture = tx {
        val workerRole = roles.save(
            Role(name = "RESOLUTOR_${UUID.randomUUID()}", permissions = mutableSetOf(permission("applications:resolutions:create"))),
        )
        val emptyRole = roles.save(Role(name = "SIN_PERMISOS_${UUID.randomUUID()}"))
        val citizenRole = roles.save(
            Role(name = "CIUDADANO_${UUID.randomUUID()}", permissions = mutableSetOf(permission("applications:own:view"))),
        )
        fun user(name: String, role: Role) = users.saveAndFlush(
            User(name = name, email = "${UUID.randomUUID()}@example.com", roles = mutableSetOf(role)),
        )
        val worker = user("Trabajadora social", workerRole)
        val otherWorker = user("Otro trabajador", workerRole)
        val noPermission = user("Sin permiso", emptyRole)
        val citizen = user("Titular", citizenRole)
        val program = programs.save(Program(name = "Programa ${UUID.randomUUID()}", createdBy = worker))
        val edition = editions.save(
            ProgramEdition(
                program = program,
                name = "Edicion ${UUID.randomUUID()}",
                startDate = LocalDate.now().minusDays(30),
                endDate = LocalDate.now().plusDays(30),
                maxCapacity = maxCapacity,
                status = ProgramEditionStatus.ACTIVE,
                createdBy = worker,
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
        Fixture(
            workerId = requireNotNull(worker.id),
            workerToken = jwt.createToken(worker, workerRole),
            otherWorkerToken = jwt.createToken(otherWorker, workerRole),
            noPermissionToken = jwt.createToken(noPermission, emptyRole),
            citizenToken = jwt.createToken(citizen, citizenRole),
            editionId = requireNotNull(edition.id),
            periodId = requireNotNull(period.id),
        )
    }

    /** Solicitud lista para resolver: en evaluación, asignada al trabajador y con evaluación registrada. */
    private fun evaluatedApplication(
        status: ApplicationStatus = ApplicationStatus.IN_EVALUATION,
        applicant: User? = null,
        evaluated: Boolean = true,
    ): UUID = tx {
        val worker = users.getReferenceById(fixture.workerId)
        val titular = applicant ?: users.saveAndFlush(User(name = "Titular", email = "applicant-${UUID.randomUUID()}@example.com"))
        val application = applications.saveAndFlush(
            Application(
                user = titular,
                registeredBy = titular,
                programEdition = editions.getReferenceById(fixture.editionId),
                enrollmentPeriod = periods.getReferenceById(fixture.periodId),
                status = status,
                assignedWorker = worker,
                submittedAt = LocalDateTime.now().minusDays(1),
            ),
        )
        if (evaluated) {
            evaluations.saveAndFlush(
                ApplicationEvaluation(
                    application = application,
                    vulnerability = VulnerabilityLevel.HIGH,
                    priority = EvaluationPriority.HIGH,
                    recommendation = "Se recomienda el ingreso.",
                    professional = worker,
                    evaluatedAt = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )
        }
        requireNotNull(application.id)
    }

    private fun resolve(
        applicationId: UUID,
        decision: String?,
        reason: String? = null,
        token: String? = fixture.workerToken,
        body: String = json.writeValueAsString(mapOf("decision" to decision, "reason" to reason)),
    ): MvcResult {
        val request = post("/api/admin/applications/$applicationId/resolution")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").stringValue())
    }

    private fun stored(id: UUID): Application = tx { applications.findDetailById(id)!!.also { it.resolvedBy?.name } }

    @Test
    fun `aprueba registra motivo fecha responsable y auditoria`() {
        val id = evaluatedApplication()
        val logsBefore = logs.count()
        val result = resolve(id, "APPROVED", "  Cumple los requisitos.  ")
        expect(result, 200)
        val body = json.readTree(result.response.contentAsString)
        assertEquals("APPROVED", body.get("status").stringValue())
        assertEquals("Cumple los requisitos.", body.get("resolutionReason").stringValue())
        assertEquals(fixture.workerId, body.get("resolvedByUserId").longValue())
        assertEquals("Trabajadora social", body.get("resolvedByName").stringValue())
        assertNotNull(body.get("resolvedAt").stringValue())
        assertEquals(ApplicationStatus.APPROVED, stored(id).status)
        assertEquals(logsBefore + 1, logs.count())
        // Ya es final: no se puede volver a resolver.
        expect(resolve(id, "REJECTED", "Cambio de criterio."), 409, "APPLICATION_RESOLUTION_STATUS_NOT_ALLOWED")
    }

    @Test
    fun `aprobar sin motivo es valido pero rechazar y esperar lo exigen`() {
        expect(resolve(evaluatedApplication(), "APPROVED"), 200)
        val other = evaluatedApplication()
        expect(resolve(other, "REJECTED"), 400, "APPLICATION_RESOLUTION_INVALID")
        expect(resolve(other, "WAITLISTED", "   "), 400, "APPLICATION_RESOLUTION_INVALID")
        expect(resolve(other, "REJECTED", "x".repeat(1001)), 400, "APPLICATION_RESOLUTION_INVALID")
        expect(resolve(other, null, "Motivo"), 400, "APPLICATION_RESOLUTION_INVALID")
        expect(resolve(other, null, body = """{"decision":"REJECTED","reason":"Motivo","extra":1}"""), 400, "APPLICATION_RESOLUTION_INVALID")
        assertEquals(ApplicationStatus.IN_EVALUATION, stored(other).status)
    }

    @Test
    fun `respeta el cupo y la lista de espera se resuelve despues`() {
        expect(resolve(evaluatedApplication(), "APPROVED"), 200)
        val second = evaluatedApplication()
        expect(resolve(second, "APPROVED"), 409, "APPLICATION_EDITION_CAPACITY_REACHED")
        expect(resolve(second, "WAITLISTED", "Sin cupo en esta edicion."), 200)
        assertEquals(ApplicationStatus.WAITLISTED, stored(second).status)
        // Desde la espera no se vuelve a poner en espera.
        expect(resolve(second, "WAITLISTED", "Otra vez."), 409, "APPLICATION_RESOLUTION_STATUS_NOT_ALLOWED")
        expect(resolve(second, "APPROVED"), 409, "APPLICATION_EDITION_CAPACITY_REACHED")

        tx { editions.findById(fixture.editionId).orElseThrow().maxCapacity = 2 }
        expect(resolve(second, "APPROVED", "Se libero un cupo."), 200)
        assertEquals("Se libero un cupo.", stored(second).resolutionReason)
    }

    @Test
    fun `rechaza estados no resolubles y exige evaluacion visitas cerradas y documentacion`() {
        expect(resolve(evaluatedApplication(ApplicationStatus.SUBMITTED), "APPROVED"), 409, "APPLICATION_RESOLUTION_STATUS_NOT_ALLOWED")
        expect(resolve(evaluatedApplication(evaluated = false), "APPROVED"), 409, "APPLICATION_RESOLUTION_REQUIRES_EVALUATION")

        val withVisit = evaluatedApplication()
        tx {
            val start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
            visits.saveAndFlush(
                ApplicationVisit(
                    application = applications.getReferenceById(withVisit),
                    type = ApplicationVisitType.VISIT,
                    startsAt = start,
                    endsAt = start.plusHours(1),
                    location = "Domicilio",
                    responsible = users.getReferenceById(fixture.workerId),
                    scheduledAt = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )
        }
        expect(resolve(withVisit, "REJECTED", "No corresponde."), 409, "APPLICATION_RESOLUTION_SCHEDULED_VISITS")

        val missingDocs = evaluatedApplication()
        tx {
            documentRequirements.saveAndFlush(
                ProgramDocumentRequirement(
                    programEdition = editions.getReferenceById(fixture.editionId),
                    code = "DNI_${UUID.randomUUID().toString().take(8)}",
                    name = "DNI",
                    required = true,
                ),
            )
        }
        expect(resolve(missingDocs, "APPROVED"), 409, "APPLICATION_RESOLUTION_PENDING_DOCUMENTS")
        // La documentación faltante no impide rechazar.
        expect(resolve(missingDocs, "REJECTED", "Documentacion incompleta."), 200)
    }

    @Test
    fun `solo resuelve el asignado con permiso y nunca el propio titular`() {
        val id = evaluatedApplication()
        expect(resolve(id, "APPROVED", token = null), 401, "AUTH_UNAUTHENTICATED")
        expect(resolve(id, "APPROVED", token = fixture.noPermissionToken), 403, "AUTH_FORBIDDEN")
        expect(resolve(id, "APPROVED", token = fixture.otherWorkerToken), 403, "APPLICATION_RESOLUTION_ASSIGNED_TO_ANOTHER_WORKER")
        expect(resolve(UUID.randomUUID(), "APPROVED"), 404, "APPLICATION_NOT_FOUND")

        val own = evaluatedApplication(applicant = tx { users.findById(fixture.workerId).orElseThrow() })
        expect(resolve(own, "APPROVED"), 403, "APPLICATION_RESOLUTION_OWN_APPLICATION")
        assertEquals(ApplicationStatus.IN_EVALUATION, stored(id).status)
    }

    @Test
    fun `el titular ve el resultado y el motivo en su consulta propia`() {
        val citizenId = tx {
            users.findAll().first { user -> user.roles.any { it.name.startsWith("CIUDADANO_") } && user.name == "Titular" }.id!!
        }
        val id = evaluatedApplication(applicant = tx { users.findById(citizenId).orElseThrow() })
        expect(resolve(id, "REJECTED", "Supera el ingreso maximo."), 200)

        val result = mvc.perform(
            get("/api/applications/$id").header("Authorization", "Bearer ${fixture.citizenToken}"),
        ).andReturn()
        expect(result, 200)
        val body = json.readTree(result.response.contentAsString)
        assertEquals("REJECTED", body.get("status").stringValue())
        assertEquals("Supera el ingreso maximo.", body.get("resolutionReason").stringValue())
        assertTrue(body.has("resolvedAt"))
        assertFalse(body.has("resolvedByUserId"))
    }
}
