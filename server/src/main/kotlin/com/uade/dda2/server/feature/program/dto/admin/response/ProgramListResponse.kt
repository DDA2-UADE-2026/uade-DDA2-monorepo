package com.uade.dda2.server.feature.program.dto.admin.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Página de programas sociales.")
data class ProgramListResponse(
    @field:Schema(description = "Programas incluidos en la página actual.", accessMode = Schema.AccessMode.READ_ONLY)
    val content: List<ProgramListItemResponse>,
    @field:Schema(description = "Número de página, comenzando en cero.", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
    val page: Int,
    @field:Schema(description = "Cantidad máxima de elementos por página.", example = "20", accessMode = Schema.AccessMode.READ_ONLY)
    val size: Int,
    @field:Schema(description = "Cantidad total de programas.", example = "42", accessMode = Schema.AccessMode.READ_ONLY)
    val totalElements: Long,
    @field:Schema(description = "Cantidad total de páginas.", example = "3", accessMode = Schema.AccessMode.READ_ONLY)
    val totalPages: Int,
)
