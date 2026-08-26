package com.uade.dda2.server.feature.program.dto.admin.response

data class ProgramListResponse(
    val content: List<ProgramListItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)