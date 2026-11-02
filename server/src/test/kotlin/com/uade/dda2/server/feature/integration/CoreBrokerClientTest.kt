package com.uade.dda2.server.feature.integration

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.client.CoreBrokerClient
import com.uade.dda2.server.feature.integration.dto.CoreQueueState
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.amqp.rabbit.connection.Connection
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import kotlin.test.assertEquals

class CoreBrokerClientTest {
    @Test fun `la comprobacion solo consulta pasivamente sin consumir purgar o publicar`() {
        val factory = mock(ConnectionFactory::class.java)
        val connection = mock(Connection::class.java)
        val channel = mock(Channel::class.java)
        val declared = mock(AMQP.Queue.DeclareOk::class.java)
        `when`(factory.createConnection()).thenReturn(connection)
        `when`(connection.createChannel(false)).thenReturn(channel)
        `when`(channel.queueDeclarePassive("q.desarrollo-social")).thenReturn(declared)
        `when`(declared.messageCount).thenReturn(7)
        `when`(declared.consumerCount).thenReturn(0)
        `when`(channel.isOpen).thenReturn(true)
        assertEquals(CoreQueueState(7, 0), CoreBrokerClient(factory, CoreProperties()).inspectQueue())
        verify(channel).queueDeclarePassive("q.desarrollo-social")
        verify(channel).isOpen
        verify(channel).close()
        verifyNoMoreInteractions(channel)
    }
}
