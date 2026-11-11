package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.error.ApiException
import com.uade.dda2.server.feature.integration.client.*
import com.uade.dda2.server.feature.integration.dto.*
import com.uade.dda2.server.feature.integration.error.*
import com.uade.dda2.server.feature.integration.service.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.mockito.Mockito.*
import org.springframework.beans.factory.support.StaticListableBeanFactory
import org.springframework.http.HttpStatus
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.*

class CoreDashboardBranchTest {
    private class Fixture(available: Boolean = true) {
        val properties = CoreProperties().apply { enabled = true; machineSecret = "test-only" }
        val access = mock(CoreDashboardAccess::class.java)
        val local = mock(CoreLocalMetricsService::class.java)
        val dashboard = mock(CoreDashboardClient::class.java)
        val registry = mock(CoreRegistryClient::class.java)
        val events = mock(CoreEventClient::class.java)
        val broker = mock(CoreBrokerClient::class.java)
        val transport = mock(CoreHttpTransport::class.java)
        private val factory = StaticListableBeanFactory().apply {
            if (available) {
                addBean("dashboard", dashboard); addBean("registry", registry); addBean("events", events)
                addBean("broker", broker); addBean("transport", transport)
            }
        }
        val service = CoreDashboardService(properties, access, local,
            factory.getBeanProvider(CoreDashboardClient::class.java), factory.getBeanProvider(CoreRegistryClient::class.java),
            factory.getBeanProvider(CoreEventClient::class.java), factory.getBeanProvider(CoreBrokerClient::class.java),
            factory.getBeanProvider(CoreHttpTransport::class.java), Clock.fixed(Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC))
        init {
            `when`(transport.readiness()).thenReturn(JsonMapper.builder().build().readTree("{\"status\":\"up\"}"))
            `when`(broker.inspectQueue()).thenReturn(CoreQueueState(2, 1))
        }
    }

    @TestFactory
    fun `readiness uses sanitized body statuses not just HTTP success`() = listOf(
        "\"OK\"" to CoreConnectionState.UP,
        "{\"status\":\"ready\"}" to CoreConnectionState.UP,
        "{\"status\":\"degraded\"}" to CoreConnectionState.DEGRADED,
        "{\"status\":\"down\"}" to CoreConnectionState.DOWN,
        "{\"status\":5}" to CoreConnectionState.DOWN,
        "{\"status\":\"bad status with spaces\"}" to CoreConnectionState.DOWN,
        "{}" to CoreConnectionState.DOWN,
        "{\"status\":\"up\",\"checks\":[]}" to CoreConnectionState.UP,
    ).map { (body, expected) -> dynamicTest(body) {
        val f = Fixture()
        `when`(f.transport.readiness()).thenReturn(JsonMapper.builder().build().readTree(body))
        val result = f.service.status()
        assertEquals(expected, result.readiness.state)
        assertEquals(CoreConnectionState.UP, result.http.state)
        assertEquals(2, result.amqp.pendingMessages)
        verify(f.access).check()
    } }

    @Test
    fun `readiness filters malformed dependency names and values and limits the summary`() {
        val f = Fixture()
        val checks = (1..12).joinToString(",") { "\"check$it\":\"up\"" }
        val body = """{"status":"up","checks":{"bad name":"up","invalid":null,"number":3,"object":{"status":"ready"},$checks}}"""
        `when`(f.transport.readiness()).thenReturn(JsonMapper.builder().build().readTree(body))
        val result = f.service.status().readiness
        assertEquals(10, result.checks.size)
        assertEquals("ready", result.checks["object"])
        assertFalse(result.checks.containsKey("bad name"))
        assertFalse(result.checks.containsKey("number"))
    }

    @TestFactory
    fun `status isolates upstream failures and sanitizes trace ids`() = listOf<String?>(null, "trace-valid:1", "secret with spaces").map { trace -> dynamicTest("trace=$trace") {
        val f = Fixture()
        `when`(f.transport.readiness()).thenThrow(CoreHttpException(503, "CORE_UNAVAILABLE", trace))
        `when`(f.dashboard.dashboard(24)).thenThrow(CoreHttpException(401, "CORE_DENIED", trace))
        `when`(f.broker.inspectQueue()).thenThrow(CoreIntegrationException("CORE_QUEUE_UNAVAILABLE"))
        val result = f.service.status()
        assertEquals(CoreConnectionState.DOWN, result.readiness.state)
        assertEquals(503, result.readiness.upstreamStatus)
        assertEquals("CORE_DENIED", result.http.code)
        assertEquals(if (trace == "trace-valid:1") trace else null, result.http.traceId)
        assertEquals(result.http.traceId, result.readiness.traceId)
        assertEquals("CORE_QUEUE_UNAVAILABLE", result.amqp.code)
    } }

