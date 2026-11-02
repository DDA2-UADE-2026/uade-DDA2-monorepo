package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.CoreOutboxLease
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.messaging.CoreAmqpPublisher
import org.junit.jupiter.api.Test
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageDeliveryMode
import org.springframework.amqp.core.ReturnedMessage
import org.springframework.amqp.rabbit.connection.CorrelationData
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.UUID
import kotlin.test.*

class CoreAmqpPublisherTest {
    private val lease = CoreOutboxLease(UUID.randomUUID(), UUID.randomUUID(), "{\"data\":{}}", UUID.randomUUID().toString())

    @Test fun `publica JSON persistente con UUID y routing key vacia`() {
        val template = TestTemplate { exchange, routing, message, correlation ->
            assertEquals("muni.inbox", exchange)
            assertEquals("", routing)
            assertEquals(lease.eventId.toString(), message.messageProperties.messageId)
            assertEquals("application/json", message.messageProperties.contentType)
            assertEquals(MessageDeliveryMode.PERSISTENT, message.messageProperties.deliveryMode)
            assertEquals(lease.token.toString(), correlation.id)
            correlation.future.complete(CorrelationData.Confirm(true, null))
        }
        CoreAmqpPublisher(template, CoreProperties()).publish(lease)
    }

    @Test fun `un confirm positivo con retorno no es un envio correcto`() {
        val template = TestTemplate { exchange, routing, message, correlation ->
            correlation.setReturned(ReturnedMessage(message, 312, "NO_ROUTE", exchange, routing))
            correlation.future.complete(CorrelationData.Confirm(true, null))
        }
        val error = assertFailsWith<CoreIntegrationException> { CoreAmqpPublisher(template, CoreProperties()).publish(lease) }
        assertEquals("CORE_PUBLISH_RETURNED", error.code)
    }

    private class TestTemplate(val send: (String, String, Message, CorrelationData) -> Unit) : RabbitTemplate() {
        override fun send(exchange: String?, routingKey: String?, message: Message, correlationData: CorrelationData?) {
            send.invoke(requireNotNull(exchange), requireNotNull(routingKey), message, requireNotNull(correlationData))
        }
    }
}
