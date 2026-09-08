package com.uade.dda2.server.feature.log.mapper

import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.log.dto.response.LogActorResponse
import com.uade.dda2.server.feature.log.dto.response.LogResponse
import com.uade.dda2.server.feature.log.entity.Log
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

@Component
class LogMapper(
    private val jsonMapper: JsonMapper,
) {
    fun toResponse(log: Log): LogResponse =
        LogResponse(
            id = log.id,
            actor = log.user?.toLogActorResponse(),
            action = log.action,
            entityType = log.entityType,
            entityId = log.entityId,
            oldValues = log.oldValues.toJsonNode(),
            newValues = log.newValues.toJsonNode(),
            createdAt = log.createdAt,
        )

    private fun String?.toJsonNode(): JsonNode? =
        this?.let(jsonMapper::readTree)
}

private fun User.toLogActorResponse(): LogActorResponse =
    LogActorResponse(
        id = requireNotNull(id),
        username = username,
        name = name,
    )
