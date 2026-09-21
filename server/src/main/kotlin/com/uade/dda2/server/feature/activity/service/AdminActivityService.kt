package com.uade.dda2.server.feature.activity.service

import com.uade.dda2.server.feature.activity.dto.request.CreateActivityRequest
import com.uade.dda2.server.feature.activity.dto.request.UpdateActivityRequest
import com.uade.dda2.server.feature.activity.dto.response.ActivityListResponse
import com.uade.dda2.server.feature.activity.dto.response.ActivityResponse
import com.uade.dda2.server.feature.activity.entity.Activity
import com.uade.dda2.server.feature.activity.entity.ActivityStatus
import com.uade.dda2.server.feature.activity.error.ActivityErrors
import com.uade.dda2.server.feature.activity.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.activity.mapper.toEntity
import com.uade.dda2.server.feature.activity.mapper.toListResponse
import com.uade.dda2.server.feature.activity.mapper.toResponse
import com.uade.dda2.server.feature.activity.mapper.updateFrom
import com.uade.dda2.server.feature.activity.repository.ActivityRepository
import com.uade.dda2.server.feature.activity.validator.ActivityValidator
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class AdminActivityService(
    private val activityRepository: ActivityRepository,
    private val activityValidator: ActivityValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun list(page: Int, size: Int): ActivityListResponse =
        activityRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).toListResponse()

    @Transactional(readOnly = true)
    fun get(id: UUID): ActivityResponse = findActivity(id).toResponse()

    @Transactional
    fun create(request: CreateActivityRequest): ActivityResponse {
        activityValidator.validateCreate(request)
        val activity = activityRepository.saveAndFlush(
            request.toEntity(currentUserService.userReference()),
        )
        record(activity, LogAction.CREATE)
        return activity.toResponse()
    }

    @Transactional
    fun update(id: UUID, request: UpdateActivityRequest): ActivityResponse {
        val activity = findActivityForUpdate(id)
        activityValidator.validateUpdate(activity, request)
        val oldValues = json(activity.toAuditSnapshot())
        activity.updateFrom(request)
        activityRepository.saveAndFlush(activity)
        record(activity, LogAction.UPDATE, oldValues)
        return activity.toResponse()
    }

    @Transactional
    fun publish(id: UUID): ActivityResponse =
        changeStatus(id, ActivityStatus.OPEN, activityValidator::validatePublish)

    @Transactional
    fun close(id: UUID): ActivityResponse =
        changeStatus(id, ActivityStatus.CLOSED, activityValidator::validateClose)

    private fun changeStatus(
        id: UUID,
        newStatus: ActivityStatus,
        validate: (Activity) -> Unit,
    ): ActivityResponse {
        val activity = findActivityForUpdate(id)
        validate(activity)
        val oldValues = json(activity.toAuditSnapshot())
        activity.status = newStatus
        activityRepository.saveAndFlush(activity)
        record(activity, LogAction.UPDATE, oldValues)
        return activity.toResponse()
    }

    private fun findActivity(id: UUID): Activity =
        activityRepository.findById(id).orElseThrow { ActivityErrors.notFound(id) }

    private fun findActivityForUpdate(id: UUID): Activity =
        activityRepository.findByIdForUpdate(id) ?: throw ActivityErrors.notFound(id)

    private fun record(activity: Activity, action: LogAction, oldValues: String? = null) {
        logService.record(
            user = currentUserService.userReference(),
            action = action,
            entityType = LogEntityType.ACTIVITY,
            entityId = requireNotNull(activity.id).toString(),
            oldValues = oldValues,
            newValues = json(activity.toAuditSnapshot()),
        )
    }

    private fun json(value: Any): String = requireNotNull(jsonMapper.writeValueAsString(value))
}
