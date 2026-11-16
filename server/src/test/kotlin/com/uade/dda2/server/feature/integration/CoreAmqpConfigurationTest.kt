package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.config.CoreAmqpConfiguration
import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.service.CoreInboxService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.core.Message
import org.springframework.boot.amqp.autoconfigure.RabbitProperties
import org.springframework.test.util.ReflectionTestUtils
import java.time.Duration
import javax.net.ssl.SSLSocket
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Direct bean construction only: no Spring context, connection, listener start or TLS handshake. */
class CoreAmqpConfigurationTest {
    private val configuration = CoreAmqpConfiguration()

    private fun rabbit(tls: Boolean = true) = RabbitProperties().apply {
        host = "broker.example.invalid"
        port = 5679
        username = "fake-core-user"
        password = "fake-core-password"
        virtualHost = "/fake-core"
        ssl.setEnabled(tls)
        ssl.isValidateServerCertificate = true
        ssl.isVerifyHostname = true
    }

    private fun core() = CoreProperties().apply {
        apiBaseUrl = "https://api.example.invalid/api/v1"
        queue = "q.fake-core"
        connectTimeout = Duration.ofMillis(1234)
        readTimeout = Duration.ofMillis(4321)
        consumer.prefetch = 7
    }

    @Test fun `remote broker requires TLS`() {
        val error = assertFailsWith<IllegalArgumentException> {
            configuration.coreConnectionFactory(rabbit(tls = false), core())
        }
        assertEquals("Core remoto requiere AMQPS.", error.message)
    }

    @Test fun `certificate validation cannot be disabled`() {
        val properties = rabbit().apply { ssl.isValidateServerCertificate = false }
        val error = assertFailsWith<IllegalArgumentException> {
            configuration.coreConnectionFactory(properties, core())
        }
        assertEquals("No se puede desactivar la validación TLS de Core.", error.message)
    }

    @Test fun `hostname verification cannot be disabled`() {
        val properties = rabbit().apply { ssl.isVerifyHostname = false }
        val error = assertFailsWith<IllegalArgumentException> {
            configuration.coreConnectionFactory(properties, core())
        }
        assertEquals("No se puede desactivar la validación TLS de Core.", error.message)
    }

    @Test fun `factory preserves fake target and bounded timeouts with Spring managed recovery`() {
        val factory = configuration.coreConnectionFactory(rabbit(), core())
        try {
            val native = factory.rabbitConnectionFactory
            assertEquals("broker.example.invalid", native.host)
            assertEquals(5679, native.port)
            assertEquals("fake-core-user", native.username)
            assertEquals("fake-core-password", native.password)
            assertEquals("/fake-core", native.virtualHost)
            assertTrue(native.isSSL)
            assertEquals(1234, native.connectionTimeout)
            assertEquals(4321, native.handshakeTimeout)
            assertEquals(4321, native.channelRpcTimeout)
            assertEquals(30, native.requestedHeartbeat)
            assertFalse(native.isAutomaticRecoveryEnabled)
            assertTrue(factory.isPublisherConfirms)
            assertFalse(factory.isSimplePublisherConfirms)
            assertTrue(factory.isPublisherReturns)
        } finally {
            factory.destroy()
        }
    }

    @Test fun `template requires returns for unroutable messages`() {
        val factory = configuration.coreConnectionFactory(rabbit(), core())
        try {
            val template = configuration.coreRabbitTemplate(factory)
            assertTrue(template.isMandatoryFor(Message(byteArrayOf())) == true)
        } finally {
            factory.destroy()
        }
    }

    @Test fun `listener uses manual acknowledgements and bounded prefetch without declaring or starting`() {
        val factory = configuration.coreConnectionFactory(rabbit(), core())
        val inbox = mock(CoreInboxService::class.java)
        try {
            val properties = core()
            assertFalse(properties.consumer.enabled)
            val container = configuration.coreListenerContainer(factory, inbox, properties)
            assertEquals(listOf("q.fake-core"), container.queueNames.toList())
            assertEquals(AcknowledgeMode.MANUAL, container.acknowledgeMode)
            // These Spring getters are protected; invoke them without initializing the container.
            assertEquals(7, ReflectionTestUtils.invokeMethod<Int>(container, "getPrefetchCount"))
            assertEquals(false, ReflectionTestUtils.invokeMethod<Boolean>(container, "isAutoDeclare"))
            assertEquals(false, ReflectionTestUtils.invokeMethod<Boolean>(container, "isMissingQueuesFatal"))
            assertEquals(false, ReflectionTestUtils.invokeMethod<Boolean>(container, "isDefaultRequeueRejected"))
            assertFalse(container.isAutoStartup)
            assertFalse(container.isRunning)
            verifyNoInteractions(inbox)
        } finally {
            factory.destroy()
        }
    }

    @Test fun `TLS socket configurator enables HTTPS endpoint identification without connecting`() {
        val factory = configuration.coreConnectionFactory(rabbit(), core())
        try {
            val native = factory.rabbitConnectionFactory
            // The zero-argument overload creates an unconnected socket and performs no handshake.
            (native.socketFactory.createSocket() as SSLSocket).use { socket ->
                assertFalse(socket.isConnected)
                native.socketConfigurator.configure(socket)
                assertEquals("HTTPS", socket.sslParameters.endpointIdentificationAlgorithm)
                assertFalse(socket.isConnected)
            }
        } finally {
            factory.destroy()
        }
    }
}
