package com.uade.dda2.server.feature.program

import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramImageRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import com.uade.dda2.server.security.JwtService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:program-images;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "app.enrollment-period.expiration.cron=-",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ProgramImageFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var programs: ProgramRepository
    @Autowired lateinit var editions: ProgramEditionRepository
    @Autowired lateinit var programImages: ProgramImageRepository
    @Autowired lateinit var transactions: PlatformTransactionManager

    private lateinit var fixture: Fixture

    data class Fixture(
        val programId: UUID,
        val adminToken: String,
        val noPermissionToken: String,
    )

    @BeforeEach
    fun setup() {
        fixture = transaction {
            val create = permissions.findAll().firstOrNull { it.name == "programs:management:create" }
                ?: permissions.save(Permission(name = "programs:management:create"))
            val edit = permissions.findAll().firstOrNull { it.name == "programs:management:edit" }
                ?: permissions.save(Permission(name = "programs:management:edit"))
            val view = permissions.findAll().firstOrNull { it.name == "programs:management:view" }
                ?: permissions.save(Permission(name = "programs:management:view"))
            val role = roles.save(
                Role(
                    name = "PROGRAM_IMAGE_ADMIN_${UUID.randomUUID().toString().take(8)}",
                    permissions = mutableSetOf(create, edit, view),
                ),
            )
            val emptyRole = roles.save(Role(name = "PROGRAM_IMAGE_VIEWER_${UUID.randomUUID().toString().take(8)}"))
            val admin = users.save(
                User(
                    name = "Administrador de portadas",
                    email = "program-image-admin-${UUID.randomUUID()}@example.com",
                    roles = mutableSetOf(role),
                ),
            )
            val viewer = users.save(
                User(
                    name = "Sin permisos",
                    email = "program-image-viewer-${UUID.randomUUID()}@example.com",
                    roles = mutableSetOf(emptyRole),
                ),
            )
            val program = programs.save(
                Program(
                    name = "Programa con portada ${UUID.randomUUID()}",
                    objective = "Objetivo de prueba",
                    createdBy = admin,
                ),
            )
            editions.save(
                ProgramEdition(
                    program = program,
                    name = "Edición disponible",
                    startDate = LocalDate.now(),
                    endDate = LocalDate.now().plusDays(30),
                    maxCapacity = 100,
                    status = ProgramEditionStatus.ACTIVE,
                    createdBy = admin,
                ),
            )

            Fixture(
                programId = requireNotNull(program.id),
                adminToken = jwt.createToken(admin, role),
                noPermissionToken = jwt.createToken(viewer, emptyRole),
            )
        }
    }

    @Test
    fun `crear publica la imagen y agrega su URL a todas las respuestas de programa`() {
        val png = png()
        val created = body(
            mvc.perform(
                multipart("/api/admin/programs/${fixture.programId}/image")
                    .file(file("portada.png", "image/png", png))
                    .header("Authorization", authorization(fixture.adminToken)),
            ).andReturn(),
            201,
        )
        val imageId = UUID.fromString(created.get("id").asString())
        val imageUrl = "/api/images/$imageId"
        assertEquals(fixture.programId.toString(), created.get("programId").asString())
        assertEquals(imageUrl, created.get("url").asString())
        assertFalse(created.has("content"))

        val publicImage = mvc.perform(get(imageUrl)).andReturn()
        assertEquals(200, publicImage.response.status)
        assertEquals("image/png", publicImage.response.contentType)
        assertEquals("no-cache", publicImage.response.getHeader("Cache-Control"))
        assertTrue(publicImage.response.getHeader("Content-Disposition")!!.startsWith("inline"))
        assertContentEquals(png, publicImage.response.contentAsByteArray)

        assertEquals(imageUrl, body(mvc.perform(get("/api/admin/programs/${fixture.programId}")
            .header("Authorization", authorization(fixture.adminToken))).andReturn(), 200).get("imageUrl").asString())

        val adminList = body(mvc.perform(get("/api/admin/programs?page=0&size=100")
            .header("Authorization", authorization(fixture.adminToken))).andReturn(), 200)
        assertEquals(imageUrl, findProgram(adminList.get("content")).get("imageUrl").asString())

        val options = body(mvc.perform(get("/api/admin/programs/options")
            .header("Authorization", authorization(fixture.adminToken))).andReturn(), 200)
        assertEquals(imageUrl, findProgram(options).get("imageUrl").asString())

        val availableList = body(mvc.perform(get("/api/programs?page=0&size=100")).andReturn(), 200)
        assertEquals(imageUrl, findProgram(availableList.get("content")).get("imageUrl").asString())
        assertEquals(imageUrl, body(mvc.perform(get("/api/programs/${fixture.programId}")).andReturn(), 200).get("imageUrl").asString())
    }

    @Test
    fun `reemplazar conserva la URL y eliminar quita la asociacion y el contenido`() {
        val created = body(create(file("portada.png", "image/png", png())), 201)
        val imageId = created.get("id").asString()
        val jpeg = jpeg()

        val updated = body(
            mvc.perform(
                multipart("/api/admin/programs/${fixture.programId}/image")
                    .file(file("nueva-portada.jpg", "image/jpeg", jpeg))
                    .with { it.method = "PUT"; it }
                    .header("Authorization", authorization(fixture.adminToken)),
            ).andReturn(),
            200,
        )
        assertEquals(imageId, updated.get("id").asString())
        assertEquals("image/jpeg", updated.get("contentType").asString())
        val publicImage = mvc.perform(get(updated.get("url").asString())).andReturn()
        assertEquals(200, publicImage.response.status)
        assertContentEquals(jpeg, publicImage.response.contentAsByteArray)

        body(create(file("otra.png", "image/png", png())), 409, "PROGRAM_IMAGE_ALREADY_EXISTS")
        body(
            mvc.perform(delete("/api/admin/programs/${fixture.programId}/image")
                .header("Authorization", authorization(fixture.adminToken))).andReturn(),
            204,
        )
        assertFalse(programImages.existsById(UUID.fromString(imageId)))
        body(mvc.perform(get("/api/images/$imageId")).andReturn(), 404, "PROGRAM_IMAGE_NOT_FOUND")
        val program = body(mvc.perform(get("/api/programs/${fixture.programId}")).andReturn(), 200)
        assertTrue(program.get("imageUrl").isNull)
        body(
            mvc.perform(
                multipart("/api/admin/programs/${fixture.programId}/image")
                    .file(file("nueva.png", "image/png", png()))
                    .with { it.method = "PUT"; it }
                    .header("Authorization", authorization(fixture.adminToken)),
            ).andReturn(),
            404,
            "PROGRAM_IMAGE_NOT_FOUND",
        )
    }

    @Test
    fun `valida permisos existencia tamaño tipo MIME extension y firma`() {
        body(create(file("empty.png", "image/png", byteArrayOf())), 400, "PROGRAM_IMAGE_EMPTY_FILE")
        body(create(file("fake.png", "image/png", jpeg())), 400, "PROGRAM_IMAGE_INVALID_FILE_TYPE")
        body(create(file("wrong.jpg", "image/png", png())), 400, "PROGRAM_IMAGE_INVALID_FILE_TYPE")
        body(create(file("cover.pdf", "application/pdf", "%PDF-1.7".toByteArray())), 400, "PROGRAM_IMAGE_INVALID_FILE_TYPE")

        val missingProgram = UUID.randomUUID()
        body(
            mvc.perform(multipart("/api/admin/programs/$missingProgram/image")
                .file(file("portada.png", "image/png", png()))
                .header("Authorization", authorization(fixture.adminToken))).andReturn(),
            404,
            "PROGRAM_NOT_FOUND",
        )
        body(mvc.perform(multipart("/api/admin/programs/${fixture.programId}/image")
            .file(file("portada.png", "image/png", png()))).andReturn(), 401)
        body(mvc.perform(multipart("/api/admin/programs/${fixture.programId}/image")
            .file(file("portada.png", "image/png", png()))
            .header("Authorization", authorization(fixture.noPermissionToken))).andReturn(), 403)
    }

    @Test
    fun `eliminar un programa sin otras relaciones tambien elimina su imagen`() {
        val programId = transaction {
            val creator = programs.findById(fixture.programId).orElseThrow().createdBy
            programs.save(
                Program(
                    name = "Programa eliminable ${UUID.randomUUID()}",
                    createdBy = creator,
                ),
            ).id!!
        }
        val created = body(
            mvc.perform(multipart("/api/admin/programs/$programId/image")
                .file(file("portada.png", "image/png", png()))
                .header("Authorization", authorization(fixture.adminToken))).andReturn(),
            201,
        )
        val imageId = UUID.fromString(created.get("id").asString())

        body(
            mvc.perform(delete("/api/admin/programs/$programId")
                .header("Authorization", authorization(fixture.adminToken))).andReturn(),
            204,
        )
        assertFalse(programs.existsById(programId))
        assertFalse(programImages.existsById(imageId))
    }

    private fun create(upload: MockMultipartFile): MvcResult =
        mvc.perform(
            multipart("/api/admin/programs/${fixture.programId}/image")
                .file(upload)
                .header("Authorization", authorization(fixture.adminToken)),
        ).andReturn()

    private fun file(name: String, type: String, bytes: ByteArray) =
        MockMultipartFile("file", name, type, bytes)

    private fun png() =
        byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)

    private fun jpeg() =
        byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3)

    private fun authorization(token: String) = "Bearer $token"

    private fun body(result: MvcResult, expectedStatus: Int, expectedCode: String? = null): JsonNode {
        assertEquals(expectedStatus, result.response.status, result.response.contentAsString)
        if (result.response.contentAsString.isBlank()) return json.createObjectNode()
        val response = json.readTree(result.response.contentAsString)
        expectedCode?.let { assertEquals(it, response.get("code").asString()) }
        return response
    }

    private fun findProgram(content: JsonNode): JsonNode =
        content.first { it.get("id").asString() == fixture.programId.toString() }

    private fun <T : Any> transaction(action: () -> T): T =
        TransactionTemplate(transactions).execute { action() }!!
}
