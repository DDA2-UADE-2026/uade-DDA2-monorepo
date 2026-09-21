package com.uade.dda2.server.feature.center.service

import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.center.dto.request.AssignCenterServiceRequest
import com.uade.dda2.server.feature.center.dto.response.CenterServiceResponse
import com.uade.dda2.server.feature.center.entity.CenterService
import com.uade.dda2.server.feature.center.error.CenterErrors
import com.uade.dda2.server.feature.center.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.center.mapper.toResponse
import com.uade.dda2.server.feature.center.repository.CenterServiceRepository
import com.uade.dda2.server.feature.center.repository.MunicipalCenterRepository
import com.uade.dda2.server.feature.center.repository.MunicipalServiceRepository
import com.uade.dda2.server.feature.center.validator.CenterLifecycleValidator
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class AdminCenterServiceService(
    private val centerRepository: MunicipalCenterRepository,
    private val municipalServiceRepository: MunicipalServiceRepository,
    private val centerServiceRepository: CenterServiceRepository,
    private val lifecycleValidator: CenterLifecycleValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun list(centerId: UUID): List<CenterServiceResponse> {
        if (!centerRepository.existsById(centerId)) throw CenterErrors.centerNotFound(centerId)
        return centerServiceRepository.findAllByCenterIdOrderByServiceNameAsc(centerId).map(CenterService::toResponse)
    }

    @Transactional
    fun assign(centerId: UUID, request: AssignCenterServiceRequest): CenterServiceResponse {
        val center = centerRepository.findByIdForUpdate(centerId) ?: throw CenterErrors.centerNotFound(centerId)
        if (!center.active) throw CenterErrors.inactiveDependency("el centro municipal")
        val service = municipalServiceRepository.findById(request.serviceId)
            .orElseThrow { CenterErrors.serviceNotFound(request.serviceId) }
        if (!service.active) throw CenterErrors.inactiveDependency("el servicio municipal")

        val existing = centerServiceRepository.findByCenterIdAndServiceId(centerId, request.serviceId)
        if (existing?.active == true) throw CenterErrors.centerServiceAlreadyActive()
        val relation = existing ?: CenterService(center = center, service = service)
        val oldValues = existing?.let { json(it.toAuditSnapshot()) }
        relation.active = true
        if (existing != null) lifecycleValidator.validateCenterServiceActivation(requireNotNull(relation.id))
        centerServiceRepository.saveAndFlush(relation)
        record(relation, if (existing == null) LogAction.CREATE else LogAction.UPDATE, oldValues)
        return relation.toResponse()
    }

    @Transactional
    fun activate(id: UUID): CenterServiceResponse {
        val relation = lockForUpdate(id)
        if (relation.active) throw CenterErrors.centerServiceAlreadyActive()
        if (!relation.center.active) throw CenterErrors.inactiveDependency("el centro municipal")
        if (!relation.service.active) throw CenterErrors.inactiveDependency("el servicio municipal")
        return changeStatus(relation, true)
    }

    @Transactional
    fun deactivate(id: UUID): CenterServiceResponse {
        val relation = lockForUpdate(id)
        if (!relation.active) throw CenterErrors.centerServiceAlreadyInactive()
        return changeStatus(relation, false)
    }

    private fun changeStatus(relation: CenterService, active: Boolean): CenterServiceResponse {
        val oldValues = json(relation.toAuditSnapshot())
        relation.active = active
        if (active) {
            lifecycleValidator.validateCenterServiceActivation(requireNotNull(relation.id), lockResources = false)
        }
        centerServiceRepository.saveAndFlush(relation)
        record(relation, LogAction.UPDATE, oldValues)
        return relation.toResponse()
    }

    private fun lockForUpdate(id: UUID): CenterService {
        if (!centerServiceRepository.existsById(id)) throw CenterErrors.centerServiceNotFound(id)
        lifecycleValidator.lockCenterServiceResources(id)
        return centerServiceRepository.findByIdForUpdate(id) ?: throw CenterErrors.centerServiceNotFound(id)
    }

    private fun record(relation: CenterService, action: LogAction, oldValues: String? = null) {
        logService.record(
            user = currentUserService.userReference(),
            action = action,
            entityType = LogEntityType.CENTER_SERVICE,
            entityId = requireNotNull(relation.id).toString(),
            oldValues = oldValues,
            newValues = json(relation.toAuditSnapshot()),
        )
    }

    private fun json(value: Any): String = requireNotNull(jsonMapper.writeValueAsString(value))
}
