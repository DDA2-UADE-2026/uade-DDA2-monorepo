package com.uade.dda2.server.feature.program.dto.response

data class ProgramEditionListResponse(
    val content: List<ProgramEditionListItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)