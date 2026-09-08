package com.uade.dda2.server.feature.program.dto.available.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Página de programas disponibles para ciudadanos.")
data class AvailableProgramListResponse(
    @field:Schema(description = "Programas incluidos en la página actual.", accessMode = Schema.AccessMode.READ_ONLY)
    val content: List<AvailableProgramListItemResponse>,
    @field:Schema(description = "Número de página, comenzando en cero.", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
    val page: Int,
    @field:Schema(description = "Cantidad máxima de elementos por página.", example = "20", accessMode = Schema.AccessMode.READ_ONLY)
    val size: Int,
    @field:Schema(description = "Cantidad total de programas disponibles.", example = "12", accessMode = Schema.AccessMode.READ_ONLY)
    val totalElements: Long,
    @field:Schema(description = "Cantidad total de páginas.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    val totalPages: Int,
)
