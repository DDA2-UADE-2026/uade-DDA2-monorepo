package com.uade.dda2.server.feature.benefit

import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.auth.entity.*
import com.uade.dda2.server.feature.auth.repository.*
import com.uade.dda2.server.feature.benefit.dto.response.BenefitGrantResponse
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
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
import org.springframework.dao.DataIntegrityViolationException
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
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.*
import java.util.UUID
import java.util.concurrent.*
import kotlin.test.*

@SpringBootTest(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
    "app.enrollment-period.expiration.cron=-",
])
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class BenefitGrantFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var programs: ProgramRepository
    @Autowired lateinit var editions: ProgramEditionRepository
    @Autowired lateinit var periods: EnrollmentPeriodRepository
    @Autowired lateinit var benefits: ProgramBenefitRepository
    @Autowired lateinit var incompatibilities: ProgramIncompatibilityRepository
    @Autowired lateinit var applications: ApplicationRepository
    @Autowired lateinit var grants: BenefitGrantRepository
    @Autowired lateinit var transactions: PlatformTransactionManager
    @Autowired lateinit var jdbc: JdbcTemplate

    companion object {
        @JvmStatic @DynamicPropertySource fun database(registry: DynamicPropertyRegistry) {
            val postgres = System.getProperty("benefit-grant.test.postgres-url")
            registry.add("spring.datasource.url") { postgres ?: "jdbc:h2:mem:benefit-grants;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000" }
            registry.add("spring.datasource.driver-class-name") { if (postgres == null) "org.h2.Driver" else "org.postgresql.Driver" }
            registry.add("spring.jpa.properties.hibernate.dialect") { if (postgres == null) "org.hibernate.dialect.H2Dialect" else "org.hibernate.dialect.PostgreSQLDialect" }
            registry.add("spring.datasource.username") { if (postgres == null) "sa" else System.getProperty("benefit-grant.test.postgres-user", "application_test") }
            registry.add("spring.datasource.password") { System.getProperty("benefit-grant.test.postgres-password", "") }
        }
    }

    data class Fixture(val applicationId: UUID, val applicantId: Long, val actorId: Long, val roleId: Long,
        val workerId: Long, val token: String, val citizenToken: String, val editionId: UUID, val programId: UUID, val benefitId: UUID)
    private lateinit var f: Fixture
    private val today get() = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
    private fun <T : Any> tx(action: () -> T): T = TransactionTemplate(transactions).execute { action() }!!
    private fun permission(name: String) = permissions.findByNameIn(setOf(name)).firstOrNull() ?: permissions.save(Permission(name = name))
    @BeforeEach fun setup() { f = fixture() }

    private fun fixture(applicantId: Long? = null, editionId: UUID? = null): Fixture = tx {
        val role = roles.save(Role(name = "ADMIN_${UUID.randomUUID()}", permissions = mutableSetOf(
            permission("applications:management:benefits:assign"), permission("applications:management:view"),
            permission("programs:management:edit"), permission("users:delete"))))
        val actor = users.saveAndFlush(User(name = "Administrativo", email = "admin@example.com", roles = mutableSetOf(role)))
        val citizenRole = roles.save(Role(name = "CIUDADANO_${UUID.randomUUID()}", permissions = mutableSetOf(permission("applications:own:view"))))
        val applicant = if (applicantId == null) users.saveAndFlush(User(name = "Titular", email = "citizen@example.com", roles = mutableSetOf(citizenRole)))
            else users.findById(applicantId).orElseThrow()
        val worker = users.saveAndFlush(User(name = "Evaluador", email = "worker@example.com"))
        val edition = if (editionId == null) {
            val program = programs.saveAndFlush(Program(name = "Programa ${UUID.randomUUID()}", createdBy = worker))
            editions.saveAndFlush(ProgramEdition(program = program, name = "Edicion ${UUID.randomUUID()}",
                startDate = today.minusDays(20), endDate = today.plusDays(20), maxCapacity = 1, currentEnrollment = 1,
                status = ProgramEditionStatus.ACTIVE, createdBy = worker))
        } else editions.findById(editionId).orElseThrow().also {
            // Simulate the reservation already made when this additional application was approved.
            it.maxCapacity += 1
            it.currentEnrollment += 1
        }
        val period = periods.saveAndFlush(EnrollmentPeriod(programEdition = edition, openDate = today, closeDate = today, status = EnrollmentPeriodStatus.CLOSED))
        val resolved = today.minusDays(1).atStartOfDay()
        val application = applications.saveAndFlush(Application(user = applicant, registeredBy = applicant,
            programEdition = edition, enrollmentPeriod = period, status = ApplicationStatus.APPROVED, resolutionReason = "Motivo privado",
            resolvedBy = worker, assignedWorker = worker, submittedAt = resolved.minusDays(1), resolvedAt = resolved, updatedAt = resolved))
        val benefit = if (editionId == null) benefits.saveAndFlush(ProgramBenefit(programEdition = edition,
            benefitType = ProgramBenefitType.FOOD_ASSISTANCE, description = "Descripcion reservada", amount = BigDecimal("1250.50")))
            else benefits.findAllByProgramEditionId(editionId).first()
        Fixture(application.id!!, applicant.id!!, actor.id!!, role.id!!, worker.id!!, jwt.createToken(actor, role),
            if (applicantId == null) jwt.createToken(applicant, citizenRole) else "", edition.id!!, edition.program.id!!, benefit.id!!)
    }

    private fun body(benefitId: UUID = f.benefitId, start: LocalDate = today.minusDays(5), end: LocalDate = today.plusDays(10)) =
        json.writeValueAsString(mapOf("programBenefitId" to benefitId, "startDate" to start, "endDate" to end))
    private fun assign(fixture: Fixture = f, token: String? = fixture.token, content: String = body(fixture.benefitId)): MvcResult {
        val request = post("/api/admin/applications/${fixture.applicationId}/benefit-grants").contentType(MediaType.APPLICATION_JSON).content(content)
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }
    private fun list(fixture: Fixture = f, token: String = fixture.token) = mvc.perform(
        get("/api/admin/applications/${fixture.applicationId}/benefit-grants").header("Authorization", "Bearer $token")).andReturn()
    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").stringValue())
    }
    private fun response(result: MvcResult) = json.readValue(result.response.contentAsString, BenefitGrantResponse::class.java)
    private fun unchanged() = tx {
        assertTrue(grants.findAllByApplicationIdOrderByAssignedAtAscIdAsc(f.applicationId).isEmpty())
        assertEquals(today.minusDays(1).atStartOfDay(), applications.findById(f.applicationId).orElseThrow().updatedAt)
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        true
    }

    @Test fun `otorga activo con copia vigencia actor auditoria y privacidad sin ocupar otro cupo`() {
        expect(list(), 200)
        assertEquals(0, json.readTree(list().response.contentAsString).size())
        val result = assign()
        expect(result, 201)
        val grant = response(result)
        assertEquals("ACTIVE", grant.status.name)
        assertEquals(f.applicantId, grant.userId)
        assertEquals(f.actorId, grant.assignedByUserId)
        assertEquals(f.benefitId, grant.programBenefitId)
        assertEquals(BigDecimal("1250.50"), grant.amount)
        assertEquals("Descripcion reservada", grant.description)
        assertEquals(today.minusDays(5), grant.startDate)
        assertEquals(today.plusDays(10), grant.endDate)
        assertEquals(ZoneOffset.UTC, grant.assignedAt.offset)
        tx {
            val a = applications.findDetailById(f.applicationId)!!
            assertEquals(ApplicationStatus.APPROVED, a.status)
            assertEquals("Motivo privado", a.resolutionReason)
            assertEquals(f.workerId, a.resolvedBy?.id)
            assertEquals(f.workerId, a.assignedWorker?.id)
            assertEquals(today.minusDays(1).atStartOfDay(), a.resolvedAt)
            assertEquals(LocalDateTime.ofInstant(grant.assignedAt.toInstant(), ZoneId.of("America/Argentina/Buenos_Aires")), a.updatedAt)
            assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
            val audit = jdbc.queryForObject("select new_values::text from logs where entity_type = 'benefit_grant' and entity_id = ?", String::class.java, grant.id.toString())!!
            assertTrue(audit.contains("assignedByUserId"))
            assertTrue(audit.contains("startDate"))
            assertFalse(audit.contains("Descripcion reservada"))
            assertFalse(audit.contains("1250.50"))
        }
        val own = mvc.perform(get("/api/applications/${f.applicationId}").header("Authorization", "Bearer ${f.citizenToken}")).andReturn()
        expect(own, 200)
        val ownJson = json.readTree(own.response.contentAsString)
        listOf("benefitGrants", "grants", "assignedByUserId", "amount").forEach { assertFalse(ownJson.has(it)) }
        expect(list(token = f.citizenToken), 403)
        expect(assign(), 409, "BENEFIT_GRANT_ALREADY_EXISTS")
        assertEquals(1, json.readTree(list().response.contentAsString).size())
    }

    @Test fun `admite diferentes beneficios con monto opcional y orden cronologico`() {
        val second = addBenefit()
        expect(assign(), 201)
        expect(assign(content = body(second)), 201)
        val entries = json.readTree(list().response.contentAsString)
        assertEquals(2, entries.size())
        assertEquals(f.benefitId.toString(), entries[0].get("programBenefitId").stringValue())
        assertEquals(second.toString(), entries[1].get("programBenefitId").stringValue())
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }
    private fun addBenefit(): UUID = tx {
        benefits.saveAndFlush(ProgramBenefit(programEdition = editions.findById(f.editionId).orElseThrow(),
            benefitType = ProgramBenefitType.TAX_EXEMPTION, description = null, amount = null)).id!!
    }

    @ParameterizedTest @EnumSource(ApplicationStatus::class, names = ["APPROVED"], mode = EnumSource.Mode.EXCLUDE)
    fun `exige solicitud aprobada`(status: ApplicationStatus) {
        tx { applications.findById(f.applicationId).orElseThrow().status = status }
        expect(assign(), 409, "BENEFIT_GRANT_APPLICATION_NOT_APPROVED")
        unchanged()
    }
    @ParameterizedTest @EnumSource(ProgramEditionStatus::class, names = ["ACTIVE"], mode = EnumSource.Mode.EXCLUDE)
    fun `exige edicion activa aun con solicitud aprobada`(status: ProgramEditionStatus) {
        tx { editions.findById(f.editionId).orElseThrow().status = status }
        expect(assign(), 409, "BENEFIT_GRANT_EDITION_NOT_ACTIVE")
        unchanged()
    }

    @Test fun `no acepta beneficio inexistente o de otra edicion`() {
        expect(assign(content = body(UUID.randomUUID())), 404, "PROGRAM_BENEFIT_NOT_FOUND")
        expect(assign(content = body(fixture().benefitId)), 404, "PROGRAM_BENEFIT_NOT_FOUND")
        unchanged()
    }
    @Test fun `solicitud inexistente devuelve 404 en ambas operaciones`() {
        val missing = f.copy(applicationId = UUID.randomUUID())
        expect(assign(missing), 404, "APPLICATION_NOT_FOUND")
        expect(list(missing), 404, "APPLICATION_NOT_FOUND")
    }

    @Test fun `rechaza datos faltantes nulos incorrectos y campos controlados por servidor`() {
        val valid = body()
        val invalid = listOf("{}", "null", "[]", """{"programBenefitId":null,"startDate":"$today","endDate":"$today"}""",
            """{"programBenefitId":"${f.benefitId}","endDate":"$today"}""",
            """{"programBenefitId":"${f.benefitId}","startDate":"$today","endDate":null}""",
            """{"programBenefitId":"x","startDate":"$today","endDate":"$today"}""",
            """{"programBenefitId":"${f.benefitId}","startDate":"2026-99-01","endDate":"$today"}""",
            valid.dropLast(1) + ",\"amount\":1}", valid.dropLast(1) + ",\"status\":\"ACTIVE\"}",
            valid.dropLast(1) + ",\"userId\":${f.actorId}}", valid.dropLast(1) + ",\"assignedByUserId\":${f.workerId}}",
            valid.dropLast(1) + ",\"assignedAt\":\"2026-10-01T00:00:00Z\"}", valid.dropLast(1) + ",\"description\":\"x\"}")
        invalid.forEach { expect(assign(content = it), 400, "BENEFIT_GRANT_INVALID_DATA") }
        unchanged()
    }
    @Test fun `rechaza vigencias invertidas futuras vencidas o fuera de la edicion`() {
        listOf(today.plusDays(1) to today, today.plusDays(1) to today.plusDays(5), today.minusDays(5) to today.minusDays(1),
            today.minusDays(21) to today.plusDays(1), today.minusDays(1) to today.plusDays(21)).forEach { (start, end) ->
            expect(assign(content = body(start = start, end = end)), 400, "BENEFIT_GRANT_INVALID_DATA")
        }
        unchanged()
    }
    @ParameterizedTest @ValueSource(strings = ["TODAY", "EDITION"])
    fun `extremos inclusivos permiten hoy y limites exactos de la edicion`(range: String) {
        val start = if (range == "TODAY") today else today.minusDays(20)
        val end = if (range == "TODAY") today else today.plusDays(20)
        expect(assign(content = body(start = start, end = end)), 201)
    }

    private fun incompatible(other: Fixture, reverse: Boolean = false) = tx {
        val first = programs.findById(if (reverse) other.programId else f.programId).orElseThrow()
        val second = programs.findById(if (reverse) f.programId else other.programId).orElseThrow()
        incompatibilities.saveAndFlush(ProgramIncompatibility(id = ProgramIncompatibilityId(first.id, second.id),
            program = first, incompatibleWithProgram = second))
    }
    @ParameterizedTest @ValueSource(booleans = [false, true])
    fun `revalida incompatibilidades en ambos sentidos`(reverse: Boolean) {
        incompatible(fixture(applicantId = f.applicantId), reverse)
        expect(assign(), 409, "BENEFIT_GRANT_INCOMPATIBLE_APPLICATION")
        unchanged()
    }
    @ParameterizedTest @ValueSource(ints = [-1, 0, 1])
    fun `superposicion de ediciones incluye extremos`(gap: Int) {
        val other = fixture(applicantId = f.applicantId)
        incompatible(other)
        tx {
            val edition = editions.findById(other.editionId).orElseThrow()
            edition.startDate = today.plusDays(20L + gap)
            edition.endDate = today.plusDays(40)
        }
        expect(assign(), if (gap > 0) 201 else 409, if (gap > 0) null else "BENEFIT_GRANT_INCOMPATIBLE_APPLICATION")
    }
    @Test fun `otras personas y solicitudes sin aprobar no bloquean`() {
        incompatible(fixture())
        val other = fixture(applicantId = f.applicantId)
        incompatible(other)
        tx { applications.findById(other.applicationId).orElseThrow().status = ApplicationStatus.SUBMITTED }
        expect(assign(), 201)
    }

    private fun updateCatalog() = mvc.perform(put("/api/admin/program-editions/${f.editionId}/benefits/${f.benefitId}")
        .header("Authorization", "Bearer ${f.token}").contentType(MediaType.APPLICATION_JSON)
        .content("""{"benefitType":"HOUSING_SUBSIDY","description":"Nueva descripcion","amount":9999.00}""")).andReturn()
    @Test fun `copia no cambia al editar catalogo y futuros otorgamientos usan nuevos valores`() {
        expect(assign(), 201)
        expect(updateCatalog(), 200)
        val original = json.readTree(list().response.contentAsString)[0]
        assertEquals("FOOD_ASSISTANCE", original.get("benefitType").stringValue())
        assertEquals("Descripcion reservada", original.get("description").stringValue())
        assertEquals(0, BigDecimal("1250.50").compareTo(original.get("amount").decimalValue()))
        val next = fixture(editionId = f.editionId)
        val result = assign(next)
        expect(result, 201)
        assertEquals(ProgramBenefitType.HOUSING_SUBSIDY, response(result).benefitType)
        assertEquals(BigDecimal("9999.00"), response(result).amount)
        assertEquals(2, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }
    @Test fun `bloquea baja del beneficio otorgado y permite borrar otro sin otorgamientos`() {
        expect(assign(), 201)
        expect(mvc.perform(delete("/api/admin/program-editions/${f.editionId}/benefits/${f.benefitId}")
            .header("Authorization", "Bearer ${f.token}")).andReturn(), 409, "PROGRAM_BENEFIT_HAS_GRANTS")
        val other = addBenefit()
        expect(mvc.perform(delete("/api/admin/program-editions/${f.editionId}/benefits/$other")
            .header("Authorization", "Bearer ${f.token}")).andReturn(), 204)
    }
    private fun editEdition(start: LocalDate = today.minusDays(20), end: LocalDate = today.plusDays(20)): MvcResult = mvc.perform(
        put("/api/admin/program-editions/${f.editionId}").header("Authorization", "Bearer ${f.token}")
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(mapOf("name" to "Edicion actualizada", "startDate" to start,
                "endDate" to end, "maxCapacity" to 1)))).andReturn()
    @Test fun `edicion no puede excluir otorgamientos pero puede ampliar fechas`() {
        expect(assign(), 201)
        expect(editEdition(start = today.minusDays(4)), 409, "PROGRAM_EDITION_DATE_RANGE_EXCLUDES_BENEFIT_GRANTS")
        expect(editEdition(end = today.plusDays(9)), 409, "PROGRAM_EDITION_DATE_RANGE_EXCLUDES_BENEFIT_GRANTS")
        expect(editEdition(start = today.minusDays(30), end = today.plusDays(30)), 200)
    }
    @Test fun `no permite eliminar al administrativo referenciado`() {
        expect(assign(), 201)
        expect(mvc.perform(delete("/users/${f.actorId}").header("Authorization", "Bearer ${fixture().token}")).andReturn(),
            409, "USER_HAS_APPLICATION_REFERENCES")
        assertTrue(users.existsById(f.actorId))
    }

    @Test fun `requiere token usuario activo rol actual y permisos en JWT y base`() {
        expect(assign(token = null), 401)
        expect(assign(token = f.citizenToken), 403)
        tx { roles.findById(f.roleId).orElseThrow().permissions.removeIf { it.name == "applications:management:benefits:assign" }; true }
        expect(assign(), 403)
        expect(list(), 200)
        tx { roles.findById(f.roleId).orElseThrow().permissions.removeIf { it.name == "applications:management:view" }; true }
        expect(list(), 403)
        tx { roles.findById(f.roleId).orElseThrow().permissions.add(permission("applications:management:benefits:assign")) }
        tx { users.findById(f.actorId).orElseThrow().roles.clear(); true }
        expect(assign(), 403)
        tx { users.findById(f.actorId).orElseThrow().roles.add(roles.findById(f.roleId).orElseThrow()) }
        tx { users.findById(f.actorId).orElseThrow().active = false }
        expect(assign(), 401)
        expect(list(), 401)
        unchanged()
    }
    @Test fun `no usa permiso de otro rol ni permiso agregado despues de emitir JWT`() {
        val reader = tx {
            val role = roles.saveAndFlush(Role(name = "LECTOR_${UUID.randomUUID()}", permissions = mutableSetOf(permission("applications:management:view"))))
            val actor = users.findById(f.actorId).orElseThrow()
            actor.roles.add(role)
            jwt.createToken(actor, role) to role.id!!
        }
        expect(assign(token = reader.first), 403)
        tx { roles.findById(reader.second).orElseThrow().permissions.add(permission("applications:management:benefits:assign")) }
        expect(assign(token = reader.first), 403)
        unchanged()
    }
    @Test fun `fallo de auditoria revierte otorgamiento y updatedAt sin alterar cupo`() {
        jdbc.execute("alter table logs add constraint ck_grant_test_audit_failure check (entity_type <> 'benefit_grant' or user_id <> ${f.actorId})")
        try { expect(assign(), 409, "DATA_INTEGRITY_VIOLATION"); unchanged() }
        finally { jdbc.execute("alter table logs drop constraint ck_grant_test_audit_failure") }
    }
    @Test fun `migracion PostgreSQL es repetible y no fabrica otorgamientos ni cupos`() {
        assumeTrue(System.getProperty("benefit-grant.test.postgres-url") != null)
        val before = grants.count()
        val migration = Files.readString(Path.of("docs/database/migrations/2026-10-19-benefit-grants.sql"))
        // Create the migration table from scratch in a private schema, independently of Hibernate's DDL.
        val schema = "grant_migration_" + UUID.randomUUID().toString().replace("-", "")
        jdbc.execute("create schema $schema")
        try {
            jdbc.execute("""
                create table $schema.users (id bigint primary key);
                create table $schema.application (id uuid primary key, status varchar(30));
                create table $schema.program_benefit (id uuid primary key);
                create table $schema.permissions (id bigserial primary key, name varchar(100) unique);
                create table $schema.roles (id bigserial primary key, name varchar(50) unique);
                create table $schema.role_permissions (role_id bigint references $schema.roles(id),
                    permission_id bigint references $schema.permissions(id), primary key (role_id, permission_id));
                insert into $schema.users values (1);
                insert into $schema.roles(name) values ('ADMIN'), ('CIUDADANO');
            """.trimIndent())
            jdbc.update("insert into $schema.application values (?, 'APPROVED')", f.applicationId)
            jdbc.update("insert into $schema.program_benefit values (?)", f.benefitId)
            repeat(2) { jdbc.execute(migration.replace("public.", "$schema.")) }
            assertEquals(0L, jdbc.queryForObject("select count(*) from $schema.benefit_grant", Long::class.java))
            assertEquals(1L, jdbc.queryForObject("select count(*) from $schema.role_permissions rp join $schema.roles r on r.id = rp.role_id where r.name = 'ADMIN'", Long::class.java))
            assertEquals(0L, jdbc.queryForObject("select count(*) from $schema.role_permissions rp join $schema.roles r on r.id = rp.role_id where r.name = 'CIUDADANO'", Long::class.java))
            val insert = "insert into $schema.benefit_grant (id, application_id, program_benefit_id, benefit_type, start_date, end_date, status, assigned_by_user_id, assigned_at) " +
                "values (?, ?, ?, 'FOOD_ASSISTANCE', current_date, current_date, 'ACTIVE', 1, current_timestamp)"
            jdbc.update(insert, UUID.randomUUID(), f.applicationId, f.benefitId)
            assertFailsWith<DataIntegrityViolationException> { jdbc.update(insert, UUID.randomUUID(), f.applicationId, f.benefitId) }
        } finally {
            jdbc.execute("drop schema $schema cascade")
        }
        assertEquals(before, grants.count())
        unchanged()
        expect(assign(), 201)
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
    @Test fun `dos altas simultaneas del mismo beneficio producen una creacion y un conflicto`() {
        val results = concurrently({ assign() }, { assign() })
        assertEquals(listOf(201, 409), results.map { it.response.status }.sorted())
        expect(results.single { it.response.status == 409 }, 409, "BENEFIT_GRANT_ALREADY_EXISTS")
        assertEquals(1, json.readTree(list().response.contentAsString).size())
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }
    @Test fun `dos beneficios distintos se otorgan concurrentemente sin ocupar mas cupos`() {
        val other = addBenefit()
        val results = concurrently({ assign() }, { assign(content = body(other)) })
        assertTrue(results.all { it.response.status == 201 }, results.map { it.response.contentAsString }.toString())
        assertEquals(2, json.readTree(list().response.contentAsString).size())
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }
    @Test fun `edicion y otorgamiento concurrentes no dejan vigencia fuera de rango`() {
        val results = concurrently({ assign() }, { editEdition(end = today.plusDays(5)) })
        assertTrue(results.map { it.response.status } in listOf(listOf(201, 409), listOf(400, 200)))
        tx {
            val edition = editions.findById(f.editionId).orElseThrow()
            grants.findAllByApplicationIdOrderByAssignedAtAscIdAsc(f.applicationId).forEach {
                assertTrue(it.startDate >= edition.startDate && it.endDate <= edition.endDate)
            }
            true
        }
    }

    @Test fun `baja de catalogo y otorgamiento concurrentes conservan referencias validas`() {
        val results = concurrently({ assign() }, {
            mvc.perform(delete("/api/admin/program-editions/${f.editionId}/benefits/${f.benefitId}")
                .header("Authorization", "Bearer ${f.token}")).andReturn()
        })
        assertTrue(results.map { it.response.status } in listOf(listOf(201, 409), listOf(404, 204)),
            results.map { it.response.contentAsString }.toString())
        tx {
            grants.findAllByApplicationIdOrderByAssignedAtAscIdAsc(f.applicationId).forEach {
                assertTrue(benefits.existsById(it.programBenefit.id!!))
            }
            true
        }
    }
}
