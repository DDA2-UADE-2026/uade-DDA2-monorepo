package com.uade.dda2.server.feature.center.validator

import org.springframework.stereotype.Component
import java.time.LocalTime

@Component
class EffectiveScheduleValidator {
    data class TimeRange(val start: LocalTime, val end: LocalTime)

    fun isCovered(start: LocalTime, end: LocalTime, openingHours: Collection<TimeRange>): Boolean {
        var cursor = start
        for (range in openingHours.sortedBy(TimeRange::start)) {
            if (range.end <= cursor) continue
            if (range.start > cursor) return false
            if (range.end > cursor) cursor = range.end
            if (cursor >= end) return true
        }
        return false
    }
}
