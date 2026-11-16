package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.dto.CoreEnvelope
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.service.CoreEnvelopeCodec
import com.uade.dda2.server.feature.integration.service.M8ContractCodec
import com.uade.dda2.server.feature.integration.validator.M8PayloadValidator
import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.*

class M8ContractCodecTest {
    private val json = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
    private val codec = M8ContractCodec(json, M8PayloadValidator(Validation.buildDefaultValidatorFactory().validator))
    private val id = UUID.randomUUID()
    private val at = OffsetDateTime.parse("2026-11-02T15:00:00Z")
    private val date = LocalDate.parse("2026-11-02")
    private fun envelope(data: M8Payload): CoreEnvelope {
        val type = M8EventType.fromPayload(data)
        return CoreEnvelope(UUID.randomUUID(), type.wireName, "1.0", at, type.sourceModule, json.valueToTree(data),
            if (data is SocialDocumentationRequestedData || data is SocialBenefitUpdatedData || data is SocialDocumentationDeliveredData) id else null,
            if (data is SocialDocumentationDeliveredData) UUID.randomUUID() else null)
    }
    private fun benefit() = SocialBenefitUpdatedData(id, id, id, id, id, "Edición", id, SocialBenefitEventType.FOOD_ASSISTANCE,
        null, SocialBenefitEventStatus.APPROVED, date, date.plusDays(1), at)

    @Test fun `los diez contratos conservan data tipada y los nombres finales del contrato`() {
        val payloads = listOf(
            SocialDocumentationRequestedData(id, id, id, id, listOf(RequiredDocumentData(id, "DNI", "Documento", true))),
            benefit(),
            UpdateTicketStatusData(id, TicketUpdateType.STARTED, EventActorData(EventActorType.EXTERNAL_USER, null), at),
            TicketUpdatedData(id, "TK-1", null, true, "M8", "ROUTED", "ROUTED", "HIGH", at,
                TicketDetailsData(routing = TicketRoutingData("Ayuda", "Social", "Asistencia", "REQUEST", "Ayuda", "Descripción"))),
            SocialDocumentationDeliveredData(id, id, id, id, listOf(ExternalFileData(id, "EXP-1", "M8", "OPEN",
                listOf(ExternalDocumentationRequestData(id, id, "DNI", ExternalDocumentationStatus.PENDING, null))))),
            CitizenUpdatedData(id, CitizenUpdateType.BLOCKED, CitizenDetailsData(reason = "Revisar", reviewRequired = true), EventActorData(EventActorType.SYSTEM, null)),
            DebtOverdueData(id, id, "ABL", BigDecimal("1.25"), date),
            DebtSettledData(id, null, "ABL", "TRAFFIC_FINE", id, at, BigDecimal.ZERO, BigDecimal.ZERO),
            ExemptionRequestedData(id, id, "ABL", "Solicitud", BigDecimal("50"), date, date),
            UpdateExemptionStatusData(id, id, ExemptionEventStatus.REJECTED, "ABL", reason = "No corresponde"),
        )
        assertEquals(M8EventType.entries.size, payloads.size)
        payloads.forEach { payload ->
            val decoded = codec.decode(envelope(payload))
            assertEquals(payload.javaClass, decoded.javaClass)
            assertEquals(json.writeValueAsString(payload), json.writeValueAsString(decoded))
        }
        assertNull(M8EventType.fromWireName("overdueDebt"))
        assertNull(M8EventType.fromWireName("ticketCreated"))
    }

    @Test fun `campos obligatorios nullables deben existir y los numeros no aceptan coercion`() {
        val event = envelope(benefit())
        val data = json.createObjectNode().apply { setAll(event.data.properties().associate { it.key to it.value }); remove("amount") }
        assertFailsWith<CoreIntegrationException> { codec.decode(event.copy(data = data)) }
        data.put("amount", "1250.50")
        assertFailsWith<CoreIntegrationException> { codec.decode(event.copy(data = data)) }
        data.putNull("amount")
        assertIs<SocialBenefitUpdatedData>(codec.decode(event.copy(data = data)))
        data.put("status", "REJECTED")
        assertFailsWith<CoreIntegrationException> { codec.decode(event.copy(data = data)) }
    }

    @Test fun `reglas condicionales exigen causas mensajes y variantes completas`() {
        assertFailsWith<CoreIntegrationException> { codec.decode(envelope(benefit().copy(status = SocialBenefitEventStatus.SUSPENDED))) }
        assertFailsWith<CoreIntegrationException> { codec.decode(envelope(UpdateExemptionStatusData(id, id, ExemptionEventStatus.APPROVED, "ABL"))) }
        assertFailsWith<CoreIntegrationException> { codec.decode(envelope(UpdateTicketStatusData(id, TicketUpdateType.RESOLVED, EventActorData(EventActorType.SYSTEM, null), at))) }
        assertFailsWith<CoreIntegrationException> { codec.decode(envelope(UpdateTicketStatusData(id, TicketUpdateType.PROGRESS, EventActorData(EventActorType.SYSTEM, null), at))) }
    }

    @Test fun `M2 v179 acepta ROUTED sin opcionales y respuesta de informacion con adjuntos en data`() {
        val attachment = EventAttachmentData("archivo.pdf", "application/pdf", "https://example.org/archivo")
        val data = TicketUpdatedData(id, "TK-1", id, false, "M8", "INFORMATION_PROVIDED", "IN_PROGRESS", "HIGH", at,
            TicketDetailsData(informationResponse = InformationResponseData()), attachments = listOf(attachment))
        assertEquals(data, codec.decode(envelope(data)))
        val serialized = json.valueToTree<tools.jackson.databind.JsonNode>(data)
        assertTrue(serialized.has("isAnonymous"))
        assertFalse(serialized.has("anonymous"))
        assertFalse(serialized["details"]["informationResponse"].has("attachments"))
    }

    @Test fun `sobre de salida tiene ocho claves canonicas como maximo e ignora metadatos de Core en entrada`() {
        val base = envelope(DebtOverdueData(id, id, "ABL", BigDecimal.TEN, date))
        val wire = CoreEnvelopeCodec(json)
        val raw = json.readTree(wire.encode(base))
        assertFalse(raw.has("correlationId") || raw.has("causationId"))
        val augmented = json.createObjectNode().apply { setAll(raw.properties().associate { it.key to it.value }); put("receivedAt", at.toString()) }
        assertEquals(wire.hash(base), wire.hash(wire.decodeIncoming(json.writeValueAsBytes(augmented))))
        assertFailsWith<CoreIntegrationException> { wire.decode(json.writeValueAsBytes(augmented)) }
    }
}
