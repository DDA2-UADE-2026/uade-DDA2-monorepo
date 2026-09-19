package com.uade.dda2.server.feature.center.service

import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.center.dto.request.CreateMunicipalServiceRequest
import com.uade.dda2.server.feature.center.dto.request.UpdateMunicipalServiceRequest
import com.uade.dda2.server.feature.center.dto.response.MunicipalServiceListResponse
import com.uade.dda2.server.feature.center.dto.response.MunicipalServiceResponse
import com.uade.dda2.server.feature.center.entity.MunicipalNameNormalizer
import com.uade.dda2.server.feature.center.entity.MunicipalService
import com.uade.dda2.server.feature.center.error.CenterErrors
import com.uade.dda2.server.feature.center.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.center.mapper.toEntity
import com.uade.dda2.server.feature.center.mapper.toListResponse
import com.uade.dda2.server.feature.center.mapper.toResponse
import com.uade.dda2.server.feature.center.mapper.updateFrom
import com.uade.dda2.server.feature.center.repository.MunicipalServiceRepository
import com.uade.dda2.server.feature.center.validator.CenterLifecycleValidator
import com.uade.dda2.server.feature.center.validator.MunicipalServiceValidator
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class AdminMunicipalServiceService(
    private val repository: MunicipalServiceRepository,
    private val validator: MunicipalServiceValidator,
    private val lifecycleValidator: CenterLifecycleValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun list(page: Int, size: Int, search: String?, active: Boolean?): MunicipalServiceListResponse =
        repository.search(normalizedSearch(search), active, PageRequest.of(page, size)).toListResponse()

    @Transactional(readOnly = true)
    fun get(id: UUID): MunicipalServiceResponse = find(id).toResponse()

    @Transactional
    fun create(request: CreateMunicipalServiceRequest): MunicipalServiceResponse {
        validator.validateName(request.name)
        val service = repository.saveAndFlush(request.toEntity())
        record(service, LogAction.CREATE)
        return service.toResponse()
    }

    @Transactional
    fun update(id: UUID, request: UpdateMunicipalServiceRequest): MunicipalServiceResponse {
        val service = findForUpdate(id)
        validator.validateName(request.name, id)
        val oldValues = json(service.toAuditSnapshot())
        service.updateFrom(request)
        repository.saveAndFlush(service)
        record(service, LogAction.UPDATE, oldValues)
        return service.toResponse()
    }

    @Transactional
    fun activate(id: UUID): MunicipalServiceResponse = changeStatus(id, true)

    @Transactional
    fun deactivate(id: UUID): MunicipalServiceResponse = changeStatus(id, false)

    private fun changeStatus(id: UUID, active: Boolean): MunicipalServiceResponse {
        val service = findForUpdate(id)
        if (service.active == active) {
            throw if (active) CenterErrors.serviceAlreadyActive() else CenterErrors.serviceAlreadyInactive()
        }
        val oldValues = json(service.toAuditSnapshot())
        service.active = active
        if (active) lifecycleValidator.validateServiceActivation(id)
        repository.saveAndFlush(service)
        record(service, LogAction.UPDATE, oldValues)
        return service.toResponse()
    }

    private fun find(id: UUID): MunicipalService =
        repository.findById(id).orElseThrow { CenterErrors.serviceNotFound(id) }

    private fun findForUpdate(id: UUID): MunicipalService =
        repository.findByIdForUpdate(id) ?: throw CenterErrors.serviceNotFound(id)

    private fun normalizedSearch(search: String?): String? =
        search?.takeIf { it.isNotBlank() }?.let(MunicipalNameNormalizer::normalize)

    private fun record(service: MunicipalService, action: LogAction, oldValues: String? = null) {
        logService.record(
            user = currentUserService.userReference(),
            action = action,
            entityType = LogEntityType.MUNICIPAL_SERVICE,
            entityId = requireNotNull(service.id).toString(),
            oldValues = oldValues,
            newValues = json(service.toAuditSnapshot()),
        )
    }

    private fun json(value: Any): String = requireNotNull(jsonMapper.writeValueAsString(value))
}
