package com.uade.dda2.server.feature.integration

import com.rabbitmq.client.Channel
import com.uade.dda2.server.feature.integration.dto.*
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.messaging.CoreMessageListener
import com.uade.dda2.server.feature.integration.service.CoreInboxService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import java.io.IOException
import kotlin.test.assertFailsWith

class CoreMessageListenerTest {
    private val inbox = mock(CoreInboxService::class.java)
    private val channel = mock(Channel::class.java)
    private val message = Message("{}".toByteArray(), MessageProperties().apply { deliveryTag = 7L })
    private val listener = CoreMessageListener(inbox)

    @Test fun `confirma despues del retorno del servicio y admite duplicados`() {
        `when`(inbox.process(message.body, CoreMessageMetadata())).thenReturn(CoreInboxResult.DUPLICATE)
        listener.onMessage(message, channel)
        val order = inOrder(inbox, channel)
        order.verify(inbox).process(message.body, CoreMessageMetadata())
        order.verify(channel).basicAck(7L, false)
        verify(channel, never()).basicNack(7L, false, false)
    }

    @Test fun `rechaza sin requeue un fallo del procesamiento`() {
        `when`(inbox.process(message.body, CoreMessageMetadata())).thenThrow(CoreIntegrationException("CORE_UNSUPPORTED_EVENT"))
        listener.onMessage(message, channel)
        verify(channel).basicNack(7L, false, false)
        verify(channel, never()).basicAck(7L, false)
    }

    @Test fun `un fallo de ack no rechaza un resultado ya confirmado`() {
        `when`(inbox.process(message.body, CoreMessageMetadata())).thenReturn(CoreInboxResult.PROCESSED)
        doThrow(IOException("channel closed")).`when`(channel).basicAck(7L, false)
        assertFailsWith<IOException> { listener.onMessage(message, channel) }
        verify(channel, never()).basicNack(7L, false, false)
    }
}
