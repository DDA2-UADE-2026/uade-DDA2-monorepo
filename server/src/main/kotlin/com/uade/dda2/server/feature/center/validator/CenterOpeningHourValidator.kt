package com.uade.dda2.server.feature.center.validator

import com.uade.dda2.server.feature.center.entity.CenterOpeningHour
import com.uade.dda2.server.feature.center.error.CenterErrors
import com.uade.dda2.server.feature.center.repository.CenterOpeningHourRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAvailabilityRepository
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Component
class CenterOpeningHourValidator(
    private val openingHourRepository: CenterOpeningHourRepository,
    private val availabilityRepository: ProfessionalAvailabilityRepository,
    private val effectiveScheduleValidator: EffectiveScheduleValidator,
) {
    fun validateRange(startTime: LocalTime, endTime: LocalTime) {
        if (startTime >= endTime) throw CenterErrors.invalidTimeRange()
    }

    fun validateNoOverlap(
        centerId: UUID,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
        excludedId: UUID? = null,
    ) {
        val overlaps = if (excludedId == null) {
            openingHourRepository.findActiveOverlaps(centerId, dayOfWeek, startTime, endTime)
        } else {
            openingHourRepository.findActiveOverlapsExcludingId(centerId, dayOfWeek, startTime, endTime, excludedId)
        }
        if (overlaps.isNotEmpty()) throw CenterErrors.openingHourOverlap()
    }

    fun validateAvailabilityCoverage(
        centerId: UUID,
        dayOfWeek: DayOfWeek,
        projectedHours: Collection<CenterOpeningHour>,
    ) {
        val ranges = projectedHours
            .filter { it.active && it.dayOfWeek == dayOfWeek }
            .map { EffectiveScheduleValidator.TimeRange(it.startTime, it.endTime) }
        val uncovered = availabilityRepository.findEffectiveByCenterIdAndDayOfWeek(centerId, dayOfWeek)
            .any { !effectiveScheduleValidator.isCovered(it.startTime, it.endTime, ranges) }
        if (uncovered) throw CenterErrors.availabilityWouldLoseCoverage()
    }
}
