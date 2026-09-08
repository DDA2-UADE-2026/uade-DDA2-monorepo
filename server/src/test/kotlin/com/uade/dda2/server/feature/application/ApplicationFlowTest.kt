package com.uade.dda2.server.feature.application

import com.uade.dda2.server.feature.application.dto.response.ApplicationResponse
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

@SpringBootTest(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
    "app.enrollment-period.expiration.cron=-",
])
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ApplicationFlowTest {
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
    @Autowired lateinit var transactions: PlatformTransactionManager
    @Autowired lateinit var jdbc: JdbcTemplate

    private lateinit var fixture: Fixture
    private val today get() = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))

    data class Fixture(val userId: Long, val editionId: UUID, val periodId: UUID, val token: String, val otherRoleToken: String)

    companion object {
        // The optional PostgreSQL task supplies only the disposable test database; never the app's DB_URL.
        @JvmStatic @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) {
            val postgresUrl = System.getProperty("application.test.postgres-url")
            registry.add("spring.datasource.url") { postgresUrl ?: "jdbc:h2:mem:applications;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000" }
            registry.add("spring.datasource.driver-class-name") { if (postgresUrl == null) "org.h2.Driver" else "org.postgresql.Driver" }
            registry.add("spring.jpa.properties.hibernate.dialect") { if (postgresUrl == null) "org.hibernate.dialect.H2Dialect" else "org.hibernate.dialect.PostgreSQLDialect" }
            registry.add("spring.datasource.username") { if (postgresUrl == null) "sa" else "application_test" }
            registry.add("spring.datasource.password") { System.getProperty("application.test.postgres-password", "") }
        }
    }

    @BeforeEach
    fun setup() { fixture = newFixture() }

    private fun <T : Any> tx(action: () -> T): T = TransactionTemplate(transactions).execute { action() }!!

    private fun newFixture(): Fixture = tx {
        val citizen = roles.findByNameIn(listOf("CIUDADANO")).firstOrNull() ?: roles.save(Role(name = "CIUDADANO", permissions =
            mutableSetOf(permissions.save(Permission(name = "applications:own:create")), permissions.save(Permission(name = "applications:own:view")))))
        val employee = roles.findByNameIn(listOf("ADMINISTRATIVO")).firstOrNull() ?: roles.save(Role(name = "ADMINISTRATIVO"))
        val user = users.saveAndFlush(User(name = "Test", email = "test@example.com", roles = mutableSetOf(citizen, employee)))
        val program = programs.save(Program(name = "Program ${UUID.randomUUID()}", createdBy = user))
        val edition = editions.save(ProgramEdition(program = program, name = "Edition", startDate = today.minusDays(60),
            endDate = today.plusDays(60), maxCapacity = 1, currentEnrollment = 1, status = ProgramEditionStatus.ACTIVE, createdBy = user))
        val period = periods.saveAndFlush(EnrollmentPeriod(programEdition = edition, openDate = today.minusDays(1), closeDate = today.plusDays(1), status = EnrollmentPeriodStatus.OPEN))
        Fixture(user.id!!, edition.id!!, period.id!!, jwt.createToken(user, citizen), jwt.createToken(user, employee))
    }

    private fun submit(f: Fixture = fixture, periodId: UUID = f.periodId, key: String? = null, body: String? = null, token: String? = f.token): MvcResult {
        val request = post("/api/applications").contentType(MediaType.APPLICATION_JSON)
            .content(body ?: """{"enrollmentPeriodId":"$periodId"}""")
        if (token != null) request.header("Authorization", "Bearer $token")
        if (key != null) request.header("Idempotency-Key", key)
        return mvc.perform(request).andReturn()
    }

    private fun response(result: MvcResult): ApplicationResponse = json.readValue(result.response.contentAsString, ApplicationResponse::class.java)
    private fun expect(result: MvcResult, status: Int, code: String? = null) {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (code != null) assertEquals(code, json.readTree(result.response.contentAsString).get("code").asText())
    }

    private fun newPeriod(): UUID = tx {
        periods.saveAndFlush(EnrollmentPeriod(programEdition = editions.getReferenceById(fixture.editionId),
            openDate = today, closeDate = today.plusDays(1), status = EnrollmentPeriodStatus.OPEN)).id!!
    }

    @Test
    fun `presenta con cupo agotado y registra identidad numero y auditoria`() {
        val result = submit()
        expect(result, 201)
        val application = response(result)
        assertEquals("/api/applications/${application.id}", result.response.getHeader("Location"))
        assertEquals("false", result.response.getHeader("Idempotency-Replayed"))
        assertTrue(application.applicationNumber > 0)
        assertEquals(ApplicationStatus.SUBMITTED, application.status)
        assertEquals(today, application.submittedAt.toLocalDate())
        tx {
            val stored = applications.findById(application.id).orElseThrow()
            assertEquals(fixture.userId, stored.user.id)
            assertEquals(fixture.userId, stored.registeredBy.id)
            assertEquals(fixture.editionId, stored.programEdition.id)
            assertNull(stored.requestHash)
            assertNull(stored.assignedWorker)
            assertNull(stored.resolutionReason)
            assertEquals(1, editions.findById(fixture.editionId).orElseThrow().currentEnrollment)
        }
        val log = jdbc.queryForMap("select user_id, action, new_values from logs where entity_type = 'application' and entity_id = ?", application.id.toString())
        assertEquals(fixture.userId, (log["user_id"] as Number).toLong())
        assertEquals("CREATE", log["action"])
        val snapshot = when (val raw = log["new_values"]) {
            is ByteArray -> raw.toString(Charsets.UTF_8)
            else -> raw.toString()
        }
        assertTrue(snapshot.contains("applicationNumber"))
        assertEquals(fixture.userId, application.userId)
        assertEquals(fixture.userId, application.registeredByUserId)
    }

    @Test
    fun `permiso depende del rol activo y token de seleccion no sirve`() {
        expect(submit(token = null), 401)
        expect(submit(token = "invalid"), 401)
        expect(submit(token = fixture.otherRoleToken), 403)
        val selection = tx { jwt.createRoleSelectionToken(users.findById(fixture.userId).orElseThrow()) }
        expect(submit(token = selection), 401)
        expect(mvc.perform(get("/api/applications").header("Authorization", "Bearer ${fixture.otherRoleToken}")).andReturn(), 403)
    }

    @Test
    fun `usuario inactivo o rol retirado no puede presentar ni consultar`() {
        tx { users.findById(fixture.userId).orElseThrow().active = false }
        expect(submit(), 401)
        expect(mvc.perform(get("/api/applications").header("Authorization", "Bearer ${fixture.token}")).andReturn(), 401)
        tx { users.findById(fixture.userId).orElseThrow().apply { active = true; roles.clear() } }
        expect(submit(), 403)
    }

    @Test
    fun `request rechaza campos internos y payloads invalidos`() {
        for (field in listOf("userId", "registeredByUserId", "registeredBy", "status", "applicationNumber", "submittedAt", "programEditionId", "assignedWorkerUserId", "idempotencyKey")) {
            expect(submit(body = """{"enrollmentPeriodId":"${fixture.periodId}","$field":"injected"}"""), 400)
        }
        for (body in listOf("{}", """{"enrollmentPeriodId":null}""", """{"enrollmentPeriodId":"invalid"}""")) expect(submit(body = body), 400)
        expect(submit(periodId = UUID.randomUUID()), 404, "APPLICATION_ENROLLMENT_PERIOD_NOT_FOUND")
        expect(submit(key = ""), 400)
        expect(submit(key = "has spaces"), 400)
        expect(submit(key = "x".repeat(129)), 400)
    }

    @ParameterizedTest @EnumSource(EnrollmentPeriodStatus::class, names = ["SCHEDULED", "SUSPENDED", "CLOSED"])
    fun `convocatorias no abiertas rechazan`(status: EnrollmentPeriodStatus) {
        tx { periods.findById(fixture.periodId).orElseThrow().status = status }
        expect(submit(), 409, "APPLICATION_ENROLLMENT_PERIOD_NOT_OPEN")
    }

    @Test
    fun `valida fechas inclusivas`() {
        tx { periods.findById(fixture.periodId).orElseThrow().apply { openDate = today.plusDays(1); closeDate = today.plusDays(2) } }
        expect(submit(), 409, "APPLICATION_OUTSIDE_ENROLLMENT_PERIOD")
        tx { periods.findById(fixture.periodId).orElseThrow().apply { openDate = today.minusDays(2); closeDate = today.minusDays(1) } }
        expect(submit(), 409, "APPLICATION_OUTSIDE_ENROLLMENT_PERIOD")
        tx { periods.findById(fixture.periodId).orElseThrow().apply { openDate = today; closeDate = today } }
        expect(submit(), 201)
    }

    @ParameterizedTest @EnumSource(ProgramEditionStatus::class, names = ["DRAFT", "SUSPENDED", "CLOSED"])
    fun `edicion no activa rechaza`(status: ProgramEditionStatus) {
        tx { editions.findById(fixture.editionId).orElseThrow().status = status }
        expect(submit(), 409, "APPLICATION_PROGRAM_EDITION_NOT_ACTIVE")
    }

    @ParameterizedTest @EnumSource(ApplicationStatus::class)
    fun `una sola solicitud por convocatoria en cualquier estado`(status: ApplicationStatus) {
        val existing = response(submit())
        tx { applications.findById(existing.id).orElseThrow().status = status }
        expect(submit(), 409, "APPLICATION_ALREADY_EXISTS_FOR_PERIOD")
    }

    @ParameterizedTest @EnumSource(ApplicationStatus::class)
    fun `otra convocatoria solo admite anteriores rechazadas o cerradas`(status: ApplicationStatus) {
        val existing = response(submit())
        tx { applications.findById(existing.id).orElseThrow().status = status }
        val next = submit(periodId = newPeriod())
        if (status in setOf(ApplicationStatus.REJECTED, ApplicationStatus.CLOSED)) expect(next, 201)
        else expect(next, 409, "APPLICATION_ALREADY_EXISTS_FOR_EDITION")
    }

    @Test
    fun `idempotencia devuelve original aun cerrada la convocatoria sin repetir auditoria`() {
        val first = submit(key = "request-1")
        expect(first, 201)
        tx { periods.findById(fixture.periodId).orElseThrow().status = EnrollmentPeriodStatus.CLOSED }
        val replay = submit(key = "request-1")
        expect(replay, 200)
        assertEquals("true", replay.response.getHeader("Idempotency-Replayed"))
        assertEquals(response(first), response(replay))
        assertEquals(1, jdbc.queryForObject("select count(*) from logs where entity_type = 'application' and entity_id = ?", Int::class.java, response(first).id.toString()))
        tx { assertEquals(64, applications.findById(response(first).id).orElseThrow().requestHash!!.length) }
        expect(submit(key = "request-1", periodId = UUID.randomUUID()), 409, "APPLICATION_IDEMPOTENCY_CONFLICT")
    }

    @Test
    fun `misma clave es independiente por usuario`() {
        val first = submit(key = "same-key")
        val second = submit(newFixture(), key = "same-key")
        expect(first, 201); expect(second, 201)
        assertNotEquals(response(first).id, response(second).id)
    }

    @Test
    fun `consultas filtran propiedad y paginan sin filtrar por parametros de usuario`() {
        val own = response(submit())
        val other = response(submit(newFixture()))
        expect(mvc.perform(get("/api/applications/${own.id}").header("Authorization", "Bearer ${fixture.token}")).andReturn(), 200)
        expect(mvc.perform(get("/api/applications/${other.id}").header("Authorization", "Bearer ${fixture.token}")).andReturn(), 404)
        val listing = mvc.perform(get("/api/applications").param("userId", "999").param("size", "1")
            .header("Authorization", "Bearer ${fixture.token}")).andReturn()
        expect(listing, 200)
        val body = json.readTree(listing.response.contentAsString)
        assertEquals(1, body.get("totalElements").asInt())
        assertEquals(own.id.toString(), body.get("content").get(0).get("id").asText())
        for ((key, value) in listOf("page" to "-1", "size" to "0", "size" to "101")) {
            expect(mvc.perform(get("/api/applications").param(key, value).header("Authorization", "Bearer ${fixture.token}")).andReturn(), 400)
        }
    }

    private fun concurrent(count: Int, action: (Int) -> MvcResult): List<MvcResult> {
        val ready = CountDownLatch(count)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(count)
        try {
            val futures = (0 until count).map { index -> executor.submit<MvcResult> {
                ready.countDown(); check(start.await(10, TimeUnit.SECONDS)); action(index)
            } }
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown()
            return futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally { executor.shutdownNow() }
    }

    @Test
    fun `reintentos concurrentes crean exactamente una solicitud`() {
        val results = concurrent(4) { submit(key = "parallel-retry") }
        assertEquals(listOf(200, 200, 200, 201), results.map { it.response.status }.sorted())
        assertEquals(1, results.map { response(it).id }.toSet().size)
    }

    @Test
    fun `pedidos concurrentes sin clave o con claves distintas no duplican`() {
        val results = concurrent(4) { submit(key = if (it % 2 == 0) null else "key-$it") }
        assertEquals(listOf(201, 409, 409, 409), results.map { it.response.status }.sorted())
    }

    @Test
    fun `dos convocatorias concurrentes de una edicion no eluden la regla`() {
        val nextPeriod = newPeriod()
        val results = concurrent(2) { submit(periodId = if (it == 0) fixture.periodId else nextPeriod) }
        assertEquals(listOf(201, 409), results.map { it.response.status }.sorted())
    }

    @Test
    fun `secuencia global es unica en presentaciones concurrentes de distintos usuarios`() {
        val fixtures = List(6) { newFixture() }
        val results = concurrent(fixtures.size) { submit(fixtures[it]) }
        results.forEach { expect(it, 201) }
        val numbers = results.map { response(it).applicationNumber }
        assertEquals(fixtures.size, numbers.toSet().size)
        assertTrue(numbers.all { it > 0 })
        assertTrue(response(submit()).applicationNumber > numbers.max())
    }

    @Test
    fun `base respalda unicidad funcional y de idempotencia`() {
        val result = response(submit(key = "db-key"))
        val nextPeriod = newPeriod()
        for ((periodId, key) in listOf(fixture.periodId to "another-key", nextPeriod to "db-key")) {
            assertFails {
                tx { applications.saveAndFlush(Application(user = users.getReferenceById(fixture.userId),
                    registeredBy = users.getReferenceById(fixture.userId),
                    programEdition = editions.getReferenceById(fixture.editionId), enrollmentPeriod = periods.getReferenceById(periodId),
                    submittedAt = LocalDateTime.now(), idempotencyKey = key, requestHash = "a".repeat(64))) }
            }
        }
        assertTrue(applications.existsById(result.id))
    }

    @Test
    fun `un fallo de auditoria revierte la solicitud y permite reintentar la clave`() {
        jdbc.execute("alter table logs add constraint ck_application_test_audit_failure check (entity_type <> 'application' or user_id <> ${fixture.userId})")
        try {
            expect(submit(key = "retry-after-failure"), 409)
            assertFalse(applications.existsByUserIdAndEnrollmentPeriodId(fixture.userId, fixture.periodId))
            assertNull(applications.findByUserIdAndIdempotencyKey(fixture.userId, "retry-after-failure"))
        } finally {
            jdbc.execute("alter table logs drop constraint ck_application_test_audit_failure")
        }
        expect(submit(key = "retry-after-failure"), 201)
    }

    @Test
    fun `cors permite usar idempotencia desde el frontend sin cambiarlo`() {
        val result = mvc.perform(options("/api/applications")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "authorization,content-type,idempotency-key")).andReturn()
        expect(result, 200)
        assertTrue(result.response.getHeader("Access-Control-Allow-Headers")!!.lowercase().contains("idempotency-key"))
    }

    data class Administrator(val id: Long, val token: String, val citizenToken: String)

    private fun administrator(): Administrator = tx {
        val role = roles.findByNameIn(listOf("ASISTENCIA")).firstOrNull() ?: roles.save(Role(name = "ASISTENCIA", permissions =
            mutableSetOf("applications:management:create", "users:delete").map { name ->
                permissions.findAll().firstOrNull { it.name == name } ?: permissions.save(Permission(name = name))
            }.toMutableSet()))
        val citizen = roles.findByNameIn(listOf("CIUDADANO")).single()
        val user = users.saveAndFlush(User(name = "Administrativo", email = "administrativo@example.com", roles = mutableSetOf(role, citizen)))
        Administrator(user.id!!, jwt.createToken(user, role), jwt.createToken(user, citizen))
    }

    private fun assisted(admin: Administrator, userId: Long = fixture.userId, periodId: UUID = fixture.periodId,
                         key: String? = null, body: String? = null, token: String? = admin.token): MvcResult {
        val request = post("/api/admin/applications").contentType(MediaType.APPLICATION_JSON)
            .content(body ?: """{"userId":$userId,"enrollmentPeriodId":"$periodId"}""")
        if (token != null) request.header("Authorization", "Bearer $token")
        if (key != null) request.header("Idempotency-Key", key)
        return mvc.perform(request).andReturn()
    }

    @Test
    fun `asistida guarda al titular y registrante por separado sin consumir cupo ni asignar trabajador`() {
        val admin = administrator()
        val result = assisted(admin)
        expect(result, 201)
        val application = response(result)
        assertEquals(fixture.userId, application.userId)
        assertEquals(admin.id, application.registeredByUserId)
        assertEquals("false", result.response.getHeader("Idempotency-Replayed"))
        tx {
            val stored = applications.findById(application.id).orElseThrow()
            assertEquals(fixture.userId, stored.user.id)
            assertEquals(admin.id, stored.registeredBy.id)
            assertNull(stored.assignedWorker)
            assertEquals(ApplicationStatus.SUBMITTED, stored.status)
            assertEquals(1, editions.findById(fixture.editionId).orElseThrow().currentEnrollment)
        }
        val log = jdbc.queryForMap("select user_id, new_values from logs where entity_type = 'application' and entity_id = ?", application.id.toString())
        assertEquals(admin.id, (log["user_id"] as Number).toLong())
        val raw = log["new_values"]
        val parsed = json.readTree(if (raw is ByteArray) raw.toString(Charsets.UTF_8) else raw.toString())
        // The H2 JSON column can expose the stored JSON string with one extra quoting layer.
        val snapshot = if (parsed.isTextual) json.readTree(parsed.asText()) else parsed
        assertEquals(fixture.userId, snapshot.get("userId").asLong())
        assertEquals(admin.id, snapshot.get("registeredByUserId").asLong())
        assertFalse(result.response.contentAsString.contains("email"))
    }

    @Test
    fun `asistencia admite usuario existente sin snapshot identidad externa o credenciales locales`() {
        val admin = administrator()
        val applicant = tx { users.saveAndFlush(User(name = "Titular", email = "titular@example.com")).id!! }
        val result = assisted(admin, userId = applicant)
        expect(result, 201)
        assertEquals(applicant, response(result).userId)
    }

    @Test
    fun `permiso asistido depende del rol activo y no habilita presentaciones propias`() {
        val admin = administrator()
        expect(assisted(admin, token = null), 401)
        expect(assisted(admin, token = "invalid"), 401)
        expect(assisted(admin, token = admin.citizenToken), 403)
        expect(assisted(admin, token = fixture.token), 403)
        val selection = tx { jwt.createRoleSelectionToken(users.findById(admin.id).orElseThrow()) }
        expect(assisted(admin, token = selection), 401)
        expect(assisted(admin, userId = admin.id), 403, "APPLICATION_ASSISTED_SELF_NOT_ALLOWED")
        expect(submit(token = admin.token), 403)
        expect(assisted(admin), 201)
    }

    @Test
    fun `administrativo inactivo o con rol retirado no registra ni recupera por idempotencia`() {
        val admin = administrator()
        expect(assisted(admin, key = "assisted-auth"), 201)
        tx { users.findById(admin.id).orElseThrow().active = false }
        expect(assisted(admin, key = "assisted-auth"), 401)
        tx { users.findById(admin.id).orElseThrow().apply { active = true; roles.clear() } }
        expect(assisted(admin, key = "assisted-auth"), 403)
    }

    @Test
    fun `asistida valida titular payload y campos que solo decide el servidor`() {
        val admin = administrator()
        expect(assisted(admin, userId = Long.MAX_VALUE), 404, "APPLICATION_USER_NOT_FOUND")
        expect(assisted(admin, periodId = UUID.randomUUID()), 404, "APPLICATION_ENROLLMENT_PERIOD_NOT_FOUND")
        for (body in listOf("{}", """{"userId":${fixture.userId}}""", """{"enrollmentPeriodId":"${fixture.periodId}"}""",
            """{"userId":0,"enrollmentPeriodId":"${fixture.periodId}"}""", """{"userId":null,"enrollmentPeriodId":"${fixture.periodId}"}""")) {
            expect(assisted(admin, body = body), 400)
        }
        for (field in listOf("registeredByUserId", "registeredBy", "registradoPor", "citizenId", "status", "applicationNumber", "assignedWorkerUserId")) {
            expect(assisted(admin, body = """{"userId":${fixture.userId},"enrollmentPeriodId":"${fixture.periodId}","$field":123}"""), 400)
        }
        expect(assisted(admin, key = "with spaces"), 400, "APPLICATION_INVALID_IDEMPOTENCY_KEY")
        assertFalse(applications.existsByUserIdAndEnrollmentPeriodId(fixture.userId, fixture.periodId))
    }

    @Test
    fun `asistida conserva validacion de convocatoria fechas y edicion`() {
        val admin = administrator()
        tx { periods.findById(fixture.periodId).orElseThrow().status = EnrollmentPeriodStatus.CLOSED }
        expect(assisted(admin), 409, "APPLICATION_ENROLLMENT_PERIOD_NOT_OPEN")
        tx { periods.findById(fixture.periodId).orElseThrow().apply { status = EnrollmentPeriodStatus.OPEN; closeDate = today.minusDays(1) } }
        expect(assisted(admin), 409, "APPLICATION_OUTSIDE_ENROLLMENT_PERIOD")
        tx {
            periods.findById(fixture.periodId).orElseThrow().closeDate = today
            editions.findById(fixture.editionId).orElseThrow().status = ProgramEditionStatus.SUSPENDED
        }
        expect(assisted(admin), 409, "APPLICATION_PROGRAM_EDITION_NOT_ACTIVE")
    }

    @Test
    fun `titular consulta asistida pero registrante no la recibe como propia`() {
        val admin = administrator()
        val application = response(assisted(admin))
        val detail = mvc.perform(get("/api/applications/${application.id}").header("Authorization", "Bearer ${fixture.token}")).andReturn()
        expect(detail, 200)
        assertEquals(application, response(detail))
        val listing = mvc.perform(get("/api/applications").header("Authorization", "Bearer ${fixture.token}")).andReturn()
        expect(listing, 200)
        assertEquals(application.id.toString(), json.readTree(listing.response.contentAsString).get("content").get(0).get("id").asText())
        expect(mvc.perform(get("/api/applications/${application.id}").header("Authorization", "Bearer ${admin.citizenToken}")).andReturn(), 404)
        val adminListing = mvc.perform(get("/api/applications").header("Authorization", "Bearer ${admin.citizenToken}")).andReturn()
        expect(adminListing, 200)
        assertEquals(0, json.readTree(adminListing.response.contentAsString).get("totalElements").asInt())
        expect(mvc.perform(get("/api/applications/${application.id}").header("Authorization", "Bearer ${admin.token}")).andReturn(), 403)
    }

    @Test
    fun `duplicados comparten titular entre presentacion propia y asistida sin cambiar regla de rechazo`() {
        val admin = administrator()
        val original = response(assisted(admin))
        expect(submit(), 409, "APPLICATION_ALREADY_EXISTS_FOR_PERIOD")
        val nextPeriod = newPeriod()
        expect(assisted(admin, periodId = nextPeriod), 409, "APPLICATION_ALREADY_EXISTS_FOR_EDITION")
        tx { applications.findById(original.id).orElseThrow().status = ApplicationStatus.REJECTED }
        expect(assisted(admin), 409, "APPLICATION_ALREADY_EXISTS_FOR_PERIOD")
        expect(submit(periodId = nextPeriod), 201)
        expect(assisted(admin, periodId = nextPeriod), 409, "APPLICATION_ALREADY_EXISTS_FOR_PERIOD")
    }

    @Test
    fun `reintento asistido comparte clave por titular y conserva registrante aunque cambie el actor o cierre convocatoria`() {
        val firstAdmin = administrator()
        val secondAdmin = administrator()
        val first = assisted(firstAdmin, key = "shared-assistance")
        expect(first, 201)
        tx { periods.findById(fixture.periodId).orElseThrow().status = EnrollmentPeriodStatus.CLOSED }
        for (replay in listOf(assisted(secondAdmin, key = "shared-assistance"), submit(key = "shared-assistance"))) {
            expect(replay, 200)
            assertEquals("true", replay.response.getHeader("Idempotency-Replayed"))
            assertEquals(response(first), response(replay))
        }
        assertEquals(1, jdbc.queryForObject("select count(*) from logs where entity_type = 'application' and entity_id = ?", Int::class.java, response(first).id.toString()))
        expect(assisted(secondAdmin, key = "shared-assistance", periodId = UUID.randomUUID()), 409, "APPLICATION_IDEMPOTENCY_CONFLICT")
    }

    @Test
    fun `mismo administrativo puede usar misma clave para titulares diferentes`() {
        val admin = administrator()
        val other = newFixture()
        val first = assisted(admin, key = "per-applicant")
        val second = assisted(admin, userId = other.userId, key = "per-applicant")
        expect(first, 201); expect(second, 201)
        assertNotEquals(response(first).id, response(second).id)
    }

    @Test
    fun `dos administrativos concurrentes reintentan sin duplicar ni cambiar al registrante original`() {
        val admins = List(2) { administrator() }
        val results = concurrent(2) { assisted(admins[it], key = "concurrent-assistance") }
        assertEquals(listOf(200, 201), results.map { it.response.status }.sorted())
        assertEquals(response(results[0]), response(results[1]))
        val creator = admins[results.indexOfFirst { it.response.status == 201 }].id
        assertEquals(creator, response(results[0]).registeredByUserId)
    }

    @Test
    fun `presentacion propia y asistida concurrentes no duplican`() {
        val admin = administrator()
        val results = concurrent(2) { if (it == 0) submit() else assisted(admin) }
        assertEquals(listOf(201, 409), results.map { it.response.status }.sorted())
        tx {
            val stored = applications.findById(response(results.single { it.response.status == 201 }).id).orElseThrow()
            assertEquals(if (results[0].response.status == 201) fixture.userId else admin.id, stored.registeredBy.id)
        }
    }

    @Test
    fun `no permite eliminar un usuario referenciado solo como registrante`() {
        val admin = administrator()
        val manager = administrator()
        expect(assisted(admin), 201)
        expect(mvc.perform(delete("/users/${admin.id}").header("Authorization", "Bearer ${manager.token}")).andReturn(),
            409, "USER_HAS_APPLICATION_REFERENCES")
        assertTrue(users.existsById(admin.id))
    }

    @Test
    fun `fallo de auditoria asistida revierte y libera clave del titular`() {
        val admin = administrator()
        jdbc.execute("alter table logs add constraint ck_assisted_test_audit_failure check (entity_type <> 'application' or user_id <> ${admin.id})")
        try {
            expect(assisted(admin, key = "assisted-rollback"), 409)
            assertFalse(applications.existsByUserIdAndEnrollmentPeriodId(fixture.userId, fixture.periodId))
            assertNull(applications.findByUserIdAndIdempotencyKey(fixture.userId, "assisted-rollback"))
        } finally {
            jdbc.execute("alter table logs drop constraint ck_assisted_test_audit_failure")
        }
        expect(assisted(admin, key = "assisted-rollback"), 201)
    }
}
