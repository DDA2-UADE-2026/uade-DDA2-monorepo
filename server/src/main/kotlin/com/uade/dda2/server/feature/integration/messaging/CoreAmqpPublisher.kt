package com.uade.dda2.server.feature.integration.messaging

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.CoreOutboxLease
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.util.CoreFailureClassifier
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageDeliveryMode
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.connection.CorrelationData
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class CoreAmqpPublisher(private val template: RabbitTemplate, private val properties: CoreProperties) {
    fun publish(lease: CoreOutboxLease) {
        val metadata = MessageProperties().apply {
            contentType = "application/json"
            contentEncoding = "UTF-8"
            deliveryMode = MessageDeliveryMode.PERSISTENT
            setMessageId(lease.eventId.toString())
            setHeader("x-core-trace-id", lease.traceId)
        }
        val correlation = CorrelationData(lease.token.toString())
        try {
            template.send("muni.inbox", "", Message(lease.payloadJson.toByteArray(Charsets.UTF_8), metadata), correlation)
            val confirm = correlation.future.get(properties.publisher.confirmTimeout.toMillis(), TimeUnit.MILLISECONDS)
            if (correlation.returned != null) throw CoreIntegrationException("CORE_PUBLISH_RETURNED")
            if (!confirm.ack()) throw CoreIntegrationException("CORE_PUBLISH_NACK")
        } catch (exception: CoreIntegrationException) { throw exception }
        catch (_: TimeoutException) { throw CoreIntegrationException("CORE_PUBLISH_TIMEOUT") }
        catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            throw CoreIntegrationException("CORE_PUBLISH_INTERRUPTED")
        } catch (exception: Exception) { throw CoreIntegrationException(CoreFailureClassifier.code(exception, "CORE_PUBLISH_UNAVAILABLE")) }
    }
}
