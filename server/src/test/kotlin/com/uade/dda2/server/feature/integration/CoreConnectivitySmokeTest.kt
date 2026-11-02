package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.client.*
import com.uade.dda2.server.feature.integration.dto.CoreEnvelope
import com.uade.dda2.server.feature.integration.entity.CoreOutboxStatus
import com.uade.dda2.server.feature.integration.messaging.CoreAmqpPublisher
import com.uade.dda2.server.feature.integration.repository.*
import com.uade.dda2.server.feature.integration.service.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.*

@Tag("core-smoke")
@EnabledIfEnvironmentVariable(named = "CORE_SMOKE_ENABLED", matches = "true")
@SpringBootTest(properties = ["app.core.enabled=true", "app.core.probe.enabled=true", "app.core.consumer.enabled=false",
    "app.core.publisher.enabled=false", "app.bootstrap.admin.enabled=false", "app.enrollment-period.expiration.cron=-",
    "app.benefit-grant.expiration.enabled=false", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("docs")
class CoreConnectivitySmokeTest {
    @Autowired lateinit var properties: CoreProperties
    @Autowired lateinit var transport: CoreHttpTransport
    @Autowired lateinit var registry: CoreRegistryClient
    @Autowired lateinit var events: CoreEventClient
    @Autowired lateinit var broker: CoreBrokerClient
    @Autowired lateinit var container: SimpleMessageListenerContainer
    @Autowired lateinit var probe: CoreProbeHandler
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var enqueue: CoreOutboxService
    @Autowired lateinit var store: CoreOutboxStore
    @Autowired lateinit var sender: CoreAmqpPublisher
    @Autowired lateinit var outbox: CoreOutboxRepository
    @Autowired lateinit var inbox: CoreInboxRepository
    @Autowired lateinit var transactions: PlatformTransactionManager

    companion object {
        @JvmStatic @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) {
            require(System.getenv("CORE_SMOKE_DB_CONFIRMED_DISPOSABLE") == "true") { "Confirmar una base técnica desechable con CORE_SMOKE_DB_CONFIRMED_DISPOSABLE=true." }
            val url = requireNotNull(System.getenv("CORE_SMOKE_DB_URL")) { "Falta CORE_SMOKE_DB_URL." }
            require(Regex("jdbc:postgresql://[a-zA-Z0-9.\\-\\[\\]:]+/core_smoke_[a-z0-9_]+").matches(url)) {
                "La base técnica debe llamarse core_smoke_<nombre>, sin parámetros extra en su URL."
            }
            require(url != System.getenv("DB_URL")) { "La prueba no puede usar DB_URL del backend." }
            registry.add("spring.datasource.url") { url }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.PostgreSQLDialect" }
            registry.add("spring.datasource.username") { requireNotNull(System.getenv("CORE_SMOKE_DB_USERNAME")) }
            registry.add("spring.datasource.password") { requireNotNull(System.getenv("CORE_SMOKE_DB_PASSWORD")) }
            registry.add("spring.sql.init.mode") { "always" }
            registry.add("spring.sql.init.schema-locations") { Path.of("docs/database/core-integration.sql").toAbsolutePath().toUri().toString() }
        }
    }

    @Test fun `publica por AMQPS y recibe desde Core sin integrar otros modulos`() {
        val eventId = UUID.fromString(requireNotNull(System.getenv("CORE_SMOKE_EVENT_ID")) { "Falta CORE_SMOKE_EVENT_ID." })
        assertEquals(4, eventId.version(), "Generar un UUIDv4 para esta prueba.")
        val type = properties.probe.eventType
        val ready = transport.readiness()
        assertEquals("up", ready.get("status").asString(), "Core no está listo según el cuerpo de readiness.")
        val register = System.getenv("CORE_SMOKE_REGISTER") == "true"
        val expectedSchema = probe.schema(json)
        val declared = registry.eventType(type) ?: run {
            check(register) { "Falta el tipo técnico. CORE_SMOKE_REGISTER=true permite su alta deliberada tras acordarlo con Core." }
            registry.declareType(type, expectedSchema)
        }
        assertEquals(properties.module, declared.ownerModule)
        assertEquals(expectedSchema, declared.jsonSchema, "No se modifica automáticamente un schema distinto.")
        val publication = registry.publications().firstOrNull { it.eventType == type } ?: run {
            check(register) { "Falta la declaración de publicación." }; registry.declarePublication(type)
        }
        assertTrue(publication.active)
        val subscription = registry.subscriptions().firstOrNull { it.eventType == type } ?: run {
            check(register) { "Falta la suscripción técnica." }; registry.subscribe(type)
        }
        assertTrue(subscription.active)
        assertEquals(properties.queue, subscription.queueName)
        assertEquals(4, subscription.maxAttempts)
        assertTrue(registry.subscriptions().none { it.active && it.eventType != type },
            "Resolver con Core las suscripciones de negocio antes de consumir la cola compartida.")
        val mapping = registry.eventMap().firstOrNull { it.get("eventType").asString() == type }
        val receivers = assertNotNull(mapping).get("consumedBy").asSequence().map { it.asString() }.toSet()
        assertEquals(setOf(properties.module), receivers)
        val queue = broker.inspectQueue()
        assertEquals(0, queue.messageCount, "Resolver el backlog con Core; la prueba no purga mensajes.")
        assertEquals(0, queue.consumerCount, "La prueba necesita la cola sin otros consumidores M8 compitiendo por el mensaje.")
        assertFalse(outbox.existsById(eventId), "Usar UUID nuevo; la retransmisión es una prueba aparte.")
        assertFalse(inbox.existsById(eventId))
        val envelope = CoreEnvelope(eventId, type, "1.0", OffsetDateTime.now(ZoneOffset.UTC), properties.module,
            json.createObjectNode().put("probe", "core-connectivity").put("runId", properties.probe.runId.toString()), properties.probe.runId)
        try {
            container.start()
            TransactionTemplate(transactions).executeWithoutResult { enqueue.enqueue(envelope) }
            CoreOutboxPublisher(store, sender, properties).publishPending()
            assertEquals(CoreOutboxStatus.BROKER_CONFIRMED, outbox.findById(eventId).orElseThrow().status)
            await { inbox.existsById(eventId) }
            await { events.event(eventId)?.deliveries?.any { it.targetModule == properties.module } == true }
            val event = assertNotNull(events.event(eventId))
            assertEquals("AMQP", event.ingestionChannel)
            assertEquals("ROUTED", event.status)
            assertEquals(eventId.toString(), inbox.findById(eventId).orElseThrow().messageId)
            println("Core smoke correcto: eventId=$eventId correlationId=${properties.probe.runId}; evidencia adicional en outbox/inbox y Core.")
        } finally { container.stop() }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos()
        while (!condition()) {
            check(System.nanoTime() < deadline) { "Timeout esperando evidencia de Core o inbox local." }
            Thread.sleep(250)
        }
    }
}
