package com.uade.dda2.server.feature.benefit.scheduler

import com.uade.dda2.server.config.BenefitGrantExpirationProperties
import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
import com.uade.dda2.server.feature.benefit.service.BenefitGrantExpirationService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

@Component
@ConditionalOnProperty(prefix = "app.benefit-grant.expiration", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class BenefitGrantExpirationScheduler(
    private val grants: BenefitGrantRepository,
    private val service: BenefitGrantExpirationService,
    private val properties: BenefitGrantExpirationProperties,
    private val timeProperties: EnrollmentPeriodExpirationProperties,
    private val clock: Clock,
) {
    private val running = AtomicBoolean(false)

    @EventListener(ApplicationReadyEvent::class)
    @Scheduled(cron = "\${app.benefit-grant.expiration.cron}", zone = "\${app.enrollment-period.expiration.zone-id}")
    fun expireBenefits() {
        if (!running.compareAndSet(false, true)) return
        try {
            require(properties.batchSize > 0) { "Benefit expiration batch size must be positive." }
            val today = LocalDate.now(clock.withZone(timeProperties.zone()))
            var cursor: UUID? = null
            var expired = 0
            var failures = 0
            do {
                val candidates = grants.findExpirationCandidates(BenefitGrantExpirationService.EXPIRABLE_STATUSES,
                    today, cursor, PageRequest.of(0, properties.batchSize))
                candidates.forEach { applicationId ->
                    try { expired += service.expireApplication(applicationId) }
                    catch (failure: Exception) {
                        failures++
                        logger.error("Fallo de vencimiento de beneficios: applicationId={}, errorType={}", applicationId, failure.javaClass.simpleName)
                    }
                }
                if (candidates.isNotEmpty()) cursor = candidates.last()
            } while (candidates.size == properties.batchSize)
            if (expired > 0 || failures > 0) logger.info("Vencimiento de beneficios: fecha={}, vencidos={}, solicitudesFallidas={}", today, expired, failures)
        } finally { running.set(false) }
    }

    private companion object { val logger = LoggerFactory.getLogger(BenefitGrantExpirationScheduler::class.java) }
}
