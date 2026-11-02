package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.feature.integration.dto.CoreEnvelope
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.service.CoreEnvelopeCodec
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.*

class CoreEnvelopeCodecTest {
    private val json = JsonMapper.builder().findAndAddModules().build()
    private val codec = CoreEnvelopeCodec(json)
    private val envelope = CoreEnvelope(UUID.randomUUID(), "socialCoreConnectivityProbe", "1.0",
        OffsetDateTime.parse("2026-11-02T10:00:00-03:00"), "desarrollo-social", json.readTree("""{"a":1,"b":2}"""))

    @Test fun `usa el sobre propio canonico y preserva el evento`() {
        val wire = codec.encode(envelope)
        assertEquals(envelope, codec.decode(wire.toByteArray()))
        assertTrue(json.readTree(wire).has("occurredAt"))
        assertFalse(json.readTree(wire).has("occuredAt"))
        assertFalse(json.readTree(wire).has("correlationId"))
    }

    @Test fun `rechaza errata de fecha falta de offset y extras de raiz`() {
        val wire = codec.encode(envelope)
        listOf(wire.replace("occurredAt", "occuredAt"), wire.replace("10:00-03:00", "10:00"),
            wire.dropLast(1) + ",\"traceId\":\"extra\"}").forEach {
            assertFailsWith<CoreIntegrationException> { codec.decode(it.toByteArray()) }
        }
    }

    @Test fun `hash ignora orden de propiedades y offsets equivalentes pero detecta cambios`() {
        val other = envelope.copy(occurredAt = OffsetDateTime.parse("2026-11-02T13:00:00Z"),
            data = json.readTree("""{"b":2.0,"a":1}"""))
        assertEquals(codec.hash(envelope), codec.hash(other))
        assertNotEquals(codec.hash(envelope), codec.hash(envelope.copy(data = json.readTree("""{"a":2}"""))))
    }
}
