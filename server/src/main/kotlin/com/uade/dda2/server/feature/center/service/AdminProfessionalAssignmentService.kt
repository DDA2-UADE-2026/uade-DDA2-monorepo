package com.uade.dda2.server.feature.center.service

import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.center.dto.request.AssignProfessionalRequest
import com.uade.dda2.server.feature.center.dto.response.ProfessionalAssignmentResponse
import com.uade.dda2.server.feature.center.entity.ProfessionalAssignment
import com.uade.dda2.server.feature.center.error.CenterErrors
import com.uade.dda2.server.feature.center.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.center.mapper.toResponse
import com.uade.dda2.server.feature.center.repository.CenterServiceRepository
import com.uade.dda2.server.feature.center.repository.MunicipalCenterRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAssignmentRepository
import com.uade.dda2.server.feature.center.validator.CenterLifecycleValidator
import com.uade.dda2.server.feature.center.validator.ProfessionalAssignmentValidator
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class AdminProfessionalAssignmentService(
    private val centerRepository: MunicipalCenterRepository,
    private val centerServiceRepository: CenterServiceRepository,
    private val assignmentRepository: ProfessionalAssignmentRepository,
    private val userRepository: UserRepository,
    private val validator: ProfessionalAssignmentValidator,
    private val lifecycleValidator: CenterLifecycleValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun list(centerServiceId: UUID): List<ProfessionalAssignmentResponse> {
        if (!centerServiceRepository.existsById(centerServiceId)) throw CenterErrors.centerServiceNotFound(centerServiceId)
        return assignmentRepository.findAllByCenterServiceIdOrderByProfessionalNameAsc(centerServiceId)
            .map(ProfessionalAssignment::toResponse)
    }

    @Transactional
    fun assign(centerServiceId: UUID, request: AssignProfessionalRequest): ProfessionalAssignmentResponse {
        val centerService = centerServiceRepository.findById(centerServiceId)
            .orElseThrow { CenterErrors.centerServiceNotFound(centerServiceId) }
        centerRepository.findByIdForUpdate(requireNotNull(centerService.center.id))
            ?: throw CenterErrors.centerNotFound(requireNotNull(centerService.center.id))
        val professional = userRepository.findByIdForUpdate(request.professionalId)
            ?: throw CenterErrors.professionalNotFound(request.professionalId)
        validateDependencies(centerService.active, centerService.center.active, centerService.service.active)
        validator.validateEligible(professional)

        val existing = assignmentRepository.findByProfessionalIdAndCenterServiceId(request.professionalId, centerServiceId)
        if (existing?.active == true) throw CenterErrors.professionalAssignmentAlreadyActive()
        val assignment = existing ?: ProfessionalAssignment(professional = professional, centerService = centerService)
        val oldValues = existing?.let { json(it.toAuditSnapshot()) }
        assignment.active = true
        if (existing != null) lifecycleValidator.validateAssignmentActivation(requireNotNull(assignment.id))
        assignmentRepository.saveAndFlush(assignment)
        record(assignment, if (existing == null) LogAction.CREATE else LogAction.UPDATE, oldValues)
        return assignment.toResponse()
    }

    @Transactional
    fun activate(id: UUID): ProfessionalAssignmentResponse {
        val assignment = lockAssignment(id)
        if (assignment.active) throw CenterErrors.professionalAssignmentAlreadyActive()
        validateDependencies(
            assignment.centerService.active,
            assignment.centerService.center.active,
            assignment.centerService.service.active,
        )
        validator.validateEligible(assignment.professional)
        return changeStatus(assignment, true)
    }

    @Transactional
    fun deactivate(id: UUID): ProfessionalAssignmentResponse {
        val assignment = lockAssignment(id)
        if (!assignment.active) throw CenterErrors.professionalAssignmentAlreadyInactive()
        return changeStatus(assignment, false)
    }

    private fun lockAssignment(id: UUID): ProfessionalAssignment {
        val preview = assignmentRepository.findById(id).orElseThrow { CenterErrors.professionalAssignmentNotFound(id) }
        val centerId = requireNotNull(preview.centerService.center.id)
        val professionalId = requireNotNull(preview.professional.id)
        centerRepository.findByIdForUpdate(centerId) ?: throw CenterErrors.centerNotFound(centerId)
        userRepository.findByIdForUpdate(professionalId) ?: throw CenterErrors.professionalNotFound(professionalId)
        return assignmentRepository.findByIdForUpdate(id) ?: throw CenterErrors.professionalAssignmentNotFound(id)
    }

    private fun validateDependencies(centerServiceActive: Boolean, centerActive: Boolean, serviceActive: Boolean) {
        if (!centerActive) throw CenterErrors.inactiveDependency("el centro municipal")
        if (!serviceActive) throw CenterErrors.inactiveDependency("el servicio municipal")
        if (!centerServiceActive) throw CenterErrors.inactiveDependency("el servicio del centro")
    }

    private fun changeStatus(assignment: ProfessionalAssignment, active: Boolean): ProfessionalAssignmentResponse {
        val oldValues = json(assignment.toAuditSnapshot())
        assignment.active = active
        if (active) lifecycleValidator.validateAssignmentActivation(requireNotNull(assignment.id))
        assignmentRepository.saveAndFlush(assignment)
        record(assignment, LogAction.UPDATE, oldValues)
        return assignment.toResponse()
    }

    private fun record(assignment: ProfessionalAssignment, action: LogAction, oldValues: String? = null) {
        logService.record(
            user = currentUserService.userReference(),
            action = action,
            entityType = LogEntityType.PROFESSIONAL_ASSIGNMENT,
            entityId = requireNotNull(assignment.id).toString(),
            oldValues = oldValues,
            newValues = json(assignment.toAuditSnapshot()),
        )
    }

    private fun json(value: Any): String = requireNotNull(jsonMapper.writeValueAsString(value))
}
