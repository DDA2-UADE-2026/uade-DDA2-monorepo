package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.integration.client.M1DocumentClient
import com.uade.dda2.server.feature.integration.client.M1DocumentFile
import com.uade.dda2.server.feature.integration.entity.ExternalDocumentationReceipt
import com.uade.dda2.server.feature.integration.error.M1DocumentException
import com.uade.dda2.server.feature.integration.error.M1DocumentFailure
import com.uade.dda2.server.feature.integration.repository.ExternalDocumentationReceiptRepository
import com.uade.dda2.server.feature.log.repository.LogRepository
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import com.uade.dda2.server.security.JwtService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.*

/** Cliente de archivos sustituido; HTTP de M1 y todos los transportes de Core desactivados. */
@SpringBootTest(properties = [
    "app.core.enabled=false", "app.core.business.enabled=false", "app.core.publisher.enabled=false",
    "app.core.consumer.enabled=false", "app.core.check-on-startup=false", "app.m1.auth.enabled=false",
    "app.m1.documents.enabled=false", "app.enrollment-period.expiration.cron=-",
    "app.cors.allowed-origins[0]=http://localhost:5173",
    "spring.datasource.url=jdbc:h2:mem:external-file-proxy;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
])
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ExternalDocumentFileFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var programs: ProgramRepository
    @Autowired lateinit var editions: ProgramEditionRepository
    @Autowired lateinit var periods: EnrollmentPeriodRepository
    @Autowired lateinit var applications: ApplicationRepository
    @Autowired lateinit var receipts: ExternalDocumentationReceiptRepository
    @Autowired lateinit var logs: LogRepository
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var manager: PlatformTransactionManager
    @MockitoBean lateinit var client: M1DocumentClient
    private lateinit var applicationId: UUID
    private lateinit var requestId: UUID
    private lateinit var documentId: UUID
    private lateinit var token: String
    private var userId = 0L
    private var roleId = 0L
    private val path get() = "/api/admin/applications/$applicationId/external-documents/$requestId/file"
    private fun <T> tx(action: () -> T): T = requireNotNull(TransactionTemplate(manager).execute { action() })

    @BeforeEach fun setup() {
        tx {
            val permission = permissions.findByNameIn(listOf("applications:management:documents:view")).firstOrNull()
                ?: permissions.save(Permission(name = "applications:management:documents:view"))
            val role = roles.save(Role(name = "FILE_${UUID.randomUUID()}", permissions = mutableSetOf(permission)))
            val user = users.saveAndFlush(User(name = "Administrativo de prueba", email = "file@example.invalid", roles = mutableSetOf(role)))
            userId = user.id!!
            roleId = role.id!!
            token = jwt.createToken(user, role)
            val program = programs.saveAndFlush(Program(name = "Archivos ${UUID.randomUUID()}", createdBy = user))
            val edition = editions.saveAndFlush(ProgramEdition(program = program, name = "Prueba", createdBy = user,
                startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(30), maxCapacity = 10))
            val period = periods.saveAndFlush(EnrollmentPeriod(programEdition = edition, openDate = LocalDate.now(), closeDate = LocalDate.now().plusDays(10)))
            applicationId = applications.saveAndFlush(Application(user = user, registeredBy = user,
                programEdition = edition, enrollmentPeriod = period, submittedAt = LocalDateTime.now())).id!!
            requestId = UUID.randomUUID()
            documentId = UUID.randomUUID()
            receipts.saveAndFlush(ExternalDocumentationReceipt(requestId, applicationId, UUID.randomUUID(), UUID.randomUUID(),
                "EXP-LOCAL", documentId, "COMPLETED", UUID.randomUUID(), Instant.now()))
        }
        doAnswer {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "No mantener una transacción durante HTTP")
            M1DocumentFile("%PDF-local".toByteArray(), MediaType.APPLICATION_PDF, "documento-$documentId.pdf")
        }.`when`(client).download(documentId)
    }

    @Test fun `descarga protegida por bearer o cookie devuelve adjunto sin cambiar revision`() {
        val auditCount = logs.count()
        for (cookie in listOf(false, true)) {
            val request = get(path).header("Origin", "http://localhost:5173")
            if (cookie) request.cookie(Cookie("uade_session", token)) else request.header("Authorization", "Bearer $token")
            mvc.perform(request).andExpect(status().isOk)
                .andExpect(content().bytes("%PDF-local".toByteArray())).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"documento-$documentId.pdf\""))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().string("Access-Control-Expose-Headers", org.hamcrest.Matchers.containsString("Content-Disposition")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().doesNotExist("Authorization")).andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(header().doesNotExist("Location"))
        }
        verify(client, times(2)).download(documentId)
        assertEquals(ApplicationDocumentStatus.PENDING, receipts.findById(requestId).orElseThrow().reviewStatus)
        assertEquals(auditCount, logs.count())
    }

    @Test fun `sesion ausente y usuario sin permiso no llegan al cliente`() {
        mvc.perform(get(path)).andExpect(status().isUnauthorized)
        val restricted = tx {
            val role = roles.save(Role(name = "NO_FILE_${UUID.randomUUID()}"))
            val user = users.saveAndFlush(User(name = "Sin permiso", email = "restricted@example.invalid", roles = mutableSetOf(role)))
            jwt.createToken(user, role)
        }
        mvc.perform(get(path).header("Authorization", "Bearer $restricted")).andExpect(status().isForbidden)
        verifyNoInteractions(client)
    }

    @ParameterizedTest @ValueSource(strings = ["inactive", "role-removed", "permission-removed"])
    fun `revocacion local se respeta aun con JWT vigente`(scenario: String) {
        tx {
            when (scenario) {
                "inactive" -> users.findById(userId).orElseThrow().active = false
                "role-removed" -> users.findById(userId).orElseThrow().roles.clear()
                else -> roles.findById(roleId).orElseThrow().permissions.clear()
            }
        }
        mvc.perform(get(path).header("Authorization", "Bearer $token"))
            .andExpect(status().`is`(if (scenario == "inactive") 401 else 403))
        verifyNoInteractions(client)
    }

    @Test fun `referencia desconocida ajena o UUID de documento no permiten descargar`() {
        val otherApplication = tx {
            val original = applications.findById(applicationId).orElseThrow()
            val other = users.saveAndFlush(User(name = "Otro titular", email = "other@example.invalid"))
            applications.saveAndFlush(Application(user = other, registeredBy = original.registeredBy,
                programEdition = original.programEdition, enrollmentPeriod = original.enrollmentPeriod, submittedAt = LocalDateTime.now())).id!!
        }
        for (target in listOf(path.replace(applicationId.toString(), UUID.randomUUID().toString()),
            path.replace(applicationId.toString(), otherApplication.toString()),
            path.replace(requestId.toString(), documentId.toString())))
            mvc.perform(get(target).header("Authorization", "Bearer $token")).andExpect(status().isNotFound)
        verifyNoInteractions(client)
    }

    @ParameterizedTest @ValueSource(strings = ["PENDING", "missing-document"])
    fun `solo entrega COMPLETED con documentId habilita lectura`(scenario: String) {
        tx { receipts.findById(requestId).orElseThrow().apply {
            if (scenario == "PENDING") status = "PENDING" else documentId = null
        } }
        mvc.perform(get(path).header("Authorization", "Bearer $token")).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("M8_DOCUMENT_FILE_NOT_READY"))
        verifyNoInteractions(client)
    }

    @Test fun `cada descarga resuelve la referencia vigente y permite revisar documentos ya observados`() {
        val replacement = UUID.randomUUID()
        tx { receipts.findById(requestId).orElseThrow().apply { documentId = replacement; reviewStatus = ApplicationDocumentStatus.OBSERVED } }
        `when`(client.download(replacement)).thenReturn(M1DocumentFile(byteArrayOf(1), MediaType.APPLICATION_OCTET_STREAM, "documento-$replacement.bin"))
        mvc.perform(get(path).header("Authorization", "Bearer $token").param("documentId", documentId.toString()))
            .andExpect(status().isOk).andExpect(content().bytes(byteArrayOf(1)))
        verify(client).download(replacement)
        verify(client, never()).download(documentId)
    }

    @ParameterizedTest @EnumSource(M1DocumentFailure::class)
    fun `errores de proveedor respetan contrato problem sin encabezados remotos`(failure: M1DocumentFailure) {
        `when`(client.download(documentId)).thenThrow(M1DocumentException(failure))
        mvc.perform(get(path).header("Authorization", "Bearer $token"))
            .andExpect(status().`is`(failure.status.value())).andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("M1_DOCUMENT_${failure.name}"))
            .andExpect(header().doesNotExist("Authorization")).andExpect(header().doesNotExist("Set-Cookie"))
    }
}
