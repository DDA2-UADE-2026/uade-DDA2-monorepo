package com.uade.dda2.server.feature.indicator

import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationEvaluation
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisit
import com.uade.dda2.server.feature.application.entity.ApplicationVisitStatus
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:indicators;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class IndicatorFlowTest {
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
    @Autowired lateinit var transactions: PlatformTransactionManager

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")
    private val today = LocalDate.now(zone)

    data class Fixture(
        val token: String,
        val unauthorizedToken: String,
        val workerId: Long,
        val programId: UUID,
        val editionId: UUID,
        val periodId: UUID,
    )

    private lateinit var fixture: Fixture

    @BeforeEach
    fun setup() {
        fixture = tx {
            fun permission(name: String) = permissions.findAll().firstOrNull { it.name == name } ?: permissions.save(Permission(name = name))
            val viewerRole = roles.save(Role(name = "INDICADORES_${UUID.randomUUID()}", permissions = mutableSetOf(permission("indicators:view"))))
            val emptyRole = roles.save(Role(name = "SIN_PERMISOS_${UUID.randomUUID()}"))
            val viewer = users.saveAndFlush(User(name = "Coordinacion", email = "${UUID.randomUUID()}@example.com", roles = mutableSetOf(viewerRole)))
            val unauthorized = users.saveAndFlush(User(name = "Sin permiso", email = "${UUID.randomUUID()}@example.com", roles = mutableSetOf(emptyRole)))
            val worker = users.saveAndFlush(User(name = "Trabajadora social", email = "${UUID.randomUUID()}@example.com"))
            val program = programs.save(Program(name = "Programa ${UUID.randomUUID()}", createdBy = viewer))
            val edition = editions.save(
                ProgramEdition(
                    program = program,
                    name = "Edicion 2026",
                    startDate = today.minusDays(200),
                    endDate = today.plusDays(200),
                    maxCapacity = 4,
                    status = ProgramEditionStatus.ACTIVE,
                    createdBy = viewer,
                ),
            )
            val period = periods.save(
                EnrollmentPeriod(
                    programEdition = edition,
                    openDate = today.minusDays(150),
                    closeDate = today.plusDays(10),
                    status = EnrollmentPeriodStatus.OPEN,
                ),
            )
            Fixture(
                token = jwt.createToken(viewer, viewerRole),
                unauthorizedToken = jwt.createToken(unauthorized, emptyRole),
                workerId = requireNotNull(worker.id),
                programId = requireNotNull(program.id),
                editionId = requireNotNull(edition.id),
                periodId = requireNotNull(period.id),
            )
        }
    }

    private fun <T : Any> tx(action: () -> T): T = TransactionTemplate(transactions).execute { action() }!!

    private fun application(
        status: ApplicationStatus,
        submittedDaysAgo: Long,
        resolvedDaysAgo: Long? = null,
        assigned: Boolean = true,
        evaluatedDaysAgo: Long? = null,
        evaluation: Pair<VulnerabilityLevel, EvaluationPriority> = VulnerabilityLevel.HIGH to EvaluationPriority.URGENT,
    ): UUID = tx {
        val applicant = users.saveAndFlush(User(name = "Titular", email = "${UUID.randomUUID()}@example.com"))
        val worker = users.getReferenceById(fixture.workerId)
        val saved = applications.saveAndFlush(
            Application(
                user = applicant,
                registeredBy = applicant,
                programEdition = editions.getReferenceById(fixture.editionId),
                enrollmentPeriod = periods.getReferenceById(fixture.periodId),
                status = status,
                assignedWorker = if (assigned) worker else null,
                submittedAt = today.minusDays(submittedDaysAgo).atTime(10, 0),
                resolvedAt = resolvedDaysAgo?.let { today.minusDays(it).atTime(12, 0) },
            ),
        )
        if (evaluatedDaysAgo != null) {
            evaluations.saveAndFlush(
                ApplicationEvaluation(
                    application = saved,
                    vulnerability = evaluation.first,
                    priority = evaluation.second,
                    recommendation = "Recomendacion",
                    professional = worker,
                    evaluatedAt = today.minusDays(evaluatedDaysAgo).atTime(10, 0).atZone(zone).toOffsetDateTime(),
                ),
            )
        }
        requireNotNull(saved.id)
    }

    private fun visit(applicationId: UUID, status: ApplicationVisitStatus, startsDaysAgo: Long) = tx {
        val start = today.minusDays(startsDaysAgo).atTime(9, 0).atZone(zone).toOffsetDateTime()
        val worker = users.getReferenceById(fixture.workerId)
        // La base exige los datos de ciclo de vida que corresponden a cada estado final.
        val finished = status == ApplicationVisitStatus.REALIZED || status == ApplicationVisitStatus.NOT_REALIZED
        val cancelled = status == ApplicationVisitStatus.CANCELLED
        visits.saveAndFlush(
            ApplicationVisit(
                application = applications.getReferenceById(applicationId),
                type = ApplicationVisitType.VISIT,
                startsAt = start,
                endsAt = start.plusHours(1),
                location = "Domicilio",
                responsible = worker,
                status = status,
                scheduledAt = start.minusDays(3),
                cancellationReason = if (cancelled) "Reorganizacion de agenda." else null,
                cancelledAt = if (cancelled) start.minusDays(1) else null,
                cancelledBy = if (cancelled) worker else null,
                resultRecordedAt = if (finished) start.plusHours(2) else null,
                resultRecordedBy = if (finished) worker else null,
            ),
        )
    }

    private fun indicators(
        from: LocalDate? = today.minusDays(29),
        to: LocalDate? = today,
        programId: UUID? = fixture.programId,
        token: String? = fixture.token,
    ): MvcResult {
        val request = get("/api/admin/indicators")
        if (from != null) request.param("from", from.toString())
        if (to != null) request.param("to", to.toString())
        if (programId != null) request.param("programId", programId.toString())
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }

    private fun body(result: MvcResult): JsonNode {
        assertEquals(200, result.response.status, result.response.contentAsString)
        return json.readTree(result.response.contentAsString)
    }

    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").stringValue())
    }

    @Test
    fun `agrega solicitudes resoluciones cupos y tiempos del periodo`() {
        // En el período (últimos 30 días).
        application(ApplicationStatus.SUBMITTED, submittedDaysAgo = 2)
        application(ApplicationStatus.IN_EVALUATION, submittedDaysAgo = 5, evaluatedDaysAgo = 3)
        application(ApplicationStatus.APPROVED, submittedDaysAgo = 20, resolvedDaysAgo = 4, evaluatedDaysAgo = 10)
        application(ApplicationStatus.REJECTED, submittedDaysAgo = 25, resolvedDaysAgo = 5, evaluatedDaysAgo = 15)
        application(ApplicationStatus.WAITLISTED, submittedDaysAgo = 12, resolvedDaysAgo = 1, evaluatedDaysAgo = 6)
        // Período anterior: presentada hace 40 días, aprobada fuera del período.
        application(ApplicationStatus.APPROVED, submittedDaysAgo = 40, resolvedDaysAgo = 35, evaluatedDaysAgo = 38)

        val data = body(indicators())
        val summary = data.get("summary")
        assertEquals(5, summary.get("applicationsSubmitted").longValue())
        assertEquals(1, summary.get("previousApplicationsSubmitted").longValue())
        assertEquals(3, summary.get("resolved").longValue())
        assertEquals(0.5, summary.get("approvalRate").doubleValue(), 1e-9)
        // Cupo actual: dos aprobadas sobre cuatro, sin importar el período.
        assertEquals(2, summary.get("approvedTotal").longValue())
        assertEquals(4, summary.get("capacityTotal").longValue())
        assertEquals(0.5, summary.get("capacityUsedRate").doubleValue(), 1e-9)
        assertEquals("WEEK", data.get("bucket").stringValue())

        val resolutions = data.get("resolutions")
        assertEquals(1, resolutions.get("approved").longValue())
        assertEquals(1, resolutions.get("rejected").longValue())
        assertEquals(1, resolutions.get("waitlisted").longValue())

        val statuses = data.get("statusDistribution").associate { it.get("status").stringValue() to it.get("count").longValue() }
        assertEquals(1, statuses["SUBMITTED"])
        assertEquals(1, statuses["IN_EVALUATION"])
        assertTrue("DRAFT" !in statuses)

        val program = data.get("programs").single()
        assertEquals(5, program.get("submitted").longValue())
        assertEquals(2, program.get("inProgress").longValue())

        val trend = data.get("applicationsTrend")
        assertEquals(5, trend.sumOf { it.get("submitted").longValue() })
        assertEquals(3, trend.sumOf { it.get("resolved").longValue() })

        val edition = data.get("capacity").single()
        assertEquals(2, edition.get("approved").longValue())
        assertEquals(1, edition.get("waitlisted").longValue())

        // Resoluciones del período: 16, 20 y 11 días hasta resolver; promedio cercano a 15,7.
        val stage = data.get("stageTimes")
        assertEquals(3, stage.get("sampleSize").longValue())
        assertEquals((16 + 20 + 11) / 3.0, stage.get("averageDaysToResolution").doubleValue(), 0.2)
        assertEquals((10 + 10 + 6) / 3.0, stage.get("averageDaysToEvaluation").doubleValue(), 0.2)
    }

    @Test
    fun `mapa de evaluacion casos pendientes visitas y carga de trabajo`() {
        application(ApplicationStatus.IN_EVALUATION, 8, evaluatedDaysAgo = 3, evaluation = VulnerabilityLevel.HIGH to EvaluationPriority.URGENT)
        application(ApplicationStatus.IN_EVALUATION, 9, evaluatedDaysAgo = 4, evaluation = VulnerabilityLevel.HIGH to EvaluationPriority.URGENT)
        application(ApplicationStatus.IN_EVALUATION, 7, evaluatedDaysAgo = 2, evaluation = VulnerabilityLevel.LOW to EvaluationPriority.MEDIUM)
        // Sin evaluar hace más de 14 días y sin asignar.
        application(ApplicationStatus.SUBMITTED, 20, assigned = false)
        application(ApplicationStatus.PENDING_DOCUMENTATION, 30, assigned = false)
        val withVisits = application(ApplicationStatus.IN_VISIT, 6, evaluatedDaysAgo = 5, evaluation = VulnerabilityLevel.MEDIUM to EvaluationPriority.HIGH)
        visit(withVisits, ApplicationVisitStatus.REALIZED, startsDaysAgo = 2)
        visit(withVisits, ApplicationVisitStatus.CANCELLED, startsDaysAgo = 3)
        visit(withVisits, ApplicationVisitStatus.SCHEDULED, startsDaysAgo = -3)

        val data = body(indicators(to = today.plusDays(7)))
        val matrix = data.get("evaluationMatrix")
        assertEquals(16, matrix.size())
        fun cell(v: String, p: String) = matrix.first { it.get("vulnerability").stringValue() == v && it.get("priority").stringValue() == p }.get("count").longValue()
        assertEquals(2, cell("HIGH", "URGENT"))
        assertEquals(1, cell("LOW", "MEDIUM"))
        assertEquals(0, cell("CRITICAL", "LOW"))

        val attention = data.get("attention")
        assertEquals(2, attention.get("unassigned").longValue())
        assertEquals(2, attention.get("awaitingEvaluation").longValue())
        assertEquals(30, attention.get("oldestAwaitingEvaluation").first().get("daysWaiting").longValue())

        assertEquals(1, data.get("summary").get("visitsRealized").longValue())
        val visitTrend = data.get("visitsTrend")
        assertEquals(1, visitTrend.sumOf { it.get("realized").longValue() })
        assertEquals(1, visitTrend.sumOf { it.get("cancelled").longValue() })
        assertEquals(1, visitTrend.sumOf { it.get("scheduled").longValue() })

        val load = data.get("workload").single()
        assertEquals(fixture.workerId, load.get("userId").longValue())
        assertEquals(4, load.get("activeCases").longValue())
        assertEquals(1, load.get("scheduledVisits").longValue())
    }

    @Test
    fun `usa meses en periodos largos y aisla por programa`() {
        application(ApplicationStatus.SUBMITTED, 100)
        val data = body(indicators(from = today.minusDays(179)))
        assertEquals("MONTH", data.get("bucket").stringValue())
        assertTrue(data.get("applicationsTrend").all { LocalDate.parse(it.get("periodStart").stringValue()).dayOfMonth == 1 })
        assertEquals(1, data.get("summary").get("applicationsSubmitted").longValue())

        val otherProgram = body(indicators(programId = UUID.randomUUID()))
        assertEquals(0, otherProgram.get("summary").get("applicationsSubmitted").longValue())
        assertEquals(0, otherProgram.get("capacity").size())
    }

    @Test
    fun `valida periodo y permisos`() {
        expect(indicators(token = null), 401, "AUTH_UNAUTHENTICATED")
        expect(indicators(token = fixture.unauthorizedToken), 403, "AUTH_FORBIDDEN")
        expect(indicators(from = today, to = today.minusDays(1)), 400, "INDICATORS_INVALID_RANGE")
        expect(indicators(from = today.minusDays(1200)), 400, "INDICATORS_INVALID_RANGE")
        // Sin fechas toma los últimos 90 días.
        val defaults = body(indicators(from = null, to = null))
        assertEquals(today.toString(), defaults.get("to").stringValue())
        assertEquals(today.minusDays(89).toString(), defaults.get("from").stringValue())
        assertTrue(OffsetDateTime.parse(defaults.get("generatedAt").stringValue()).isBefore(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1)))
    }
}
