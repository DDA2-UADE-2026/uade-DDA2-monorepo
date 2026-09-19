package com.uade.dda2.server.feature.center.validator

import org.junit.jupiter.api.Test
import java.time.LocalTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EffectiveScheduleValidatorTest {
    private val validator = EffectiveScheduleValidator()

    @Test
    fun `acepta cobertura en una franja`() {
        assertTrue(validator.isCovered(time(9), time(11), listOf(range(8, 12))))
    }

    @Test
    fun `une franjas adyacentes sin dejar huecos`() {
        assertTrue(validator.isCovered(time(10), time(14), listOf(range(12, 18), range(8, 12))))
    }

    @Test
    fun `rechaza cobertura con interrupciones`() {
        assertFalse(validator.isCovered(time(10), time(15), listOf(range(8, 12), range(13, 18))))
    }

    private fun range(start: Int, end: Int) = EffectiveScheduleValidator.TimeRange(time(start), time(end))

    private fun time(hour: Int) = LocalTime.of(hour, 0)
}
