package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.application.repository.*
import com.uade.dda2.server.feature.application.service.ApplicationDocumentStatesService
import com.uade.dda2.server.feature.auth.entity.*
import com.uade.dda2.server.feature.auth.repository.*
import com.uade.dda2.server.feature.benefit.entity.*
import com.uade.dda2.server.feature.benefit.repository.*
import com.uade.dda2.server.feature.enrollmentperiod.entity.*
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.dto.*
import com.uade.dda2.server.feature.integration.entity.SocialTicketState
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.event.M8BusinessFact
import com.uade.dda2.server.feature.integration.repository.*
import com.uade.dda2.server.feature.integration.service.*
import com.uade.dda2.server.feature.program.entity.*
import com.uade.dda2.server.feature.program.entity.enums.*
import com.uade.dda2.server.feature.program.repository.*
import com.uade.dda2.server.security.JwtService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.time.*
import java.util.UUID
import kotlin.test.*

@SpringBootTest(properties = [
    "app.core.enabled=true", "app.core.business.enabled=true", "app.core.check-on-startup=false",
    "app.core.publisher.enabled=false", "app.core.consumer.enabled=false",
    "app.core.api-base-url=http://127.0.0.1/api/v1",
    "spring.rabbitmq.host=127.0.0.1", "spring.rabbitmq.ssl.enabled=false",
    "spring.rabbitmq.username=test", "spring.rabbitmq.password=test",
    "spring.datasource.url=jdbc:h2:mem:m8-business;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
    "app.enrollment-period.expiration.cron=-",
])
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class M8BusinessFlowTest {
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
    @Autowired lateinit var benefits: ProgramBenefitRepository
    @Autowired lateinit var grants: BenefitGrantRepository
    @Autowired lateinit var changes: BenefitGrantChangeRepository
    @Autowired lateinit var visits: ApplicationVisitRepository
    @Autowired lateinit var snapshots: CitizenSnapshotRepository
    @Autowired lateinit var outbox: CoreOutboxRepository
    @Autowired lateinit var inbox: CoreInboxRepository
    @Autowired lateinit var tickets: SocialTicketRepository
    @Autowired lateinit var debts: RevenueDebtRepository
    @Autowired lateinit var exemptions: RevenueExemptionRepository
    @Autowired lateinit var receipts: ExternalDocumentationReceiptRepository
    @Autowired lateinit var receiver: CoreInboxService
    @Autowired lateinit var wire: CoreEnvelopeCodec
    @Autowired lateinit var codec: M8ContractCodec
    @Autowired lateinit var events: ApplicationEventPublisher
    @Autowired lateinit var policy: M8BusinessPolicy
    @Autowired lateinit var documentStates: ApplicationDocumentStatesService
    @Autowired lateinit var manager: PlatformTransactionManager
    @MockitoBean lateinit var clock: Clock
    private val now = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")).atTime(12, 0).atOffset(ZoneOffset.ofHours(-3))
    private val today = now.toLocalDate()
    private lateinit var f: Fixture

    data class Fixture(val citizenId: UUID, val userId: Long, val workerId: Long, val editionId: UUID, val programId: UUID,
        val periodId: UUID, val applicationId: UUID, val requirementId: UUID, val token: String, val citizenToken: String)

    private fun <T> tx(block: () -> T): T = TransactionTemplate(manager).execute { block() }!!

    @BeforeEach fun setup() {
        `when`(clock.instant()).thenReturn(now.toInstant())
        f = tx {
            fun perm(name: String) = permissions.findByNameIn(listOf(name)).firstOrNull() ?: permissions.save(Permission(name = name))
            val admin = roles.save(Role(name = "M8_${UUID.randomUUID()}", permissions = mutableSetOf(
                perm("tickets:management:view"), perm("tickets:management:manage"), perm("revenue:management:view"),
                perm("revenue:management:manage"), perm("users:view"), perm("applications:management:documents:view"),
                perm("applications:management:documents:review"), perm("applications:management:benefits:assign"),
                perm("applications:management:benefits:manage"), perm("applications:evaluations:create"), perm("applications:resolutions:create"))))
            val own = roles.save(Role(name = "M8_OWN_${UUID.randomUUID()}", permissions = mutableSetOf(perm("applications:own:create"))))
            val worker = users.saveAndFlush(User(name = "Administrativo", email = "m8@test.local", roles = mutableSetOf(admin)))
            val citizenId = UUID.randomUUID()
            val citizen = users.saveAndFlush(User(name = "Titular", email = "citizen@test.local", externalCitizenId = citizenId.toString(), roles = mutableSetOf(own)))
            val program = programs.saveAndFlush(Program(name = "M8 ${UUID.randomUUID()}", createdBy = worker))
            val edition = editions.saveAndFlush(ProgramEdition(program = program, name = "Edición ${UUID.randomUUID()}",
                startDate = today.minusDays(30), endDate = today.plusDays(60), maxCapacity = 10, currentEnrollment = 1,
                status = ProgramEditionStatus.ACTIVE, createdBy = worker))
            val period = periods.saveAndFlush(EnrollmentPeriod(programEdition = edition, openDate = today.minusDays(10), closeDate = today.plusDays(10), status = EnrollmentPeriodStatus.OPEN))
            val application = applications.saveAndFlush(Application(user = citizen, registeredBy = citizen, programEdition = edition,
                enrollmentPeriod = period, submittedAt = today.minusDays(2).atStartOfDay()))
            val requirement = requirements.saveAndFlush(ProgramDocumentRequirement(programEdition = edition, code = "DNI", name = "Documento", required = true))
            Fixture(citizenId, citizen.id!!, worker.id!!, edition.id!!, program.id!!, period.id!!, application.id!!, requirement.id!!,
                jwt.createToken(worker, admin), jwt.createToken(citizen, own))
        }
    }

    private fun message(payload: M8Payload, at: OffsetDateTime = now, correlation: UUID? = null, cause: UUID? = null): CoreEnvelope {
        val type = M8EventType.fromPayload(payload)
        return CoreEnvelope(UUID.randomUUID(), type.wireName, "1.0", at, type.sourceModule, json.valueToTree(payload), correlation, cause)
    }
    private fun receive(event: CoreEnvelope): CoreInboxResult = receiver.process(wire.encode(event).toByteArray(), CoreMessageMetadata())
    private fun send(payload: M8Payload, at: OffsetDateTime = now, correlation: UUID? = null, cause: UUID? = null): CoreEnvelope =
        message(payload, at, correlation, cause).also { receive(it) }
    private fun ticket(id: UUID = UUID.randomUUID(), type: String = "ROUTED", area: String = "M8", at: OffsetDateTime = now,
        anonymous: Boolean = false, reassignment: Boolean = false, details: TicketDetailsData? = null) = TicketUpdatedData(
        ticketId = id, publicId = "TK-2026-123", citizenId = if (anonymous) null else f.citizenId, isAnonymous = anonymous,
        responsibleAreaId = area, updateType = type, currentStatus = if (type == "ROUTED") "ROUTED" else "IN_PROGRESS",
        currentPriority = "HIGH", updatedAt = at, details = details ?: if (type == "ROUTED") TicketDetailsData(routing =
            TicketRoutingData("Solicitud social", "Social", "Asistencia", "REQUEST", "Ayuda", "Necesita asistencia", reassignment = reassignment)) else null)
    private fun out(applicationId: UUID = f.applicationId) = outbox.findAll().filter { it.correlationId == applicationId }
    private fun requestDoc(): CoreEnvelope = tx {
        events.publishEvent(M8BusinessFact.ApplicationSubmitted(applications.findById(f.applicationId).orElseThrow()))
        wire.decode(out().single().payloadJson.toByteArray())
    }
    private fun grant(status: BenefitGrantStatus, type: ProgramBenefitType = ProgramBenefitType.FOOD_ASSISTANCE): UUID = tx {
        val application = applications.findById(f.applicationId).orElseThrow()
        application.status = ApplicationStatus.APPROVED
        val benefit = benefits.saveAndFlush(ProgramBenefit(programEdition = application.programEdition, benefitType = type))
        val grant = grants.saveAndFlush(BenefitGrant(application = application, programBenefit = benefit, benefitType = type,
            description = null, amount = null, startDate = today.minusDays(10), endDate = today.plusDays(20), status = status,
            assignedBy = users.getReferenceById(f.workerId), assignedAt = now.minusDays(5)))
        changes.saveAndFlush(BenefitGrantChange(grant = grant, sequenceNumber = 1, action = BenefitGrantChangeAction.ASSIGNED,
            previousStatus = null, status = BenefitGrantStatus.ACTIVE, previousStartDate = null, previousEndDate = null,
            startDate = grant.startDate, endDate = grant.endDate, reason = null, responsible = users.getReferenceById(f.workerId), changedAt = now.minusDays(5)))
        if (status == BenefitGrantStatus.SUSPENDED) changes.saveAndFlush(BenefitGrantChange(grant = grant, sequenceNumber = 2,
            action = BenefitGrantChangeAction.SUSPENDED, previousStatus = BenefitGrantStatus.ACTIVE, status = BenefitGrantStatus.SUSPENDED,
            previousStartDate = grant.startDate, previousEndDate = grant.endDate, startDate = grant.startDate, endDate = grant.endDate,
            reason = "Revisión", responsible = users.getReferenceById(f.workerId), changedAt = now.minusDays(2)))
        requireNotNull(grant.id)
    }
    private fun api(method: String, path: String, body: Any? = null, token: String? = f.token): Pair<Int, JsonNode?> {
        val builder = when (method) { "GET" -> get(path); "POST" -> post(path); "PUT" -> put(path); else -> patch(path) }
        token?.let { builder.header("Authorization", "Bearer $it") }
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))
        val result = mvc.perform(builder).andReturn().response
        return result.status to result.contentAsString.takeIf { it.isNotBlank() }?.let { json.readTree(it) }
    }
    private fun expect(result: Pair<Int, JsonNode?>, status: Int) { assertEquals(status, result.first, result.second.toString()) }

    @Test fun `la presentacion y el pedido documental son atomicos y requieren vinculacion M1`() {
        tx { applications.deleteById(f.applicationId) }
        tx { users.findById(f.userId).orElseThrow().externalCitizenId = null }
        expect(api("POST", "/api/applications", mapOf("enrollmentPeriodId" to f.periodId), f.citizenToken), 409)
        assertTrue(tx { applications.findIdsByUserAndStatuses(f.userId, ApplicationStatus.entries).isEmpty() })
        assertTrue(out().isEmpty())
        tx { users.findById(f.userId).orElseThrow().externalCitizenId = f.citizenId.toString() }
        val created = api("POST", "/api/applications", mapOf("enrollmentPeriodId" to f.periodId), f.citizenToken)
        expect(created, 201)
        val applicationId = UUID.fromString(created.second!!["id"].asString())
        val payload = out(applicationId).single()
        assertEquals("socialDocumentationRequested", payload.eventType)
        assertEquals(f.requirementId.toString(), json.readTree(payload.payloadJson)["data"]["requiredDocuments"][0]["requirementId"].asString())
    }

    @Test fun `un rollback local nunca deja eventos huerfanos`() {
        assertFailsWith<IllegalStateException> { tx {
            events.publishEvent(M8BusinessFact.ApplicationSubmitted(applications.findById(f.applicationId).orElseThrow()))
            error("cancelar")
        } }
        assertTrue(out().isEmpty())
    }

    @Test fun `broadcast ajeno ignora contenido y el reloj evita cierres o reaperturas por mensajes atrasados`() {
        val id = UUID.randomUUID()
        send(ticket(id, area = "M3", at = now.plusSeconds(3)))
        assertFalse(tickets.existsById(id))
        send(ticket(id, at = now.plusSeconds(1)))
        assertFalse(tickets.existsById(id))
        send(ticket(id, at = now.plusSeconds(4)))
        send(ticket(id, area = "M3", at = now.plusSeconds(2)))
        assertEquals(SocialTicketState.RECEIVED, tickets.findById(id).orElseThrow().state)
        send(ticket(id, area = "M3", at = now.plusSeconds(5)))
        assertEquals(SocialTicketState.CLOSED, tickets.findById(id).orElseThrow().state)
    }

    @Test fun `ticket anonimo gestiona sin usuario y STARTED sale una vez por ciclo de derivacion`() {
        val routed = send(ticket(anonymous = true), correlation = UUID.randomUUID())
        val id = UUID.fromString(routed.data["ticketId"].asString())
        val base = "/api/admin/integrations/core/tickets/$id"
        expect(api("PUT", "$base/assignee", mapOf("workerId" to f.workerId)), 200)
        expect(api("PUT", "$base/assignee", mapOf("workerId" to f.workerId)), 200)
        val first = outbox.findAll().filter { json.readTree(it.payloadJson)["causationId"]?.asString() == routed.eventId.toString() }
        assertEquals(1, first.size)
        assertEquals(routed.correlationId, first.single().correlationId)
        assertTrue(json.readTree(first.single().payloadJson)["data"]["updatedBy"]["id"].isNull)
        expect(api("PUT", "$base/application", mapOf("applicationId" to f.applicationId)), 409)
        send(ticket(id, anonymous = true, reassignment = true, at = now.plusSeconds(1)))
        expect(api("PUT", "$base/assignee", mapOf("workerId" to f.workerId)), 200)
        assertEquals(2, outbox.findAll().count { json.readTree(it.payloadJson)["data"]["ticketId"]?.asString() == id.toString() })
    }

    @Test fun `informacion adicional escalamiento y reapertura respetan la causa y no repiten STARTED`() {
        val routed = send(ticket(), correlation = UUID.randomUUID())
        val id = UUID.fromString(routed.data["ticketId"].asString())
        val base = "/api/admin/integrations/core/tickets/$id"
        expect(api("PUT", "$base/assignee", mapOf("workerId" to f.workerId)), 200)
        val body = mapOf("updateType" to "INFORMATION_REQUIRED", "details" to mapOf("informationRequest" to mapOf("messageForCitizen" to "Adjuntá el comprobante")))
        expect(api("POST", "$base/updates", body), 202)
        expect(api("POST", "$base/updates", body), 409)
        val reply = send(ticket(id, type = "INFORMATION_PROVIDED", at = now.plusSeconds(1), details = TicketDetailsData(
            informationResponse = InformationResponseData("Aquí está el comprobante"))), correlation = UUID.randomUUID())
        expect(api("POST", "$base/updates", mapOf("updateType" to "PROGRESS", "publicMessage" to "Documentación recibida")), 202)
        assertEquals(reply.correlationId, outbox.findAll().single { json.readTree(it.payloadJson)["causationId"]?.asString() == reply.eventId.toString() }.correlationId)
        send(ticket(id, type = "ESCALATION_CHANGED", at = now.plusSeconds(2), details = TicketDetailsData(escalation = TicketEscalationData(true, "MANUAL", now))))
        assertTrue(tickets.findById(id).orElseThrow().requiresAttention)
        assertEquals("MANUAL", json.readTree(tickets.findById(id).orElseThrow().snapshotJson)["escalation"]["reasonCode"].asString())
        expect(api("POST", "$base/updates", mapOf("updateType" to "RESOLVED", "publicMessage" to "Caso evaluado", "details" to mapOf("resolution" to mapOf("type" to "REQUEST_FULFILLED")))), 202)
        send(ticket(id, type = "REOPENED", at = now.plusSeconds(3), details = TicketDetailsData(reopen = TicketReopenData("No se resolvió"))))
        expect(api("PUT", "$base/assignee", mapOf("workerId" to f.workerId)), 200)
        expect(api("POST", "$base/updates", mapOf("updateType" to "STARTED")), 409)
        send(ticket(id, type = "CANCELLED", at = now.plusSeconds(4)))
        expect(api("POST", "$base/updates", mapOf("updateType" to "PROGRESS", "publicMessage" to "avance")), 409)
    }

    @Test fun `reclasificacion no crea gestion ni conserva datos administrativos`() {
        val event = send(ticket(type = "INFORMATION_PROVIDED", details = TicketDetailsData(informationResponse = InformationResponseData(purpose = "RECLASSIFICATION"))))
        assertFalse(tickets.existsById(UUID.fromString(event.data["ticketId"].asString())))
    }

    @Test fun `contratos invalidos fallan sin reservar inbox y Core puede reintentarlos`() {
        val bad = message(ticket()).let { it.copy(sourceModule = "otro-modulo") }
        assertFailsWith<CoreIntegrationException> { receive(bad) }
        assertFalse(inbox.existsById(bad.eventId))
        val valid = bad.copy(sourceModule = "atencion-ciudadana")
        assertEquals(CoreInboxResult.PROCESSED, receive(valid))
        assertEquals(CoreInboxResult.DUPLICATE, receive(valid))
    }

    @Test fun `documentacion COMPLETED exige revision y verifica pertenencia y causalidad`() {
        val requested = requestDoc()
        val requestId = UUID.randomUUID()
        val fileId = UUID.randomUUID()
        fun delivered(status: ExternalDocumentationStatus, documentId: UUID?, citizenId: UUID = f.citizenId) = SocialDocumentationDeliveredData(
            citizenId, f.applicationId, f.programId, f.editionId, listOf(ExternalFileData(fileId, "EXP-2026-1", "M8", "OPEN",
                listOf(ExternalDocumentationRequestData(requestId, f.requirementId, "DNI", status, documentId)))))
        val invalid = message(delivered(ExternalDocumentationStatus.PENDING, null, UUID.randomUUID()), correlation = f.applicationId, cause = requested.eventId)
        assertFailsWith<CoreIntegrationException> { receive(invalid) }
        assertFalse(inbox.existsById(invalid.eventId))
        send(delivered(ExternalDocumentationStatus.PENDING, null), correlation = f.applicationId, cause = requested.eventId)
        assertTrue(tx { documentStates.findStatesByApplicationIdIn(listOf(f.applicationId)).isEmpty() })
        send(delivered(ExternalDocumentationStatus.COMPLETED, UUID.randomUUID()), at = now.plusSeconds(1), correlation = f.applicationId, cause = requested.eventId)
        assertEquals(ApplicationDocumentStatus.PENDING, tx { documentStates.findStatesByApplicationIdIn(listOf(f.applicationId)).single().status })
        expect(api("PATCH", "/api/admin/applications/${f.applicationId}/external-documents/$requestId/review", mapOf("status" to "VALID")), 200)
        assertEquals(ApplicationDocumentStatus.VALID, tx { documentStates.findStatesByApplicationIdIn(listOf(f.applicationId)).single().status })
        send(delivered(ExternalDocumentationStatus.COMPLETED, UUID.randomUUID()), at = now.plusSeconds(2), correlation = f.applicationId, cause = requested.eventId)
        assertEquals(ApplicationDocumentStatus.PENDING, receipts.findById(requestId).orElseThrow().reviewStatus)
    }

    @Test fun `ciudadano desconocido no crea usuarios y bloqueo no suspende beneficios`() {
        val count = users.count()
        send(CitizenUpdatedData(UUID.randomUUID(), CitizenUpdateType.BLOCKED, CitizenDetailsData(reason = "Revisar", reviewRequired = true), EventActorData(EventActorType.SYSTEM, null)))
        assertEquals(count, users.count())
        val grantId = grant(BenefitGrantStatus.ACTIVE)
        send(CitizenUpdatedData(f.citizenId, CitizenUpdateType.BLOCKED, CitizenDetailsData(reason = "Revisar", reviewRequired = true), EventActorData(EventActorType.SYSTEM, null)))
        assertEquals(BenefitGrantStatus.ACTIVE, grants.findById(grantId).orElseThrow().status)
        expect(api("GET", "/api/admin/integrations/core/citizen-alerts"), 200)
        assertFailsWith<com.uade.dda2.server.error.ConflictException> { tx { policy.validateNewApplication(users.findById(f.userId).orElseThrow()) } }
    }

    @Test fun `fallecimiento finaliza activos y suspendidos cierra solicitudes cancela visitas y conserva causa`() {
        val active = grant(BenefitGrantStatus.ACTIVE)
        val suspended = grant(BenefitGrantStatus.SUSPENDED)
        val visitId = tx { visits.saveAndFlush(ApplicationVisit(application = applications.getReferenceById(f.applicationId),
            type = ApplicationVisitType.VISIT, startsAt = now.plusDays(1), endsAt = now.plusDays(1).plusHours(1), location = "Domicilio",
            responsible = users.getReferenceById(f.workerId), scheduledAt = now)).id!! }
        val event = send(CitizenUpdatedData(f.citizenId, CitizenUpdateType.DECEASED,
            CitizenDetailsData(deceasedAt = today.minusDays(1), source = "REGISTRO_CIVIL"), EventActorData(EventActorType.SYSTEM, null)))
        for (id in listOf(active, suspended)) assertEquals(BenefitGrantStatus.FINALIZED, grants.findById(id).orElseThrow().status)
        val app = applications.findById(f.applicationId).orElseThrow()
        assertEquals(ApplicationStatus.CLOSED, app.status)
        assertEquals(event.eventId, app.closedByEventId)
        assertEquals("Fallecimiento del titular", app.closedReason)
        assertEquals(0, editions.findById(f.editionId).orElseThrow().currentEnrollment)
        assertEquals(event.eventId, visits.findById(visitId).orElseThrow().cancelledByEventId)
        val published = out()
        assertEquals(2, published.size)
        assertTrue(published.all { json.readTree(it.payloadJson)["causationId"]?.asString() == event.eventId.toString() })
        assertTrue(published.all { json.readTree(it.payloadJson)["data"]["status"].asString() == "FINALIZED" })
        assertTrue(published.all { json.readTree(it.payloadJson)["data"]["endDate"].asString() == today.minusDays(1).toString() })
        assertEquals(CoreInboxResult.DUPLICATE, receive(event))
        assertEquals(2, out().size)
    }

    @Test fun `Rentas registra contexto sin cambiar elegibilidad y resuelve exenciones fuera de orden`() {
        val debtId = UUID.randomUUID()
        send(DebtOverdueData(debtId, f.citizenId, "ABL", BigDecimal("200.00"), today.minusDays(5)))
        send(DebtSettledData(debtId, null, "ABL", "TRAFFIC_FINE", UUID.randomUUID(), now, BigDecimal.ZERO, BigDecimal.ZERO))
        assertFalse(debts.findById(debtId).orElseThrow().settled)
        send(DebtSettledData(debtId, f.citizenId, "ABL", "MUNICIPAL_TAX", UUID.randomUUID(), now, BigDecimal.ZERO, BigDecimal.ZERO))
        send(DebtOverdueData(debtId, f.citizenId, "ABL", BigDecimal.TEN, today), at = now.plusSeconds(1))
        assertTrue(debts.findById(debtId).orElseThrow().settled)
        val requestId = UUID.randomUUID()
        send(UpdateExemptionStatusData(requestId, f.citizenId, ExemptionEventStatus.REJECTED, "ABL", reason = "No corresponde"))
        send(ExemptionRequestedData(requestId, f.citizenId, "ABL", "Solicitud", BigDecimal("50"), today, today.plusDays(1)), at = now.minusSeconds(1))
        val exemption = exemptions.findById(requestId).orElseThrow()
        assertEquals("REJECTED", exemption.status)
        assertEquals("Solicitud", exemption.requestReason)
        assertEquals(ApplicationStatus.SUBMITTED, applications.findById(f.applicationId).orElseThrow().status)
    }

    @Test fun `exencion ambigua requiere seleccion administrativa de beneficio del mismo titular`() {
        val first = grant(BenefitGrantStatus.ACTIVE, ProgramBenefitType.TAX_EXEMPTION)
        grant(BenefitGrantStatus.ACTIVE, ProgramBenefitType.TAX_EXEMPTION)
        val requestId = UUID.randomUUID()
        send(ExemptionRequestedData(requestId, f.citizenId, "ABL", "Solicitud", BigDecimal("50"), today, today.plusDays(1)))
        assertTrue(exemptions.findById(requestId).orElseThrow().ambiguousBenefit)
        expect(api("PUT", "/api/admin/integrations/core/revenue/exemptions/$requestId/benefit", mapOf("benefitGrantId" to first)), 200)
        assertEquals(first, exemptions.findById(requestId).orElseThrow().benefitGrantId)
    }

    @Test fun `nuevos endpoints exigen autenticacion y permiso del rol activo`() {
        for (path in listOf("/api/admin/integrations/core/tickets", "/api/admin/integrations/core/revenue/debts",
            "/api/admin/integrations/core/revenue/exemptions", "/api/admin/integrations/core/citizen-alerts",
            "/api/admin/applications/${f.applicationId}/external-documents")) {
            expect(api("GET", path, token = null), 401)
            expect(api("GET", path, token = f.citizenToken), 403)
            expect(api("GET", path), 200)
        }
        val routed = send(ticket())
        expect(api("POST", "/api/admin/integrations/core/tickets/${routed.data["ticketId"].asString()}/updates", mapOf("updateType" to "RESOLVED")), 400)
    }
    @Test fun `otorgamiento suspension renovacion y finalizacion publican un evento por cambio del mismo beneficio`() {
        val benefitId = tx {
            requirements.deleteById(f.requirementId)
            val application = applications.findById(f.applicationId).orElseThrow()
            application.status = ApplicationStatus.APPROVED
            benefits.saveAndFlush(ProgramBenefit(programEdition = application.programEdition, benefitType = ProgramBenefitType.FOOD_ASSISTANCE)).id!!
        }
        val base = "/api/admin/applications/${f.applicationId}/benefit-grants"
        val created = api("POST", base, mapOf("programBenefitId" to benefitId, "startDate" to today.minusDays(1), "endDate" to today.plusDays(10)))
        expect(created, 201)
        val grantId = created.second!!["id"].asString()
        expect(api("POST", "$base/$grantId/suspend", mapOf("reason" to "Revisión administrativa")), 200)
        expect(api("POST", "$base/$grantId/renew", mapOf("reason" to "Continúa la necesidad", "endDate" to today.plusDays(20), "requirementChecks" to emptyList<Any>())), 200)
        expect(api("POST", "$base/$grantId/finalize", mapOf("reason" to "Terminó la asistencia")), 200)
        val data = out().map { json.readTree(it.payloadJson)["data"] }
        assertEquals(4, data.size)
        assertEquals(2, data.count { it["status"].asString() == "APPROVED" })
        assertEquals(1, data.count { it["status"].asString() == "SUSPENDED" })
        assertEquals(1, data.count { it["status"].asString() == "FINALIZED" })
        assertTrue(data.all { it["benefitId"].asString() == grantId && it.has("amount") && it["amount"].isNull })
        assertTrue(data.filter { it["status"].asString() == "APPROVED" }.all { it["reason"].isNull })
    }

    @Test fun `solicitud vinculada emite inicio visita reprogramacion y resolucion sin duplicar gestion`() {
        val routed = send(ticket(), correlation = UUID.randomUUID())
        val ticketId = UUID.fromString(routed.data["ticketId"].asString())
        val base = "/api/admin/integrations/core/tickets/$ticketId"
        expect(api("PUT", "$base/application", mapOf("applicationId" to f.applicationId)), 200)
        tx {
            val application = applications.findById(f.applicationId).orElseThrow()
            val worker = users.getReferenceById(f.workerId)
            application.assignedWorker = worker
            events.publishEvent(M8BusinessFact.WorkerAssigned(application))
            val visit = visits.saveAndFlush(ApplicationVisit(application = application, type = ApplicationVisitType.VISIT,
                startsAt = now.plusDays(1), endsAt = now.plusDays(1).plusHours(1), location = "Centro", responsible = worker, scheduledAt = now))
            events.publishEvent(M8BusinessFact.VisitScheduled(visit))
            visit.startsAt = now.plusDays(2)
            visit.endsAt = now.plusDays(2).plusHours(1)
            events.publishEvent(M8BusinessFact.VisitScheduled(visit))
            events.publishEvent(M8BusinessFact.ApplicationResolved(ApplicationResolution(application = application,
                previousStatus = ApplicationStatus.IN_EVALUATION, decision = ApplicationResolutionDecision.REJECTED,
                reason = "Motivo privado", resolvedBy = worker, resolvedAt = now)))
        }
        val updates = outbox.findAll().filter { json.readTree(it.payloadJson)["data"]["ticketId"]?.asString() == ticketId.toString() }
        assertEquals(4, updates.size)
        assertEquals(setOf("STARTED", "PROGRESS", "RESOLVED"), updates.map { json.readTree(it.payloadJson)["data"]["updateType"].asString() }.toSet())
        assertEquals(SocialTicketState.RESOLVED, tickets.findById(ticketId).orElseThrow().state)
        assertTrue(updates.none { it.payloadJson.contains("Motivo privado") })
    }

    @Test fun `fallecimiento y respuestas se revierten juntos si falla la transaccion`() {
        val grantId = grant(BenefitGrantStatus.ACTIVE)
        val event = message(CitizenUpdatedData(f.citizenId, CitizenUpdateType.DECEASED,
            CitizenDetailsData(deceasedAt = today, source = "REGISTRO_CIVIL"), EventActorData(EventActorType.SYSTEM, null)))
        assertFailsWith<IllegalStateException> { tx { receive(event); error("fallo posterior") } }
        assertFalse(inbox.existsById(event.eventId))
        assertEquals(BenefitGrantStatus.ACTIVE, grants.findById(grantId).orElseThrow().status)
        assertEquals(ApplicationStatus.APPROVED, applications.findById(f.applicationId).orElseThrow().status)
        assertTrue(out().isEmpty())
        assertEquals(CoreInboxResult.PROCESSED, receive(event))
        assertEquals(1, out().size)
    }

    @Test fun `domicilio tiene reloj independiente y un REGISTERED atrasado no lo pisa`() {
        val first = CitizenAddressData(UUID.randomUUID(), "Calle vieja", "1")
        val second = CitizenAddressData(UUID.randomUUID(), "Calle nueva", "2")
        send(CitizenUpdatedData(f.citizenId, CitizenUpdateType.ADDRESS_UPDATED,
            CitizenDetailsData(previousAddress = first, newAddress = second), EventActorData(EventActorType.AREA_USER, null)), at = now.plusSeconds(1))
        send(CitizenUpdatedData(f.citizenId, CitizenUpdateType.REGISTERED,
            CitizenDetailsData(dni = "12345678", cuil = "20123456789", name = "Nombre", lastname = "Apellido", birthdate = today.minusYears(30),
                state = "ACTIVE", nationality = "AR", address = first), EventActorData(EventActorType.SELF_SERVICE, f.citizenId)), at = now)
        val snapshot = snapshots.findById(f.userId).orElseThrow()
        assertEquals("Calle nueva 2", snapshot.address)
        assertEquals("Nombre Apellido", snapshot.fullName)
    }

    @Test fun `evaluacion automatica y endpoint anterior de resolucion actualizan el ticket vinculado`() {
        val routed = send(ticket(), correlation = UUID.randomUUID())
        val ticketId = UUID.fromString(routed.data["ticketId"].asString())
        expect(api("PUT", "/api/admin/integrations/core/tickets/$ticketId/application", mapOf("applicationId" to f.applicationId)), 200)
        expect(api("POST", "/api/applications/${f.applicationId}/evaluation", mapOf("vulnerability" to "HIGH", "priority" to "URGENT", "recommendation" to "Recomendación privada")), 201)
        expect(api("POST", "/api/admin/applications/${f.applicationId}/resolution", mapOf("decision" to "REJECTED", "reason" to "Motivo privado")), 200)
        val updates = outbox.findAll().filter { json.readTree(it.payloadJson)["data"]["ticketId"]?.asString() == ticketId.toString() }
        assertEquals(2, updates.size)
        assertEquals(setOf("STARTED", "RESOLVED"), updates.map { json.readTree(it.payloadJson)["data"]["updateType"].asString() }.toSet())
        assertTrue(updates.none { it.payloadJson.contains("privad") })
    }

}
