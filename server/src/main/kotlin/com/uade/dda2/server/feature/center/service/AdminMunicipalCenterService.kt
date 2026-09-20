package com.uade.dda2.server.feature.center.service

import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.center.dto.request.CreateMunicipalCenterRequest
import com.uade.dda2.server.feature.center.dto.request.UpdateMunicipalCenterRequest
import com.uade.dda2.server.feature.center.dto.response.MunicipalCenterListResponse
import com.uade.dda2.server.feature.center.dto.response.MunicipalCenterResponse
import com.uade.dda2.server.feature.center.entity.MunicipalCenter
import com.uade.dda2.server.feature.center.entity.MunicipalNameNormalizer
import com.uade.dda2.server.feature.center.error.CenterErrors
import com.uade.dda2.server.feature.center.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.center.mapper.toEntity
import com.uade.dda2.server.feature.center.mapper.toListResponse
import com.uade.dda2.server.feature.center.mapper.toResponse
import com.uade.dda2.server.feature.center.mapper.updateFrom
import com.uade.dda2.server.feature.center.repository.MunicipalCenterRepository
import com.uade.dda2.server.feature.center.validator.CenterLifecycleValidator
import com.uade.dda2.server.feature.center.validator.MunicipalCenterValidator
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class AdminMunicipalCenterService(
    private val repository: MunicipalCenterRepository,
    private val validator: MunicipalCenterValidator,
    private val lifecycleValidator: CenterLifecycleValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun list(page: Int, size: Int, search: String?, active: Boolean?): MunicipalCenterListResponse =
        repository.search(normalizedSearch(search), active, PageRequest.of(page, size)).toListResponse()

    @Transactional(readOnly = true)
    fun get(id: UUID): MunicipalCenterResponse = find(id).toResponse()

    @Transactional
    fun create(request: CreateMunicipalCenterRequest): MunicipalCenterResponse {
        validator.validateName(request.name)
        val center = repository.saveAndFlush(request.toEntity())
        record(center, LogAction.CREATE)
        return center.toResponse()
    }

    @Transactional
    fun update(id: UUID, request: UpdateMunicipalCenterRequest): MunicipalCenterResponse {
        val center = findForUpdate(id)
        validator.validateName(request.name, id)
        val oldValues = json(center.toAuditSnapshot())
        center.updateFrom(request)
        repository.saveAndFlush(center)
        record(center, LogAction.UPDATE, oldValues)
        return center.toResponse()
    }

    @Transactional
    fun activate(id: UUID): MunicipalCenterResponse = changeStatus(id, true)

    @Transactional
    fun deactivate(id: UUID): MunicipalCenterResponse = changeStatus(id, false)

    private fun changeStatus(id: UUID, active: Boolean): MunicipalCenterResponse {
        val center = findForUpdate(id)
        if (center.active == active) {
            throw if (active) CenterErrors.centerAlreadyActive() else CenterErrors.centerAlreadyInactive()
        }
        val oldValues = json(center.toAuditSnapshot())
        center.active = active
        if (active) lifecycleValidator.validateCenterActivation(id)
        repository.saveAndFlush(center)
        record(center, LogAction.UPDATE, oldValues)
        return center.toResponse()
    }

    private fun find(id: UUID): MunicipalCenter =
        repository.findById(id).orElseThrow { CenterErrors.centerNotFound(id) }

    private fun findForUpdate(id: UUID): MunicipalCenter =
        repository.findByIdForUpdate(id) ?: throw CenterErrors.centerNotFound(id)

    // Nunca se pasa null al repositorio: en PostgreSQL un parámetro null dentro
    // del `concat` del LIKE se bindea como bytea y la consulta falla con
    // "operator does not exist: character varying ~~ bytea". Con "" el
    // LIKE '%%' matchea todo y el parámetro viaja como varchar.
    private fun normalizedSearch(search: String?): String =
        search?.takeIf { it.isNotBlank() }?.let(MunicipalNameNormalizer::normalize) ?: ""

    private fun record(center: MunicipalCenter, action: LogAction, oldValues: String? = null) {
        logService.record(
            user = currentUserService.userReference(),
            action = action,
            entityType = LogEntityType.MUNICIPAL_CENTER,
            entityId = requireNotNull(center.id).toString(),
            oldValues = oldValues,
            newValues = json(center.toAuditSnapshot()),
        )
    }

    private fun json(value: Any): String = requireNotNull(jsonMapper.writeValueAsString(value))
}
