package com.uade.dda2.server.feature.intervention

import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.application.repository.*
import com.uade.dda2.server.feature.auth.entity.*
import com.uade.dda2.server.feature.auth.repository.*
import com.uade.dda2.server.feature.enrollmentperiod.entity.*
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.intervention.dto.response.*
import com.uade.dda2.server.feature.intervention.entity.*
import com.uade.dda2.server.feature.intervention.repository.*
import com.uade.dda2.server.feature.intervention.service.*
import com.uade.dda2.server.feature.program.entity.*
import com.uade.dda2.server.feature.program.entity.enums.*
import com.uade.dda2.server.feature.program.repository.*
import com.uade.dda2.server.security.JwtService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.*
import org.springframework.test.web.servlet.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.nio.file.*
import java.time.*
import java.util.UUID
import java.util.concurrent.*
import kotlin.test.*
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory

@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true", "app.enrollment-period.expiration.cron=-"])
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class InterventionPlanFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var applications: ApplicationRepository
    @Autowired lateinit var evaluations: ApplicationEvaluationRepository
    @Autowired lateinit var programs: ProgramRepository
    @Autowired lateinit var editions: ProgramEditionRepository
    @Autowired lateinit var periods: EnrollmentPeriodRepository
    @Autowired lateinit var benefits: ProgramBenefitRepository
    @Autowired lateinit var plans: InterventionPlanRepository
    @Autowired lateinit var actions: InterventionActionRepository
    @Autowired lateinit var followUps: InterventionFollowUpRepository
    @Autowired lateinit var transactions: PlatformTransactionManager
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var publications: InterventionPublicationRepository
    @Autowired lateinit var entityManagerFactory: EntityManagerFactory

    companion object {
        @JvmStatic @DynamicPropertySource fun database(registry: DynamicPropertyRegistry) {
            val postgres = System.getProperty("intervention.test.postgres-url")
            registry.add("spring.datasource.url") { postgres ?: "jdbc:h2:mem:interventions;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000" }
            registry.add("spring.datasource.driver-class-name") { if (postgres == null) "org.h2.Driver" else "org.postgresql.Driver" }
            registry.add("spring.jpa.properties.hibernate.dialect") { if (postgres == null) "org.hibernate.dialect.H2Dialect" else "org.hibernate.dialect.PostgreSQLDialect" }
            registry.add("spring.datasource.username") { if (postgres == null) "sa" else System.getProperty("intervention.test.postgres-user", "application_test") }
            registry.add("spring.datasource.password") { System.getProperty("intervention.test.postgres-password", "") }
        }
    }
    data class Fixture(val id: UUID, val citizenId: Long, val actorId: Long, val roleId: Long, val evaluatorId: Long,
        val editionId: UUID, val token: String, val citizenToken: String)
    private lateinit var f: Fixture
    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")
    private val today get() = LocalDate.now(zone)
    private fun <T : Any> tx(action: () -> T): T = TransactionTemplate(transactions).execute { action() }!!
    private fun permission(name: String) = permissions.findByNameIn(setOf(name)).firstOrNull() ?: permissions.save(Permission(name = name))
    @BeforeEach fun setup() { f = fixture() }
    private fun fixture(citizenId: Long? = null): Fixture = tx {
        val role = roles.saveAndFlush(Role(name = "TRABAJADOR_${UUID.randomUUID()}", permissions = mutableSetOf(
            permission(INTERVENTION_CREATE_PERMISSION), permission(INTERVENTION_FOLLOW_UP_PERMISSION), permission(INTERVENTION_VIEW_PERMISSION), permission(INTERVENTION_PUBLISH_PERMISSION),
            permission("applications:management:benefits:assign"), permission("applications:management:benefits:manage"), permission("users:delete"))))
        val actor = users.saveAndFlush(User(name = "Trabajador", email = "worker@example.com", roles = mutableSetOf(role)))
        val citizenRole = roles.saveAndFlush(Role(name = "CIUDADANO_${UUID.randomUUID()}", permissions = mutableSetOf(permission("applications:own:view"), permission(INTERVENTION_OWN_VIEW_PERMISSION))))
        val citizen = if (citizenId == null) users.saveAndFlush(User(name = "Ciudadano", email = "citizen@example.com", roles = mutableSetOf(citizenRole)))
            else users.findByIdWithRoles(citizenId)!!
        val evaluator = users.saveAndFlush(User(name = "Evaluador anterior", email = "evaluator@example.com"))
        val program = programs.saveAndFlush(Program(name = "Programa ${UUID.randomUUID()}", createdBy = evaluator))
        val edition = editions.saveAndFlush(ProgramEdition(program = program, name = "Edicion",
            startDate = today.minusDays(20), endDate = today.plusDays(20), maxCapacity = 1, currentEnrollment = 1,
            status = ProgramEditionStatus.ACTIVE, createdBy = evaluator))
        val period = periods.saveAndFlush(EnrollmentPeriod(programEdition = edition, openDate = today, closeDate = today, status = EnrollmentPeriodStatus.CLOSED))
        val initial = today.minusDays(2).atStartOfDay()
        val application = applications.saveAndFlush(Application(user = citizen, registeredBy = citizen, programEdition = edition,
            enrollmentPeriod = period, status = ApplicationStatus.IN_EVALUATION, assignedWorker = actor, submittedAt = initial,
            resolutionReason = "Resolucion privada", resolvedBy = evaluator, resolvedAt = initial, updatedAt = initial))
        evaluations.saveAndFlush(ApplicationEvaluation(application = application, vulnerability = VulnerabilityLevel.HIGH,
            priority = EvaluationPriority.HIGH, recommendation = "Evaluacion privada", professional = evaluator,
            evaluatedAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)))
        Fixture(application.id!!, citizen.id!!, actor.id!!, role.id!!, evaluator.id!!, edition.id!!,
            jwt.createToken(actor, role), jwt.createToken(citizen, citizen.roles.first { r -> r.permissions.any { it.name == INTERVENTION_OWN_VIEW_PERMISSION } }))
    }

    private fun action(description: String = " Accion privada ", start: LocalDate = today.minusDays(10), due: LocalDate = today.minusDays(1)) =
        mapOf("description" to description, "startDate" to start, "dueDate" to due)
    private fun body() = json.writeValueAsString(mapOf("objectives" to listOf(mapOf("description" to " Objetivo privado ", "actions" to listOf(action())))))
    private fun followBody(status: String = "IN_PROGRESS", date: LocalDate = today, notes: String = " Notas privadas ",
        result: String? = if (status == "COMPLETED") " Resultado privado " else null) =
        json.writeValueAsString(mapOf("status" to status, "occurredOn" to date, "notes" to notes, "result" to result))
    private fun create(fixture: Fixture = f, token: String? = fixture.token, content: String = body()): MvcResult {
        val req = post("/api/applications/${fixture.id}/intervention-plan").contentType(MediaType.APPLICATION_JSON).content(content)
        if (token != null) req.header("Authorization", "Bearer $token")
        return mvc.perform(req).andReturn()
    }
    private fun getPlan(id: UUID = f.id, token: String? = f.token): MvcResult {
        val req = get("/api/admin/applications/$id/intervention-plan")
        if (token != null) req.header("Authorization", "Bearer $token")
        return mvc.perform(req).andReturn()
    }
    private fun follow(actionId: UUID, content: String = followBody(), id: UUID = f.id, token: String? = f.token): MvcResult {
        val req = post("/api/applications/$id/intervention-plan/actions/$actionId/follow-ups").contentType(MediaType.APPLICATION_JSON).content(content)
        if (token != null) req.header("Authorization", "Bearer $token")
        return mvc.perform(req).andReturn()
    }
    private fun history(actionId: UUID, id: UUID = f.id, token: String? = f.token): MvcResult {
        val req = get("/api/admin/applications/$id/intervention-plan/actions/$actionId/follow-ups")
        if (token != null) req.header("Authorization", "Bearer $token")
        return mvc.perform(req).andReturn()
    }
    private fun expect(r: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, r.response.status, r.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(r.response.contentAsString).get("code").stringValue())
    }
    private fun plan(r: MvcResult) = json.readValue(r.response.contentAsString, InterventionPlanResponse::class.java)
    private fun created(fixture: Fixture = f): InterventionPlanResponse { val r = create(fixture); expect(r, 201); return plan(r) }
    private fun assertCasePreserved(status: ApplicationStatus = ApplicationStatus.IN_EVALUATION, enrollment: Int = 1) = tx {
        val a = applications.findDetailById(f.id)!!
        assertEquals(status, a.status)
        assertEquals(f.actorId, a.assignedWorker?.id)
        assertEquals("Resolucion privada", a.resolutionReason)
        assertEquals(f.evaluatorId, a.resolvedBy?.id)
        assertEquals(today.minusDays(2).atStartOfDay(), a.resolvedAt)
        assertEquals(enrollment, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        true
    }

    @Test fun `crea estructura ordenada con ciudadano responsable UTC y auditoria sin textos`() {
        val content = json.writeValueAsString(mapOf("objectives" to listOf(
            mapOf("description" to " Primer objetivo ", "actions" to listOf(action(" Primera "), action(" Segunda ", today.plusDays(1), today.plusDays(10)))),
            mapOf("description" to " Segundo objetivo ", "actions" to listOf(action(" Tercera ", today, today))))))
        val r = create(content = content); expect(r, 201)
        val p = plan(r)
        assertEquals(f.id, p.applicationId); assertEquals(f.citizenId, p.userId); assertEquals(f.actorId, p.responsibleUserId)
        assertEquals(listOf("Primer objetivo", "Segundo objetivo"), p.objectives.map { it.description })
        assertEquals(listOf("Primera", "Segunda"), p.objectives[0].actions.map { it.description })
        p.objectives.flatMap { it.actions }.forEach {
            assertEquals(InterventionActionStatus.PENDING, it.status); assertEquals(f.actorId, it.responsibleUserId)
            expect(history(it.id), 200); assertEquals(0, json.readTree(history(it.id).response.contentAsString).size())
        }
        assertEquals(ZoneOffset.UTC, p.createdAt.offset)
        assertEquals(LocalDateTime.ofInstant(p.createdAt.toInstant(), zone), applications.findById(f.id).orElseThrow().updatedAt)
        assertEquals(p, plan(getPlan()))
        expect(create(), 409, "INTERVENTION_PLAN_ALREADY_EXISTS")
        val audit = jdbc.queryForObject("select new_values::text from logs where entity_type='intervention_plan' and entity_id=?", String::class.java, p.id.toString())!!
        assertTrue(audit.contains("responsibleUserId")); assertFalse(audit.contains("Primer objetivo")); assertFalse(audit.contains("Primera"))
        assertCasePreserved()
    }

    @ParameterizedTest @EnumSource(ApplicationStatus::class, names = ["IN_EVALUATION", "IN_VISIT", "WAITLISTED", "APPROVED", "REJECTED", "CLOSED"])
    fun `permite planes y seguimientos en todos los casos evaluados incluso resueltos`(status: ApplicationStatus) {
        tx { applications.findById(f.id).orElseThrow().status = status }
        val p = created()
        expect(follow(p.objectives[0].actions[0].id, followBody("COMPLETED")), 201)
        assertCasePreserved(status)
    }
    @ParameterizedTest @EnumSource(ApplicationStatus::class, names = ["DRAFT", "SUBMITTED", "IN_VALIDATION", "PENDING_DOCUMENTATION"])
    fun `estados previos no admiten plan aunque tengan datos inconsistentes de evaluacion`(status: ApplicationStatus) {
        tx { applications.findById(f.id).orElseThrow().status = status }
        expect(create(), 409, "INTERVENTION_PLAN_STATUS_NOT_ALLOWED")
        assertFalse(plans.existsByApplicationId(f.id))
    }
    @Test fun `exige evaluacion y trabajador asignado coincidente`() {
        tx { evaluations.findByApplicationId(f.id)?.let { evaluations.delete(it) }; true }
        expect(create(), 409, "INTERVENTION_PLAN_EVALUATION_REQUIRED")
        val other = fixture()
        expect(create(other, token = f.token), 409, "INTERVENTION_PLAN_ASSIGNED_TO_ANOTHER_WORKER")
        tx { applications.findById(other.id).orElseThrow().assignedWorker = null; true }
        expect(create(other), 409, "INTERVENTION_PLAN_WORKER_REQUIRED")
    }
    @Test fun `plan independiente de estado y fechas de la edicion`() {
        tx { editions.findById(f.editionId).orElseThrow().also { it.status = ProgramEditionStatus.CLOSED; it.endDate = today.minusDays(15) } }
        val p = created()
        expect(follow(p.objectives[0].actions[0].id), 201)
        assertCasePreserved()
    }

    @Test fun `valida estructura textos fechas y rechaza campos adicionales en todos los niveles`() {
        val valid = body()
        val invalid = listOf("{}", "null", "[]", """{"objectives":null}""", """{"objectives":[]}""", """{"objectives":[null]}""",
            """{"objectives":[{}]}""", """{"objectives":[{"description":"x","actions":[]}]}""",
            """{"objectives":[{"description":"x","actions":[null]}]}""", """{"objectives":[{"description":"x","actions":[{}]}]}""",
            valid.replace(" Objetivo privado ", " "), valid.replace(" Accion privada ", "a".repeat(1001)),
            valid.replace(" Objetivo privado ", "a".repeat(1001)), valid.replace(" Accion privada ", " "),
            valid.replace(today.minusDays(10).toString(), today.plusDays(1).toString()),
            valid.replace(today.minusDays(10).toString(), "2026-99-01"),
            valid.dropLast(1) + ""","userId":1}""", valid.dropLast(1) + ""","responsibleUserId":1}""",
            valid.dropLast(1) + ""","createdAt":"2026-10-19T00:00:00Z"}""",
            valid.replace("\"actions\":", "\"id\":\"x\",\"actions\":"),
            valid.replace("\"startDate\":", "\"status\":\"PENDING\",\"startDate\":"))
        invalid.forEach { expect(create(content = it), 400, "INTERVENTION_PLAN_INVALID_DATA") }
        assertFalse(plans.existsByApplicationId(f.id))
        assertEquals(today.minusDays(2).atStartOfDay(), applications.findById(f.id).orElseThrow().updatedAt)
    }

    @Test fun `avances repetidos y resultado definitivo conservan historial y fechas reales`() {
        val p = created(); val a = p.objectives[0].actions[0]
        val dates = listOf(today.minusDays(10), today.minusDays(5), today, today)
        dates.forEachIndexed { i, date ->
            val r = follow(a.id, followBody(if (i == 3) "COMPLETED" else "IN_PROGRESS", date))
            expect(r, 201)
            val entry = json.readValue(r.response.contentAsString, InterventionFollowUpResponse::class.java)
            assertEquals(i + 1, entry.sequenceNumber)
            assertEquals(if (i == 0) InterventionActionStatus.PENDING else InterventionActionStatus.IN_PROGRESS, entry.previousStatus)
            assertEquals("Notas privadas", entry.notes); assertEquals(f.actorId, entry.authorUserId)
            assertEquals(ZoneOffset.UTC, entry.recordedAt.offset)
            assertEquals(date, entry.occurredOn)
        }
        val entries = json.readTree(history(a.id).response.contentAsString)
        assertEquals(4, entries.size()); assertEquals("Resultado privado", entries[3].get("result").stringValue())
        expect(follow(a.id), 409, "INTERVENTION_ACTION_ALREADY_COMPLETED")
        expect(follow(a.id, followBody("COMPLETED")), 409)
        val current = plan(getPlan())
        assertEquals(InterventionActionStatus.COMPLETED, current.objectives[0].actions[0].status)
        assertEquals(a.description, current.objectives[0].actions[0].description)
        assertEquals(a.startDate, current.objectives[0].actions[0].startDate)
        assertEquals(a.dueDate, current.objectives[0].actions[0].dueDate)
        assertCasePreserved()
        val audits = jdbc.queryForList("select new_values::text from logs where entity_type='intervention_follow_up' and action='CREATE'", String::class.java)
        audits.forEach { assertFalse(it.contains("Notas privadas")); assertFalse(it.contains("Resultado privado")); assertTrue(it.contains("previousStatus")) }
    }

    @Test fun `valida seguimiento obligatorio fechas cronologia y campos extra sin modificar accion`() {
        val p = created(); val a = p.objectives[0].actions[0]
        val valid = followBody()
        val invalid = listOf("{}", "null", "[]", valid.replace("IN_PROGRESS", "PENDING"), valid.replace("IN_PROGRESS", "OTHER"),
            valid.replace("IN_PROGRESS", "COMPLETED"), valid.replace(" Notas privadas ", " "),
            valid.replace(" Notas privadas ", "x".repeat(2001)), valid.replace("\"result\":null", "\"result\":\" \""),
            valid.replace(today.toString(), today.plusDays(1).toString()), valid.replace(today.toString(), a.startDate.minusDays(1).toString()),
            valid.replace(today.toString(), "2026-99-01"), valid.dropLast(1) + ""","authorUserId":1}""",
            valid.dropLast(1) + ""","sequenceNumber":1}""", valid.dropLast(1) + ""","recordedAt":"2026-10-19T00:00:00Z"}""",
            followBody("COMPLETED", result = "x".repeat(2001)))
        invalid.forEach { expect(follow(a.id, it), 400, "INTERVENTION_FOLLOW_UP_INVALID_DATA") }
        assertEquals(InterventionActionStatus.PENDING, actions.findById(a.id).orElseThrow().status)
        assertEquals(0, followUps.findAllByActionIdOrderBySequenceNumberAsc(a.id).size)
        assertEquals(LocalDateTime.ofInstant(p.createdAt.toInstant(), zone), applications.findById(f.id).orElseThrow().updatedAt)
        expect(follow(a.id, followBody(date = today.minusDays(1), result = " Avance parcial ")), 201)
        expect(follow(a.id, followBody(date = today.minusDays(2))), 400)
        expect(follow(a.id, followBody(date = today.minusDays(1))), 201)
        assertEquals(2, followUps.findAllByActionIdOrderBySequenceNumberAsc(a.id).size)
    }

    @Test fun `recursos ajenos ausentes y lectura por permiso sin ser responsable`() {
        expect(getPlan(), 404, "INTERVENTION_PLAN_NOT_FOUND")
        expect(history(UUID.randomUUID()), 404, "INTERVENTION_PLAN_NOT_FOUND")
        expect(follow(UUID.randomUUID()), 404, "INTERVENTION_PLAN_NOT_FOUND")
        val p = created(); val a = p.objectives[0].actions[0].id
        val other = fixture()
        expect(getPlan(token = other.token), 200)
        expect(history(a, token = other.token), 200)
        expect(follow(a, token = other.token), 409, "INTERVENTION_PLAN_ASSIGNED_TO_ANOTHER_WORKER")
        expect(follow(a, id = other.id, token = other.token), 404, "INTERVENTION_PLAN_NOT_FOUND")
        expect(create(other), 201)
        expect(history(a, id = other.id), 404, "INTERVENTION_ACTION_NOT_FOUND")
        expect(follow(a, id = other.id, token = other.token), 404, "INTERVENTION_ACTION_NOT_FOUND")
        expect(follow(UUID.randomUUID()), 404, "INTERVENTION_ACTION_NOT_FOUND")
        expect(history(UUID.randomUUID()), 404, "INTERVENTION_ACTION_NOT_FOUND")
        val absent = UUID.randomUUID()
        expect(create(f.copy(id = absent)), 404, "APPLICATION_NOT_FOUND")
        expect(getPlan(absent), 404, "APPLICATION_NOT_FOUND")
        expect(history(a, absent), 404, "APPLICATION_NOT_FOUND")
        expect(follow(a, id = absent), 404, "APPLICATION_NOT_FOUND")
        tx { applications.findById(f.id).orElseThrow().assignedWorker = users.findById(other.actorId).orElseThrow() }
        expect(follow(a), 409, "INTERVENTION_PLAN_ASSIGNED_TO_ANOTHER_WORKER")
        expect(follow(a, token = other.token), 409, "INTERVENTION_PLAN_ASSIGNED_TO_ANOTHER_WORKER")
    }

    @Test fun `seguridad actual por usuario rol activo y permisos de JWT y base`() {
        expect(create(token = null), 401); expect(create(token = f.citizenToken), 403)
        tx { roles.findById(f.roleId).orElseThrow().permissions.removeIf { it.name == INTERVENTION_CREATE_PERMISSION }; true }
        expect(create(), 403)
        tx { roles.findById(f.roleId).orElseThrow().permissions.add(permission(INTERVENTION_CREATE_PERMISSION)) }
        val p = created(); val a = p.objectives[0].actions[0].id
        expect(follow(a, token = null), 401); expect(follow(a, token = f.citizenToken), 403)
        expect(history(a, token = f.citizenToken), 403); expect(getPlan(token = null), 401)
        tx { roles.findById(f.roleId).orElseThrow().permissions.removeIf { it.name == INTERVENTION_FOLLOW_UP_PERMISSION }; true }
        expect(follow(a), 403); expect(history(a), 200)
        tx { roles.findById(f.roleId).orElseThrow().permissions.add(permission(INTERVENTION_FOLLOW_UP_PERMISSION)) }
        val reader = tx {
            val role = roles.saveAndFlush(Role(name = "LECTOR_${UUID.randomUUID()}", permissions = mutableSetOf(permission(INTERVENTION_VIEW_PERMISSION))))
            val actor = users.findById(f.actorId).orElseThrow(); actor.roles.add(role)
            jwt.createToken(actor, role) to role.id!!
        }
        expect(getPlan(token = reader.first), 200)
        expect(follow(a, token = reader.first), 403)
        tx { roles.findById(reader.second).orElseThrow().permissions.add(permission(INTERVENTION_FOLLOW_UP_PERMISSION)) }
        expect(follow(a, token = reader.first), 403)
        tx { roles.findById(f.roleId).orElseThrow().permissions.removeIf { it.name == INTERVENTION_VIEW_PERMISSION }; true }
        expect(getPlan(), 403); expect(history(a), 403)
        tx { users.findById(f.actorId).orElseThrow().roles.clear(); true }
        expect(follow(a), 403)
        tx { users.findById(f.actorId).orElseThrow().roles.add(roles.findById(f.roleId).orElseThrow()) }
        tx { users.findById(f.actorId).orElseThrow().active = false }
        expect(create(), 401); expect(follow(a), 401); expect(getPlan(), 401); expect(history(a), 401)
        assertTrue(followUps.findAllByActionIdOrderBySequenceNumberAsc(a).isEmpty())
    }

    @Test fun `privacidad ciudadana estructura sin edicion y proteccion de responsables`() {
        val p = created(); val a = p.objectives[0].actions[0].id
        expect(follow(a), 201)
        val citizen = mvc.perform(get("/api/applications/${f.id}").header("Authorization", "Bearer ${f.citizenToken}")).andReturn()
        expect(citizen, 200)
        val content = json.readTree(citizen.response.contentAsString)
        listOf("interventionPlan", "objectives", "followUps", "notes", "result").forEach { assertFalse(content.has(it)) }
        listOf(put("/api/applications/${f.id}/intervention-plan"), delete("/api/applications/${f.id}/intervention-plan")).forEach {
            expect(mvc.perform(it.header("Authorization", "Bearer ${f.token}")).andReturn(), 405)
        }
        tx { applications.findById(f.id).orElseThrow().assignedWorker = null; true } // The plan itself must retain its responsible.
        expect(mvc.perform(delete("/users/${f.actorId}").header("Authorization", "Bearer ${fixture().token}")).andReturn(),
            409, "USER_HAS_APPLICATION_REFERENCES")
        assertTrue(users.existsById(f.actorId))
        expect(follow(a), 409, "INTERVENTION_PLAN_WORKER_REQUIRED")
    }

    @Test fun `auditoria fallida revierte alta completa incluidos objetivos y acciones`() {
        jdbc.execute("alter table logs add constraint ck_intervention_plan_audit_failure check (entity_type <> 'intervention_plan' or user_id <> ${f.actorId})")
        try {
            expect(create(), 409, "DATA_INTEGRITY_VIOLATION")
            assertFalse(plans.existsByApplicationId(f.id))
            assertEquals(today.minusDays(2).atStartOfDay(), applications.findById(f.id).orElseThrow().updatedAt)
            assertEquals(0L, jdbc.queryForObject("select count(*) from intervention_objective o left join intervention_plan p on p.id=o.plan_id where p.id is null", Long::class.java))
            assertCasePreserved()
        } finally { jdbc.execute("alter table logs drop constraint ck_intervention_plan_audit_failure") }
    }
    @ParameterizedTest @ValueSource(strings = ["IN_PROGRESS", "COMPLETED"])
    fun `auditoria fallida revierte seguimiento estado y fecha sin borrar historial previo`(status: String) {
        val p = created(); val a = p.objectives[0].actions[0].id
        expect(follow(a, followBody(date = today.minusDays(1))), 201)
        val oldTime = applications.findById(f.id).orElseThrow().updatedAt
        val previousId = followUps.findAllByActionIdOrderBySequenceNumberAsc(a).single().id
        jdbc.execute("alter table logs add constraint ck_intervention_follow_audit_failure check (entity_type <> 'intervention_follow_up' or user_id <> ${f.actorId} or entity_id = '$previousId')")
        try {
            expect(follow(a, followBody(status)), 409, "DATA_INTEGRITY_VIOLATION")
            assertEquals(InterventionActionStatus.IN_PROGRESS, actions.findById(a).orElseThrow().status)
            assertEquals(1, followUps.findAllByActionIdOrderBySequenceNumberAsc(a).size)
            assertEquals(oldTime, applications.findById(f.id).orElseThrow().updatedAt)
            assertCasePreserved()
        } finally { jdbc.execute("alter table logs drop constraint ck_intervention_follow_audit_failure") }
    }


    @Test fun `migracion PostgreSQL repetible sin planes fabricados y con restricciones completas`() {
        assumeTrue(System.getProperty("intervention.test.postgres-url") != null)
        val schema = "intervention_migration_" + UUID.randomUUID().toString().replace("-", "")
        jdbc.execute("create schema $schema")
        try {
            jdbc.execute("""
                create table $schema.users (id bigint primary key);
                create table $schema.application (id uuid primary key);
                create table $schema.permissions (id bigserial primary key, name varchar(100) unique);
                create table $schema.roles (id bigserial primary key, name varchar(50) unique);
                create table $schema.role_permissions (role_id bigint references $schema.roles(id), permission_id bigint references $schema.permissions(id),
                    primary key (role_id, permission_id));
                insert into $schema.users values (1), (2);
                insert into $schema.roles(name) values ('ADMIN'), ('CIUDADANO');
            """.trimIndent())
            jdbc.update("insert into $schema.application values (?)", f.id)
            val migration = Files.readString(Path.of("docs/database/migrations/2026-10-19-intervention-plans.sql")).replace("public.", "$schema.")
            repeat(2) { jdbc.execute(migration) }
            listOf("intervention_plan", "intervention_objective", "intervention_action", "intervention_follow_up").forEach {
                assertEquals(0L, jdbc.queryForObject("select count(*) from $schema.$it", Long::class.java))
            }
            assertEquals(3L, jdbc.queryForObject("select count(*) from $schema.role_permissions rp join $schema.roles r on r.id=rp.role_id where r.name='ADMIN'", Long::class.java))
            assertEquals(0L, jdbc.queryForObject("select count(*) from $schema.role_permissions rp join $schema.roles r on r.id=rp.role_id where r.name='CIUDADANO'", Long::class.java))
            val planId = UUID.randomUUID(); val objectiveId = UUID.randomUUID(); val actionId = UUID.randomUUID()
            val insertPlan = "insert into $schema.intervention_plan values (?, ?, 1, current_timestamp)"
            jdbc.update(insertPlan, planId, f.id)
            assertFailsWith<DataIntegrityViolationException> { jdbc.update(insertPlan, UUID.randomUUID(), f.id) }
            jdbc.update("insert into $schema.intervention_objective values (?, ?, 0, 'Objetivo')", objectiveId, planId)
            val insertAction = "insert into $schema.intervention_action values (?, ?, 0, 'Accion', ?, ?, 'PENDING')"
            jdbc.update(insertAction, actionId, objectiveId, today.minusDays(1), today)
            assertFailsWith<DataIntegrityViolationException> { jdbc.update("update $schema.intervention_action set due_date=? where id=?", today.minusDays(2), actionId) }
            val insertFollow = """insert into $schema.intervention_follow_up
                (id, action_id, sequence_number, previous_status, status, occurred_on, notes, result, author_user_id, recorded_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, 2, current_timestamp)"""
            jdbc.update(insertFollow, UUID.randomUUID(), actionId, 1, "PENDING", "IN_PROGRESS", today, "Notas", null)
            assertFailsWith<DataIntegrityViolationException> { jdbc.update(insertFollow, UUID.randomUUID(), actionId, 1, "IN_PROGRESS", "IN_PROGRESS", today, "Notas", null) }
            assertFailsWith<DataIntegrityViolationException> { jdbc.update(insertFollow, UUID.randomUUID(), actionId, 2, "IN_PROGRESS", "COMPLETED", today, "Notas", null) }
            assertFailsWith<DataIntegrityViolationException> { jdbc.update(insertFollow, UUID.randomUUID(), actionId, 2, "COMPLETED", "IN_PROGRESS", today, "Notas", null) }
            assertFailsWith<DataIntegrityViolationException> { jdbc.update(insertFollow, UUID.randomUUID(), actionId, 2, "IN_PROGRESS", "IN_PROGRESS", today, " ", null) }
            jdbc.update(insertFollow, UUID.randomUUID(), actionId, 2, "IN_PROGRESS", "COMPLETED", today, "Cierre", "Resultado")
            jdbc.update("update $schema.intervention_action set status='COMPLETED' where id=?", actionId)
            jdbc.execute(migration)
            assertEquals(1L, jdbc.queryForObject("select count(*) from $schema.intervention_plan", Long::class.java))
            assertEquals(2L, jdbc.queryForObject("select count(*) from $schema.intervention_follow_up", Long::class.java))
            assertEquals("COMPLETED", jdbc.queryForObject("select status from $schema.intervention_action", String::class.java))
            assertFailsWith<DataIntegrityViolationException> { jdbc.execute("delete from $schema.users where id=1") }
            assertFailsWith<DataIntegrityViolationException> { jdbc.execute("delete from $schema.users where id=2") }
            assertFailsWith<DataIntegrityViolationException> { jdbc.execute("delete from $schema.application") }
            assertFailsWith<DataIntegrityViolationException> { jdbc.execute("delete from $schema.intervention_action") }
            val citizenMigration = Files.readString(Path.of("docs/database/migrations/2026-10-26-citizen-intervention-plans.sql")).replace("public.", "$schema.")
            repeat(2) { jdbc.execute(citizenMigration) }
            assertEquals(0L, jdbc.queryForObject("select count(*) from $schema.intervention_follow_up_publication", Long::class.java))
            assertEquals(2L, jdbc.queryForObject("select count(*) from $schema.intervention_follow_up", Long::class.java))
            assertEquals("Notas", jdbc.queryForObject("select notes from $schema.intervention_follow_up where sequence_number=1", String::class.java))
            assertEquals(5L, jdbc.queryForObject("select count(*) from $schema.role_permissions rp join $schema.roles r on r.id=rp.role_id where r.name='ADMIN'", Long::class.java))
            assertEquals(1L, jdbc.queryForObject("select count(*) from $schema.role_permissions rp join $schema.roles r on r.id=rp.role_id where r.name='CIUDADANO'", Long::class.java))
            val source = jdbc.queryForObject("select id from $schema.intervention_follow_up where sequence_number=1", UUID::class.java)!!
            val pubId = UUID.randomUUID()
            jdbc.execute("insert into $schema.users values(3)")
            val insertPublication = """insert into $schema.intervention_follow_up_publication
                (id,follow_up_id,summary,visible,published_at,updated_at,updated_by_user_id)
                values(?,?,'Resumen publicado',true,current_timestamp,current_timestamp,3)"""
            jdbc.update(insertPublication, pubId, source)
            jdbc.execute(citizenMigration)
            assertEquals(1L, jdbc.queryForObject("select count(*) from $schema.intervention_follow_up_publication", Long::class.java))
            assertFailsWith<DataIntegrityViolationException> { jdbc.update(insertPublication, UUID.randomUUID(), source) }
            assertFailsWith<DataIntegrityViolationException> { jdbc.execute("update $schema.intervention_follow_up_publication set summary=' '") }
            assertFailsWith<DataIntegrityViolationException> { jdbc.execute("update $schema.intervention_follow_up_publication set updated_at=published_at-interval '1 day'") }
            assertFailsWith<DataIntegrityViolationException> { jdbc.execute("delete from $schema.users where id=3") }
            assertFailsWith<DataIntegrityViolationException> { jdbc.update("delete from $schema.intervention_follow_up where id=?", source) }
            jdbc.execute("update $schema.intervention_follow_up_publication set visible=false")
            jdbc.execute(citizenMigration)
            assertEquals(false, jdbc.queryForObject("select visible from $schema.intervention_follow_up_publication", Boolean::class.java))
        } finally { jdbc.execute("drop schema $schema cascade") }
        assertFalse(plans.existsByApplicationId(f.id))
    }

    private fun concurrently(first: () -> MvcResult, second: () -> MvcResult): List<MvcResult> {
        val ready = CountDownLatch(2); val start = CountDownLatch(1); val pool = Executors.newFixedThreadPool(2)
        return try {
            val tasks = listOf(first, second).map { action -> pool.submit<MvcResult> {
                ready.countDown(); assertTrue(start.await(5, TimeUnit.SECONDS)); action()
            } }
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown()
            tasks.map { it.get(25, TimeUnit.SECONDS) }
        } finally { pool.shutdownNow() }
    }
    private fun ownPlans(token: String? = f.citizenToken, query: String = ""): MvcResult {
        val request = get("/api/intervention-plans$query")
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }
    private fun ownPlan(planId: UUID, token: String? = f.citizenToken): MvcResult {
        val request = get("/api/intervention-plans/$planId")
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }
    private fun ownFollowUps(planId: UUID, actionId: UUID, token: String? = f.citizenToken, query: String = ""): MvcResult {
        val request = get("/api/intervention-plans/$planId/actions/$actionId/follow-ups$query")
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }
    private fun entry(actionId: UUID, content: String = followBody(), fixture: Fixture = f): InterventionFollowUpResponse {
        val r = follow(actionId, content, fixture.id, fixture.token); expect(r, 201)
        return json.readValue(r.response.contentAsString, InterventionFollowUpResponse::class.java)
    }
    private fun publication(actionId: UUID, followUpId: UUID, summary: String = " Resumen acordado ",
        fixture: Fixture = f, token: String? = fixture.token, content: String = json.writeValueAsString(mapOf("summary" to summary))): MvcResult {
        val request = put("/api/applications/${fixture.id}/intervention-plan/actions/$actionId/follow-ups/$followUpId/citizen-publication")
            .contentType(MediaType.APPLICATION_JSON).content(content)
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }
    private fun withdraw(actionId: UUID, followUpId: UUID, fixture: Fixture = f, token: String? = fixture.token): MvcResult {
        val request = delete("/api/applications/${fixture.id}/intervention-plan/actions/$actionId/follow-ups/$followUpId/citizen-publication")
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }
    private fun publicPlans(r: MvcResult = ownPlans()): CitizenInterventionPlanListResponse {
        expect(r, 200); return json.readValue(r.response.contentAsString, CitizenInterventionPlanListResponse::class.java)
    }
    private fun publicPlan(id: UUID, token: String = f.citizenToken): CitizenInterventionPlanResponse {
        val r = ownPlan(id, token); expect(r, 200); return json.readValue(r.response.contentAsString, CitizenInterventionPlanResponse::class.java)
    }
    private fun publicEntries(planId: UUID, actionId: UUID, query: String = ""): CitizenInterventionFollowUpListResponse {
        val r = ownFollowUps(planId, actionId, query = query); expect(r, 200)
        return json.readValue(r.response.contentAsString, CitizenInterventionFollowUpListResponse::class.java)
    }
    private fun pubResponse(r: MvcResult) = json.readValue(r.response.contentAsString, InterventionPublicationResponse::class.java)
    private fun assertPrivateTextsAbsent(body: String) {
        listOf("Notas privadas", "Resultado privado", "Evaluacion privada", "Resolucion privada", "authorUser", "responsibleUser",
            "\"notes\"", "\"result\"", "sequenceNumber", "updatedByUser").forEach { assertFalse(body.contains(it), body) }
    }

    @Test fun `planes propios incluyen casos asistidos rechazados y cerrados y ocultan datos de otros titulares`() {
        val first = created()
        val other = fixture(); val foreign = created(other)
        val same = fixture(f.citizenId); val second = created(same)
        jdbc.update("update application set registered_by_user_id=? where id=?", same.actorId, same.id)
        tx { applications.findById(f.id).orElseThrow().status = ApplicationStatus.REJECTED
            applications.findById(same.id).orElseThrow().status = ApplicationStatus.CLOSED
            editions.findById(same.editionId).orElseThrow().status = ProgramEditionStatus.CLOSED }
        val own = publicPlans()
        assertEquals(setOf(first.id, second.id), own.content.map { it.id }.toSet())
        assertEquals(2L, own.totalElements)
        assertNull(own.message)
        assertEquals(setOf(f.id, same.id), own.content.map { it.applicationId }.toSet())
        expect(ownPlan(foreign.id), 404, "INTERVENTION_PLAN_NOT_FOUND")
        expect(ownPlan(UUID.randomUUID()), 404, "INTERVENTION_PLAN_NOT_FOUND")
        expect(ownFollowUps(first.id, foreign.objectives[0].actions[0].id), 404, "INTERVENTION_ACTION_NOT_FOUND")
        expect(ownFollowUps(foreign.id, foreign.objectives[0].actions[0].id), 404)
        val attempted = publicPlans(ownPlans(query = "?userId=${other.citizenId}&applicationId=${other.id}"))
        assertEquals(own.content.map { it.id }.toSet(), attempted.content.map { it.id }.toSet())
        assertEquals(listOf(foreign.id), publicPlans(ownPlans(other.citizenToken)).content.map { it.id })
        assertPrivateTextsAbsent(ownPlans().response.contentAsString)
        val detail = publicPlan(second.id)
        assertEquals("Objetivo privado", detail.objectives[0].description) // Objectives/actions are the agreed public structure.
        assertEquals(second.objectives[0].actions[0].dueDate, detail.objectives[0].actions[0].dueDate)
        val expectedHeader = setOf("id", "applicationId", "applicationNumber", "programId", "programName", "programEditionId", "programEditionName", "createdAt", "status")
        val listed = json.readValue(ownPlans().response.contentAsString, Map::class.java)
        (listed["content"] as List<*>).forEach { assertEquals(expectedHeader, (it as Map<*, *>).keys) }
        val detailed = json.readValue(ownPlan(second.id).response.contentAsString, Map::class.java)
        assertEquals(expectedHeader + "objectives", detailed.keys)
        (detailed["objectives"] as List<*>).forEach { objective ->
            val row = objective as Map<*, *>; assertEquals(setOf("id", "description", "actions"), row.keys)
            (row["actions"] as List<*>).forEach { assertEquals(setOf("id", "description", "startDate", "dueDate", "status"), (it as Map<*, *>).keys) }
        }
    }

    @Test fun `progreso del plan deriva de todas las acciones incluso finalizaciones directas parciales`() {
        val p = plan(create(content = json.writeValueAsString(mapOf("objectives" to listOf(
            mapOf("description" to "Primero", "actions" to listOf(action("Primera"))),
            mapOf("description" to "Segundo", "actions" to listOf(action("Segunda"))))))))
        val a = p.objectives[0].actions[0].id; val b = p.objectives[1].actions[0].id
        assertEquals(InterventionActionStatus.PENDING, publicPlan(p.id).status)
        entry(a, followBody("COMPLETED"))
        assertEquals(InterventionActionStatus.IN_PROGRESS, publicPlan(p.id).status)
        assertEquals(InterventionActionStatus.IN_PROGRESS, publicPlans().content.single().status)
        entry(b, followBody("COMPLETED"))
        val completed = publicPlan(p.id)
        assertEquals(InterventionActionStatus.COMPLETED, completed.status)
        assertEquals(listOf("Primero", "Segundo"), completed.objectives.map { it.description })
        assertEquals(listOf("Primera", "Segunda"), completed.objectives.flatMap { it.actions }.map { it.description })
        assertCasePreserved()
    }

    @Test fun `publicacion correccion retiro y republicacion conservan seguimiento original y resumen privado profesional`() {
        val p = created(); val a = p.objectives[0].actions[0].id
        val first = entry(a, followBody(result = "Resultado privado"))
        assertNull(first.citizenPublication)
        val empty = publicEntries(p.id, a)
        assertTrue(empty.content.isEmpty())
        assertEquals(CitizenInterventionPlanService.EMPTY_FOLLOW_UPS_MESSAGE, empty.message)
        val originalTime = applications.findById(f.id).orElseThrow().updatedAt
        val audits = jdbc.queryForObject("select count(*) from logs", Long::class.java)!!
        expect(withdraw(a, first.id), 204)
        assertEquals(originalTime, applications.findById(f.id).orElseThrow().updatedAt)
        assertEquals(audits, jdbc.queryForObject("select count(*) from logs", Long::class.java))
        val published = publication(a, first.id); expect(published, 201)
        val pub = pubResponse(published)
        assertEquals("Resumen acordado", pub.summary); assertTrue(pub.visible)
        assertEquals(f.actorId, pub.updatedByUserId)
        assertEquals(ZoneOffset.UTC, pub.publishedAt.offset); assertEquals(0, pub.publishedAt.nano % 1000)
        val time = applications.findById(f.id).orElseThrow().updatedAt
        val count = jdbc.queryForObject("select count(*) from logs", Long::class.java)
        val duplicate = publication(a, first.id, "Resumen acordado"); expect(duplicate, 200)
        assertEquals(pub, pubResponse(duplicate))
        assertEquals(time, applications.findById(f.id).orElseThrow().updatedAt)
        assertEquals(count, jdbc.queryForObject("select count(*) from logs", Long::class.java))
        val visible = publicEntries(p.id, a)
        assertEquals(1L, visible.totalElements); assertEquals("Resumen acordado", visible.content.single().summary)
        assertEquals(first.occurredOn, visible.content.single().occurredOn)
        assertPrivateTextsAbsent(ownFollowUps(p.id, a).response.contentAsString)
        val corrected = publication(a, first.id, "Texto corregido"); expect(corrected, 200)
        assertEquals(pub.id, pubResponse(corrected).id)
        assertEquals("Texto corregido", publicEntries(p.id, a).content.single().summary)
        expect(withdraw(a, first.id), 204)
        assertEquals(0L, publicEntries(p.id, a).totalElements)
        val professional = json.readValue(history(a).response.contentAsString, Array<InterventionFollowUpResponse>::class.java).single()
        assertEquals(first.copy(citizenPublication = professional.citizenPublication), professional)
        assertFalse(professional.citizenPublication!!.visible)
        assertEquals("Texto corregido", professional.citizenPublication.summary)
        val withdrawnTime = applications.findById(f.id).orElseThrow().updatedAt
        val withdrawnLogs = jdbc.queryForObject("select count(*) from logs", Long::class.java)
        expect(withdraw(a, first.id), 204)
        assertEquals(withdrawnTime, applications.findById(f.id).orElseThrow().updatedAt)
        assertEquals(withdrawnLogs, jdbc.queryForObject("select count(*) from logs", Long::class.java))
        expect(publication(a, first.id, "Republicado"), 200)
        assertEquals("Republicado", publicEntries(p.id, a).content.single().summary)
        assertEquals(1L, publications.findAllByFollowUpIdIn(listOf(first.id)).size.toLong())
        assertCasePreserved()
        val snapshots = jdbc.queryForList("select new_values::text from logs where entity_type='intervention_follow_up' and action='UPDATE' and entity_id=?", String::class.java, first.id.toString())
        assertEquals(4, snapshots.size)
        snapshots.forEach { text -> listOf("Resumen acordado", "Texto corregido", "Republicado", "Notas privadas", "Resultado privado").forEach { assertFalse(text.contains(it)) } }
    }

    @ParameterizedTest @EnumSource(value = ApplicationStatus::class, names = ["REJECTED", "CLOSED"])
    fun `publica y retira despues de completar accion y resolver caso`(status: ApplicationStatus) {
        val p = created(); val a = p.objectives[0].actions[0].id
        val source = entry(a, followBody("COMPLETED"))
        tx { applications.findById(f.id).orElseThrow().status = status }
        expect(publication(a, source.id), 201)
        assertEquals(InterventionActionStatus.COMPLETED, publicPlan(p.id).status)
        assertEquals(1L, publicEntries(p.id, a).totalElements)
        expect(withdraw(a, source.id), 204)
        assertCasePreserved(status)
    }

    @Test fun `totales y orden solo incluyen seguimientos publicados sin revelar secuencias privadas`() {
        val p = created(); val a = p.objectives[0].actions[0].id
        val entries = (1..4).map { entry(a) }
        expect(publication(a, entries[2].id, "Tercero"), 201)
        expect(publication(a, entries[0].id, "Primero"), 201)
        val first = publicEntries(p.id, a, "?size=1")
        assertEquals(2L, first.totalElements); assertEquals(2, first.totalPages)
        assertEquals(entries[0].id, first.content.single().id)
        assertEquals(entries[2].id, publicEntries(p.id, a, "?page=1&size=1").content.single().id)
        val outside = publicEntries(p.id, a, "?page=10&size=1")
        assertTrue(outside.content.isEmpty()); assertNull(outside.message); assertEquals(2L, outside.totalElements)
        val body = json.readValue(ownFollowUps(p.id, a).response.contentAsString, Map::class.java)
        (body["content"] as List<*>).forEach { assertEquals(setOf("id", "actionId", "occurredOn", "status", "summary", "publishedAt"), (it as Map<*, *>).keys) }
        expect(withdraw(a, entries[0].id), 204)
        assertEquals(1L, publicEntries(p.id, a).totalElements)
    }

    @Test fun `ausencia total pagina fuera de rango y orden estable de planes son distinguibles`() {
        val initial = publicPlans()
        assertEquals(0L, initial.totalElements)
        assertEquals(CitizenInterventionPlanService.EMPTY_PLANS_MESSAGE, initial.message)
        val first = created(); val same = fixture(f.citizenId); val second = created(same)
        jdbc.update("update intervention_plan set created_at=? where id=?", first.createdAt, second.id)
        val order = jdbc.queryForList("select id from intervention_plan where id in (?,?) order by id desc", UUID::class.java, first.id, second.id)
        assertEquals(order.first(), publicPlans(ownPlans(query = "?size=1")).content.single().id)
        assertEquals(order.last(), publicPlans(ownPlans(query = "?page=1&size=1")).content.single().id)
        val outside = publicPlans(ownPlans(query = "?page=10&size=1"))
        assertEquals(2L, outside.totalElements); assertNull(outside.message); assertTrue(outside.content.isEmpty())
        val auditCount = jdbc.queryForObject("select count(*) from logs", Long::class.java)
        val oldTime = applications.findById(f.id).orElseThrow().updatedAt
        publicPlan(first.id); publicEntries(first.id, first.objectives[0].actions[0].id)
        assertEquals(auditCount, jdbc.queryForObject("select count(*) from logs", Long::class.java))
        assertEquals(oldTime, applications.findById(f.id).orElseThrow().updatedAt)
        assertCasePreserved()
    }

    @ParameterizedTest @ValueSource(strings = ["?page=-1", "?size=0", "?size=101", "?page=x", "?size=x"])
    fun `paginacion invalida y UUID invalidos no permiten consultar informacion`(query: String) {
        val p = created(); val a = p.objectives[0].actions[0].id
        expect(ownPlans(query = query), 400)
        expect(ownFollowUps(p.id, a, query = query), 400)
        expect(mvc.perform(get("/api/intervention-plans/no-es-uuid").header("Authorization", "Bearer ${f.citizenToken}")).andReturn(), 400)
    }

    @Test fun `lectura propia exige permiso vigente en rol activo JWT base y usuario activo`() {
        val p = created(); val a = p.objectives[0].actions[0].id
        fun expectOwn(token: String?, status: Int) {
            expect(ownPlans(token), status); expect(ownPlan(p.id, token), status); expect(ownFollowUps(p.id, a, token), status)
        }
        expectOwn(null, 401); expectOwn("invalido", 401)
        val roleId = tx { users.findByIdWithRoles(f.citizenId)!!.roles.first().id!! }
        tx { roles.findById(roleId).orElseThrow().permissions.removeIf { it.name == INTERVENTION_OWN_VIEW_PERMISSION }; true }
        expectOwn(f.citizenToken, 403)
        val oldToken = tx { val user = users.findByIdWithRoles(f.citizenId)!!; jwt.createToken(user, user.roles.first()) }
        tx { roles.findById(roleId).orElseThrow().permissions.add(permission(INTERVENTION_OWN_VIEW_PERMISSION)); true }
        expectOwn(oldToken, 403); expectOwn(f.citizenToken, 200)
        val wrongRole = tx {
            val role = roles.saveAndFlush(Role(name = "SIN_PERMISO_${UUID.randomUUID()}"))
            val user = users.findByIdWithRoles(f.citizenId)!!; user.roles.add(role); jwt.createToken(user, role)
        }
        expectOwn(wrongRole, 403)
        tx { users.findById(f.citizenId).orElseThrow().roles.removeIf { it.id == roleId }; true }
        expectOwn(f.citizenToken, 403)
        tx { users.findById(f.citizenId).orElseThrow().active = false }
        expectOwn(f.citizenToken, 401)
    }

    @Test fun `publicacion valida permiso actual responsabilidad y pertenencia de todos los recursos`() {
        val p = created(); val a = p.objectives[0].actions[0].id; val source = entry(a)
        val other = fixture(); val alien = created(other); val b = alien.objectives[0].actions[0].id
        val alienEntry = entry(b, fixture = other)
        expect(publication(a, source.id, token = null), 401)
        expect(publication(a, source.id, token = f.citizenToken), 403)
        expect(publication(a, source.id, token = other.token), 409, "INTERVENTION_PLAN_ASSIGNED_TO_ANOTHER_WORKER")
        expect(publication(b, alienEntry.id), 404, "INTERVENTION_ACTION_NOT_FOUND")
        expect(publication(a, alienEntry.id), 404, "INTERVENTION_FOLLOW_UP_NOT_FOUND")
        expect(publication(a, UUID.randomUUID()), 404, "INTERVENTION_FOLLOW_UP_NOT_FOUND")
        expect(withdraw(a, alienEntry.id), 404)
        tx { roles.findById(f.roleId).orElseThrow().permissions.removeIf { it.name == INTERVENTION_PUBLISH_PERMISSION }; true }
        expect(publication(a, source.id), 403); expect(withdraw(a, source.id), 403)
        val oldToken = tx { val user = users.findByIdWithRoles(f.actorId)!!; jwt.createToken(user, user.roles.first()) }
        tx { roles.findById(f.roleId).orElseThrow().permissions.add(permission(INTERVENTION_PUBLISH_PERMISSION)); true }
        expect(publication(a, source.id, token = oldToken), 403)
        tx { applications.findById(f.id).orElseThrow().assignedWorker = users.findById(other.actorId).orElseThrow() }
        expect(publication(a, source.id), 409, "INTERVENTION_PLAN_ASSIGNED_TO_ANOTHER_WORKER")
        tx { applications.findById(f.id).orElseThrow().assignedWorker = users.findById(f.actorId).orElseThrow() }
        tx { users.findById(f.actorId).orElseThrow().active = false }
        expect(publication(a, source.id), 401); expect(withdraw(a, source.id), 401)
        assertTrue(publications.findAllByFollowUpIdIn(listOf(source.id)).isEmpty())
    }

    @Test fun `publicacion rechaza cuerpo invalido campos administrados y no cambia los POST existentes`() {
        val p = created(); val a = p.objectives[0].actions[0].id; val source = entry(a)
        for (content in listOf("{}", "null", "[]", "{" , """{"summary":" "}""", json.writeValueAsString(mapOf("summary" to "x".repeat(2001))),
            """{"summary":"Texto","visible":true}""", """{"summary":"Texto","updatedByUserId":1}""",
            """{"summary":"Texto","notes":"Privado"}""", """{"summary":"Texto","result":"Privado"}""")) {
            expect(publication(a, source.id, content = content), 400)
        }
        expect(follow(a, content = followBody().dropLast(1) + ""","summary":"Texto"}"""), 400)
        expect(publication(a, source.id, "x".repeat(2000)), 201)
        assertEquals(2000, publicEntries(p.id, a).content.single().summary.length)
    }

    @ParameterizedTest @ValueSource(strings = ["publish", "correct", "withdraw"])
    fun `fallo de auditoria revierte publicacion y fecha sin alterar seguimiento`(operation: String) {
        val p = created(); val a = p.objectives[0].actions[0].id; val source = entry(a)
        if (operation != "publish") expect(publication(a, source.id, "Anterior"), 201)
        val old = publications.findAllByFollowUpIdIn(listOf(source.id)).singleOrNull()?.let { it.summary to it.visible }
        val oldTime = applications.findById(f.id).orElseThrow().updatedAt
        val lastLog = jdbc.queryForObject("select max(id) from logs", Long::class.java)!!
        jdbc.execute("""alter table logs add constraint ck_publication_audit_test check (id <= $lastLog
            or entity_type <> 'intervention_follow_up' or action <> 'UPDATE' or entity_id <> '${source.id}')""")
        try {
            val result = if (operation == "withdraw") withdraw(a, source.id) else publication(a, source.id, "Nuevo")
            expect(result, 409, "DATA_INTEGRITY_VIOLATION")
            val actual = publications.findAllByFollowUpIdIn(listOf(source.id)).singleOrNull()?.let { it.summary to it.visible }
            assertEquals(old, actual)
            assertEquals(oldTime, applications.findById(f.id).orElseThrow().updatedAt)
            assertEquals(source.notes, followUps.findById(source.id).orElseThrow().notes)
            assertEquals(1, followUps.findAllByActionIdOrderBySequenceNumberAsc(a).size)
            assertCasePreserved()
        } finally { jdbc.execute("alter table logs drop constraint ck_publication_audit_test") }
    }

    @Test fun `dos publicaciones iguales crean una sola fila y una sola actuacion`() {
        val p = created(); val a = p.objectives[0].actions[0].id; val source = entry(a)
        val results = concurrently({ publication(a, source.id) }, { publication(a, source.id) })
        assertEquals(listOf(200, 201), results.map { it.response.status }.sorted())
        assertEquals(pubResponse(results[0]), pubResponse(results[1]))
        assertEquals(1, publications.findAllByFollowUpIdIn(listOf(source.id)).size)
        assertEquals(1L, jdbc.queryForObject("select count(*) from logs where entity_type='intervention_follow_up' and action='UPDATE' and entity_id=?", Long::class.java, source.id.toString()))
    }

    @Test fun `corregir y retirar concurrentemente no pierde registro ni expone publicaciones retiradas`() {
        val p = created(); val a = p.objectives[0].actions[0].id; val source = entry(a)
        expect(publication(a, source.id), 201)
        val results = concurrently({ publication(a, source.id, "Corregido") }, { withdraw(a, source.id) })
        assertEquals(listOf(200, 204), results.map { it.response.status })
        val current = publications.findAllByFollowUpIdIn(listOf(source.id)).single()
        assertEquals("Corregido", current.summary)
        val visible = publicEntries(p.id, a)
        assertEquals(if (current.visible) 1L else 0L, visible.totalElements)
        assertEquals(source.notes, followUps.findById(source.id).orElseThrow().notes)
        assertCasePreserved()
    }

    @Test fun `publicar y completar seguimiento concurrentes conservan ambos sin publicar resultado profesional`() {
        val p = created(); val a = p.objectives[0].actions[0].id; val source = entry(a)
        val results = concurrently({ publication(a, source.id) }, { follow(a, followBody("COMPLETED")) })
        assertEquals(listOf(201, 201), results.map { it.response.status })
        assertEquals(InterventionActionStatus.COMPLETED, publicPlan(p.id).status)
        assertEquals(listOf(source.id), publicEntries(p.id, a).content.map { it.id })
        assertPrivateTextsAbsent(ownFollowUps(p.id, a).response.contentAsString)
        assertEquals(2, followUps.findAllByActionIdOrderBySequenceNumberAsc(a).size)
    }

    @Test fun `consulta de varios planes no agrega consultas por elemento ni carga responsables`() {
        created(); created(fixture(f.citizenId))
        val stats = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        stats.isStatisticsEnabled = true
        try {
            stats.clear(); publicPlans(ownPlans(query = "?size=1")); val one = stats.prepareStatementCount
            stats.clear(); publicPlans(ownPlans(query = "?size=2")); val two = stats.prepareStatementCount
            assertEquals(one, two); assertTrue(two <= 5, "$two consultas")
        } finally { stats.isStatisticsEnabled = false }
    }

    @Test fun `publicaciones diferentes concurrentes conservan la ultima version y una sola fila`() {
        val p = created(); val a = p.objectives[0].actions[0].id; val source = entry(a)
        val results = concurrently({ publication(a, source.id, "Version A") }, { publication(a, source.id, "Version B") })
        assertEquals(listOf(200, 201), results.map { it.response.status }.sorted())
        val current = publications.findAllByFollowUpIdIn(listOf(source.id)).single()
        assertTrue(current.summary in setOf("Version A", "Version B"))
        assertEquals(current.summary, publicEntries(p.id, a).content.single().summary)
        assertEquals(2L, jdbc.queryForObject("select count(*) from logs where entity_type='intervention_follow_up' and action='UPDATE' and entity_id=?", Long::class.java, source.id.toString()))
    }

    @Test fun `publicar y retirar inicialmente concurrentes respetan orden de bloqueo sin duplicar publicaciones`() {
        val p = created(); val a = p.objectives[0].actions[0].id; val source = entry(a)
        val results = concurrently({ publication(a, source.id) }, { withdraw(a, source.id) })
        assertEquals(listOf(201, 204), results.map { it.response.status })
        val current = publications.findAllByFollowUpIdIn(listOf(source.id)).single()
        assertEquals(if (current.visible) 1L else 0L, publicEntries(p.id, a).totalElements)
        assertCasePreserved()
    }

    @Test fun `dos altas concurrentes crean un unico plan`() {
        val results = concurrently({ create() }, { create() })
        assertEquals(listOf(201, 409), results.map { it.response.status }.sorted())
        assertTrue(plans.existsByApplicationId(f.id))
        assertCasePreserved()
    }
    @Test fun `dos finalizaciones simultaneas solo confirman un resultado`() {
        val a = created().objectives[0].actions[0].id
        val results = concurrently({ follow(a, followBody("COMPLETED")) }, { follow(a, followBody("COMPLETED")) })
        assertEquals(listOf(201, 409), results.map { it.response.status }.sorted())
        assertEquals(1, followUps.findAllByActionIdOrderBySequenceNumberAsc(a).size)
        assertEquals(InterventionActionStatus.COMPLETED, actions.findById(a).orElseThrow().status)
    }
    @Test fun `dos avances simultaneos conservan secuencia y estados previos`() {
        val a = created().objectives[0].actions[0].id
        val results = concurrently({ follow(a) }, { follow(a) })
        assertTrue(results.all { it.response.status == 201 }, results.map { it.response.contentAsString }.toString())
        val entries = followUps.findAllByActionIdOrderBySequenceNumberAsc(a)
        assertEquals(listOf(1, 2), entries.map { it.sequenceNumber })
        assertEquals(listOf(InterventionActionStatus.PENDING, InterventionActionStatus.IN_PROGRESS), entries.map { it.previousStatus })
    }
    @Test fun `seguimiento y cierre por ultimo beneficio concurrentes conservan ambos cambios`() {
        tx { applications.findById(f.id).orElseThrow().status = ApplicationStatus.APPROVED }
        val p = created(); val a = p.objectives[0].actions[0].id
        val benefit = tx { benefits.saveAndFlush(ProgramBenefit(programEdition = editions.findById(f.editionId).orElseThrow(),
            benefitType = ProgramBenefitType.FOOD_ASSISTANCE)).id!! }
        val grant = mvc.perform(post("/api/admin/applications/${f.id}/benefit-grants")
            .header("Authorization", "Bearer ${f.token}").contentType(MediaType.APPLICATION_JSON)
            .content("""{"programBenefitId":"$benefit","startDate":"$today","endDate":"${today.plusDays(5)}"}""")).andReturn()
        expect(grant, 201)
        val grantId = json.readTree(grant.response.contentAsString).get("id").stringValue()
        val results = concurrently({ follow(a, followBody("COMPLETED")) }, {
            mvc.perform(post("/api/admin/applications/${f.id}/benefit-grants/$grantId/finalize")
                .header("Authorization", "Bearer ${f.token}").contentType(MediaType.APPLICATION_JSON).content("""{"reason":"Finaliza"}""")).andReturn()
        })
        assertEquals(listOf(201, 200), results.map { it.response.status })
        assertCasePreserved(ApplicationStatus.CLOSED, 0)
        assertEquals(InterventionActionStatus.COMPLETED, actions.findById(a).orElseThrow().status)
        assertEquals(1, followUps.findAllByActionIdOrderBySequenceNumberAsc(a).size)
    }
}
