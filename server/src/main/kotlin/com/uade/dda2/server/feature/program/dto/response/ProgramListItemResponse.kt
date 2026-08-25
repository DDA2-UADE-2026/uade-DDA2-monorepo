package com.uade.dda2.server.feature.program.dto.response

import java.time.LocalDateTime
import java.util.UUID

data class ProgramListItemResponse(
    val id: UUID,
    val name: String,
    val objective: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)