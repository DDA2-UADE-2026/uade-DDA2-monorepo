package com.uade.dda2.server.feature.center.service

import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.feature.center.dto.request.CreateProfessionalAvailabilitiesRequest
import com.uade.dda2.server.feature.center.dto.request.CreateProfessionalAvailabilityRequest
import com.uade.dda2.server.feature.center.dto.request.UpdateProfessionalAvailabilityRequest
import com.uade.dda2.server.feature.center.dto.response.ProfessionalAvailabilityResponse
import com.uade.dda2.server.feature.center.entity.ProfessionalAssignment
import com.uade.dda2.server.feature.center.entity.ProfessionalAvailability
import com.uade.dda2.server.feature.center.error.CenterErrors
import com.uade.dda2.server.feature.center.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.center.mapper.toResponse
import com.uade.dda2.server.feature.center.mapper.updateFrom
import com.uade.dda2.server.feature.center.repository.MunicipalCenterRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAssignmentRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAvailabilityRepository
import com.uade.dda2.server.feature.center.validator.ProfessionalAvailabilityValidator
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class AdminProfessionalAvailabilityService(
    private val centerRepository: MunicipalCenterRepository,
    private val assignmentRepository: ProfessionalAssignmentRepository,
    private val availabilityRepository: ProfessionalAvailabilityRepository,
    private val userRepository: UserRepository,
    private val validator: ProfessionalAvailabilityValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun list(assignmentId: UUID): List<ProfessionalAvailabilityResponse> {
        if (!assignmentRepository.existsById(assignmentId)) throw CenterErrors.professionalAssignmentNotFound(assignmentId)
        return availabilityRepository.findAllByAssignmentIdOrderByDayOfWeekAscStartTimeAsc(assignmentId)
            .map(ProfessionalAvailability::toResponse)
    }

    @Transactional
    fun create(
        assignmentId: UUID,
        request: CreateProfessionalAvailabilityRequest,
    ): ProfessionalAvailabilityResponse {
        val assignment = lockAssignment(assignmentId)
        validator.validateEffective(assignment, request.dayOfWeek, request.startTime, request.endTime)
        val availability = availabilityRepository.saveAndFlush(
            ProfessionalAvailability(
                assignment = assignment,
                dayOfWeek = request.dayOfWeek,
                startTime = request.startTime,
                endTime = request.endTime,
            ),
        )
        record(availability, LogAction.CREATE)
        return availability.toResponse()
    }

    @Transactional
    fun createBulk(
        assignmentId: UUID,
        request: CreateProfessionalAvailabilitiesRequest,
    ): List<ProfessionalAvailabilityResponse> {
        val assignment = lockAssignment(assignmentId)
        val days = request.days.distinct()
        if (days.isEmpty() || days.size != request.days.size) throw CenterErrors.invalidBulkAvailabilityDays()
        validator.validateRange(request.startTime, request.endTime)
        // Los días son distintos entre sí, así que las filas nuevas no pueden
        // superponerse entre ellas: basta validar cada día contra lo efectivo.
        // Si un día falla, la transacción hace rollback y no se crea ninguna.
        days.forEach { day ->
            try {
                validator.validateEffective(assignment, day, request.startTime, request.endTime)
            } catch (conflict: ConflictException) {
                throw when (conflict.code) {
                    "PROFESSIONAL_AVAILABILITY_OUTSIDE_OPENING_HOURS" ->
                        CenterErrors.professionalAvailabilityOutsideOpeningHoursOnDay(day)
                    else -> CenterErrors.professionalAvailabilityOverlapOnDay(day)
                }
            }
        }
        return days.map { day ->
            val availability = availabilityRepository.saveAndFlush(
                ProfessionalAvailability(
                    assignment = assignment,
                    dayOfWeek = day,
                    startTime = request.startTime,
                    endTime = request.endTime,
                ),
            )
            record(availability, LogAction.CREATE)
            availability.toResponse()
        }
    }

    @Transactional
    fun update(id: UUID, request: UpdateProfessionalAvailabilityRequest): ProfessionalAvailabilityResponse {
        val availability = lockAvailability(id)
        if (availability.active) {
            validator.validateEffective(
                availability.assignment,
                request.dayOfWeek,
                request.startTime,
                request.endTime,
                id,
            )
        } else {
            validator.validateRange(request.startTime, request.endTime)
        }
        val oldValues = json(availability.toAuditSnapshot())
        availability.updateFrom(request)
        availabilityRepository.saveAndFlush(availability)
        record(availability, LogAction.UPDATE, oldValues)
        return availability.toResponse()
    }

    @Transactional
    fun activate(id: UUID): ProfessionalAvailabilityResponse {
        val availability = lockAvailability(id)
        if (availability.active) throw CenterErrors.professionalAvailabilityAlreadyActive()
        validator.validateEffective(
            availability.assignment,
            availability.dayOfWeek,
            availability.startTime,
            availability.endTime,
            id,
        )
        return changeStatus(availability, true)
    }

    @Transactional
    fun deactivate(id: UUID): ProfessionalAvailabilityResponse {
        val availability = lockAvailability(id)
        if (!availability.active) throw CenterErrors.professionalAvailabilityAlreadyInactive()
        return changeStatus(availability, false)
    }

    private fun lockAssignment(id: UUID): ProfessionalAssignment {
        val preview = assignmentRepository.findById(id).orElseThrow { CenterErrors.professionalAssignmentNotFound(id) }
        val centerId = requireNotNull(preview.centerService.center.id)
        val professionalId = requireNotNull(preview.professional.id)
        centerRepository.findByIdForUpdate(centerId) ?: throw CenterErrors.centerNotFound(centerId)
        userRepository.findByIdForUpdate(professionalId) ?: throw CenterErrors.professionalNotFound(professionalId)
        return assignmentRepository.findByIdForUpdate(id) ?: throw CenterErrors.professionalAssignmentNotFound(id)
    }

    private fun lockAvailability(id: UUID): ProfessionalAvailability {
        val preview = availabilityRepository.findById(id)
            .orElseThrow { CenterErrors.professionalAvailabilityNotFound(id) }
        lockAssignment(requireNotNull(preview.assignment.id))
        return availabilityRepository.findByIdForUpdate(id) ?: throw CenterErrors.professionalAvailabilityNotFound(id)
    }

    private fun changeStatus(
        availability: ProfessionalAvailability,
        active: Boolean,
    ): ProfessionalAvailabilityResponse {
        val oldValues = json(availability.toAuditSnapshot())
        availability.active = active
        availabilityRepository.saveAndFlush(availability)
        record(availability, LogAction.UPDATE, oldValues)
        return availability.toResponse()
    }

    private fun record(availability: ProfessionalAvailability, action: LogAction, oldValues: String? = null) {
        logService.record(
            user = currentUserService.userReference(),
            action = action,
            entityType = LogEntityType.PROFESSIONAL_AVAILABILITY,
            entityId = requireNotNull(availability.id).toString(),
            oldValues = oldValues,
            newValues = json(availability.toAuditSnapshot()),
        )
    }

    private fun json(value: Any): String = requireNotNull(jsonMapper.writeValueAsString(value))
}
