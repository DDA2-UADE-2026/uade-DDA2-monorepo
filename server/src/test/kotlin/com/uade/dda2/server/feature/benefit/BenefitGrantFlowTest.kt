package com.uade.dda2.server.feature.benefit

import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.auth.entity.*
import com.uade.dda2.server.feature.auth.repository.*
import com.uade.dda2.server.feature.benefit.dto.response.BenefitGrantResponse
import com.uade.dda2.server.feature.benefit.dto.response.CitizenBenefitGrantListResponse
import com.uade.dda2.server.feature.benefit.service.CitizenBenefitGrantService
import com.uade.dda2.server.feature.benefit.service.OWN_BENEFIT_VIEW_PERMISSION
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantStatus
import com.uade.dda2.server.feature.benefit.service.BenefitGrantExpirationService
import com.uade.dda2.server.feature.benefit.scheduler.BenefitGrantExpirationScheduler
import com.uade.dda2.server.config.BenefitGrantExpirationProperties
import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.mockito.Mockito.`when`
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantChangeRepository
import com.uade.dda2.server.feature.document.entity.Document
import com.uade.dda2.server.feature.document.repository.DocumentRepository
import com.uade.dda2.server.feature.application.repository.ApplicationDocumentRepository
import com.uade.dda2.server.feature.application.repository.ApplicationEvaluationRepository
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
    @Autowired lateinit var changes: BenefitGrantChangeRepository
    @Autowired lateinit var requirements: ProgramRequirementRepository
    @Autowired lateinit var documentRequirements: ProgramDocumentRequirementRepository
    @Autowired lateinit var applicationDocuments: ApplicationDocumentRepository
    @Autowired lateinit var documents: DocumentRepository
    @Autowired lateinit var evaluations: ApplicationEvaluationRepository
    @Autowired lateinit var transactions: PlatformTransactionManager
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var expiration: BenefitGrantExpirationService
    @Autowired lateinit var entityManagerFactory: EntityManagerFactory
    @MockitoBean lateinit var clock: Clock

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
    @BeforeEach fun setup() {
        `when`(clock.instant()).thenAnswer { Instant.now() }
        f = fixture()
    }

    private fun fixture(applicantId: Long? = null, editionId: UUID? = null): Fixture = tx {
        val role = roles.save(Role(name = "ADMIN_${UUID.randomUUID()}", permissions = mutableSetOf(
            permission("applications:management:benefits:assign"), permission("applications:management:view"),
            permission("applications:management:benefits:manage"),
            permission("applications:management:resolve"),
            permission("programs:management:edit"), permission("programs:management:create"), permission("users:delete"))))
        val actor = users.saveAndFlush(User(name = "Administrativo", email = "admin@example.com", roles = mutableSetOf(role)))
        val citizenRole = roles.save(Role(name = "CIUDADANO_${UUID.randomUUID()}", permissions = mutableSetOf(permission("applications:own:view"), permission(OWN_BENEFIT_VIEW_PERMISSION))))
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


    @Test fun `migracion de ciclo PostgreSQL conserva asignaciones y elimina restricciones de estado anteriores`() {
        assumeTrue(System.getProperty("benefit-grant.test.postgres-url") != null)
        val schema = "lifecycle_migration_" + UUID.randomUUID().toString().replace("-", "")
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
            jdbc.execute(Files.readString(Path.of("docs/database/migrations/2026-10-19-benefit-grants.sql")).replace("public.", "$schema."))
            // Simulate the additional enum CHECK generated by Hibernate before this feature.
            jdbc.execute("alter table $schema.benefit_grant add constraint legacy_hibernate_enum check (status in ('ACTIVE'))")
            val grantId = UUID.randomUUID()
            val assignedAt = OffsetDateTime.of(2026, 10, 1, 13, 25, 0, 0, ZoneOffset.UTC)
            jdbc.update("""insert into $schema.benefit_grant
                (id, application_id, program_benefit_id, benefit_type, description, amount, start_date, end_date, status, assigned_by_user_id, assigned_at)
                values (?, ?, ?, 'FOOD_ASSISTANCE', 'Original', 100, ?, ?, 'ACTIVE', 1, ?)""",
                grantId, f.applicationId, f.benefitId, today.minusDays(10), today.plusDays(10), assignedAt)
            val migration = Files.readString(Path.of("docs/database/migrations/2026-10-19-benefit-grant-lifecycle.sql")).replace("public.", "$schema.")
            repeat(2) { jdbc.execute(migration) }
            assertEquals(1L, jdbc.queryForObject("select count(*) from $schema.benefit_grant_change", Long::class.java))
            assertEquals("ASSIGNED", jdbc.queryForObject("select action from $schema.benefit_grant_change", String::class.java))
            assertNull(jdbc.queryForObject("select reason from $schema.benefit_grant_change", String::class.java))
            assertEquals(assignedAt.toInstant(), jdbc.queryForObject("select changed_at from $schema.benefit_grant_change", OffsetDateTime::class.java)!!.toInstant())
            assertEquals(0L, jdbc.queryForObject("select count(*) from $schema.benefit_grant_change_requirement", Long::class.java))
            assertEquals(1L, jdbc.queryForObject("""select count(*) from $schema.role_permissions rp
                join $schema.roles r on r.id = rp.role_id join $schema.permissions p on p.id = rp.permission_id
                where r.name = 'ADMIN' and p.name = 'applications:management:benefits:manage'""", Long::class.java))
            assertEquals(0L, jdbc.queryForObject("""select count(*) from $schema.role_permissions rp
                join $schema.roles r on r.id = rp.role_id where r.name = 'CIUDADANO'""", Long::class.java))
            jdbc.update("update $schema.benefit_grant set status = 'SUSPENDED' where id = ?", grantId)
            jdbc.execute(migration)
            assertEquals("SUSPENDED", jdbc.queryForObject("select status from $schema.benefit_grant", String::class.java))
            assertEquals(1L, jdbc.queryForObject("select count(*) from $schema.benefit_grant_change", Long::class.java))
            jdbc.update("update $schema.benefit_grant set status = 'FINALIZED' where id = ?", grantId)
            assertFailsWith<DataIntegrityViolationException> { jdbc.update("update $schema.benefit_grant set status = 'UNKNOWN' where id = ?", grantId) }
            assertFailsWith<DataIntegrityViolationException> { jdbc.update("update $schema.benefit_grant set end_date = ? where id = ?", today.minusDays(11), grantId) }
            assertFailsWith<DataIntegrityViolationException> { jdbc.update("update $schema.benefit_grant set amount = -1 where id = ?", grantId) }
            assertFailsWith<DataIntegrityViolationException> { jdbc.execute("delete from $schema.users where id = 1") }
            assertFailsWith<DataIntegrityViolationException> {
                jdbc.execute("insert into $schema.benefit_grant_change select * from $schema.benefit_grant_change")
            }
            // Legacy Hibernate enum checks on history must also be replaced, not only the explicit transition check.
            jdbc.execute("alter table $schema.benefit_grant_change add constraint old_history_action check (action in ('ASSIGNED','RENEWED','SUSPENDED','FINALIZED'))")
            jdbc.execute("alter table $schema.benefit_grant_change add constraint old_history_status check (status in ('ACTIVE','SUSPENDED','FINALIZED'))")
            jdbc.execute("alter table $schema.benefit_grant_change add constraint old_history_previous check (previous_status in ('ACTIVE','SUSPENDED','FINALIZED'))")
            val expirationMigration = Files.readString(Path.of("docs/database/migrations/2026-10-26-benefit-grant-expiration.sql")).replace("public.", "$schema.")
            repeat(2) { jdbc.execute(expirationMigration) }
            assertEquals("FINALIZED", jdbc.queryForObject("select status from $schema.benefit_grant", String::class.java))
            assertEquals(1L, jdbc.queryForObject("select count(*) from $schema.benefit_grant_change", Long::class.java))
            assertEquals(assignedAt.toInstant(), jdbc.queryForObject("select changed_at from $schema.benefit_grant_change", OffsetDateTime::class.java)!!.toInstant())
            assertEquals(1L, jdbc.queryForObject("select responsible_user_id from $schema.benefit_grant_change", Long::class.java))
            jdbc.update("update $schema.benefit_grant set status = 'EXPIRED' where id = ?", grantId)
            jdbc.update("""insert into $schema.benefit_grant_change
                (id,grant_id,sequence_number,action,previous_status,status,previous_start_date,previous_end_date,start_date,end_date,reason,responsible_user_id,changed_at)
                select ?,grant_id,2,'EXPIRED','ACTIVE','EXPIRED',start_date,end_date,start_date,end_date,?,null,changed_at
                from $schema.benefit_grant_change where sequence_number=1""", UUID.randomUUID(), BenefitGrantExpirationService.EXPIRATION_REASON)
            jdbc.execute(expirationMigration)
            assertEquals(2L, jdbc.queryForObject("select count(*) from $schema.benefit_grant_change", Long::class.java))
            assertNull(jdbc.queryForObject("select responsible_user_id from $schema.benefit_grant_change where action='EXPIRED'", Long::class.java))
            assertFailsWith<DataIntegrityViolationException> { jdbc.execute("update $schema.benefit_grant_change set responsible_user_id=1 where action='EXPIRED'") }
            assertFailsWith<DataIntegrityViolationException> { jdbc.execute("update $schema.benefit_grant_change set responsible_user_id=null where action='ASSIGNED'") }
            assertFailsWith<DataIntegrityViolationException> { jdbc.execute("update $schema.benefit_grant_change set previous_status='FINALIZED' where action='EXPIRED'") }
            assertFailsWith<DataIntegrityViolationException> { jdbc.execute("update $schema.benefit_grant_change set reason=null where action='EXPIRED'") }
            assertFailsWith<DataIntegrityViolationException> { jdbc.update("update $schema.benefit_grant set end_date = ? where id = ?", today.minusDays(11), grantId) }
        } finally { jdbc.execute("drop schema $schema cascade") }
        unchanged()
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


    private fun assigned(benefitId: UUID = f.benefitId, end: LocalDate = today.plusDays(10)): BenefitGrantResponse {
        val result = assign(content = body(benefitId, end = end))
        expect(result, 201)
        return response(result)
    }
    @Test fun `bandeja administrativa filtra y protege otorgamientos`() {
        val grant = assigned()
        val number = tx { applications.findById(f.applicationId).orElseThrow().applicationNumber!! }
        val unfiltered = mvc.perform(get("/api/admin/benefit-grants").header("Authorization", "Bearer ${f.token}")).andReturn()
        expect(unfiltered, 200)
        assertTrue(json.readTree(unfiltered.response.contentAsString).get("content").any { it.get("benefit").get("id").stringValue() == grant.id.toString() })
        val result = mvc.perform(get("/api/admin/benefit-grants")
            .param("applicationNumber", number.toString()).param("status", "ACTIVE").param("page", "0").param("size", "1")
            .header("Authorization", "Bearer ${f.token}")).andReturn()
        expect(result, 200)
        val page = json.readTree(result.response.contentAsString)
        assertEquals(1, page.get("totalElements").intValue())
        assertEquals(grant.id.toString(), page.get("content").get(0).get("benefit").get("id").stringValue())
        assertEquals(f.applicantId, page.get("content").get(0).get("userId").longValue())
        val empty = mvc.perform(get("/api/admin/benefit-grants").param("applicationNumber", number.toString())
            .param("status", "SUSPENDED").header("Authorization", "Bearer ${f.token}")).andReturn()
        expect(empty, 200)
        assertEquals(0, json.readTree(empty.response.contentAsString).get("totalElements").intValue())
        val denied = mvc.perform(get("/api/admin/benefit-grants").header("Authorization", "Bearer ${f.citizenToken}")).andReturn()
        expect(denied, 403)
        expect(mvc.perform(get("/api/admin/program-editions/${f.editionId}/benefits").header("Authorization", "Bearer ${f.token}")).andReturn(), 200)
        expect(mvc.perform(get("/api/admin/program-editions/${f.editionId}/requirements").header("Authorization", "Bearer ${f.token}")).andReturn(), 200)
    }
    private fun change(grantId: UUID, action: String, content: String = """{"reason":" Motivo del cambio "}""",
        token: String? = f.token, applicationId: UUID = f.applicationId): MvcResult {
        val request = post("/api/admin/applications/$applicationId/benefit-grants/$grantId/$action")
            .contentType(MediaType.APPLICATION_JSON).content(content)
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }
    private fun grantHistory(grantId: UUID, token: String? = f.token, applicationId: UUID = f.applicationId): MvcResult {
        val request = get("/api/admin/applications/$applicationId/benefit-grants/$grantId/history")
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }
    private fun renewal(end: LocalDate = today.plusDays(15), checks: List<Map<String, Any?>> = emptyList(), reason: String = " Renovacion privada ") =
        json.writeValueAsString(mapOf("reason" to reason, "endDate" to end, "requirementChecks" to checks))
    private fun addRequirement() = tx {
        requirements.saveAndFlush(ProgramRequirement(programEdition = editions.findById(f.editionId).orElseThrow(),
            type = ProgramRequirementType.MIN_AGE, value = "18", description = "Requisito original")).id!!
    }
    private fun addRenewalDocument(status: ApplicationDocumentStatus?) = tx {
        val a = applications.findById(f.applicationId).orElseThrow()
        val requirement = documentRequirements.saveAndFlush(ProgramDocumentRequirement(programEdition = a.programEdition,
            code = "DNI", name = "DNI", required = true))
        if (status != null) {
            val now = LocalDateTime.now()
            val document = documents.saveAndFlush(Document(originalName = "dni.pdf", contentType = "application/pdf", sizeBytes = 4,
                content = byteArrayOf(1, 2, 3, 4), uploadedBy = a.user, createdAt = now))
            applicationDocuments.saveAndFlush(ApplicationDocument(application = a, requirement = requirement, document = document,
                status = status, uploadedAt = now, observation = if (status == ApplicationDocumentStatus.OBSERVED) "Ilegible" else null,
                reviewedBy = if (status == ApplicationDocumentStatus.PENDING) null else users.findById(f.workerId).orElseThrow(),
                reviewedAt = if (status == ApplicationDocumentStatus.PENDING) null else now))
        }
        true
    }
    private fun assertGrantUnchanged(grant: BenefitGrantResponse) = tx {
        val current = grants.findById(grant.id).orElseThrow()
        assertEquals(grant.status, current.status)
        assertEquals(grant.endDate, current.endDate)
        assertEquals(1, changes.findAllByGrantIdOrderBySequenceNumberAsc(grant.id).size)
        assertEquals(ApplicationStatus.APPROVED, applications.findById(f.applicationId).orElseThrow().status)
        assertEquals(LocalDateTime.ofInstant(grant.assignedAt.toInstant(), ZoneId.of("America/Argentina/Buenos_Aires")),
            applications.findById(f.applicationId).orElseThrow().updatedAt)
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        true
    }

    @Test fun `suspende renueva y finaliza conservando secuencia datos resolucion y fechas UTC`() {
        val original = assigned()
        expect(change(original.id, "suspend"), 200)
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        assertEquals(ApplicationStatus.APPROVED, applications.findById(f.applicationId).orElseThrow().status)
        val renewed = change(original.id, "renew", renewal())
        expect(renewed, 200)
        assertEquals(BenefitGrantStatus.ACTIVE, response(renewed).status)
        assertEquals(original.startDate, response(renewed).startDate)
        assertEquals(original.amount, response(renewed).amount)
        assertEquals(original.assignedAt, response(renewed).assignedAt)
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        expect(change(original.id, "finalize"), 200)
        val result = grantHistory(original.id)
        expect(result, 200)
        val entries = json.readTree(result.response.contentAsString)
        assertEquals(listOf("ASSIGNED", "SUSPENDED", "RENEWED", "FINALIZED"), (0 until entries.size()).map { entries[it].get("action").stringValue() })
        assertEquals(listOf(1, 2, 3, 4), (0 until entries.size()).map { entries[it].get("sequenceNumber").intValue() })
        assertTrue(entries[0].get("reason").isNull)
        assertTrue(entries[0].get("previousStatus").isNull)
        assertEquals("Motivo del cambio", entries[1].get("reason").stringValue())
        assertEquals("SUSPENDED", entries[2].get("previousStatus").stringValue())
        assertEquals("ACTIVE", entries[2].get("status").stringValue())
        entries.forEach {
            assertEquals(f.actorId, it.get("responsibleUserId").longValue())
            assertEquals(ZoneOffset.UTC, OffsetDateTime.parse(it.get("changedAt").stringValue()).offset)
        }
        tx {
            val a = applications.findDetailById(f.applicationId)!!
            assertEquals(ApplicationStatus.CLOSED, a.status)
            assertEquals("Motivo privado", a.resolutionReason)
            assertEquals(f.workerId, a.resolvedBy?.id)
            assertEquals(f.workerId, a.assignedWorker?.id)
            assertEquals(today.minusDays(1).atStartOfDay(), a.resolvedAt)
            val grant = grants.findById(original.id).orElseThrow()
            assertEquals(today.plusDays(15), grant.endDate) // Finalization does not trim the agreed dates.
            assertEquals(original.startDate, grant.startDate)
            assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        }
        val logs = jdbc.queryForList("select new_values::text from logs where entity_type = 'benefit_grant_change'", String::class.java)
        assertTrue(logs.isNotEmpty())
        logs.forEach { assertFalse(it.contains("Motivo del cambio")); assertFalse(it.contains("Renovacion privada")); assertFalse(it.contains("1250.50")) }
        listOf("renew", "suspend", "finalize").forEach {
            expect(change(original.id, it, if (it == "renew") renewal(today.plusDays(16)) else """{"reason":"x"}"""), 409)
        }
        assertEquals(4, json.readTree(grantHistory(original.id).response.contentAsString).size())
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    @Test fun `finalizar uno conserva cupo y solo finalizar ultimo suspendido cierra solicitud`() {
        val first = assigned()
        val second = assigned(addBenefit())
        addBenefit() // An unassigned catalog benefit does not prevent closing the application.
        expect(change(second.id, "suspend"), 200)
        expect(change(first.id, "finalize"), 200)
        assertEquals(ApplicationStatus.APPROVED, applications.findById(f.applicationId).orElseThrow().status)
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        expect(change(second.id, "finalize"), 200)
        assertEquals(ApplicationStatus.CLOSED, applications.findById(f.applicationId).orElseThrow().status)
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        expect(assign(content = body(addBenefit())), 409)
    }

    @Test fun `estado definitivo y suspension repetida no generan cambios aunque existan otros beneficios`() {
        val first = assigned()
        assigned(addBenefit())
        expect(change(first.id, "suspend"), 200)
        expect(change(first.id, "suspend"), 409, "BENEFIT_GRANT_INVALID_TRANSITION")
        expect(change(first.id, "finalize"), 200)
        listOf("renew", "suspend", "finalize").forEach {
            expect(change(first.id, it, if (it == "renew") renewal() else """{"reason":"x"}"""), 409, "BENEFIT_GRANT_INVALID_TRANSITION")
        }
        assertEquals(3, json.readTree(grantHistory(first.id).response.contentAsString).size())
    }

    @ParameterizedTest @ValueSource(strings = ["renew", "suspend", "finalize"])
    fun `motivo obligatorio y campos del servidor rechazados sin modificar nada`(action: String) {
        val grant = assigned()
        val valid = if (action == "renew") renewal() else """{"reason":"x"}"""
        listOf("{}", "null", "[]", valid.replace(if (action == "renew") " Renovacion privada " else "x", " "),
            valid.replace(if (action == "renew") " Renovacion privada " else "x", "a".repeat(1001)),
            valid.dropLast(1) + ""","responsibleUserId":1}""", valid.dropLast(1) + ""","status":"ACTIVE"}""",
            valid.dropLast(1) + ""","startDate":"$today"}""", valid.dropLast(1) + ""","amount":1}""").forEach {
            expect(change(grant.id, action, it), 400, "BENEFIT_GRANT_INVALID_DATA")
        }
        assertGrantUnchanged(grant)
    }

    @Test fun `renovacion exige nueva fecha completa posterior dentro de edicion y cubriendo hoy`() {
        val grant = assigned()
        listOf(today.minusDays(1), today, grant.endDate, grant.endDate.minusDays(1), today.plusDays(21)).forEach {
            expect(change(grant.id, "renew", renewal(it)), 400, "BENEFIT_GRANT_INVALID_DATA")
        }
        listOf("""{"reason":"x","requirementChecks":[]}""", """{"reason":"x","endDate":null,"requirementChecks":[]}""",
            """{"reason":"x","endDate":"2026-99-01","requirementChecks":[]}""").forEach {
            expect(change(grant.id, "renew", it), 400, "BENEFIT_GRANT_INVALID_DATA")
        }
        assertGrantUnchanged(grant)
        expect(change(grant.id, "renew", renewal(today.plusDays(20))), 200)
    }

    @ParameterizedTest @ValueSource(booleans = [false, true])
    fun `rechaza renovar vencidos activos o suspendidos aunque no corrio el barrido`(suspended: Boolean) {
        val grant = assigned(end = today)
        // Simulate a grant whose agreed end has elapsed, keeping its factual initial event consistent.
        jdbc.update("update benefit_grant set end_date = ? where id = ?", today.minusDays(1), grant.id)
        jdbc.update("update benefit_grant_change set end_date = ? where grant_id = ?", today.minusDays(1), grant.id)
        if (suspended) expect(change(grant.id, "suspend"), 200)
        expect(change(grant.id, "renew", renewal(today.plusDays(2))), 409, "BENEFIT_GRANT_INVALID_TRANSITION")
        assertEquals(if (suspended) BenefitGrantStatus.SUSPENDED else BenefitGrantStatus.ACTIVE, grants.findById(grant.id).orElseThrow().status)
        assertEquals(today.minusDays(1), grants.findById(grant.id).orElseThrow().endDate)
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    @Test fun `reevalua catalogo actual y guarda copias resistentes a cambios y bajas`() {
        val grant = assigned()
        val requirementId = addRequirement()
        expect(change(grant.id, "renew", renewal()), 400, "BENEFIT_GRANT_INVALID_DATA")
        expect(change(grant.id, "renew", renewal(checks = listOf(mapOf("requirementId" to requirementId, "fulfilled" to false)))),
            409, "BENEFIT_GRANT_REQUIREMENTS_NOT_MET")
        val check = mapOf<String, Any?>("requirementId" to requirementId, "fulfilled" to true)
        val invalidChecks = listOf(listOf(check, check), listOf(mapOf("requirementId" to UUID.randomUUID(), "fulfilled" to true)),
            listOf(mapOf("requirementId" to requirementId)), listOf(mapOf("requirementId" to null, "fulfilled" to true)))
        invalidChecks.forEach { expect(change(grant.id, "renew", renewal(checks = it)), 400, "BENEFIT_GRANT_INVALID_DATA") }
        expect(change(grant.id, "renew", """{"reason":"x","endDate":"${today.plusDays(15)}","requirementChecks":[null]}"""), 400)
        expect(change(grant.id, "renew", renewal().replace("\"requirementChecks\":[]", "\"requirementChecks\":null")), 400)
        assertGrantUnchanged(grant)
        expect(change(grant.id, "renew", renewal(checks = listOf(check))), 200)
        tx { requirements.findById(requirementId).orElseThrow().also { it.value = "30"; it.description = "Cambio" } }
        tx { requirements.deleteById(requirementId) }
        val snapshot = json.readTree(grantHistory(grant.id).response.contentAsString)[1].get("requirementChecks")[0]
        assertEquals("18", snapshot.get("value").stringValue())
        assertEquals("Requisito original", snapshot.get("description").stringValue())
        assertTrue(snapshot.get("fulfilled").booleanValue())
        expect(updateCatalog(), 200)
        expect(change(grant.id, "renew", renewal(today.plusDays(16))), 200)
        val node = json.readTree(list().response.contentAsString)[0]
        assertEquals("FOOD_ASSISTANCE", node.get("benefitType").stringValue())
        assertEquals("Descripcion reservada", node.get("description").stringValue())
        assertEquals(0, grant.amount!!.compareTo(node.get("amount").decimalValue()))
    }

    @ParameterizedTest @ValueSource(strings = ["MISSING", "PENDING", "OBSERVED", "VALID"])
    fun `renovacion requiere documentos obligatorios VALID`(status: String) {
        val grant = assigned()
        addRenewalDocument(if (status == "MISSING") null else ApplicationDocumentStatus.valueOf(status))
        expect(change(grant.id, "renew", renewal()), if (status == "VALID") 200 else 409,
            if (status == "VALID") null else "BENEFIT_GRANT_DOCUMENTS_NOT_VALID")
        if (status != "VALID") assertGrantUnchanged(grant)
    }

    @ParameterizedTest @EnumSource(ProgramEditionStatus::class, names = ["ACTIVE"], mode = EnumSource.Mode.EXCLUDE)
    fun `edicion inactiva bloquea renovar pero admite suspender y finalizar`(status: ProgramEditionStatus) {
        val grant = assigned()
        tx { editions.findById(f.editionId).orElseThrow().status = status }
        expect(change(grant.id, "renew", renewal()), 409, "BENEFIT_GRANT_EDITION_NOT_ACTIVE")
        assertGrantUnchanged(grant)
        expect(change(grant.id, "suspend"), 200)
        expect(change(grant.id, "finalize"), 200)
    }
    @Test fun `edicion vencida admite finalizar y no admite renovar fuera de sus fechas`() {
        val grant = assigned()
        tx { editions.findById(f.editionId).orElseThrow().endDate = today.minusDays(1) }
        expect(change(grant.id, "renew", renewal()), 400)
        expect(change(grant.id, "finalize"), 200)
    }
    @ParameterizedTest @ValueSource(strings = ["renew", "suspend", "finalize"])
    fun `actuaciones exigen solicitud aprobada y pertenencia del otorgamiento`(action: String) {
        val grant = assigned()
        val content = if (action == "renew") renewal() else """{"reason":"x"}"""
        expect(change(grant.id, action, content, applicationId = UUID.randomUUID()), 404, "APPLICATION_NOT_FOUND")
        expect(change(UUID.randomUUID(), action, content), 404, "BENEFIT_GRANT_NOT_FOUND")
        expect(change(grant.id, action, content, applicationId = fixture().applicationId), 404, "BENEFIT_GRANT_NOT_FOUND")
        tx { applications.findById(f.applicationId).orElseThrow().status = ApplicationStatus.CLOSED }
        expect(change(grant.id, action, content), 409, "BENEFIT_GRANT_APPLICATION_NOT_APPROVED")
        expect(grantHistory(grant.id), 200)
    }

    @ParameterizedTest @ValueSource(booleans = [false, true])
    fun `renovacion revalida incompatibilidades en ambos sentidos sin bloquear suspension ni finalizacion`(reverse: Boolean) {
        val grant = assigned()
        incompatible(fixture(applicantId = f.applicantId), reverse)
        expect(change(grant.id, "renew", renewal()), 409, "BENEFIT_GRANT_INCOMPATIBLE_APPLICATION")
        assertGrantUnchanged(grant)
        expect(change(grant.id, "suspend"), 200)
        expect(change(grant.id, "finalize"), 200)
    }
    @ParameterizedTest @ValueSource(ints = [-1, 0, 1])
    fun `renovacion usa extremos inclusivos de ediciones para incompatibilidad`(gap: Int) {
        val grant = assigned()
        val other = fixture(applicantId = f.applicantId)
        incompatible(other)
        tx { editions.findById(other.editionId).orElseThrow().also {
            it.startDate = today.plusDays(20L + gap); it.endDate = today.plusDays(40)
        } }
        expect(change(grant.id, "renew", renewal()), if (gap > 0) 200 else 409)
    }

    @Test fun `historial y cambios requieren usuario rol activo y permisos vigentes sin exposicion ciudadana`() {
        val grant = assigned()
        expect(change(grant.id, "suspend", token = null), 401)
        expect(grantHistory(grant.id, token = null), 401)
        expect(change(grant.id, "suspend", token = f.citizenToken), 403)
        expect(grantHistory(grant.id, token = f.citizenToken), 403)
        tx { roles.findById(f.roleId).orElseThrow().permissions.removeIf { it.name == "applications:management:benefits:manage" }; true }
        expect(change(grant.id, "suspend"), 403)
        expect(grantHistory(grant.id), 200)
        tx { roles.findById(f.roleId).orElseThrow().permissions.add(permission("applications:management:benefits:manage")) }
        val reader = tx {
            val role = roles.saveAndFlush(Role(name = "LECTOR_${UUID.randomUUID()}", permissions = mutableSetOf(permission("applications:management:view"))))
            val actor = users.findById(f.actorId).orElseThrow()
            actor.roles.add(role)
            jwt.createToken(actor, role) to role.id!!
        }
        expect(change(grant.id, "suspend", token = reader.first), 403)
        tx { roles.findById(reader.second).orElseThrow().permissions.add(permission("applications:management:benefits:manage")) }
        expect(change(grant.id, "suspend", token = reader.first), 403)
        tx { roles.findById(f.roleId).orElseThrow().permissions.removeIf { it.name == "applications:management:view" }; true }
        expect(grantHistory(grant.id), 403)
        tx { users.findById(f.actorId).orElseThrow().roles.clear(); true }
        expect(change(grant.id, "suspend"), 403)
        tx { users.findById(f.actorId).orElseThrow().roles.add(roles.findById(f.roleId).orElseThrow()) }
        tx { users.findById(f.actorId).orElseThrow().active = false }
        expect(change(grant.id, "suspend"), 401)
        expect(grantHistory(grant.id), 401)
        assertGrantUnchanged(grant)
        val citizen = mvc.perform(get("/api/applications/${f.applicationId}").header("Authorization", "Bearer ${f.citizenToken}")).andReturn()
        expect(citizen, 200)
        listOf("history", "benefitGrants", "reason", "responsibleUserId").forEach { assertFalse(json.readTree(citizen.response.contentAsString).has(it)) }
    }

    @Test fun `protege al responsable de cambios aunque no sea quien asigno`() {
        val grant = assigned()
        val other = fixture()
        expect(change(grant.id, "suspend", token = other.token), 200)
        expect(mvc.perform(delete("/users/${other.actorId}").header("Authorization", "Bearer ${f.token}")).andReturn(),
            409, "USER_HAS_APPLICATION_REFERENCES")
        assertTrue(users.existsById(other.actorId))
        expect(grantHistory(UUID.randomUUID()), 404, "BENEFIT_GRANT_NOT_FOUND")
        expect(grantHistory(grant.id, applicationId = other.applicationId), 404, "BENEFIT_GRANT_NOT_FOUND")
        expect(grantHistory(grant.id, applicationId = UUID.randomUUID()), 404, "APPLICATION_NOT_FOUND")
    }

    @ParameterizedTest @ValueSource(strings = ["renew", "suspend", "finalize"])
    fun `auditoria fallida revierte estado vigencia historial checklist solicitud y cupo`(action: String) {
        val grant = assigned()
        val requirementId = addRequirement()
        val content = if (action == "renew") renewal(checks = listOf(mapOf("requirementId" to requirementId, "fulfilled" to true))) else """{"reason":"x"}"""
        jdbc.execute("alter table logs add constraint ck_lifecycle_audit_failure check (entity_type <> 'benefit_grant_change' or user_id <> ${f.actorId})")
        try { expect(change(grant.id, action, content), 409, "DATA_INTEGRITY_VIOLATION"); assertGrantUnchanged(grant) }
        finally { jdbc.execute("alter table logs drop constraint ck_lifecycle_audit_failure") }
    }
    @Test fun `contador inconsistente impide finalizar sin perder estado ni historia`() {
        val grant = assigned()
        tx { editions.findById(f.editionId).orElseThrow().currentEnrollment = 0 }
        expect(change(grant.id, "finalize"), 409, "BENEFIT_GRANT_INCONSISTENT_ENROLLMENT")
        assertEquals(BenefitGrantStatus.ACTIVE, grants.findById(grant.id).orElseThrow().status)
        assertEquals(1, changes.findAllByGrantIdOrderBySequenceNumberAsc(grant.id).size)
        assertEquals(ApplicationStatus.APPROVED, applications.findById(f.applicationId).orElseThrow().status)
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    @ParameterizedTest @ValueSource(strings = ["renew", "suspend", "finalize"])
    fun `operaciones simultaneas repetidas solo confirman un cambio`(action: String) {
        val grant = assigned()
        val content = if (action == "renew") renewal() else """{"reason":"x"}"""
        val results = concurrently({ change(grant.id, action, content) }, { change(grant.id, action, content) })
        assertEquals(listOf(200, if (action == "renew") 400 else 409), results.map { it.response.status }.sorted())
        assertEquals(2, changes.findAllByGrantIdOrderBySequenceNumberAsc(grant.id).size)
        assertEquals(if (action == "finalize") 0 else 1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }
    @Test fun `finalizar dos ultimos otorgamientos concurrentes libera exactamente un cupo`() {
        val first = assigned()
        val second = assigned(addBenefit())
        val results = concurrently({ change(first.id, "finalize") }, { change(second.id, "finalize") })
        assertTrue(results.all { it.response.status == 200 }, results.map { it.response.contentAsString }.toString())
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        assertEquals(ApplicationStatus.CLOSED, applications.findById(f.applicationId).orElseThrow().status)
        assertEquals(2, changes.findAllByGrantIdOrderBySequenceNumberAsc(first.id).size)
        assertEquals(2, changes.findAllByGrantIdOrderBySequenceNumberAsc(second.id).size)
    }
    @Test fun `renovacion y finalizacion concurrentes no reabren beneficio definitivo`() {
        val grant = assigned()
        val results = concurrently({ change(grant.id, "renew", renewal()) }, { change(grant.id, "finalize") })
        assertEquals(200, results[1].response.status)
        assertTrue(results[0].response.status in listOf(200, 409))
        assertEquals(BenefitGrantStatus.FINALIZED, grants.findById(grant.id).orElseThrow().status)
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }
    @Test fun `nueva asignacion y finalizacion concurrentes mantienen solicitud y reserva consistentes`() {
        val grant = assigned()
        val benefit = addBenefit()
        val results = concurrently({ assign(content = body(benefit)) }, { change(grant.id, "finalize") })
        assertEquals(200, results[1].response.status)
        assertTrue(results[0].response.status in listOf(201, 409))
        val created = results[0].response.status == 201
        assertEquals(if (created) ApplicationStatus.APPROVED else ApplicationStatus.CLOSED, applications.findById(f.applicationId).orElseThrow().status)
        assertEquals(if (created) 1 else 0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }
    @Test fun `renovacion y cambio de fechas de edicion concurrentes conservan contencion`() {
        val grant = assigned()
        val results = concurrently({ change(grant.id, "renew", renewal()) }, { editEdition(end = today.plusDays(12)) })
        assertTrue(results.map { it.response.status } in listOf(listOf(200, 409), listOf(400, 200)),
            results.map { it.response.contentAsString }.toString())
        assertTrue(grants.findById(grant.id).orElseThrow().endDate <= editions.findById(f.editionId).orElseThrow().endDate)
    }


    @Test fun `renovacion y alta de requisito concurrentes usan configuracion consistente`() {
        val grant = assigned()
        val results = concurrently({ change(grant.id, "renew", renewal()) }, {
            mvc.perform(post("/api/admin/program-editions/${f.editionId}/requirements")
                .header("Authorization", "Bearer ${f.token}").contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"MIN_AGE","value":"18","description":"Nuevo requisito"}""")).andReturn()
        })
        assertEquals(201, results[1].response.status, results[1].response.contentAsString)
        assertTrue(results[0].response.status in listOf(200, 400), results[0].response.contentAsString)
        tx {
            val history = changes.findAllByGrantIdOrderBySequenceNumberAsc(grant.id)
            assertEquals(if (results[0].response.status == 200) 2 else 1, history.size)
            if (history.size == 2) assertTrue(history.last().requirementChecks.isEmpty())
            assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        }
    }

    private fun prepareEvaluation(fixture: Fixture) = tx {
        val a = applications.findById(fixture.applicationId).orElseThrow()
        a.status = ApplicationStatus.IN_EVALUATION
        evaluations.saveAndFlush(ApplicationEvaluation(application = a, vulnerability = VulnerabilityLevel.HIGH,
            priority = EvaluationPriority.HIGH, recommendation = "Evaluacion", professional = users.findById(fixture.workerId).orElseThrow(),
            evaluatedAt = OffsetDateTime.now(ZoneOffset.UTC)))
        true
    }
    private fun approve(fixture: Fixture) = mvc.perform(post("/api/admin/applications/${fixture.applicationId}/resolutions")
        .header("Authorization", "Bearer ${fixture.token}").contentType(MediaType.APPLICATION_JSON)
        .content("""{"decision":"APPROVED","reason":"Cumple","requirementChecks":[]}""")).andReturn()

    @Test fun `liberar cupo y aprobar otra solicitud concurrentemente nunca sobreocupa la edicion`() {
        val grant = assigned()
        val candidate = fixture(editionId = f.editionId)
        prepareEvaluation(candidate)
        tx { editions.findById(f.editionId).orElseThrow().also { it.currentEnrollment = 1; it.maxCapacity = 1 } }
        val results = concurrently({ change(grant.id, "finalize") }, { approve(candidate) })
        assertEquals(200, results[0].response.status)
        assertTrue(results[1].response.status in listOf(201, 409), results[1].response.contentAsString)
        assertEquals(if (results[1].response.status == 201) 1 else 0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        assertEquals(ApplicationStatus.CLOSED, applications.findById(f.applicationId).orElseThrow().status)
    }

    @Test fun `cerrar ultimo beneficio elimina reserva incompatible pero suspension la conserva`() {
        val grant = assigned()
        val candidate = fixture(applicantId = f.applicantId)
        prepareEvaluation(candidate)
        tx { editions.findById(candidate.editionId).orElseThrow().currentEnrollment = 0 }
        incompatible(candidate)
        expect(approve(candidate), 409, "APPLICATION_RESOLUTION_INCOMPATIBLE_APPLICATION")
        expect(change(grant.id, "suspend"), 200)
        expect(approve(candidate), 409, "APPLICATION_RESOLUTION_INCOMPATIBLE_APPLICATION")
        expect(change(grant.id, "finalize"), 200)
        expect(approve(candidate), 201)
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        assertEquals(1, editions.findById(candidate.editionId).orElseThrow().currentEnrollment)
    }

    private fun overdue(grantId: UUID) {
        jdbc.update("update benefit_grant set end_date = ? where id = ?", today.minusDays(1), grantId)
        jdbc.update("update benefit_grant_change set end_date = ? where grant_id = ?", today.minusDays(1), grantId)
    }

    private fun <A, B> race(first: () -> A, second: () -> B): Pair<A, B> {
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val a = executor.submit(Callable { start.await(); first() })
            val b = executor.submit(Callable { start.await(); second() })
            start.countDown()
            return a.get(30, TimeUnit.SECONDS) to b.get(30, TimeUnit.SECONDS)
        } finally { executor.shutdownNow() }
    }

    @ParameterizedTest @ValueSource(booleans = [false, true])
    fun `vence activo o suspendido con historial de sistema sin perder datos y libera una sola reserva`(suspended: Boolean) {
        val original = assigned(end = today)
        overdue(original.id)
        if (suspended) expect(change(original.id, "suspend"), 200)
        val now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusSeconds(1).plusNanos(123456789)
        `when`(clock.instant()).thenReturn(now)
        assertEquals(1, expiration.expireApplication(f.applicationId))
        assertEquals(0, expiration.expireApplication(f.applicationId))
        val history = json.readTree(grantHistory(original.id).response.contentAsString)
        val event = history[history.size() - 1]
        assertEquals("EXPIRED", event.get("action").stringValue())
        assertEquals("EXPIRED", event.get("status").stringValue())
        assertEquals(if (suspended) "SUSPENDED" else "ACTIVE", event.get("previousStatus").stringValue())
        assertTrue(event.get("responsibleUserId").isNull)
        assertEquals("Sistema", event.get("responsibleUserName").stringValue())
        assertEquals(BenefitGrantExpirationService.EXPIRATION_REASON, event.get("reason").stringValue())
        assertEquals(0, event.get("requirementChecks").size())
        assertEquals(now.truncatedTo(java.time.temporal.ChronoUnit.MICROS), OffsetDateTime.parse(event.get("changedAt").stringValue()).toInstant())
        assertEquals(if (suspended) 3 else 2, history.size())
        tx {
            val current = grants.findById(original.id).orElseThrow()
            assertEquals(BenefitGrantStatus.EXPIRED, current.status)
            assertEquals(original.startDate, current.startDate)
            assertEquals(today.minusDays(1), current.endDate)
            assertEquals(original.description, current.description)
            assertEquals(original.amount, current.amount)
            assertEquals(original.benefitType, current.benefitType)
            assertEquals(original.assignedAt, current.assignedAt)
            assertEquals(original.assignedByUserId, current.assignedBy.id)
            val a = applications.findDetailById(f.applicationId)!!
            assertEquals(ApplicationStatus.CLOSED, a.status)
            assertEquals("Motivo privado", a.resolutionReason)
            assertEquals(f.workerId, a.resolvedBy?.id)
            assertEquals(f.workerId, a.assignedWorker?.id)
            assertEquals(today.minusDays(1).atStartOfDay(), a.resolvedAt)
            assertEquals(LocalDateTime.ofInstant(now.truncatedTo(java.time.temporal.ChronoUnit.MICROS), ZoneId.of("America/Argentina/Buenos_Aires")), a.updatedAt)
            assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        }
        val audit = jdbc.queryForMap("select user_id, new_values::text as snapshot from logs where entity_type = 'benefit_grant_change' and entity_id = ?", event.get("id").stringValue())
        assertNull(audit["user_id"])
        val snapshot = audit["snapshot"].toString()
        assertTrue(snapshot.contains("EXPIRED"))
        listOf("Descripcion reservada", "1250.50", "Motivo privado", BenefitGrantExpirationService.EXPIRATION_REASON).forEach { assertFalse(snapshot.contains(it)) }
        listOf("renew", "suspend", "finalize").forEach { expect(change(original.id, it, if (it == "renew") renewal() else """{"reason":"x"}"""), 409) }
        val own = mvc.perform(get("/api/applications/${f.applicationId}").header("Authorization", "Bearer ${f.citizenToken}")).andReturn()
        expect(own, 200)
        assertFalse(own.response.contentAsString.contains("Sistema"))
        assertFalse(own.response.contentAsString.contains("benefitGrants"))
    }

    @Test fun `ultimo dia sigue vigente en zona local aunque UTC ya sea el dia siguiente`() {
        val grant = assigned(end = today)
        val midnight = today.plusDays(1).atStartOfDay(ZoneId.of("America/Argentina/Buenos_Aires")).toInstant()
        `when`(clock.instant()).thenReturn(midnight.minusNanos(1000))
        assertEquals(0, expiration.expireApplication(f.applicationId))
        expect(change(grant.id, "renew", renewal(today.plusDays(1))), 200)
        `when`(clock.instant()).thenReturn(midnight)
        assertEquals(0, expiration.expireApplication(f.applicationId)) // Re-read the renewed date, not a stale candidate.
        `when`(clock.instant()).thenReturn(midnight.plusSeconds(86400))
        assertEquals(1, expiration.expireApplication(f.applicationId))
    }

    @Test fun `vence juntos los pendientes y solo cierra al terminar el ultimo incluso suspendido`() {
        val first = assigned(end = today)
        overdue(first.id)
        val second = assigned(addBenefit(), end = today)
        overdue(second.id)
        expect(change(second.id, "suspend"), 200)
        val third = assigned(addBenefit(), end = today.plusDays(2))
        expect(change(third.id, "suspend"), 200)
        assertEquals(2, expiration.expireApplication(f.applicationId))
        assertEquals(1, grants.countUnfinished(f.applicationId))
        assertEquals(ApplicationStatus.APPROVED, applications.findById(f.applicationId).orElseThrow().status)
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        listOf("renew", "suspend", "finalize").forEach { expect(change(first.id, it, if (it == "renew") renewal() else """{"reason":"x"}"""), 409, "BENEFIT_GRANT_INVALID_TRANSITION") }
        expect(change(third.id, "finalize"), 200)
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        assertEquals(ApplicationStatus.CLOSED, applications.findById(f.applicationId).orElseThrow().status)
    }

    @ParameterizedTest @EnumSource(ProgramEditionStatus::class)
    fun `vencimiento no depende del estado o vigencia de la edicion`(status: ProgramEditionStatus) {
        val grant = assigned(end = today)
        overdue(grant.id)
        tx { editions.findById(f.editionId).orElseThrow().also { it.status = status; it.endDate = today.minusDays(1) } }
        assertEquals(1, expiration.expireApplication(f.applicationId))
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    @ParameterizedTest @ValueSource(strings = ["benefit_grant_change", "application"])
    fun `fallo de auditoria automatica revierte todo el caso y permite reintento`(entityType: String) {
        val first = assigned(end = today)
        overdue(first.id)
        val second = assigned(addBenefit(), end = today)
        overdue(second.id)
        val before = applications.findById(f.applicationId).orElseThrow().updatedAt
        jdbc.execute("""alter table logs add constraint ck_expiration_test_audit check (entity_type <> '$entityType'
            or user_id is not null or (entity_id <> '${f.applicationId}' and new_values::text not like '%${f.applicationId}%'))""")
        try {
            assertFails { expiration.expireApplication(f.applicationId) }
            assertEquals(before, applications.findById(f.applicationId).orElseThrow().updatedAt)
            assertEquals(ApplicationStatus.APPROVED, applications.findById(f.applicationId).orElseThrow().status)
            assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
            listOf(first, second).forEach {
                assertEquals(BenefitGrantStatus.ACTIVE, grants.findById(it.id).orElseThrow().status)
                assertEquals(1, changes.findAllByGrantIdOrderBySequenceNumberAsc(it.id).size)
            }
        } finally { jdbc.execute("alter table logs drop constraint ck_expiration_test_audit") }
        assertEquals(2, expiration.expireApplication(f.applicationId))
    }

    @Test fun `contador inconsistente revierte vencimiento automatico sin perder historia`() {
        val grant = assigned(end = today)
        overdue(grant.id)
        tx { editions.findById(f.editionId).orElseThrow().currentEnrollment = 0 }
        assertFails { expiration.expireApplication(f.applicationId) }
        assertEquals(BenefitGrantStatus.ACTIVE, grants.findById(grant.id).orElseThrow().status)
        assertEquals(1, changes.findAllByGrantIdOrderBySequenceNumberAsc(grant.id).size)
        assertEquals(ApplicationStatus.APPROVED, applications.findById(f.applicationId).orElseThrow().status)
    }

    @Test fun `barrido recupera atrasados por lotes y continua despues de un caso fallido`() {
        val bad = assigned(end = today)
        overdue(bad.id)
        tx { editions.findById(f.editionId).orElseThrow().currentEnrollment = 0 }
        val other = fixture()
        val result = assign(other, content = body(other.benefitId, end = today))
        expect(result, 201)
        val healthy = response(result)
        overdue(healthy.id)
        val scheduler = BenefitGrantExpirationScheduler(grants, expiration, BenefitGrantExpirationProperties(batchSize = 1),
            EnrollmentPeriodExpirationProperties(), Clock.systemUTC())
        scheduler.expireBenefits()
        assertEquals(BenefitGrantStatus.EXPIRED, grants.findById(healthy.id).orElseThrow().status)
        assertEquals(ApplicationStatus.CLOSED, applications.findById(other.applicationId).orElseThrow().status)
        assertEquals(BenefitGrantStatus.ACTIVE, grants.findById(bad.id).orElseThrow().status)
        assertEquals(1, changes.findAllByGrantIdOrderBySequenceNumberAsc(bad.id).size)
        tx { editions.findById(f.editionId).orElseThrow().currentEnrollment = 1 }
        scheduler.expireBenefits()
        assertEquals(BenefitGrantStatus.EXPIRED, grants.findById(bad.id).orElseThrow().status)
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    @Test fun `dos procesadores vencen el mismo caso una vez incluso con varios beneficios`() {
        val first = assigned(end = today)
        overdue(first.id)
        val second = assigned(addBenefit(), end = today)
        overdue(second.id)
        val result = race({ expiration.expireApplication(f.applicationId) }, { expiration.expireApplication(f.applicationId) })
        assertEquals(listOf(0, 2), listOf(result.first, result.second).sorted())
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        listOf(first, second).forEach { assertEquals(2, changes.findAllByGrantIdOrderBySequenceNumberAsc(it.id).size) }
    }

    @ParameterizedTest @ValueSource(strings = ["renew", "suspend", "finalize"])
    fun `vencimiento compite con actuaciones manuales sin reabrir ni duplicar reserva`(action: String) {
        val grant = assigned(end = today)
        overdue(grant.id)
        val result = race({ expiration.expireApplication(f.applicationId) }, { change(grant.id, action, if (action == "renew") renewal() else """{"reason":"x"}""") })
        if (action == "renew") expect(result.second, 409)
        else assertTrue(result.second.response.status in listOf(200, 409))
        assertEquals(if (action == "suspend" || action == "renew") BenefitGrantStatus.EXPIRED else
            if (result.second.response.status == 200) BenefitGrantStatus.FINALIZED else BenefitGrantStatus.EXPIRED,
            grants.findById(grant.id).orElseThrow().status)
        assertEquals(ApplicationStatus.CLOSED, applications.findById(f.applicationId).orElseThrow().status)
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    @Test fun `vencimiento y nueva asignacion conservan reserva solo si el alta gana el bloqueo`() {
        val grant = assigned(end = today)
        overdue(grant.id)
        val benefit = addBenefit()
        val result = race({ expiration.expireApplication(f.applicationId) }, { assign(content = body(benefit)) })
        assertEquals(1, result.first)
        assertTrue(result.second.response.status in listOf(201, 409))
        val created = result.second.response.status == 201
        assertEquals(if (created) ApplicationStatus.APPROVED else ApplicationStatus.CLOSED, applications.findById(f.applicationId).orElseThrow().status)
        assertEquals(if (created) 1 else 0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    @Test fun `vencimiento y aprobacion concurrentes no sobreocupan el ultimo cupo`() {
        val grant = assigned(end = today)
        overdue(grant.id)
        val candidate = fixture(editionId = f.editionId)
        prepareEvaluation(candidate)
        tx { editions.findById(f.editionId).orElseThrow().also { it.currentEnrollment = 1; it.maxCapacity = 1 } }
        val result = race({ expiration.expireApplication(f.applicationId) }, { approve(candidate) })
        assertEquals(1, result.first)
        assertTrue(result.second.response.status in listOf(201, 409))
        assertEquals(if (result.second.response.status == 201) 1 else 0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
    }

    private fun ownGrants(token: String? = f.citizenToken, query: String = ""): MvcResult {
        val request = get("/api/benefit-grants$query")
        if (token != null) request.header("Authorization", "Bearer $token")
        return mvc.perform(request).andReturn()
    }
    private fun ownResponse(result: MvcResult): CitizenBenefitGrantListResponse {
        expect(result, 200)
        return json.readValue(result.response.contentAsString, CitizenBenefitGrantListResponse::class.java)
    }

    @Test fun `listado propio aisla titulares agrega solicitudes y reconoce otorgamiento sobre solicitud asistida`() {
        val first = assigned()
        val other = fixture()
        val alienResult = assign(other)
        expect(alienResult, 201)
        val samePerson = fixture(applicantId = f.applicantId)
        jdbc.update("update application set registered_by_user_id=? where id=?", samePerson.actorId, samePerson.applicationId)
        val secondResult = assign(samePerson)
        expect(secondResult, 201)
        val second = response(secondResult)
        val own = ownResponse(ownGrants())
        assertEquals(setOf(first.id, second.id), own.content.map { it.id }.toSet())
        assertEquals(2L, own.totalElements)
        assertNull(own.message)
        assertEquals(setOf(f.programId, samePerson.programId), own.content.map { it.programId }.toSet())
        assertEquals(setOf(response(alienResult).id), ownResponse(ownGrants(other.citizenToken)).content.map { it.id }.toSet())
        val manipulated = ownResponse(ownGrants(query = "?userId=${other.applicantId}&citizenId=${other.applicantId}&applicationId=${other.applicationId}"))
        assertEquals(own.content.map { it.id }.toSet(), manipulated.content.map { it.id }.toSet())
        val serialized = json.readValue(ownGrants().response.contentAsString, Map::class.java)
        assertEquals(setOf("content", "page", "size", "totalElements", "totalPages", "message"), serialized.keys)
        (serialized["content"] as List<*>).forEach { row ->
            assertEquals(setOf("id", "applicationId", "applicationNumber", "programBenefitId", "programId", "programName", "programEditionId",
                "programEditionName", "benefitType", "description", "amount", "status", "startDate", "endDate"), (row as Map<*, *>).keys)
        }
    }

    @Test fun `renovar suspender finalizar y vencer se reflejan sin perder registros de casos cerrados`() {
        val first = assigned()
        assertEquals(BenefitGrantStatus.ACTIVE, ownResponse(ownGrants()).content.single().status)
        expect(change(first.id, "suspend"), 200)
        assertEquals(BenefitGrantStatus.SUSPENDED, ownResponse(ownGrants()).content.single().status)
        expect(change(first.id, "renew", renewal()), 200)
        val renewed = ownResponse(ownGrants()).content.single()
        assertEquals(BenefitGrantStatus.ACTIVE, renewed.status)
        assertEquals(today.plusDays(15), renewed.endDate)
        assertEquals(first.startDate, renewed.startDate)
        expect(change(first.id, "finalize"), 200)
        assertEquals(ApplicationStatus.CLOSED, applications.findById(f.applicationId).orElseThrow().status)
        tx { editions.findById(f.editionId).orElseThrow().status = ProgramEditionStatus.CLOSED }
        assertEquals(BenefitGrantStatus.FINALIZED, ownResponse(ownGrants()).content.single().status)
        val samePerson = fixture(applicantId = f.applicantId)
        val nextResult = assign(samePerson, content = body(samePerson.benefitId, end = today))
        expect(nextResult, 201)
        val next = response(nextResult)
        overdue(next.id)
        // GET returns the stored status; only the expiration service changes it.
        assertEquals(BenefitGrantStatus.ACTIVE, ownResponse(ownGrants()).content.first { it.id == next.id }.status)
        assertEquals(1, expiration.expireApplication(samePerson.applicationId))
        val ended = ownResponse(ownGrants())
        assertEquals(setOf(BenefitGrantStatus.FINALIZED, BenefitGrantStatus.EXPIRED), ended.content.map { it.status }.toSet())
        assertEquals(today.minusDays(1), ended.content.first { it.id == next.id }.endDate)
    }

    @Test fun `consulta propia usa copias del otorgamiento y admite descripcion y monto nulos`() {
        val original = assigned()
        expect(updateCatalog(), 200)
        val item = ownResponse(ownGrants()).content.single()
        assertEquals(original.benefitType, item.benefitType)
        assertEquals(original.description, item.description)
        assertEquals(original.amount, item.amount)
        val secondBenefit = addBenefit()
        tx { benefits.findById(secondBenefit).orElseThrow().also { it.description = null; it.amount = null } }
        assigned(secondBenefit)
        val nullable = ownResponse(ownGrants()).content.first { it.programBenefitId == secondBenefit }
        assertNull(nullable.description)
        assertNull(nullable.amount)
    }

    @Test fun `sin solicitudes o sin otorgamientos devuelve pagina vacia y mensaje sin fabricar beneficios`() {
        val without = tx {
            val citizen = users.findByIdWithRoles(f.applicantId)!!
            val user = users.saveAndFlush(User(name = "Sin solicitudes", roles = citizen.roles.toMutableSet()))
            jwt.createToken(user, citizen.roles.first())
        }
        for (token in listOf(f.citizenToken, without)) {
            val result = ownResponse(ownGrants(token))
            assertTrue(result.content.isEmpty())
            assertEquals(0L, result.totalElements)
            assertEquals(0, result.totalPages)
            assertEquals(0, result.page)
            assertEquals(20, result.size)
            assertEquals(CitizenBenefitGrantService.EMPTY_MESSAGE, result.message)
        }
        assertTrue(grants.findAllByApplicationIdOrderByAssignedAtAscIdAsc(f.applicationId).isEmpty())
    }

    @Test fun `pagina por fecha e id descendentes y distingue pagina fuera de rango de ausencia total`() {
        val first = assigned()
        val second = assigned(addBenefit())
        val third = assigned(addBenefit())
        val timestamp = first.assignedAt.plusSeconds(2)
        jdbc.update("update benefit_grant set assigned_at=? where id in (?,?)", timestamp, second.id, third.id)
        val tied = jdbc.queryForList("select id from benefit_grant where application_id=? and assigned_at=? order by id desc", UUID::class.java, f.applicationId, timestamp)
        val firstPage = ownResponse(ownGrants(query = "?page=0&size=2"))
        assertEquals(tied, firstPage.content.map { it.id })
        assertEquals(3L, firstPage.totalElements)
        assertEquals(2, firstPage.totalPages)
        val secondPage = ownResponse(ownGrants(query = "?page=1&size=2"))
        assertEquals(listOf(first.id), secondPage.content.map { it.id })
        assertNull(secondPage.message)
        val outside = ownResponse(ownGrants(query = "?page=10&size=2"))
        assertTrue(outside.content.isEmpty())
        assertEquals(3L, outside.totalElements)
        assertNull(outside.message)
        assertEquals(100, ownResponse(ownGrants(query = "?size=100")).size)
    }

    @ParameterizedTest @ValueSource(strings = ["?page=-1", "?size=0", "?size=101", "?page=x", "?size=x"])
    fun `paginacion ciudadana invalida devuelve 400`(query: String) {
        expect(ownGrants(query = query), 400)
    }

    @Test fun `consulta propia valida usuario rol activo y permiso vigente en JWT y base`() {
        assigned()
        expect(ownGrants(token = null), 401)
        expect(ownGrants(token = "token-invalido"), 401)
        val citizenRoleId = tx { users.findByIdWithRoles(f.applicantId)!!.roles.first().id!! }
        tx { roles.findById(citizenRoleId).orElseThrow().permissions.removeIf { it.name == OWN_BENEFIT_VIEW_PERMISSION }; true }
        expect(ownGrants(), 403)
        tx { roles.findById(citizenRoleId).orElseThrow().permissions.add(permission(OWN_BENEFIT_VIEW_PERMISSION)); true }
        expect(ownGrants(), 200)
        val oldToken = tx {
            val citizen = users.findByIdWithRoles(f.applicantId)!!
            val role = citizen.roles.first()
            role.permissions.removeIf { it.name == OWN_BENEFIT_VIEW_PERMISSION }
            jwt.createToken(citizen, role)
        }
        tx { roles.findById(citizenRoleId).orElseThrow().permissions.add(permission(OWN_BENEFIT_VIEW_PERMISSION)); true }
        expect(ownGrants(token = oldToken), 403)
        val wrongRole = tx {
            val citizen = users.findByIdWithRoles(f.applicantId)!!
            val role = roles.saveAndFlush(Role(name = "SIN_PERMISO_${UUID.randomUUID()}"))
            citizen.roles.add(role)
            jwt.createToken(citizen, role)
        }
        expect(ownGrants(token = wrongRole), 403)
        tx { users.findById(f.applicantId).orElseThrow().roles.removeIf { it.id == citizenRoleId }; true }
        expect(ownGrants(), 403)
        tx { users.findById(f.applicantId).orElseThrow().active = false }
        expect(ownGrants(), 401)
    }

    @Test fun `otro rol configurable puede consultar solo sus datos con permiso propio`() {
        val grant = assigned()
        val token = tx {
            val citizen = users.findByIdWithRoles(f.applicantId)!!
            val custom = roles.saveAndFlush(Role(name = "ROL_PROPIO_${UUID.randomUUID()}", permissions = mutableSetOf(permission(OWN_BENEFIT_VIEW_PERMISSION))))
            citizen.roles.add(custom)
            jwt.createToken(citizen, custom)
        }
        assertEquals(listOf(grant.id), ownResponse(ownGrants(token)).content.map { it.id })
        tx { roles.findById(f.roleId).orElseThrow().permissions.add(permission(OWN_BENEFIT_VIEW_PERMISSION)); true }
        val adminToken = tx { val actor = users.findByIdWithRoles(f.actorId)!!; jwt.createToken(actor, actor.roles.first()) }
        assertTrue(ownResponse(ownGrants(adminToken)).content.isEmpty()) // Own permission never means management access.
    }

    @Test fun `consulta no cambia fechas cupos historial ni auditoria y mantiene contrato de solicitud`() {
        val grant = assigned()
        val before = applications.findById(f.applicationId).orElseThrow().updatedAt
        val auditCount = jdbc.queryForObject("select count(*) from logs", Long::class.java)
        repeat(2) { ownResponse(ownGrants()) }
        assertEquals(before, applications.findById(f.applicationId).orElseThrow().updatedAt)
        assertEquals(1, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        assertEquals(1, changes.findAllByGrantIdOrderBySequenceNumberAsc(grant.id).size)
        assertEquals(auditCount, jdbc.queryForObject("select count(*) from logs", Long::class.java))
        val application = mvc.perform(get("/api/applications/${f.applicationId}").header("Authorization", "Bearer ${f.citizenToken}")).andReturn()
        expect(application, 200)
        assertFalse(application.response.contentAsString.contains("benefitGrants"))
        assertFalse(application.response.contentAsString.contains("assignedBy"))
    }

    @Test fun `listado carga contexto de varios programas sin consultas adicionales por elemento`() {
        assigned()
        val another = fixture(applicantId = f.applicantId)
        expect(assign(another), 201)
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.isStatisticsEnabled = true
        try {
            statistics.clear()
            ownResponse(ownGrants(query = "?size=1"))
            val one = statistics.prepareStatementCount
            statistics.clear()
            ownResponse(ownGrants(query = "?size=2"))
            val two = statistics.prepareStatementCount
            assertEquals(one, two)
            assertTrue(two <= 4, "Se ejecutaron $two consultas para dos elementos")
        } finally { statistics.isStatisticsEnabled = false }
    }

    @Test fun `permiso ciudadano se migra repetidamente sin tocar otorgamientos ni dar acceso a otros roles`() {
        assumeTrue(System.getProperty("benefit-grant.test.postgres-url") != null)
        val schema = "citizen_grant_migration_" + UUID.randomUUID().toString().replace("-", "")
        val before = grants.count()
        jdbc.execute("create schema $schema")
        try {
            jdbc.execute("""create table $schema.permissions(id bigserial primary key, name varchar(100) unique);
                create table $schema.roles(id bigserial primary key, name varchar(100) unique);
                create table $schema.role_permissions(role_id bigint references $schema.roles(id), permission_id bigint references $schema.permissions(id), primary key(role_id, permission_id));
                insert into $schema.roles(name) values ('ADMIN'),('CIUDADANO'),('PROFESIONAL');
                insert into $schema.permissions(name) values ('existente');
                insert into $schema.role_permissions select r.id,p.id from $schema.roles r cross join $schema.permissions p where r.name='PROFESIONAL';""")
            val migration = Files.readString(Path.of("docs/database/migrations/2026-10-26-citizen-benefit-grants.sql")).replace("public.", "$schema.")
            repeat(2) { jdbc.execute(migration) }
            assertEquals(1L, jdbc.queryForObject("select count(*) from $schema.permissions where name='$OWN_BENEFIT_VIEW_PERMISSION'", Long::class.java))
            val granted = jdbc.queryForList("""select r.name from $schema.roles r join $schema.role_permissions rp on rp.role_id=r.id
                join $schema.permissions p on p.id=rp.permission_id where p.name='$OWN_BENEFIT_VIEW_PERMISSION' order by r.name""", String::class.java)
            assertEquals(listOf("ADMIN", "CIUDADANO"), granted)
            assertEquals(3L, jdbc.queryForObject("select count(*) from $schema.role_permissions", Long::class.java))
        } finally { jdbc.execute("drop schema $schema cascade") }
        assertEquals(before, grants.count())
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
