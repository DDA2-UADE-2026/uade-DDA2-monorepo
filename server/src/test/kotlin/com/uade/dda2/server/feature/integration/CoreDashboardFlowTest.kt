package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.integration.client.*
import com.uade.dda2.server.feature.integration.dto.*
import com.uade.dda2.server.feature.integration.entity.CoreInbox
import com.uade.dda2.server.feature.integration.entity.CoreOutbox
import com.uade.dda2.server.feature.integration.entity.CoreOutboxStatus
import com.uade.dda2.server.feature.integration.error.CoreHttpException
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.repository.CoreInboxRepository
import com.uade.dda2.server.feature.integration.repository.CoreOutboxRepository
import com.uade.dda2.server.feature.integration.service.CoreDashboardAccess.Companion.CORE_VIEW_PERMISSION
import com.uade.dda2.server.security.JwtService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID
import kotlin.test.*

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:core-dashboard;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
    "app.enrollment-period.expiration.cron=-",
])
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class CoreDashboardFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var core: CoreProperties
    @Autowired lateinit var outbox: CoreOutboxRepository
    @Autowired lateinit var inbox: CoreInboxRepository
    @Autowired lateinit var transactions: PlatformTransactionManager
    @MockitoBean lateinit var dashboard: CoreDashboardClient
    @MockitoBean lateinit var registry: CoreRegistryClient
    @MockitoBean lateinit var events: CoreEventClient
    @MockitoBean lateinit var broker: CoreBrokerClient

    private val root = "/api/admin/integrations/core"
    private lateinit var viewer: User
    private lateinit var viewerRole: Role
    private lateinit var token: String
    private lateinit var noPermission: String

    @BeforeEach fun setup() {
        core.enabled = true
        core.machineSecret = "private-fixture-secret"
        core.publisher.enabled = false
        core.consumer.enabled = false
        inbox.deleteAll()
        outbox.deleteAll()
        TransactionTemplate(transactions).executeWithoutResult {
            val permission = permissions.findAll().firstOrNull { it.name == CORE_VIEW_PERMISSION }
                ?: permissions.save(Permission(name = CORE_VIEW_PERMISSION))
            viewerRole = roles.save(Role(name = "CORE_${UUID.randomUUID()}", permissions = mutableSetOf(permission)))
            val emptyRole = roles.save(Role(name = "EMPTY_${UUID.randomUUID()}"))
            viewer = users.saveAndFlush(User(name = "Operador", email = "${UUID.randomUUID()}@example.com", roles = mutableSetOf(viewerRole)))
            val other = users.saveAndFlush(User(name = "Sin permiso", email = "${UUID.randomUUID()}@example.com", roles = mutableSetOf(emptyRole)))
            token = jwt.createToken(viewer, viewerRole)
            noPermission = jwt.createToken(other, emptyRole)
        }
    }

    private fun request(path: String, credential: String? = token, expected: Int = 200): JsonNode {
        val request = get(root + path)
        credential?.let { request.header("Authorization", "Bearer $it") }
        val result = mvc.perform(request).andReturn().response
        assertEquals(expected, result.status, result.contentAsString)
        assertFalse(result.contentAsString.contains(core.machineSecret.takeIf { it.isNotBlank() } ?: "not-a-secret-value"))
        return json.readTree(result.contentAsString)
    }

    @Test fun `todos los endpoints requieren sesion y permiso antes de consultar Core`() {
        for (path in listOf("/status", "/metrics", "/dashboard", "/publications", "/subscriptions", "/events", "/events/${UUID.randomUUID()}")) {
            request(path, credential = null, expected = 401)
            request(path, credential = noPermission, expected = 403)
        }
        verifyNoInteractions(dashboard, registry, events, broker)
    }

    @Test fun `usuario inactivo rol retirado y permiso revocado invalidan tambien tokens vigentes`() {
        users.saveAndFlush(viewer.apply { active = false })
        request("/status", expected = 401)
        users.saveAndFlush(viewer.apply { active = true; roles.clear() })
        request("/status", expected = 403)
        TransactionTemplate(transactions).executeWithoutResult {
            val user = users.findByIdWithRoles(requireNotNull(viewer.id))!!
            user.roles.add(roles.getReferenceById(requireNotNull(viewerRole.id)))
            roles.findById(requireNotNull(viewerRole.id)).orElseThrow().permissions.clear()
        }
        request("/status", expected = 403)
        verifyNoInteractions(dashboard, registry, events, broker)
    }

    @Test fun `estado distingue integracion desactivada de secret HTTP ausente y no intenta HTTP`() {
        core.enabled = false
        val disabled = request("/status")
        assertEquals("DISABLED", disabled["http"]["state"].asString())
        assertEquals("DISABLED", disabled["amqp"]["state"].asString())
        request("/dashboard", expected = 503)
        core.enabled = true
        core.machineSecret = ""
        `when`(broker.inspectQueue()).thenReturn(CoreQueueState(2, 0))
        val partial = request("/status")
        assertEquals("NOT_CONFIGURED", partial["http"]["state"].asString())
        assertEquals("UP", partial["amqp"]["state"].asString())
        assertEquals(2, partial["amqp"]["pendingMessages"].asInt())
        assertFalse(partial["machineSecretConfigured"].asBoolean())
        verifyNoInteractions(dashboard, registry, events)
    }

    @Test fun `estado comprueba HTTP y AMQPS sin devolver credenciales ni confundir fallos con exito`() {
        `when`(dashboard.dashboard(24)).thenReturn(json.readValue(CoreDashboardFixtures.json(), CoreModuleDashboard::class.java))
        `when`(broker.inspectQueue()).thenReturn(CoreQueueState(0, 0))
        val connected = request("/status")
        assertEquals("UP", connected["http"]["state"].asString())
        assertEquals(200, connected["http"]["upstreamStatus"].asInt())
        assertEquals("UP", connected["amqp"]["state"].asString())
        assertFalse(connected.has("accessToken"))
        `when`(dashboard.dashboard(24)).thenThrow(CoreHttpException(403, "MACHINE_FORBIDDEN", "trace-123"))
        `when`(broker.inspectQueue()).thenThrow(CoreIntegrationException("CORE_TLS_FAILURE"))
        val failed = request("/status")
        assertEquals("DOWN", failed["http"]["state"].asString())
        assertEquals(403, failed["http"]["upstreamStatus"].asInt())
        assertEquals("trace-123", failed["http"]["traceId"].asString())
        assertEquals("DOWN", failed["amqp"]["state"].asString())
    }

    @Test fun `dashboard separa ventana remota de contadores locales y metricas funcionan sin Core`() {
        val created = Instant.parse("2026-11-01T10:00:00Z")
        for ((index, status) in CoreOutboxStatus.entries.withIndex()) {
            outbox.saveAndFlush(CoreOutbox(status = status, createdAt = created.plusSeconds(index.toLong()),
                leaseToken = if (status == CoreOutboxStatus.IN_FLIGHT) UUID.randomUUID() else null,
                leaseUntil = if (status == CoreOutboxStatus.IN_FLIGHT) created.plusSeconds(100) else null))
        }
        inbox.saveAndFlush(CoreInbox())
        `when`(dashboard.dashboard(12)).thenReturn(json.readValue(CoreDashboardFixtures.json(windowHours = 12), CoreModuleDashboard::class.java))
        val result = request("/dashboard?windowHours=12")
        assertEquals(12, result["core"]["windowHours"].asInt())
        assertEquals(8, result["core"]["published"]["total"].asInt())
        for (field in listOf("pending", "inFlight", "brokerConfirmed", "inboxProcessed")) assertEquals(1, result["local"][field].asInt())
        assertEquals(created, Instant.parse(result["local"]["oldestUnconfirmedAt"].asString()))
        core.enabled = false
        assertEquals(1, request("/metrics")["pending"].asInt())
        verify(dashboard).dashboard(12)
        verifyNoInteractions(registry, events, broker)
    }

    @Test fun `filtros invalidos se rechazan antes de consultar red`() {
        for (path in listOf("/dashboard?windowHours=0", "/dashboard?windowHours=721", "/events?page=0", "/events?size=201",
            "/events?eventType=", "/events?status=unknown", "/events?correlationId=bad", "/events/bad")) request(path, expected = 400)
        verifyNoInteractions(dashboard, registry, events, broker)
    }

    @Test fun `consultas de registro y eventos reutilizan clientes sin ejecutar escrituras`() {
        val id = UUID.randomUUID()
        `when`(registry.publications()).thenReturn(listOf(CorePublication(UUID.randomUUID(), core.module, "probe", true)))
        `when`(registry.subscriptions()).thenReturn(listOf(CoreSubscription(UUID.randomUUID(), core.module, "probe", core.queue, 4, true)))
        `when`(events.events(1, 25, null, null, null)).thenReturn(CoreEventPage(emptyList(), 0, 1, 25, 0))
        `when`(events.event(id)).thenReturn(CoreEventDetail(id, "probe", "ROUTED", "AMQP", json.createObjectNode()))
        assertEquals(core.module, request("/publications")[0]["moduleName"].asString())
        assertEquals(core.queue, request("/subscriptions")[0]["queueName"].asString())
        assertEquals(1, request("/events")["page"].asInt())
        assertEquals(id.toString(), request("/events/$id")["eventId"].asString())
        verify(registry).publications()
        verify(registry).subscriptions()
        verify(events).events(1, 25, null, null, null)
        verify(events).event(id)
        verifyNoMoreInteractions(registry, events)
    }

    @Test fun `errores remotos conservan diagnostico y no se presentan como perdida de sesion local`() {
        `when`(registry.publications()).thenThrow(CoreHttpException(401, "INVALID_MODULE_TOKEN"))
        val failure = request("/publications", expected = 502)
        assertEquals("INVALID_MODULE_TOKEN", failure["code"].asString())
        assertContains(failure["message"].asString(), "HTTP 401")
        `when`(registry.subscriptions()).thenThrow(CoreIntegrationException("CORE_HTTP_SECRET_MISSING"))
        assertEquals("CORE_HTTP_SECRET_MISSING", request("/subscriptions", expected = 503)["code"].asString())
        assertEquals("CORE_EVENT_NOT_FOUND", request("/events/${UUID.randomUUID()}", expected = 404)["code"].asString())
    }
}
