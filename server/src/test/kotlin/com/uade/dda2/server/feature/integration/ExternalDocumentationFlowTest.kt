package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.application.service.ApplicationDocumentStatesService
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.integration.client.CoreTokenClient
import com.uade.dda2.server.feature.integration.client.M1AuthClient
import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.dto.CoreEnvelope
import com.uade.dda2.server.feature.integration.dto.CoreInboxResult
import com.uade.dda2.server.feature.integration.dto.CoreMessageMetadata
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.repository.CoreInboxRepository
import com.uade.dda2.server.feature.integration.repository.CoreOutboxRepository
import com.uade.dda2.server.feature.integration.repository.ExternalDocumentationReceiptRepository
import com.uade.dda2.server.feature.integration.service.*
import com.uade.dda2.server.feature.log.repository.LogRepository
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
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when`
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.*

/** Codec, inbox, servicio y JPA reales; ningún cliente HTTP/AMQP remoto se crea en este contexto. */
@SpringBootTest(properties = [
    "app.core.enabled=false", "app.core.business.enabled=false", "app.core.check-on-startup=false",
    "app.core.publisher.enabled=false", "app.core.consumer.enabled=false", "app.m1.auth.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:external-documentation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
    "app.enrollment-period.expiration.cron=-",
])
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class ExternalDocumentationFlowTest {
    @Autowired lateinit var context: ApplicationContext
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
    @Autowired lateinit var requirements: ProgramDocumentRequirementRepository
    @Autowired lateinit var receipts: ExternalDocumentationReceiptRepository
    @Autowired lateinit var inbox: CoreInboxRepository
    @Autowired lateinit var outbox: CoreOutboxRepository
    @Autowired lateinit var logs: LogRepository
    @Autowired lateinit var documents: ExternalDocumentationService
    @Autowired lateinit var states: ApplicationDocumentStatesService
    @Autowired lateinit var codec: M8ContractCodec
    @Autowired lateinit var wire: CoreEnvelopeCodec
    @Autowired lateinit var properties: CoreProperties
    @Autowired lateinit var manager: PlatformTransactionManager
    @MockitoBean lateinit var clock: Clock

    private val now = OffsetDateTime.parse("2026-11-16T12:00:00-03:00")
    private lateinit var f: Fixture
    private lateinit var processor: CoreInboxService

    private data class Fixture(val applicationId: UUID, val citizenId: UUID, val userId: Long,
        val programId: UUID, val editionId: UUID, val requirementId: UUID, val secondRequirementId: UUID,
        val requestId: UUID, val secondRequestId: UUID, val fileId: UUID, val outgoing: CoreEnvelope, val token: String)

    private fun <T> tx(action: () -> T): T = requireNotNull(TransactionTemplate(manager).execute { action() })

    @BeforeEach fun setup() {
        `when`(clock.instant()).thenReturn(now.plusMinutes(10).toInstant())
        val handler = object : CoreEventHandler {
            override val eventType = M8EventType.SOCIAL_DOCUMENTATION_DELIVERED.wireName
            override val eventVersion = M8EventType.VERSION
            override fun handle(envelope: CoreEnvelope) = documents.receive(codec.typed(envelope, SocialDocumentationDeliveredData::class.java))
        }
        processor = CoreInboxService(inbox, wire, listOf(handler), properties, clock)
        f = fixture()
    }

    private fun fixture(): Fixture = tx {
        fun permission(name: String) = permissions.findByNameIn(listOf(name)).firstOrNull() ?: permissions.save(Permission(name = name))
        val role = roles.save(Role(name = "DOC_${UUID.randomUUID()}", permissions = mutableSetOf(
            permission("applications:management:documents:view"), permission("applications:management:documents:review"))))
        val admin = users.saveAndFlush(User(name = "Revisor de prueba", email = "reviewer@example.invalid", roles = mutableSetOf(role)))
        val citizenId = UUID.randomUUID()
        val citizen = users.saveAndFlush(User(name = "Titular de prueba", email = "citizen@example.invalid", externalCitizenId = citizenId.toString()))
        val program = programs.saveAndFlush(Program(name = "Documentación ${UUID.randomUUID()}", createdBy = admin))
        val edition = editions.saveAndFlush(ProgramEdition(program = program, name = "Edición de prueba", createdBy = admin,
            startDate = now.toLocalDate().minusDays(1), endDate = now.toLocalDate().plusDays(30), maxCapacity = 10,
            status = ProgramEditionStatus.ACTIVE))
        val period = periods.saveAndFlush(EnrollmentPeriod(programEdition = edition, openDate = now.toLocalDate().minusDays(1),
            closeDate = now.toLocalDate().plusDays(10), status = EnrollmentPeriodStatus.OPEN))
        val application = applications.saveAndFlush(Application(user = citizen, registeredBy = admin, programEdition = edition,
            enrollmentPeriod = period, submittedAt = now.toLocalDateTime()))
        val first = requirements.saveAndFlush(ProgramDocumentRequirement(programEdition = edition, code = "DNI", name = "Documento", required = true))
        val second = requirements.saveAndFlush(ProgramDocumentRequirement(programEdition = edition, code = "DOMICILIO", name = "Domicilio", required = true))
        val data = SocialDocumentationRequestedData(citizenId, application.id!!, program.id!!, edition.id!!,
            listOf(RequiredDocumentData(first.id!!, first.code, first.name, true), RequiredDocumentData(second.id!!, second.code, second.name, true)))
        val outgoing = CoreEnvelope(UUID.randomUUID(), M8EventType.SOCIAL_DOCUMENTATION_REQUESTED.wireName, "1.0", now,
            "desarrollo-social", json.valueToTree(data), application.id)
        CoreOutboxService(outbox, wire, properties, clock).enqueue(outgoing)
        Fixture(application.id!!, citizenId, citizen.id!!, program.id!!, edition.id!!, first.id!!, second.id!!,
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), outgoing, jwt.createToken(admin, role))
    }

    private fun data(status: ExternalDocumentationStatus = ExternalDocumentationStatus.PENDING, documentId: UUID? = null): SocialDocumentationDeliveredData =
        SocialDocumentationDeliveredData(f.citizenId, f.applicationId, f.programId, f.editionId,
            listOf(ExternalFileData(f.fileId, "EXP-PRUEBA-1", "M8", "OPEN", listOf(
                ExternalDocumentationRequestData(f.requestId, f.requirementId, "DNI", status, documentId),
                ExternalDocumentationRequestData(f.secondRequestId, f.secondRequirementId, "DOMICILIO", ExternalDocumentationStatus.PENDING, null)))))

    private fun event(data: SocialDocumentationDeliveredData = data(), seconds: Long = 1): CoreEnvelope =
        CoreEnvelope(UUID.randomUUID(), M8EventType.SOCIAL_DOCUMENTATION_DELIVERED.wireName, "1.0", now.plusSeconds(seconds),
            "ciudadanos", json.valueToTree(data), f.applicationId, f.outgoing.eventId)

    private fun receive(event: CoreEnvelope): CoreInboxResult = tx {
        processor.process(wire.encode(event).toByteArray(Charsets.UTF_8), CoreMessageMetadata(
            messageId = event.eventId.toString(), eventIdHeader = event.eventId.toString(), targetModule = "desarrollo-social"))
    }

    private fun rejected(event: CoreEnvelope, code: String) {
        val alreadyProcessed = inbox.existsById(event.eventId)
        assertEquals(code, assertFailsWith<CoreIntegrationException> { receive(event) }.code)
        assertEquals(alreadyProcessed, inbox.existsById(event.eventId), "Un rechazo no crea ni elimina la marca de inbox")
    }

    private fun review(status: String = "VALID", observation: String? = null, token: String = f.token) =
        mvc.perform(patch("/api/admin/applications/${f.applicationId}/external-documents/${f.requestId}/review")
            .header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(mapOf("status" to status, "observation" to observation))))

    @Test fun `las dos respuestas conservan IDs y COMPLETED queda pendiente de revision humana`() {
        val initial = event()
        assertEquals(CoreInboxResult.PROCESSED, receive(initial))
        val initialRows = receipts.findAllByApplicationId(f.applicationId)
        assertEquals(setOf(f.requestId, f.secondRequestId), initialRows.map { it.requestId }.toSet())
        assertTrue(initialRows.all { it.status == "PENDING" && it.documentId == null && it.lastEventId == initial.eventId })
        assertTrue(tx { states.findStatesByApplicationIdIn(listOf(f.applicationId)).isEmpty() })
        review().andExpect(status().isConflict)

        val documentId = UUID.randomUUID()
        val completed = event(data(ExternalDocumentationStatus.COMPLETED, documentId), 2)
        assertEquals(initial.correlationId, completed.correlationId)
        assertEquals(f.outgoing.eventId, completed.causationId)
        assertEquals(CoreInboxResult.PROCESSED, receive(completed))
        val row = receipts.findById(f.requestId).orElseThrow()
        assertEquals(documentId, row.documentId)
        assertEquals("COMPLETED", row.status)
        assertEquals(ApplicationDocumentStatus.PENDING, row.reviewStatus)
        assertNull(row.reviewedAt)
        assertEquals(completed.eventId, row.lastEventId)
        assertEquals(ApplicationDocumentStatus.PENDING, tx { states.findStatesByApplicationIdIn(listOf(f.applicationId)).single().status })
        mvc.perform(get("/api/admin/applications/${f.applicationId}/external-documents").header("Authorization", "Bearer ${f.token}"))
            .andExpect(status().isOk).andExpect(jsonPath("$.length()").value(2))
        review().andExpect(status().isOk).andExpect(jsonPath("$.reviewStatus").value("VALID"))
        assertEquals(ApplicationDocumentStatus.VALID, tx { states.findStatesByApplicationIdIn(listOf(f.applicationId)).single().status })
        assertEquals(1, outbox.findAll().count { it.correlationId == f.applicationId })
    }

    @Test fun `reentregar cualquiera de las respuestas no repite persistencia ni auditoria`() {
        for (reply in listOf(event(), event(data(ExternalDocumentationStatus.COMPLETED, UUID.randomUUID()), 2))) {
            assertEquals(CoreInboxResult.PROCESSED, receive(reply))
            val count = logs.count()
            assertEquals(CoreInboxResult.DUPLICATE, receive(reply))
            assertEquals(count, logs.count())
        }
        assertEquals(2, receipts.findAllByApplicationId(f.applicationId).size)
    }

    @Test fun `mismo eventId con otro contenido se rechaza y no cambia la entrega`() {
        val pending = event()
        receive(pending)
        rejected(pending.copy(data = json.valueToTree(data(ExternalDocumentationStatus.COMPLETED, UUID.randomUUID()))), "CORE_EVENT_ID_CONFLICT")
        assertEquals("PENDING", receipts.findById(f.requestId).orElseThrow().status)
    }

    @Test fun `la entrega puede llegar antes del aviso inicial y el aviso viejo no la revierte`() {
        val completed = event(data(ExternalDocumentationStatus.COMPLETED, UUID.randomUUID()), 2)
        receive(completed)
        review().andExpect(status().isOk)
        receive(event(seconds = 1))
        val row = receipts.findById(f.requestId).orElseThrow()
        assertEquals("COMPLETED", row.status)
        assertEquals(ApplicationDocumentStatus.VALID, row.reviewStatus)
        assertEquals(completed.eventId, row.lastEventId)
        assertEquals(completed.occurredAt.toInstant(), row.sourceUpdatedAt)
    }

    @Test fun `un documento anterior no pisa la referencia ni la revision vigentes`() {
        val documentId = UUID.randomUUID()
        val latest = event(data(ExternalDocumentationStatus.COMPLETED, documentId), 3)
        receive(latest)
        review().andExpect(status().isOk)
        receive(event(data(ExternalDocumentationStatus.COMPLETED, UUID.randomUUID()), 2))
        val row = receipts.findById(f.requestId).orElseThrow()
        assertEquals(documentId, row.documentId)
        assertEquals(ApplicationDocumentStatus.VALID, row.reviewStatus)
        assertEquals(latest.eventId, row.lastEventId)
    }

    @Test fun `un nuevo aviso del mismo documento conserva la revision y uno distinto la limpia`() {
        val documentId = UUID.randomUUID()
        receive(event(data(ExternalDocumentationStatus.COMPLETED, documentId)))
        review("OBSERVED", "Falta una página").andExpect(status().isOk)
        receive(event(data(ExternalDocumentationStatus.COMPLETED, documentId), 2))
        assertEquals(ApplicationDocumentStatus.OBSERVED, receipts.findById(f.requestId).orElseThrow().reviewStatus)
        receive(event(data(ExternalDocumentationStatus.COMPLETED, UUID.randomUUID()), 3))
        val row = receipts.findById(f.requestId).orElseThrow()
        assertEquals(ApplicationDocumentStatus.PENDING, row.reviewStatus)
        assertNull(row.observation)
        assertNull(row.reviewedBy)
        assertNull(row.reviewedAt)
    }

    @Test fun `respuesta parcial no elimina solicitudes documentales que no vuelve a mencionar`() {
        receive(event())
        val completed = data(ExternalDocumentationStatus.COMPLETED, UUID.randomUUID())
        receive(event(completed.copy(files = listOf(completed.files.single().copy(
            documentationRequests = listOf(completed.files.single().documentationRequests.first())))), 2))
        assertEquals("PENDING", receipts.findById(f.secondRequestId).orElseThrow().status)
        assertEquals(2, receipts.findAllByApplicationId(f.applicationId).size)
    }

    @ParameterizedTest @ValueSource(strings = ["citizen", "program", "edition"])
    fun `rechaza referencias de otro ciudadano programa o edicion`(field: String) {
        val payload = when (field) {
            "citizen" -> data().copy(citizenId = UUID.randomUUID())
            "program" -> data().copy(programId = UUID.randomUUID())
            else -> data().copy(programEditionId = UUID.randomUUID())
        }
        rejected(event(payload), "M8_DOCUMENT_IDENTITY_MISMATCH")
        assertTrue(receipts.findAllByApplicationId(f.applicationId).isEmpty())
    }

    @ParameterizedTest @ValueSource(strings = ["correlation", "missing-cause", "unknown-cause", "other-type", "other-application", "response-as-cause"])
    fun `ambas respuestas exigen correlacion y el evento del pedido original como causa`(scenario: String) {
        val initial = event()
        if (scenario == "response-as-cause") receive(initial)
        if (scenario == "other-type" || scenario == "other-application") tx {
            val original = outbox.findById(f.outgoing.eventId).orElseThrow()
            if (scenario == "other-type") original.eventType = "socialBenefitUpdated" else original.correlationId = UUID.randomUUID()
        }
        val base = event(data(ExternalDocumentationStatus.COMPLETED, UUID.randomUUID()), 2)
        val invalid = when (scenario) {
            "correlation" -> base.copy(correlationId = UUID.randomUUID())
            "missing-cause" -> base.copy(causationId = null)
            "unknown-cause" -> base.copy(causationId = UUID.randomUUID())
            "response-as-cause" -> base.copy(causationId = initial.eventId)
            else -> base
        }
        val code = when (scenario) { "correlation" -> "M8_INVALID_CORRELATION"; "missing-cause" -> "M8_CAUSATION_REQUIRED"; else -> "M8_DOCUMENT_REQUEST_UNKNOWN" }
        rejected(invalid, code)
    }

    @ParameterizedTest @ValueSource(strings = ["unknown", "wrong-type", "not-requested"])
    fun `solo admite requisitos y tipos incluidos en el pedido`(scenario: String) {
        val extra = if (scenario == "not-requested") tx {
            requirements.saveAndFlush(ProgramDocumentRequirement(programEdition = editions.getReferenceById(f.editionId),
                code = "EXTRA", name = "No solicitado", required = false)).id!!
        } else UUID.randomUUID()
        val payload = data()
        val first = payload.files.single().documentationRequests.first()
        val invalid = when (scenario) {
            "wrong-type" -> first.copy(documentType = "OTRO")
            "not-requested" -> first.copy(requirementId = extra, documentType = "EXTRA")
            else -> first.copy(requirementId = extra)
        }
        rejected(event(payload.copy(files = listOf(payload.files.single().copy(documentationRequests = listOf(invalid))))),
            if (scenario == "unknown") "M8_DOCUMENT_REQUIREMENT_UNKNOWN" else "M8_DOCUMENT_REQUIREMENT_MISMATCH")
    }

    @ParameterizedTest @ValueSource(strings = ["pending-with-document", "completed-without-document", "missing-document-key", "numeric-document", "invalid-document-uuid", "duplicate-request", "duplicate-request-other-file"])
    fun `valida nulabilidad UUID y unicidad de las solicitudes dentro de una respuesta`(scenario: String) {
        val base = event()
        val payload = base.data as ObjectNode
        val file = payload["files"][0] as ObjectNode
        val requests = file["documentationRequests"] as tools.jackson.databind.node.ArrayNode
        val first = requests[0] as ObjectNode
        when (scenario) {
            "pending-with-document" -> first.put("documentId", UUID.randomUUID().toString())
            "completed-without-document" -> first.put("status", "COMPLETED")
            "missing-document-key" -> first.remove("documentId")
            "numeric-document" -> { first.put("status", "COMPLETED"); first.put("documentId", 4) }
            "invalid-document-uuid" -> { first.put("status", "COMPLETED"); first.put("documentId", "invalid") }
            "duplicate-request" -> requests.add(first.deepCopy())
            "duplicate-request-other-file" -> {
                val other = file.deepCopy()
                other.put("fileId", UUID.randomUUID().toString())
                other.putArray("documentationRequests").add(first.deepCopy())
                (payload["files"] as tools.jackson.databind.node.ArrayNode).add(other)
            }
        }
        rejected(base, "M8_INVALID_PAYLOAD")
        assertTrue(receipts.findAllByApplicationId(f.applicationId).isEmpty())
    }

    @ParameterizedTest @ValueSource(strings = ["initial", "after-review"])
    fun `un item invalido revierte todos los cambios y la marca de inbox`(scenario: String) {
        val documentId = UUID.randomUUID()
        if (scenario == "after-review") {
            receive(event(data(ExternalDocumentationStatus.COMPLETED, documentId)))
            review().andExpect(status().isOk)
        }
        val before = logs.count()
        val payload = data(ExternalDocumentationStatus.COMPLETED, UUID.randomUUID())
        val entries = payload.files.single().documentationRequests
        val invalid = event(payload.copy(files = listOf(payload.files.single().copy(
            documentationRequests = listOf(entries[0], entries[1].copy(documentType = "OTRO"))))), 2)
        rejected(invalid, "M8_DOCUMENT_REQUIREMENT_MISMATCH")
        assertEquals(before, logs.count())
        if (scenario == "initial") assertTrue(receipts.findAllByApplicationId(f.applicationId).isEmpty())
        else {
            val row = receipts.findById(f.requestId).orElseThrow()
            assertEquals(documentId, row.documentId)
            assertEquals(ApplicationDocumentStatus.VALID, row.reviewStatus)
        }
    }

    @Test fun `cambiar el vinculo local no permite responder por un ciudadano distinto del pedido original`() {
        val otherCitizen = UUID.randomUUID()
        tx { users.findById(f.userId).orElseThrow().externalCitizenId = otherCitizen.toString() }
        rejected(event(data().copy(citizenId = otherCitizen)), "M8_DOCUMENT_IDENTITY_MISMATCH")
    }

    @Test fun `un tipo documental modificado en el catalogo no reemplaza al solicitado originalmente`() {
        tx { requirements.findById(f.requirementId).orElseThrow().code = "OTRO" }
        val payload = data()
        val first = payload.files.single().documentationRequests.first().copy(documentType = "OTRO")
        rejected(event(payload.copy(files = listOf(payload.files.single().copy(documentationRequests = listOf(first))))),
            "M8_DOCUMENT_REQUIREMENT_MISMATCH")
    }

    @ParameterizedTest @ValueSource(strings = ["missing-data", "null-data", "malformed"])
    fun `un pedido local ilegible no permite aplicar la respuesta`(scenario: String) {
        tx {
            outbox.findById(f.outgoing.eventId).orElseThrow().payloadJson = when (scenario) {
                "missing-data" -> "{}"
                "null-data" -> """{"data":null}"""
                else -> "not-json"
            }
        }
        rejected(event(), "M8_DOCUMENT_REQUEST_UNKNOWN")
        assertTrue(receipts.findAllByApplicationId(f.applicationId).isEmpty())
    }

    @Test fun `un requestId de otra solicitud no se puede reasignar`() {
        receive(event())
        val old = f
        f = fixture()
        val payload = data()
        val first = payload.files.single().documentationRequests.first().copy(requestId = old.requestId)
        rejected(event(payload.copy(files = listOf(payload.files.single().copy(documentationRequests = listOf(first))))), "M8_DOCUMENT_REQUEST_CONFLICT")
        assertEquals(old.applicationId, receipts.findById(old.requestId).orElseThrow().applicationId)
    }

    @Test fun `solicitud desconocida se confirma en inbox sin crear referencias ni eventos`() {
        val unknown = UUID.randomUUID()
        val reply = event(data().copy(applicationId = unknown)).copy(correlationId = unknown)
        val outboxCount = outbox.count()
        val auditCount = logs.count()
        assertEquals(CoreInboxResult.PROCESSED, receive(reply))
        assertTrue(inbox.existsById(reply.eventId))
        assertTrue(receipts.findAllByApplicationId(unknown).isEmpty())
        assertEquals(outboxCount, outbox.count())
        assertEquals(auditCount, logs.count())
    }

    @ParameterizedTest @ValueSource(strings = ["missing-observation", "observation-on-valid", "pending-review"])
    fun `revision administrativa exige un resultado y observacion coherentes`(scenario: String) {
        receive(event(data(ExternalDocumentationStatus.COMPLETED, UUID.randomUUID())))
        when (scenario) {
            "missing-observation" -> review("OBSERVED")
            "observation-on-valid" -> review("VALID", "No corresponde")
            else -> review("PENDING")
        }.andExpect(status().isConflict)
        assertEquals(ApplicationDocumentStatus.PENDING, receipts.findById(f.requestId).orElseThrow().reviewStatus)
    }

    @Test fun `consulta y revision siguen protegidas y revocar el permiso tiene efecto`() {
        receive(event())
        val path = "/api/admin/applications/${f.applicationId}/external-documents"
        mvc.perform(get(path)).andExpect(status().isUnauthorized)
        val viewerToken = tx {
            val role = roles.save(Role(name = "VIEW_${UUID.randomUUID()}", permissions = mutableSetOf(
                permissions.findByNameIn(listOf("applications:management:documents:view")).single())))
            val user = users.saveAndFlush(User(name = "Consulta", email = "viewer@example.invalid", roles = mutableSetOf(role)))
            jwt.createToken(user, role)
        }
        mvc.perform(get(path).header("Authorization", "Bearer $viewerToken")).andExpect(status().isOk)
        review(token = viewerToken).andExpect(status().isForbidden)
        tx { roles.findAll().filter { it.name.startsWith("VIEW_") }.forEach { it.permissions.clear() } }
        mvc.perform(get(path).header("Authorization", "Bearer $viewerToken")).andExpect(status().isForbidden)
    }

    @Test fun `contexto de verificacion no tiene clientes M1 Core ni conexiones AMQP`() {
        assertTrue(context.getBeansOfType(M1AuthClient::class.java).isEmpty())
        assertTrue(context.getBeansOfType(CoreTokenClient::class.java).isEmpty())
        assertTrue(context.getBeansOfType(ConnectionFactory::class.java).isEmpty())
        assertTrue(context.getBeansOfType(CoreOutboxPublisher::class.java).isEmpty())
    }
}
