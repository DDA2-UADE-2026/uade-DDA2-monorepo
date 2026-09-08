package com.uade.dda2.server.feature.application

import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.repository.ApplicationDocumentRepository
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.document.repository.DocumentRepository
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:application-documents;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
    "app.enrollment-period.expiration.cron=-",
])
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ApplicationDocumentFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var programs: ProgramRepository
    @Autowired lateinit var editions: ProgramEditionRepository
    @Autowired lateinit var periods: EnrollmentPeriodRepository
    @Autowired lateinit var requirements: ProgramDocumentRequirementRepository
    @Autowired lateinit var applications: ApplicationRepository
    @Autowired lateinit var applicationDocuments: ApplicationDocumentRepository
    @Autowired lateinit var documents: DocumentRepository
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var transactions: PlatformTransactionManager

    private lateinit var f: Fixture

    data class Fixture(
        val ownerId: Long,
        val applicationId: UUID,
        val editionId: UUID,
        val programId: UUID,
        val requiredId: UUID,
        val optionalId: UUID,
        val ownerToken: String,
        val otherToken: String,
        val noPermissionToken: String,
        val adminId: Long,
        val adminToken: String,
    )

    @BeforeEach
    fun setup() { f = fixture() }

    private fun <T : Any> tx(action: () -> T): T = TransactionTemplate(transactions).execute { action() }!!

    private fun role(name: String, permissionNames: Set<String>): Role {
        val current = roles.findByNameIn(listOf(name)).firstOrNull() ?: Role(name = name)
        val existing = permissions.findAll().associateBy { it.name }.toMutableMap()
        permissionNames.forEach { permissionName ->
            current.permissions.add(existing[permissionName] ?: permissions.save(Permission(name = permissionName)).also { existing[permissionName] = it })
        }
        return roles.save(current)
    }

    private fun fixture(): Fixture = tx {
        val citizen = role("CIUDADANO_DOCUMENTOS", setOf(
            "applications:own:view", "applications:own:documents:view", "applications:own:documents:manage",
        ))
        val admin = role("ADMIN_DOCUMENTOS", setOf(
            "programs:management:view", "programs:management:create", "programs:management:edit",
            "applications:management:documents:view", "applications:management:documents:review", "users:delete",
        ))
        val empty = role("SIN_PERMISOS_DOCUMENTOS", emptySet())
        val owner = users.save(User(name = "Titular ${UUID.randomUUID()}", email = "owner-${UUID.randomUUID()}@example.com", roles = mutableSetOf(citizen, empty)))
        val other = users.save(User(name = "Otro", email = "other-${UUID.randomUUID()}@example.com", roles = mutableSetOf(citizen)))
        val reviewer = users.save(User(name = "Revisor", email = "reviewer-${UUID.randomUUID()}@example.com", roles = mutableSetOf(admin)))
        val program = programs.save(Program(name = "Documentos ${UUID.randomUUID()}", createdBy = owner))
        val today = LocalDate.now()
        val edition = editions.save(ProgramEdition(program = program, name = "Edición documental", startDate = today.minusDays(5),
            endDate = today.plusDays(30), maxCapacity = 100, status = ProgramEditionStatus.ACTIVE, createdBy = owner))
        val period = periods.save(EnrollmentPeriod(programEdition = edition, openDate = today.minusDays(1), closeDate = today.plusDays(10), status = EnrollmentPeriodStatus.OPEN))
        val required = requirements.save(ProgramDocumentRequirement(programEdition = edition, code = "DNI", name = "Documento de identidad", required = true))
        val optional = requirements.save(ProgramDocumentRequirement(programEdition = edition, code = "EXTRA", name = "Documento opcional", required = false))
        val application = applications.saveAndFlush(Application(user = owner, registeredBy = owner, programEdition = edition,
            enrollmentPeriod = period, submittedAt = LocalDateTime.now()))
        Fixture(owner.id!!, application.id!!, edition.id!!, program.id!!, required.id!!, optional.id!!,
            jwt.createToken(owner, citizen), jwt.createToken(other, citizen), jwt.createToken(owner, empty), reviewer.id!!, jwt.createToken(reviewer, admin))
    }

    private fun auth(token: String) = "Bearer $token"
    private fun expect(result: MvcResult, status: Int, code: String? = null): JsonNode? {
        assertEquals(status, result.response.status, result.response.contentAsString)
        if (result.response.contentAsString.isBlank()) return null
        val body = json.readTree(result.response.contentAsString)
        if (code != null) assertEquals(code, body.get("code").asString())
        return body
    }

    private fun file(name: String, type: String, bytes: ByteArray) = MockMultipartFile("file", name, type, bytes)
    private fun pdf(suffix: String = "content") = "%PDF-1.7\n$suffix".toByteArray()
    private fun putDocument(requirementId: UUID = f.requiredId, upload: MockMultipartFile = file("dni.pdf", "application/pdf", pdf()), token: String = f.ownerToken): MvcResult =
        mvc.perform(multipart("/api/applications/${f.applicationId}/documents/$requirementId").file(upload)
            .with { it.method = "PUT"; it }.header("Authorization", auth(token))).andReturn()

    @Test
    fun `pendientes incluye obligatorios faltantes u observados y excluye opcionales y entregas pendientes`() {
        val before = expect(mvc.perform(get("/api/applications/${f.applicationId}").header("Authorization", auth(f.ownerToken))).andReturn(), 200)!!
        assertEquals("MISSING", before.get("pendingDocuments").get(0).get("reason").asString())
        assertEquals(f.requiredId.toString(), before.get("pendingDocuments").get(0).get("requirementId").asString())
        assertEquals(1, before.get("pendingDocuments").size())

        expect(putDocument(), 201)
        val detail = expect(mvc.perform(get("/api/applications/${f.applicationId}").header("Authorization", auth(f.ownerToken))).andReturn(), 200)!!
        assertEquals(0, detail.get("pendingDocuments").size())
        val listing = expect(mvc.perform(get("/api/applications").header("Authorization", auth(f.ownerToken))).andReturn(), 200)!!
        assertEquals(0, listing.get("content").get(0).get("pendingDocuments").size())
    }

    @Test
    fun `solicitud conserva nombres y catalogo completo aunque la edicion este cerrada`() {
        tx { editions.findById(f.editionId).orElseThrow().status = ProgramEditionStatus.CLOSED }
        val detail = expect(mvc.perform(get("/api/applications/${f.applicationId}")
            .header("Authorization", auth(f.ownerToken))).andReturn(), 200)!!
        assertEquals(f.programId.toString(), detail.get("programId").asString())
        assertEquals("Edición documental", detail.get("programEditionName").asString())
        assertTrue(detail.get("programName").asString().startsWith("Documentos "))
        assertEquals(2, detail.get("documentRequirements").size())
        assertTrue(detail.get("documentRequirements").any { it.get("id").asString() == f.optionalId.toString() })
        val listing = expect(mvc.perform(get("/api/applications").header("Authorization", auth(f.ownerToken))).andReturn(), 200)!!
        assertEquals(detail.get("documentRequirements"), listing.get("content").get(0).get("documentRequirements"))
        assertEquals(detail.get("programName"), listing.get("content").get(0).get("programName"))
    }

    @Test
    fun `catalogo CRUD normaliza codigo se publica y se bloquea al existir solicitudes`() {
        val today = LocalDate.now()
        val unlocked = tx {
            editions.saveAndFlush(ProgramEdition(program = programs.getReferenceById(f.programId), name = "Sin solicitudes ${UUID.randomUUID()}",
                startDate = today, endDate = today.plusDays(15), maxCapacity = 10, status = ProgramEditionStatus.ACTIVE,
                createdBy = users.getReferenceById(f.ownerId))).id!!
        }
        val created = expect(mvc.perform(post("/api/admin/program-editions/$unlocked/document-requirements")
            .contentType(MediaType.APPLICATION_JSON).content("""{"code":"income_receipt","name":"Recibo","description":"Último recibo","required":true}""")
            .header("Authorization", auth(f.adminToken))).andReturn(), 201)!!
        val id = UUID.fromString(created.get("id").asString())
        assertEquals("INCOME_RECEIPT", created.get("code").asString())
        expect(mvc.perform(post("/api/admin/program-editions/$unlocked/document-requirements")
            .contentType(MediaType.APPLICATION_JSON).content("""{"code":"INCOME_RECEIPT","name":"Duplicado","required":true}""")
            .header("Authorization", auth(f.adminToken))).andReturn(), 409, "PROGRAM_DOCUMENT_REQUIREMENT_CODE_EXISTS")
        val updated = expect(mvc.perform(put("/api/admin/program-editions/$unlocked/document-requirements/$id")
            .contentType(MediaType.APPLICATION_JSON).content("""{"code":"receipt","name":"Recibo actualizado","required":false}""")
            .header("Authorization", auth(f.adminToken))).andReturn(), 200)!!
        assertEquals("RECEIPT", updated.get("code").asString())

        val available = expect(mvc.perform(get("/api/programs/${f.programId}").header("Authorization", auth(f.ownerToken))).andReturn(), 200)!!
        assertTrue(available.toString().contains("documentRequirements"))
        assertTrue(available.toString().contains("RECEIPT"))

        expect(mvc.perform(put("/api/admin/program-editions/${f.editionId}/document-requirements/${f.requiredId}")
            .contentType(MediaType.APPLICATION_JSON).content("""{"code":"DNI","name":"Otro nombre","required":true}""")
            .header("Authorization", auth(f.adminToken))).andReturn(), 409, "PROGRAM_DOCUMENT_REQUIREMENTS_LOCKED")
        expect(mvc.perform(delete("/api/admin/program-editions/${f.editionId}/document-requirements/${f.requiredId}")
            .header("Authorization", auth(f.adminToken))).andReturn(), 409, "PROGRAM_DOCUMENT_REQUIREMENTS_LOCKED")
    }

    @Test
    fun `listados publican objetivo periodos configurados y actividad del programa`() {
        val objective = "Objetivo ciudadano ${UUID.randomUUID()}"
        val inactiveProgramId = tx {
            programs.findById(f.programId).orElseThrow().objective = objective
            val owner = users.getReferenceById(f.ownerId)
            val edition = editions.getReferenceById(f.editionId)
            val futurePeriod = periods.saveAndFlush(
                EnrollmentPeriod(
                    programEdition = edition,
                    openDate = LocalDate.now().plusDays(11),
                    closeDate = LocalDate.now().plusDays(15),
                    status = EnrollmentPeriodStatus.SCHEDULED,
                ),
            )
            assertNotNull(futurePeriod.id)
            programs.saveAndFlush(
                Program(
                    name = "Programa inactivo ${UUID.randomUUID()}",
                    objective = "Sin convocatorias activas",
                    createdBy = owner,
                ),
            ).id!!
        }

        val publicList = expect(
            mvc.perform(get("/api/programs?page=0&size=100").header("Authorization", auth(f.ownerToken))).andReturn(),
            200,
        )!!
        val publicProgram = publicList.get("content").first { it.get("id").asString() == f.programId.toString() }
        assertEquals(objective, publicProgram.get("objective").asString())

        val publicDetail = expect(
            mvc.perform(get("/api/programs/${f.programId}").header("Authorization", auth(f.ownerToken))).andReturn(),
            200,
        )!!
        val edition = publicDetail.get("editions").first { it.get("id").asString() == f.editionId.toString() }
        val enrollmentPeriods = edition.get("enrollmentPeriods")
        val statuses = (0 until enrollmentPeriods.size())
            .map { index -> enrollmentPeriods.get(index).get("status").asString() }
            .toSet()
        assertEquals(setOf("OPEN", "SCHEDULED"), statuses)

        val adminList = expect(
            mvc.perform(get("/api/admin/programs?page=0&size=100").header("Authorization", auth(f.adminToken))).andReturn(),
            200,
        )!!
        val listedPrograms = adminList.get("content")
        val activeProgram = listedPrograms.first { it.get("id").asString() == f.programId.toString() }
        val inactiveProgram = listedPrograms.first { it.get("id").asString() == inactiveProgramId.toString() }
        assertTrue(activeProgram.get("active").asBoolean())
        assertFalse(inactiveProgram.get("active").asBoolean())
    }

    @Test
    fun `carga reemplaza elimina y descarga inline PDF JPEG y PNG sin archivos huerfanos`() {
        val variants = listOf(
            file("identidad.pdf", "application/pdf", pdf()),
            file("identidad.jpg", "image/jpeg", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2)),
            file("identidad.png", "image/png", byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1)),
        )
        var linkId: UUID? = null
        var previousDocumentId: UUID? = null
        variants.forEachIndexed { index, upload ->
            val body = expect(putDocument(upload = upload), if (index == 0) 201 else 200)!!
            val currentLinkId = UUID.fromString(body.get("id").asString())
            val currentDocumentId = UUID.fromString(body.get("documentId").asString())
            if (linkId == null) linkId = currentLinkId else assertEquals(linkId, currentLinkId)
            previousDocumentId?.let { assertFalse(documents.existsById(it)) }
            previousDocumentId = currentDocumentId
            assertEquals(1, applicationDocuments.findMetadataByApplicationId(f.applicationId).size)
            assertEquals(0, orphanDocumentCount())
            assertEquals("PENDING", body.get("status").asString())
        }
        val download = mvc.perform(get("/api/applications/${f.applicationId}/documents/$linkId/content")
            .header("Authorization", auth(f.ownerToken))).andReturn()
        assertEquals(200, download.response.status)
        assertEquals("image/png", download.response.contentType)
        assertTrue(download.response.getHeader("Content-Disposition")!!.startsWith("inline"))
        assertEquals("private, no-store", download.response.getHeader("Cache-Control"))
        assertContentEquals(variants.last().bytes, download.response.contentAsByteArray)

        val beforeUpdate = tx { applications.findById(f.applicationId).orElseThrow().updatedAt }
        expect(mvc.perform(delete("/api/applications/${f.applicationId}/documents/$linkId")
            .header("Authorization", auth(f.ownerToken))).andReturn(), 204)
        assertEquals(0, applicationDocuments.findMetadataByApplicationId(f.applicationId).size)
        assertEquals(0, orphanDocumentCount())
        val afterUpdate = tx { applications.findById(f.applicationId).orElseThrow().updatedAt }
        assertTrue(afterUpdate >= beforeUpdate)
    }

    @Test
    fun `rechaza archivos vacios grandes y con extension MIME o firma inconsistentes`() {
        expect(putDocument(upload = file("empty.pdf", "application/pdf", byteArrayOf())), 400, "APPLICATION_DOCUMENT_EMPTY_FILE")
        expect(putDocument(upload = file("fake.png", "image/png", pdf())), 400, "APPLICATION_DOCUMENT_INVALID_FILE_TYPE")
        expect(putDocument(upload = file("wrong.pdf", "image/png", pdf())), 400, "APPLICATION_DOCUMENT_INVALID_FILE_TYPE")
        expect(putDocument(upload = file("file.exe", "application/pdf", pdf())), 400, "APPLICATION_DOCUMENT_INVALID_FILE_TYPE")
        expect(putDocument(upload = file("large.pdf", "application/pdf", ByteArray(10 * 1024 * 1024 + 1).also { pdf().copyInto(it) })),
            413, "APPLICATION_DOCUMENT_FILE_TOO_LARGE")
        assertEquals(0, applicationDocuments.findMetadataByApplicationId(f.applicationId).size)
        assertEquals(0, orphanDocumentCount())
    }

    @Test
    fun `revision observada aparece pendiente y reemplazar reinicia revision`() {
        val uploaded = expect(putDocument(), 201)!!
        val linkId = uploaded.get("id").asString()
        expect(mvc.perform(patch("/api/admin/applications/${f.applicationId}/documents/$linkId/review")
            .contentType(MediaType.APPLICATION_JSON).content("""{"status":"OBSERVED"}""")
            .header("Authorization", auth(f.adminToken))).andReturn(), 409, "APPLICATION_DOCUMENT_OBSERVATION_REQUIRED")
        val reviewed = expect(mvc.perform(patch("/api/admin/applications/${f.applicationId}/documents/$linkId/review")
            .contentType(MediaType.APPLICATION_JSON).content("""{"status":"OBSERVED","observation":"Imagen ilegible"}""")
            .header("Authorization", auth(f.adminToken))).andReturn(), 200)!!
        assertEquals("OBSERVED", reviewed.get("status").asString())
        assertEquals(f.adminId, reviewed.get("reviewedByUserId").asLong())
        val detail = expect(mvc.perform(get("/api/applications/${f.applicationId}").header("Authorization", auth(f.ownerToken))).andReturn(), 200)!!
        assertEquals("OBSERVED", detail.get("pendingDocuments").get(0).get("reason").asString())
        assertEquals("Imagen ilegible", detail.get("pendingDocuments").get(0).get("observation").asString())
        expect(mvc.perform(patch("/api/admin/applications/${f.applicationId}/documents/$linkId/review")
            .contentType(MediaType.APPLICATION_JSON).content("""{"status":"VALID"}""")
            .header("Authorization", auth(f.adminToken))).andReturn(), 409, "APPLICATION_DOCUMENT_INVALID_REVIEW_STATUS")

        val replacement = expect(putDocument(upload = file("nuevo.pdf", "application/pdf", pdf("new"))), 200)!!
        assertEquals("PENDING", replacement.get("status").asString())
        assertTrue(replacement.get("observation").isNull)
        assertTrue(replacement.get("reviewedAt").isNull)
    }

    @Test
    fun `propiedad permisos y rol activo protegen operaciones`() {
        expect(mvc.perform(get("/api/applications/${f.applicationId}/documents")).andReturn(), 401)
        expect(mvc.perform(get("/api/applications/${f.applicationId}/documents").header("Authorization", "Bearer invalid")).andReturn(), 401)
        expect(mvc.perform(get("/api/applications/${f.applicationId}/documents").header("Authorization", auth(f.noPermissionToken))).andReturn(), 403)
        expect(mvc.perform(get("/api/applications/${f.applicationId}/documents").header("Authorization", auth(f.otherToken))).andReturn(), 404)
        expect(putDocument(token = f.otherToken), 404)
        val upload = expect(putDocument(), 201)!!
        val linkId = upload.get("id").asString()
        expect(mvc.perform(get("/api/applications/${f.applicationId}/documents/$linkId/content")
            .header("Authorization", auth(f.otherToken))).andReturn(), 404)
    }

    @ParameterizedTest
    @EnumSource(ApplicationStatus::class, names = ["APPROVED", "REJECTED", "CLOSED"])
    fun `estados finales impiden cargar reemplazar eliminar y revisar`(status: ApplicationStatus) {
        val linkId = expect(putDocument(), 201)!!.get("id").asString()
        tx { applications.findById(f.applicationId).orElseThrow().status = status }
        expect(putDocument(), 409, "APPLICATION_DOCUMENTS_FINALIZED")
        expect(mvc.perform(delete("/api/applications/${f.applicationId}/documents/$linkId")
            .header("Authorization", auth(f.ownerToken))).andReturn(), 409, "APPLICATION_DOCUMENTS_FINALIZED")
        expect(mvc.perform(patch("/api/admin/applications/${f.applicationId}/documents/$linkId/review")
            .contentType(MediaType.APPLICATION_JSON).content("""{"status":"VALID"}""")
            .header("Authorization", auth(f.adminToken))).andReturn(), 409, "APPLICATION_DOCUMENTS_FINALIZED")
    }

    @Test
    fun `administrativo lista descarga revisa y auditoria nunca guarda bytes`() {
        val linkId = expect(putDocument(), 201)!!.get("id").asString()
        val listed = expect(mvc.perform(get("/api/admin/applications/${f.applicationId}/documents")
            .header("Authorization", auth(f.adminToken))).andReturn(), 200)!!
        assertEquals(1, listed.size())
        assertFalse(listed.get(0).has("content"))
        val download = mvc.perform(get("/api/admin/applications/${f.applicationId}/documents/$linkId/content")
            .header("Authorization", auth(f.adminToken))).andReturn()
        assertEquals(200, download.response.status)
        assertContentEquals(pdf(), download.response.contentAsByteArray)
        expect(mvc.perform(patch("/api/admin/applications/${f.applicationId}/documents/$linkId/review")
            .contentType(MediaType.APPLICATION_JSON).content("""{"status":"VALID","observation":"no corresponde"}""")
            .header("Authorization", auth(f.adminToken))).andReturn(), 409, "APPLICATION_DOCUMENT_OBSERVATION_NOT_ALLOWED")
        expect(mvc.perform(patch("/api/admin/applications/${f.applicationId}/documents/$linkId/review")
            .contentType(MediaType.APPLICATION_JSON).content("""{"status":"VALID"}""")
            .header("Authorization", auth(f.adminToken))).andReturn(), 200)
        val values = jdbc.queryForList("select old_values, new_values from logs where entity_type = 'application_document' and entity_id = ?", linkId)
        assertEquals(2, values.size)
        values.flatMap { it.values }.filterNotNull().forEach { raw ->
            val text = if (raw is ByteArray) raw.toString(Charsets.UTF_8) else raw.toString()
            assertFalse(text.contains("%PDF"))
            assertFalse(text.contains("content\":"))
        }
    }

    @Test
    fun `reemplazos concurrentes conservan un vinculo y un archivo`() {
        expect(putDocument(), 201)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures = (1..2).map { index -> pool.submit<MvcResult> {
                ready.countDown(); check(start.await(10, TimeUnit.SECONDS))
                putDocument(upload = file("parallel-$index.pdf", "application/pdf", pdf("parallel-$index")))
            } }
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown()
            futures.forEach { expect(it.get(30, TimeUnit.SECONDS), 200) }
        } finally { pool.shutdownNow() }
        assertEquals(1, applicationDocuments.findMetadataByApplicationId(f.applicationId).size)
        assertEquals(0, orphanDocumentCount())
    }

    @Test
    fun `fallo de auditoria revierte archivo vinculo y actualizacion`() {
        val before = tx { applications.findById(f.applicationId).orElseThrow().updatedAt }
        jdbc.execute("alter table logs add constraint ck_document_audit_failure_${f.ownerId} check (entity_type <> 'application_document' or user_id <> ${f.ownerId})")
        try {
            expect(putDocument(), 409)
            assertEquals(0, applicationDocuments.findMetadataByApplicationId(f.applicationId).size)
            assertEquals(0, orphanDocumentCount())
            assertEquals(before, tx { applications.findById(f.applicationId).orElseThrow().updatedAt })
        } finally {
            jdbc.execute("alter table logs drop constraint ck_document_audit_failure_${f.ownerId}")
        }
    }

    @Test
    fun `no permite eliminar al revisor referenciado por un documento`() {
        val linkId = expect(putDocument(), 201)!!.get("id").asString()
        expect(mvc.perform(patch("/api/admin/applications/${f.applicationId}/documents/$linkId/review")
            .contentType(MediaType.APPLICATION_JSON).content("""{"status":"VALID"}""")
            .header("Authorization", auth(f.adminToken))).andReturn(), 200)
        val managerToken = tx {
            val managerRole = role("GESTOR_BAJAS_DOCUMENTOS", setOf("users:delete"))
            val manager = users.save(User(name = "Gestor de bajas", email = "manager-${UUID.randomUUID()}@example.com", roles = mutableSetOf(managerRole)))
            jwt.createToken(manager, managerRole)
        }
        expect(mvc.perform(delete("/users/${f.adminId}").header("Authorization", auth(managerToken))).andReturn(),
            409, "USER_HAS_APPLICATION_REFERENCES")
        assertTrue(users.existsById(f.adminId))
    }

    private fun orphanDocumentCount(): Int = jdbc.queryForObject(
        "select count(*) from document d left join application_document ad on ad.document_id = d.id where ad.id is null",
        Int::class.java,
    )!!
}
