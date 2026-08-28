package com.uade.dda2.server.feature.program.dto.admin.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Página de ediciones de un programa.")
data class ProgramEditionListResponse(
    @field:Schema(description = "Ediciones incluidas en la página actual.", accessMode = Schema.AccessMode.READ_ONLY)
    val content: List<ProgramEditionListItemResponse>,
    @field:Schema(description = "Número de página, comenzando en cero.", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
    val page: Int,
    @field:Schema(description = "Cantidad máxima de elementos por página.", example = "20", accessMode = Schema.AccessMode.READ_ONLY)
    val size: Int,
    @field:Schema(description = "Cantidad total de ediciones.", example = "8", accessMode = Schema.AccessMode.READ_ONLY)
    val totalElements: Long,
    @field:Schema(description = "Cantidad total de páginas.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    val totalPages: Int,
)
