package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.*
import com.uade.dda2.server.feature.integration.entity.CoreOutboxStatus
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.repository.*
import com.uade.dda2.server.feature.integration.service.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.*

@SpringBootTest(properties = [
    "app.core.enabled=true", "app.core.api-base-url=http://127.0.0.1/api/v1", "app.core.machine-secret=test-only-secret",
    "app.core.probe.enabled=true", "app.core.probe.run-id=4b8e2f10-6c3a-4d7e-9f21-0a5c3e7d9b12",
    "app.core.publisher.enabled=false", "app.core.consumer.enabled=false",
    "spring.rabbitmq.host=127.0.0.1", "spring.rabbitmq.ssl.enabled=false",
    "spring.rabbitmq.username=test", "spring.rabbitmq.password=test",
    "spring.datasource.url=jdbc:h2:mem:core-technical;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
    "app.enrollment-period.expiration.cron=-",
])
@ActiveProfiles("docs")
class CorePersistenceTest {
    @Autowired lateinit var outbox: CoreOutboxRepository
    @Autowired lateinit var inbox: CoreInboxRepository
    @Autowired lateinit var enqueue: CoreOutboxService
    @Autowired lateinit var store: CoreOutboxStore
    @Autowired lateinit var receiver: CoreInboxService
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var properties: CoreProperties
    @Autowired lateinit var manager: PlatformTransactionManager
    @MockitoBean lateinit var clock: Clock
    private var now = Instant.parse("2026-11-02T15:00:00Z")

    @BeforeEach fun reset() {
        now = Instant.parse("2026-11-02T15:00:00Z")
        `when`(clock.instant()).thenAnswer { now }
        inbox.deleteAll()
        outbox.deleteAll()
    }

    private fun envelope() = CoreEnvelope(UUID.randomUUID(), properties.probe.eventType, "1.0",
        OffsetDateTime.parse("2026-11-02T12:00:00-03:00"), properties.module,
        json.createObjectNode().put("probe", "core-connectivity").put("runId", properties.probe.runId.toString()), properties.probe.runId)

    private fun enqueue(event: CoreEnvelope) { TransactionTemplate(manager).executeWithoutResult { enqueue.enqueue(event) } }

    @Test fun `rollback del llamador revierte tambien la outbox`() {
        val event = envelope()
        assertFailsWith<IllegalStateException> {
            TransactionTemplate(manager).executeWithoutResult { enqueue.enqueue(event); error("rollback") }
        }
        assertFalse(outbox.existsById(event.eventId))
    }

    @Test fun `reserva vencida conserva evento y callback viejo no confirma intento nuevo`() {
        val event = envelope()
        enqueue(event)
        val old = assertNotNull(store.reserveNext())
        assertNull(store.reserveNext())
        now = now.plusSeconds(61)
        val fresh = assertNotNull(store.reserveNext())
        assertEquals(old.eventId, fresh.eventId)
        assertEquals(old.payloadJson, fresh.payloadJson)
        assertNotEquals(old.token, fresh.token)
        assertFalse(store.confirm(old))
        assertTrue(store.confirm(fresh))
        assertEquals(CoreOutboxStatus.BROKER_CONFIRMED, outbox.findById(event.eventId).orElseThrow().status)
    }

    @Test fun `dos publicadores solo reservan una vez la misma fila`() {
        enqueue(envelope())
        val pool = Executors.newFixedThreadPool(2)
        try {
            val claims = pool.invokeAll(listOf(Callable { store.reserveNext() }, Callable { store.reserveNext() }))
            assertEquals(1, claims.map { it.get() }.count { it != null })
        } finally { pool.shutdownNow() }
    }

    @Test fun `inbox deduplica y rechaza UUID reutilizado con otro contenido`() {
        val event = envelope()
        val codec = CoreEnvelopeCodec(json)
        val wire = codec.encode(event).toByteArray()
        assertEquals(CoreInboxResult.PROCESSED, receiver.process(wire, CoreMessageMetadata()))
        assertEquals(CoreInboxResult.DUPLICATE, receiver.process(wire, CoreMessageMetadata()))
        assertEquals(1L, inbox.count())
        assertFailsWith<CoreIntegrationException> {
            receiver.process(codec.encode(event.copy(causationId = UUID.randomUUID())).toByteArray(), CoreMessageMetadata())
        }
        assertEquals(1L, inbox.count())
    }

    @Test fun `fallo del handler revierte la reserva de inbox y permite reintento`() {
        val event = envelope()
        val codec = CoreEnvelopeCodec(json)
        val invalid = event.copy(data = json.createObjectNode().put("probe", "wrong").put("runId", properties.probe.runId.toString()))
        assertFailsWith<CoreIntegrationException> { receiver.process(codec.encode(invalid).toByteArray(), CoreMessageMetadata()) }
        assertFalse(inbox.existsById(event.eventId))
        assertEquals(CoreInboxResult.PROCESSED, receiver.process(codec.encode(event).toByteArray(), CoreMessageMetadata()))
    }
}
