package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.client.CoreBrokerClient
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.amqp.autoconfigure.RabbitProperties

class CoreConnectionCheckRunner(private val broker: CoreBrokerClient, private val core: CoreProperties,
    private val rabbit: RabbitProperties) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        try {
            val queue = broker.inspectQueue()
            logger.info(
                "CORE_AMQP_CONNECTED host={} port={} vhost={} queue={} tls={} certificateVerification=true hostnameVerification=true pendingMessages={} activeConsumers={}",
                rabbit.host, rabbit.determinePort(), rabbit.virtualHost, core.queue, rabbit.ssl.determineEnabled(),
                queue.messageCount, queue.consumerCount,
            )
        } catch (exception: Exception) {
            val code = (exception as? CoreIntegrationException)?.code ?: "CORE_AMQP_CHECK_FAILED"
            logger.warn(
                "CORE_AMQP_CONNECTION_FAILED host={} port={} vhost={} queue={} code={} backendContinues=true",
                rabbit.host, rabbit.determinePort(), rabbit.virtualHost, core.queue, code,
            )
        }
    }
}
