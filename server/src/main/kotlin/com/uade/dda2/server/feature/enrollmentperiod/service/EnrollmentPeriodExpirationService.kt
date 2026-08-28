package com.uade.dda2.server.feature.enrollmentperiod.service

import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.enrollmentperiod.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

@Service
class EnrollmentPeriodExpirationService(
    private val enrollmentPeriodRepository: EnrollmentPeriodRepository,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {

    @Transactional
    fun closeExpired(currentDate: LocalDate): Int {
        val expiredPeriods = enrollmentPeriodRepository
            .findExpiredForUpdate(
                statuses = EXPIRABLE_STATUSES,
                currentDate = currentDate,
            )
            .filter { enrollmentPeriod ->
                enrollmentPeriod.status in EXPIRABLE_STATUSES &&
                        enrollmentPeriod.closeDate.isBefore(currentDate)
            }

        if (expiredPeriods.isEmpty()) {
            return 0
        }

        val oldValuesById = expiredPeriods.associate { enrollmentPeriod ->
            requireNotNull(enrollmentPeriod.id) to json(enrollmentPeriod.toAuditSnapshot())
        }

        expiredPeriods.forEach { enrollmentPeriod ->
            enrollmentPeriod.status = EnrollmentPeriodStatus.CLOSED
        }
        enrollmentPeriodRepository.flush()

        expiredPeriods.forEach { enrollmentPeriod ->
            recordAutomaticClose(
                enrollmentPeriod = enrollmentPeriod,
                oldValues = requireNotNull(oldValuesById[requireNotNull(enrollmentPeriod.id)]),
            )
        }

        return expiredPeriods.size
    }

    private fun recordAutomaticClose(
        enrollmentPeriod: EnrollmentPeriod,
        oldValues: String,
    ) {
        logService.record(
            user = null,
            action = LogAction.UPDATE,
            entityType = LogEntityType.ENROLLMENT_PERIOD,
            entityId = requireNotNull(enrollmentPeriod.id).toString(),
            oldValues = oldValues,
            newValues = json(enrollmentPeriod.toAuditSnapshot()),
        )
    }

    private fun json(value: Any): String =
        requireNotNull(jsonMapper.writeValueAsString(value))

    private companion object {
        val EXPIRABLE_STATUSES = setOf(
            EnrollmentPeriodStatus.SCHEDULED,
            EnrollmentPeriodStatus.OPEN,
            EnrollmentPeriodStatus.SUSPENDED,
        )
    }
}
