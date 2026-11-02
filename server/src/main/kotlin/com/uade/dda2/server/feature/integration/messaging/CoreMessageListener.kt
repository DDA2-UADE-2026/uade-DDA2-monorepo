package com.uade.dda2.server.feature.integration.messaging

import com.rabbitmq.client.Channel
import com.uade.dda2.server.feature.integration.dto.CoreMessageMetadata
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.service.CoreInboxService
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener
import java.util.UUID

class CoreMessageListener(private val inbox: CoreInboxService) : ChannelAwareMessageListener {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onMessage(message: Message, channel: Channel?) {
        requireNotNull(channel) { "El ack manual requiere un canal AMQP." }
        val properties = message.messageProperties
        val metadata = CoreMessageMetadata(properties.messageId,
            properties.headers["x-core-event-id"]?.toString(),
            properties.headers["x-core-target-module"]?.toString(),
            properties.headers["x-core-attempt"]?.toString()?.toIntOrNull(),
            properties.headers["x-core-trace-id"]?.toString())
        val eventId = runCatching { UUID.fromString(metadata.messageId ?: metadata.eventIdHeader) }.getOrNull()
        val result = try {
            inbox.process(message.body, metadata) // El proxy confirma la transacción antes de retornar.
        } catch (exception: Exception) {
            val code = (exception as? CoreIntegrationException)?.code ?: "CORE_PROCESSING_FAILED"
            logger.warn("core_message_rejected eventId={} error={}", eventId, code)
            channel.basicNack(properties.deliveryTag, false, false)
            return
        }
        // Un error al confirmar no se convierte en rechazo de un efecto ya confirmado en la base.
        channel.basicAck(properties.deliveryTag, false)
        logger.info("core_message_processed eventId={} result={}", eventId, result)
    }
}
