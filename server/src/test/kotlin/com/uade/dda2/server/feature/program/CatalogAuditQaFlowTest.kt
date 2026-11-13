package com.uade.dda2.server.feature.program

import com.uade.dda2.server.feature.auth.entity.*
import com.uade.dda2.server.feature.auth.repository.*
import com.uade.dda2.server.feature.program.entity.*
import com.uade.dda2.server.feature.program.entity.enums.*
import com.uade.dda2.server.feature.program.repository.*
import com.uade.dda2.server.feature.enrollmentperiod.entity.*
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.log.entity.*
import com.uade.dda2.server.feature.log.repository.LogRepository
import com.uade.dda2.server.security.JwtService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.http.MediaType
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.util.UUID
import kotlin.test.*

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:catalog-audit-qa;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.enrollment-period.expiration.cron=-",
])
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class CatalogAuditQaFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var programs: ProgramRepository
    @Autowired lateinit var editions: ProgramEditionRepository
    @Autowired lateinit var benefits: ProgramBenefitRepository
    @Autowired lateinit var requirements: ProgramRequirementRepository
    @Autowired lateinit var periods: EnrollmentPeriodRepository
    @Autowired lateinit var logs: LogRepository
    @Autowired lateinit var transactions: PlatformTransactionManager
    private lateinit var actor: User
    private lateinit var token: String
    private lateinit var forbiddenToken: String
    private fun <T> tx(block: () -> T): T = TransactionTemplate(transactions).execute { block() }!!
    @BeforeEach fun setup() {
        tx {
            val p = permissions.findAll().firstOrNull { it.name == "logs:view" }
                ?: permissions.save(Permission(name = "logs:view"))
            val role = roles.save(Role(name = "QA_AUDIT_${UUID.randomUUID()}", permissions = mutableSetOf(p)))
            val denied = roles.save(Role(name = "QA_DENIED_${UUID.randomUUID()}"))
            actor = users.saveAndFlush(User(name = "QA Auditor", email = "qa-${UUID.randomUUID()}@example.com", roles = mutableSetOf(role)))
            token = jwt.createToken(actor, role)
            val other = users.saveAndFlush(User(name = "QA Reader", email = "qa-${UUID.randomUUID()}@example.com", roles = mutableSetOf(denied)))
            forbiddenToken = jwt.createToken(other, denied)
        }
    }
    private fun program(status: ProgramEditionStatus, from: LocalDate, to: LocalDate): ProgramEdition = tx {
        val p = programs.saveAndFlush(Program(name = "QA Programa ${UUID.randomUUID()}", objective = "Objetivo QA", createdBy = actor))
        editions.saveAndFlush(ProgramEdition(program = p, name = "Edición QA", startDate = from, endDate = to,
            maxCapacity = 10, currentEnrollment = 3, status = status, createdBy = actor))
    }
    private fun getBody(url: String) = mvc.perform(get(url)).andReturn().let {
        assertEquals(200, it.response.status, it.response.contentAsString)
        json.readTree(it.response.contentAsString)
    }
    @Test fun `catalogo excluye borradores suspendidas cerradas y vencidas y admite futuras activas`() {
        val today = LocalDate.now()
        val visible = listOf(program(ProgramEditionStatus.ACTIVE, today, today.plusDays(2)),
            program(ProgramEditionStatus.ACTIVE, today.plusDays(10), today.plusDays(20)))
        val hidden = listOf(program(ProgramEditionStatus.DRAFT, today, today.plusDays(2)),
            program(ProgramEditionStatus.SUSPENDED, today, today.plusDays(2)),
            program(ProgramEditionStatus.CLOSED, today, today.plusDays(2)),
            program(ProgramEditionStatus.ACTIVE, today.minusDays(3), today.minusDays(1)))
        val ids = getBody("/api/programs?size=100").get("content").toList().map { it.get("id").asString() }
        visible.forEach { assertTrue(it.program.id.toString() in ids); getBody("/api/programs/${it.program.id}") }
        hidden.forEach {
            assertFalse(it.program.id.toString() in ids)
            assertEquals(404, mvc.perform(get("/api/programs/${it.program.id}")).andReturn().response.status)
        }
    }
    @Test fun `detalle conserva nombre beneficios requisitos fechas y cupos`() {
        val today = LocalDate.now()
        val ed = program(ProgramEditionStatus.ACTIVE, today, today.plusDays(20))
        tx {
            benefits.saveAndFlush(ProgramBenefit(programEdition = ed, benefitType = ProgramBenefitType.TAX_EXEMPTION, description = "Beneficio QA"))
            requirements.saveAndFlush(ProgramRequirement(programEdition = ed, type = ProgramRequirementType.MIN_AGE, value = "18", description = "Mayor de edad"))
            periods.saveAndFlush(EnrollmentPeriod(programEdition = ed, openDate = today, closeDate = today.plusDays(5), status = EnrollmentPeriodStatus.OPEN))
        }
        val detail = getBody("/api/programs/${ed.program.id}")
        assertEquals(ed.program.name, detail.get("name").asString())
        val item = detail.get("editions").single()
        assertEquals(7, item.get("availableCapacity").intValue())
        assertEquals("Beneficio QA", item.get("benefits").single().get("description").asString())
        assertEquals("18", item.get("requirements").single().get("value").asString())
        assertEquals(today.toString(), item.get("enrollmentPeriods").single().get("openDate").asString())
        assertEquals(today.plusDays(5).toString(), item.get("enrollmentPeriods").single().get("closeDate").asString())
    }
    @Test fun `catalogo pagina vacia y parametros invalidos`() {
        assertEquals(0, getBody("/api/programs?page=100000&size=1").get("content").size())
        for (query in listOf("page=-1", "size=0", "size=101"))
            assertEquals(400, mvc.perform(get("/api/programs?$query")).andReturn().response.status)
    }
    private fun audit() = tx { logs.saveAndFlush(Log(user = actor, action = LogAction.UPDATE,
        entityType = LogEntityType.PROGRAM, entityId = UUID.randomUUID().toString())) }
    @Test fun `auditoria informa actor fecha operacion entidad y filtros`() {
        val entry = audit()
        for (url in listOf("/logs", "/logs/users/${actor.id}", "/logs/entities/PROGRAM/${entry.entityId}")) {
            val result = mvc.perform(get(url).header("Authorization", "Bearer $token")).andReturn()
            assertEquals(200, result.response.status)
            val item = json.readTree(result.response.contentAsString).first { it.get("id").longValue() == entry.id }
            assertEquals(actor.id, item.get("actor").get("id").longValue())
            assertEquals("UPDATE", item.get("action").asString())
            assertEquals("PROGRAM", item.get("entityType").asString())
            assertEquals(entry.entityId, item.get("entityId").asString())
            assertNotNull(item.get("createdAt").asString())
        }
    }
    @Test fun `auditoria requiere permiso en las tres consultas`() {
        val entry = audit()
        for (url in listOf("/logs", "/logs/users/${actor.id}", "/logs/entities/PROGRAM/${entry.entityId}")) {
            assertEquals(401, mvc.perform(get(url)).andReturn().response.status)
            assertEquals(403, mvc.perform(get(url).header("Authorization", "Bearer $forbiddenToken")).andReturn().response.status)
        }
    }
    @Test fun `auditoria no admite escrituras por API y conserva registro`() {
        val entry = audit()
        for (request in listOf(post("/logs"), put("/logs"), patch("/logs"), delete("/logs"))) {
            val result = mvc.perform(request.header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON).content("{}" )).andReturn()
            assertEquals(405, result.response.status)
        }
        assertTrue(logs.existsById(entry.id))
        assertEquals(entry.entityId, logs.findById(entry.id).orElseThrow().entityId)
    }
}
