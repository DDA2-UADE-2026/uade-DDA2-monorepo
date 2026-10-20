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
    private fun fixture(): Fixture = tx {
        val role = roles.saveAndFlush(Role(name = "TRABAJADOR_${UUID.randomUUID()}", permissions = mutableSetOf(
            permission(INTERVENTION_CREATE_PERMISSION), permission(INTERVENTION_FOLLOW_UP_PERMISSION), permission(INTERVENTION_VIEW_PERMISSION),
            permission("applications:management:benefits:assign"), permission("applications:management:benefits:manage"), permission("users:delete"))))
        val actor = users.saveAndFlush(User(name = "Trabajador", email = "worker@example.com", roles = mutableSetOf(role)))
        val citizenRole = roles.saveAndFlush(Role(name = "CIUDADANO_${UUID.randomUUID()}", permissions = mutableSetOf(permission("applications:own:view"))))
        val citizen = users.saveAndFlush(User(name = "Ciudadano", email = "citizen@example.com", roles = mutableSetOf(citizenRole)))
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
            jwt.createToken(actor, role), jwt.createToken(citizen, citizenRole))
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
    private fun created(): InterventionPlanResponse { val r = create(); expect(r, 201); return plan(r) }
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
        val audits = jdbc.queryForList("select new_values::text from logs where entity_type='intervention_follow_up'", String::class.java)
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
