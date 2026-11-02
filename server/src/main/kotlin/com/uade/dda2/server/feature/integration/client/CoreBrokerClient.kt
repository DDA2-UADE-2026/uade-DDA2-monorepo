package com.uade.dda2.server.feature.integration.client

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.CoreQueueState
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.util.CoreFailureClassifier
import org.springframework.amqp.rabbit.connection.ConnectionFactory

class CoreBrokerClient(private val factory: ConnectionFactory, private val properties: CoreProperties) {
    // La consulta pasiva tiene canal propio: un 404 del broker puede cerrarlo.
    fun inspectQueue(): CoreQueueState {
        try {
            val channel = factory.createConnection().createChannel(false)
            try {
                val result = channel.queueDeclarePassive(properties.queue)
                return CoreQueueState(result.messageCount, result.consumerCount)
            }
            finally { if (channel.isOpen) channel.close() }
        } catch (exception: Exception) { throw CoreIntegrationException(CoreFailureClassifier.code(exception, "CORE_QUEUE_UNAVAILABLE")) }
    }
}
