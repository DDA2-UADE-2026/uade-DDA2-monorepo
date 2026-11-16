package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.CoreEnvelope
import com.uade.dda2.server.feature.integration.dto.CoreInboxResult
import com.uade.dda2.server.feature.integration.dto.CoreMessageMetadata
import com.uade.dda2.server.feature.integration.entity.CoreOutboxStatus
import com.uade.dda2.server.feature.integration.messaging.CoreAmqpPublisher
import com.uade.dda2.server.feature.integration.repository.CoreInboxRepository
import com.uade.dda2.server.feature.integration.repository.CoreOutboxRepository
import com.uade.dda2.server.feature.integration.service.CoreEnvelopeCodec
import com.uade.dda2.server.feature.integration.service.CoreInboxService
import com.uade.dda2.server.feature.integration.service.CoreOutboxService
import com.uade.dda2.server.feature.integration.service.CoreOutboxStore
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.rabbitmq.RabbitMQContainer
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import org.mockito.Mockito.`when`

/** Integration coverage against disposable services only; this test never targets the shared Core. */
@Tag("testcontainers")
@Testcontainers
@SpringBootTest(properties = [
    "app.core.enabled=true",
    "app.core.api-base-url=http://127.0.0.1/api/v1",
    "app.core.machine-secret=test-only-secret",
    "app.core.probe.enabled=true",
    "app.core.probe.run-id=4b8e2f10-6c3a-4d7e-9f21-0a5c3e7d9b12",
    "app.core.publisher.enabled=false",
    "app.core.consumer.enabled=false",
    "app.core.publisher.confirm-timeout=10s",
    "spring.rabbitmq.ssl.enabled=false",
    "spring.rabbitmq.virtual-host=/",
    "spring.rabbitmq.username=guest",
    "spring.rabbitmq.password=guest",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
    "app.security.jwt.secret=testcontainers-only-signing-key-never-use-outside-tests",
    "app.bootstrap.admin.enabled=false",
    "app.benefit-grant.expiration.enabled=false",
    "app.enrollment-period.expiration.cron=-",
])
class CorePostgresRabbitTestcontainersTest {
    @Autowired lateinit var outbox: CoreOutboxRepository
    @Autowired lateinit var inbox: CoreInboxRepository
    @Autowired lateinit var enqueueService: CoreOutboxService
    @Autowired lateinit var outboxStore: CoreOutboxStore
    @Autowired lateinit var inboxService: CoreInboxService
    @Autowired lateinit var codec: CoreEnvelopeCodec
    @Autowired lateinit var properties: CoreProperties
    @Autowired lateinit var manager: PlatformTransactionManager
    @Autowired lateinit var rabbitTemplate: RabbitTemplate
    @Autowired lateinit var json: JsonMapper
    @MockitoBean lateinit var clock: Clock

    private val now = Instant.parse("2026-11-15T15:00:00Z")

    @BeforeEach
    fun reset() {
        `when`(clock.instant()).thenReturn(now)
        inbox.deleteAll()
        outbox.deleteAll()
    }

    private fun newEnvelope() = CoreEnvelope(
        UUID.randomUUID(), properties.probe.eventType, "1.0",
        OffsetDateTime.parse("2026-11-15T12:00:00-03:00"), properties.module,
        json.createObjectNode().put("probe", "core-connectivity").put("runId", properties.probe.runId.toString()),
        properties.probe.runId,
    )

    private fun enqueue(envelope: CoreEnvelope) {
        TransactionTemplate(manager).executeWithoutResult { enqueueService.enqueue(envelope) }
    }

    @Test
    fun `PostgreSQL revierte outbox junto con la transaccion y retiene el evento si el broker no confirma`() {
        val event = newEnvelope()
        assertFailsWith<IllegalStateException> {
            TransactionTemplate(manager).executeWithoutResult {
                enqueueService.enqueue(event)
                error("rollback intencional del test")
            }
        }
        assertFalse(outbox.existsById(event.eventId))

        enqueue(event)
        val lease = assertNotNull(outboxStore.reserveNext())
        outboxStore.retry(lease, "CORE_PUBLISH_UNAVAILABLE")
        val retained = outbox.findById(event.eventId).orElseThrow()
        assertEquals(CoreOutboxStatus.PENDING, retained.status)
        assertEquals(event.eventId, retained.eventId)
    }

    @Test
    fun `PostgreSQL inbox procesa una entrega y deduplica el mismo evento`() {
        val event = newEnvelope()
        val bytes = codec.encode(event).toByteArray()
        assertEquals(CoreInboxResult.PROCESSED, inboxService.process(bytes, CoreMessageMetadata()))
        assertEquals(CoreInboxResult.DUPLICATE, inboxService.process(bytes, CoreMessageMetadata()))
        assertEquals(1L, inbox.count())
    }

    @Test
    fun `RabbitMQ local confirma la publicacion persistente y entrega el mensaje`() {
        val exchange = DirectExchange("muni.inbox", true, false)
        val queue = Queue("q.testcontainers.core", true, false, false)
        val admin = RabbitAdmin(rabbitTemplate.connectionFactory)
        admin.declareExchange(exchange)
        admin.declareQueue(queue)
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(""))

        val lease = com.uade.dda2.server.feature.integration.dto.CoreOutboxLease(
            UUID.randomUUID(), UUID.randomUUID(), "{\"event\":\"testcontainers\"}", UUID.randomUUID().toString(),
        )
        CoreAmqpPublisher(rabbitTemplate, properties, "guest").publish(lease)

        val received: Message = rabbitTemplate.receive(queue.name, 5_000) ?: error("No se recibió el mensaje local")
        assertEquals(lease.eventId.toString(), received.messageProperties.messageId)
        assertEquals(lease.payloadJson, received.body.toString(Charsets.UTF_8))
    }

    companion object {
        @Container
        @JvmField
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("integration_test")
            .withUsername("test")
            .withPassword("test")

        @Container
        @JvmField
        val rabbit = RabbitMQContainer("rabbitmq:3.13-management-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun containerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.rabbitmq.host", rabbit::getHost)
            registry.add("spring.rabbitmq.port", rabbit::getAmqpPort)
        }
    }
}
