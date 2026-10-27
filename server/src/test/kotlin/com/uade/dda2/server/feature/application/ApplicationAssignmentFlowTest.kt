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
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:application-assignment;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ApplicationAssignmentFlowTest {
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
    @Autowired lateinit var visits: ApplicationVisitRepository
    @Autowired lateinit var evaluations: ApplicationEvaluationRepository
    @Autowired lateinit var logs: LogRepository
    @Autowired lateinit var transactions: PlatformTransactionManager

    data class Fixture(
        val applicationId: UUID,
        val adminToken: String,
        val unauthorizedToken: String,
        val workerId: Long,
        val otherWorkerId: Long,
        val visitsOnlyId: Long,
        val ineligibleId: Long,
        val editionId: UUID,
        val periodId: UUID,
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
        val adminRole = roles.save(
            Role(name = "ASIGNADOR_${UUID.randomUUID()}", permissions = mutableSetOf(permission("applications:management:assign"))),
        )
        val workerRole = roles.save(
            Role(
                name = "TRABAJADOR_${UUID.randomUUID()}",
                permissions = mutableSetOf(permission("applications:visits:create"), permission("applications:evaluations:create")),
            ),
        )
        val visitsOnlyRole = roles.save(
            Role(name = "SOLO_VISITAS_${UUID.randomUUID()}", permissions = mutableSetOf(permission("applications:visits:create"))),
        )
        val emptyRole = roles.save(Role(name = "SIN_PERMISOS_${UUID.randomUUID()}"))
        fun user(name: String, role: Role) = users.saveAndFlush(
            User(name = name, email = "${UUID.randomUUID()}@example.com", roles = mutableSetOf(role)),
        )
        val admin = user("Coordinacion", adminRole)
        val unauthorized = user("Sin permiso", emptyRole)
        val worker = user("Trabajadora social", workerRole)
        val otherWorker = user("Otro trabajador", workerRole)
        val visitsOnly = user("Solo visitas", visitsOnlyRole)
        val ineligible = user("Sin rol social", emptyRole)
        val applicant = users.saveAndFlush(User(name = "Titular", email = "applicant-${UUID.randomUUID()}@example.com"))
        val program = programs.save(Program(name = "Programa ${UUID.randomUUID()}", createdBy = admin))
        val edition = editions.save(
            ProgramEdition(
                program = program,
                name = "Edicion ${UUID.randomUUID()}",
                startDate = LocalDate.now().minusDays(30),
                endDate = LocalDate.now().plusDays(30),
                maxCapacity = 100,
                status = ProgramEditionStatus.ACTIVE,
                createdBy = admin,
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
            adminToken = jwt.createToken(admin, adminRole),
            unauthorizedToken = jwt.createToken(unauthorized, emptyRole),
            workerId = requireNotNull(worker.id),
            otherWorkerId = requireNotNull(otherWorker.id),
            visitsOnlyId = requireNotNull(visitsOnly.id),
            ineligibleId = requireNotNull(ineligible.id),
            editionId = requireNotNull(edition.id),
            periodId = requireNotNull(period.id),
        )
    }

    private fun newApplication(status: ApplicationStatus): UUID = tx {
        val applicant = users.saveAndFlush(User(name = "Titular", email = "applicant-${UUID.randomUUID()}@example.com"))
        requireNotNull(
            applications.saveAndFlush(
                Application(
                    user = applicant,
                    registeredBy = applicant,
                    programEdition = editions.getReferenceById(fixture.editionId),
                    enrollmentPeriod = periods.getReferenceById(fixture.periodId),
                    status = status,
                    submittedAt = LocalDateTime.now().minusDays(1),
                ),
            ).id,
        )
    }

    private fun assign(
        userId: Long?,
        applicationId: UUID = fixture.applicationId,
        token: String? = fixture.adminToken,
        body: String = json.writeValueAsString(mapOf("userId" to userId)),
    ): MvcResult {
        val request = put("/api/admin/applications/$applicationId/assigned-worker")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }

    private fun unassign(applicationId: UUID = fixture.applicationId, token: String? = fixture.adminToken): MvcResult {
        val request = delete("/api/admin/applications/$applicationId/assigned-worker")
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").stringValue())
    }

    private fun assignedWorkerId(applicationId: UUID = fixture.applicationId): Long? =
        tx { applications.findById(applicationId).map { it.assignedWorker?.id ?: -1L }.orElseThrow() }.takeIf { it != -1L }

    @Test
    fun `asigna y reasigna un trabajador habilitado y lo audita`() {
        val logsBefore = logs.count()
        val result = assign(fixture.workerId)
        expect(result, 200)
        val body = json.readTree(result.response.contentAsString)
        assertEquals(fixture.workerId, body.get("assignedWorkerUserId").longValue())
        assertEquals("Trabajadora social", body.get("assignedWorkerName").stringValue())
        assertEquals(fixture.workerId, assignedWorkerId())
        assertEquals(logsBefore + 1, logs.count())

        // Mismo trabajador: no cambia ni audita de nuevo.
        expect(assign(fixture.workerId), 200)
        assertEquals(logsBefore + 1, logs.count())

        expect(assign(fixture.otherWorkerId), 200)
        assertEquals(fixture.otherWorkerId, assignedWorkerId())
        assertEquals(logsBefore + 2, logs.count())
    }

    @Test
    fun `exige autenticacion y permiso de asignacion`() {
        expect(assign(fixture.workerId, token = null), 401, "AUTH_UNAUTHENTICATED")
        expect(assign(fixture.workerId, token = fixture.unauthorizedToken), 403, "AUTH_FORBIDDEN")
        assertEquals(null, assignedWorkerId())
    }

    @Test
    fun `valida cuerpo usuario elegible y solicitud existente`() {
        expect(assign(null), 400, "APPLICATION_ASSIGNMENT_INVALID")
        expect(assign(null, body = """{"userId":${fixture.workerId},"extra":true}"""), 400, "APPLICATION_ASSIGNMENT_INVALID")
        expect(assign(Long.MAX_VALUE), 404, "APPLICATION_ASSIGNEE_NOT_FOUND")
        expect(assign(fixture.workerId, applicationId = UUID.randomUUID()), 404, "APPLICATION_NOT_FOUND")
        expect(assign(fixture.ineligibleId), 409, "APPLICATION_ASSIGNEE_NOT_ELIGIBLE")
        // El titular nunca trabaja su propia solicitud, aunque tuviera un rol habilitado.
        val applicantId = tx { applications.findById(fixture.applicationId).map { it.user.id!! }.orElseThrow() }
        tx {
            val applicant = users.findById(applicantId).orElseThrow()
            applicant.roles.addAll(users.findById(fixture.workerId).orElseThrow().roles)
        }
        expect(assign(applicantId), 409, "APPLICATION_ASSIGNEE_IS_APPLICANT")
        // Sin evaluacion registrada, el asignado tambien tiene que poder evaluar.
        expect(assign(fixture.visitsOnlyId), 409, "APPLICATION_ASSIGNEE_NOT_ELIGIBLE")

        tx { users.findById(fixture.workerId).orElseThrow().active = false }
        expect(assign(fixture.workerId), 404, "APPLICATION_ASSIGNEE_NOT_FOUND")
        assertEquals(null, assignedWorkerId())
    }

    @Test
    fun `vuelve a la asignacion automatica solo antes de la evaluacion`() {
        expect(assign(fixture.workerId), 200)
        expect(unassign(token = null), 401, "AUTH_UNAUTHENTICATED")
        expect(unassign(token = fixture.unauthorizedToken), 403, "AUTH_FORBIDDEN")
        expect(unassign(UUID.randomUUID()), 404, "APPLICATION_NOT_FOUND")

        val logsBefore = logs.count()
        val result = unassign()
        expect(result, 200)
        assertTrue(json.readTree(result.response.contentAsString).get("assignedWorkerUserId").isNull)
        assertEquals(null, assignedWorkerId())
        assertEquals(logsBefore + 1, logs.count())
        // Sin asignado no hay nada que quitar.
        expect(unassign(), 200)
        assertEquals(logsBefore + 1, logs.count())

        expect(assign(fixture.workerId), 200)
        tx {
            evaluations.saveAndFlush(
                ApplicationEvaluation(
                    application = applications.getReferenceById(fixture.applicationId),
                    vulnerability = VulnerabilityLevel.HIGH,
                    priority = EvaluationPriority.HIGH,
                    recommendation = "Visitar el domicilio.",
                    professional = users.getReferenceById(fixture.workerId),
                    evaluatedAt = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )
        }
        expect(unassign(), 409, "APPLICATION_UNASSIGNMENT_EVALUATED")
        assertEquals(fixture.workerId, assignedWorkerId())

        val approved = newApplication(ApplicationStatus.APPROVED)
        expect(unassign(approved), 409, "APPLICATION_ASSIGNMENT_STATUS_NOT_ALLOWED")
    }

    @Test
    fun `rechaza estados resueltos y reasignar con visitas programadas`() {
        val approved = newApplication(ApplicationStatus.APPROVED)
        expect(assign(fixture.workerId, applicationId = approved), 409, "APPLICATION_ASSIGNMENT_STATUS_NOT_ALLOWED")

        expect(assign(fixture.workerId), 200)
        tx {
            val application = applications.findById(fixture.applicationId).orElseThrow()
            application.status = ApplicationStatus.IN_VISIT
            val start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
            visits.saveAndFlush(
                ApplicationVisit(
                    application = application,
                    type = ApplicationVisitType.VISIT,
                    startsAt = start,
                    endsAt = start.plusHours(1),
                    location = "Domicilio",
                    responsible = users.getReferenceById(fixture.workerId),
                    scheduledAt = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )
        }
        expect(assign(fixture.otherWorkerId), 409, "APPLICATION_ASSIGNMENT_SCHEDULED_VISITS")
        assertEquals(fixture.workerId, assignedWorkerId())
        // Reasignar al mismo responsable sigue siendo un no-op permitido.
        expect(assign(fixture.workerId), 200)
    }
}
