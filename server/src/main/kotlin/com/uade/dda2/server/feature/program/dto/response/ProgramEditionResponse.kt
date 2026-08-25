package com.uade.dda2.server.feature.program.dto.response

import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ProgramEditionResponse(
    val id: UUID,

    val programId: UUID,
    val programName: String,

    val name: String,

    val startDate: LocalDate,
    val endDate: LocalDate,

    val maxCapacity: Int,
    val currentEnrollment: Int,

    val status: ProgramEditionStatus,

    val createdBy: ProgramCreatedByResponse,

    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)