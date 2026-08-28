package com.uade.dda2.server.feature.enrollmentperiod.scheduler

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.enrollmentperiod.service.EnrollmentPeriodExpirationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class EnrollmentPeriodExpirationScheduler(
    private val enrollmentPeriodExpirationService: EnrollmentPeriodExpirationService,
    private val properties: EnrollmentPeriodExpirationProperties,
) {

    @Scheduled(
        cron = "\${app.enrollment-period.expiration.cron}",
        zone = "\${app.enrollment-period.expiration.zone-id}",
    )
    fun closeExpiredPeriods() {
        val currentDate = LocalDate.now(properties.zone())
        val closedPeriods = enrollmentPeriodExpirationService.closeExpired(currentDate)

        if (closedPeriods > 0) {
            logger.info(
                "Se cerraron automáticamente {} períodos de inscripción vencidos para la fecha {}.",
                closedPeriods,
                currentDate,
            )
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(EnrollmentPeriodExpirationScheduler::class.java)
    }
}
