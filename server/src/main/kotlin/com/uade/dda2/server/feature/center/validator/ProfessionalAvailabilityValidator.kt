package com.uade.dda2.server.feature.center.validator

import com.uade.dda2.server.feature.center.entity.ProfessionalAssignment
import com.uade.dda2.server.feature.center.error.CenterErrors
import com.uade.dda2.server.feature.center.repository.CenterOpeningHourRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAvailabilityRepository
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Component
class ProfessionalAvailabilityValidator(
    private val openingHourRepository: CenterOpeningHourRepository,
    private val availabilityRepository: ProfessionalAvailabilityRepository,
    private val assignmentValidator: ProfessionalAssignmentValidator,
    private val effectiveScheduleValidator: EffectiveScheduleValidator,
) {
    fun validateEffective(
        assignment: ProfessionalAssignment,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
        excludedId: UUID? = null,
    ) {
        validateRange(startTime, endTime)
        validateDependencies(assignment)
        validateCoverage(assignment, dayOfWeek, startTime, endTime)
        validateNoOverlap(assignment, dayOfWeek, startTime, endTime, excludedId)
    }

    fun validateRange(startTime: LocalTime, endTime: LocalTime) {
        if (startTime >= endTime) throw CenterErrors.invalidAvailabilityRange()
    }

    private fun validateDependencies(assignment: ProfessionalAssignment) {
        if (!assignment.active) throw CenterErrors.inactiveDependency("la asignación profesional")
        if (!assignment.centerService.active) throw CenterErrors.inactiveDependency("el servicio del centro")
        if (!assignment.centerService.center.active) throw CenterErrors.inactiveDependency("el centro municipal")
        if (!assignment.centerService.service.active) throw CenterErrors.inactiveDependency("el servicio municipal")
        assignmentValidator.validateEligible(assignment.professional)
    }

    private fun validateCoverage(
        assignment: ProfessionalAssignment,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
    ) {
        val centerId = requireNotNull(assignment.centerService.center.id)
        val ranges = openingHourRepository
            .findAllByCenterIdAndActiveOrderByDayOfWeekAscStartTimeAsc(centerId, true)
            .filter { it.dayOfWeek == dayOfWeek }
            .map { EffectiveScheduleValidator.TimeRange(it.startTime, it.endTime) }
        if (!effectiveScheduleValidator.isCovered(startTime, endTime, ranges)) {
            throw CenterErrors.professionalAvailabilityOutsideOpeningHours()
        }
    }

    private fun validateNoOverlap(
        assignment: ProfessionalAssignment,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
        excludedId: UUID?,
    ) {
        val professionalId = requireNotNull(assignment.professional.id)
        val overlaps = if (excludedId == null) {
            availabilityRepository.findEffectiveOverlapsByProfessionalId(
                professionalId,
                dayOfWeek,
                startTime,
                endTime,
            )
        } else {
            availabilityRepository.findEffectiveOverlapsByProfessionalIdExcludingId(
                professionalId,
                dayOfWeek,
                startTime,
                endTime,
                excludedId,
            )
        }
        if (overlaps.isNotEmpty()) throw CenterErrors.professionalAvailabilityOverlap()
    }
}
