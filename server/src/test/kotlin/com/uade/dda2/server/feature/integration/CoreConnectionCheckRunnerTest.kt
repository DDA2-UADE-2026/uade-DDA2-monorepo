package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.client.CoreBrokerClient
import com.uade.dda2.server.feature.integration.dto.CoreQueueState
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.service.CoreConnectionCheckRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.*
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.amqp.autoconfigure.RabbitProperties
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import kotlin.test.*

@ExtendWith(OutputCaptureExtension::class)
class CoreConnectionCheckRunnerTest {
    private val broker = mock(CoreBrokerClient::class.java)
    private val core = CoreProperties().apply { enabled = true; checkOnStartup = true }
    private val rabbit = RabbitProperties().apply {
        host = "core-uade-2026.duckdns.org"
        port = 5671
        virtualHost = "core"
        username = "module_desarrollo-social"
        password = "private-password-never-logged"
        ssl.enabled = true
    }

    @Test fun `registra conexion confirmada sin exigir secret HTTP ni registrar secretos`(output: CapturedOutput) {
        core.validate()
        `when`(broker.inspectQueue()).thenReturn(CoreQueueState(3, 0))
        CoreConnectionCheckRunner(broker, core, rabbit).run(DefaultApplicationArguments())
        assertContains(output.all, "CORE_AMQP_CONNECTED")
        assertContains(output.all, "tls=true")
        assertContains(output.all, "pendingMessages=3")
        assertFalse(output.all.contains(rabbit.password))
        verify(broker).inspectQueue()
    }

    @Test fun `fallo de conexion queda visible sin detener el backend`(output: CapturedOutput) {
        `when`(broker.inspectQueue()).thenThrow(CoreIntegrationException("CORE_TLS_FAILURE"))
        CoreConnectionCheckRunner(broker, core, rabbit).run(DefaultApplicationArguments())
        assertContains(output.all, "CORE_AMQP_CONNECTION_FAILED")
        assertContains(output.all, "code=CORE_TLS_FAILURE")
        assertContains(output.all, "backendContinues=true")
        assertFalse(output.all.contains("CORE_AMQP_CONNECTED"))
    }
}
