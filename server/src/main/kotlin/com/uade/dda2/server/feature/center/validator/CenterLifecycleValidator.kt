package com.uade.dda2.server.feature.center.validator

import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.center.entity.ProfessionalAvailability
import com.uade.dda2.server.feature.center.error.CenterErrors
import com.uade.dda2.server.feature.center.repository.MunicipalCenterRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAvailabilityRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CenterLifecycleValidator(
    private val availabilityRepository: ProfessionalAvailabilityRepository,
    private val centerRepository: MunicipalCenterRepository,
    private val userRepository: UserRepository,
    private val availabilityValidator: ProfessionalAvailabilityValidator,
) {
    fun validateCenterActivation(centerId: UUID) =
        validate(availabilityRepository.findActiveByCenterId(centerId))

    fun validateServiceActivation(serviceId: UUID) =
        validate(availabilityRepository.findActiveByServiceId(serviceId))

    fun validateCenterServiceActivation(centerServiceId: UUID) =
        validate(availabilityRepository.findActiveByCenterServiceId(centerServiceId))

    fun validateAssignmentActivation(assignmentId: UUID) =
        validate(availabilityRepository.findActiveByAssignmentId(assignmentId))

    fun lockProfessionalActivation(professionalId: Long) {
        lockAffectedResources(
            availabilityRepository.findActiveByProfessionalId(professionalId),
            setOf(professionalId),
        )
    }

    fun validateProfessionalActivation(professionalId: Long) =
        validate(availabilityRepository.findActiveByProfessionalId(professionalId), lockResources = false)

    private fun validate(
        availabilities: Collection<ProfessionalAvailability>,
        lockResources: Boolean = true,
    ) {
        if (lockResources) lockAffectedResources(availabilities)
        availabilities.filter(::isEffective).forEach { availability ->
            availabilityValidator.validateEffective(
                availability.assignment,
                availability.dayOfWeek,
                availability.startTime,
                availability.endTime,
                requireNotNull(availability.id),
            )
        }
    }

    private fun lockAffectedResources(
        availabilities: Collection<ProfessionalAvailability>,
        additionalProfessionalIds: Set<Long> = emptySet(),
    ) {
        availabilities
            .map { requireNotNull(it.assignment.centerService.center.id) }
            .distinct()
            .sorted()
            .forEach { centerId ->
                centerRepository.findByIdForUpdate(centerId) ?: throw CenterErrors.centerNotFound(centerId)
            }
        (availabilities.map { requireNotNull(it.assignment.professional.id) } + additionalProfessionalIds)
            .distinct()
            .sorted()
            .forEach { professionalId ->
                userRepository.findByIdForUpdate(professionalId)
                    ?: throw CenterErrors.professionalNotFound(professionalId)
            }
    }

    private fun isEffective(availability: ProfessionalAvailability): Boolean {
        val assignment = availability.assignment
        val professional = assignment.professional
        val centerService = assignment.centerService
        val hasProfessionalRole = professional.roles.any { it.name.equals("PROFESIONAL_CENTRO", ignoreCase = true) }
        return availability.active &&
            assignment.active &&
            professional.active &&
            hasProfessionalRole &&
            centerService.active &&
            centerService.center.active &&
            centerService.service.active
    }
}