    @Test
    fun `unexpected failures expose stable fallback codes`() {
        val f = Fixture()
        `when`(f.transport.readiness()).thenThrow(IllegalStateException("secret"))
        `when`(f.dashboard.dashboard(24)).thenThrow(IllegalStateException("secret"))
        `when`(f.broker.inspectQueue()).thenThrow(IllegalStateException("secret"))
        val result = f.service.status()
        assertEquals("CORE_READINESS_CHECK_FAILED", result.readiness.code)
        assertEquals("CORE_HTTP_CHECK_FAILED", result.http.code)
        assertEquals("CORE_AMQP_CHECK_FAILED", result.amqp.code)
    }

    @Test
    fun `missing machine secret does not disable public readiness or broker checks`() {
        val f = Fixture()
        f.properties.machineSecret = " "
        val result = f.service.status()
        assertEquals(CoreConnectionState.NOT_CONFIGURED, result.http.state)
        assertEquals(CoreConnectionState.UP, result.readiness.state)
        verifyNoInteractions(f.dashboard)
        f.properties.enabled = false
        val disabled = f.service.status()
        assertEquals(CoreConnectionState.DISABLED, disabled.readiness.state)
        assertEquals(CoreConnectionState.DISABLED, disabled.http.state)
        assertEquals(CoreConnectionState.DISABLED, disabled.amqp.state)
        assertFalse(disabled.publisherEnabled)
        assertFalse(disabled.consumerEnabled)
    }

    @TestFactory
    fun `remote errors retain safe codes and appropriate gateway status`() = listOf(
        CoreHttpException(403, "DENIED") to HttpStatus.BAD_GATEWAY,
        CoreIntegrationException("CORE_INVALID_BODY") to HttpStatus.BAD_GATEWAY,
        CoreIntegrationException("CORE_DASHBOARD_IDENTITY_MISMATCH") to HttpStatus.BAD_GATEWAY,
        CoreIntegrationException("CORE_UNAVAILABLE") to HttpStatus.SERVICE_UNAVAILABLE,
        IllegalStateException("secret") to HttpStatus.BAD_GATEWAY,
    ).map { (failure, expected) -> dynamicTest(failure.javaClass.simpleName + failure.message) {
        val f = Fixture()
        `when`(f.registry.publications()).thenThrow(failure)
        val error = assertFailsWith<ApiException> { f.service.publications() }
        assertEquals(expected, error.status)
        assertEquals((failure as? CoreIntegrationException)?.code ?: "CORE_INVALID_RESPONSE", error.code)
        assertFalse(error.message.contains("secret"))
    } }

    @Test
    fun `unavailable and disabled clients return service unavailable`() {
        val f = Fixture(available = false)
        assertEquals("CORE_CLIENT_UNAVAILABLE", assertFailsWith<ApiException> { f.service.subscriptions() }.code)
        f.properties.enabled = false
        assertEquals("CORE_DISABLED", assertFailsWith<ApiException> { f.service.publications() }.code)
    }

    @TestFactory
    fun `invalid event filters are rejected before remote access`() = listOf(
        Triple(0, 10, null), Triple(1, 0, null), Triple(1, 201, null), Triple(1, 10, " "), Triple(1, 10, "a".repeat(121)),
    ).map { (page, size, type) -> dynamicTest("$page $size $type") {
        val f = Fixture()
        assertEquals("CORE_INVALID_EVENT_FILTER", assertFailsWith<ApiException> { f.service.events(page, size, type, null, null) }.code)
        verifyNoInteractions(f.events)
    } }

    @Test
    fun `boundary filters are forwarded and missing events return not found`() {
        val f = Fixture()
        for ((size, type) in listOf(1 to null, 200 to "a".repeat(120))) {
            val page = CoreEventPage(emptyList(), 0, 1, size, 0)
            `when`(f.events.events(1, size, type, null, null)).thenReturn(page)
            assertSame(page, f.service.events(1, size, type, null, null))
        }
        assertEquals("CORE_EVENT_NOT_FOUND", assertFailsWith<ApiException> { f.service.event(UUID.randomUUID()) }.code)
        for (window in listOf(0, 721)) assertEquals("CORE_INVALID_WINDOW", assertFailsWith<ApiException> { f.service.dashboard(window) }.code)
    }
}
