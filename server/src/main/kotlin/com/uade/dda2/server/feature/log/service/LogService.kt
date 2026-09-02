package com.uade.dda2.server.feature.log.service

import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.log.dto.response.LogResponse
import com.uade.dda2.server.feature.log.entity.Log
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.mapper.LogMapper
import com.uade.dda2.server.feature.log.repository.LogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LogService(
    private val logRepository: LogRepository,
    private val logMapper: LogMapper,
) {
    fun record(
        user: User?,
        action: LogAction,
        entityType: LogEntityType,
        entityId: String,
        oldValues: String? = null,
        newValues: String? = null,
    ): Log =
        logRepository.save(
            Log(
                user = user,
                action = action,
                entityType = entityType,
                entityId = entityId,
                oldValues = oldValues,
                newValues = newValues,
            ),
        )

    fun recordLogin(user: User): Log =
        record(
            user = user,
            action = LogAction.LOGIN,
            entityType = LogEntityType.USER,
            entityId = requireNotNull(user.id).toString(),
        )

    @Transactional(readOnly = true)
    fun findByEntity(
        entityType: LogEntityType,
        entityId: String,
    ): List<LogResponse> =
        logRepository
            .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                entityType = entityType,
                entityId = entityId,
            )
            .map(logMapper::toResponse)

    @Transactional(readOnly = true)
    fun findByUser(userId: Long): List<LogResponse> =
        logRepository
            .findByUserIdOrderByCreatedAtDesc(userId)
            .map(logMapper::toResponse)
}
