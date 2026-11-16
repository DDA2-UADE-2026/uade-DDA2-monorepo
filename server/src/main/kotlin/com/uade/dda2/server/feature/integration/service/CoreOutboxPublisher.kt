package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.messaging.CoreAmqpPublisher
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled

class CoreOutboxPublisher(private val store: CoreOutboxStore, private val sender: CoreAmqpPublisher,
    private val properties: CoreProperties) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${app.core.publisher.poll-interval:5s}")
    fun publishPending() {
        try {
            repeat(properties.publisher.batchSize) {
                if (Thread.currentThread().isInterrupted) return
                // Reservar de a uno evita que venza la reserva de un lote mientras espera confirms anteriores.
                val lease = store.reserveNext() ?: return
                try {
                    sender.publish(lease)
                    if (store.confirm(lease)) logger.info("core_broker_confirmed eventId={}", lease.eventId)
                } catch (exception: Exception) {
                    val code = (exception as? CoreIntegrationException)?.code ?: "CORE_PUBLISH_UNAVAILABLE"
                    store.retry(lease, code)
                    logger.warn("core_publish_pending eventId={} error={}", lease.eventId, code)
                }
            }
        } catch (exception: Exception) {
            logger.warn("core_outbox_poll_failed error={}", exception.javaClass.simpleName)
        }
    }
}
