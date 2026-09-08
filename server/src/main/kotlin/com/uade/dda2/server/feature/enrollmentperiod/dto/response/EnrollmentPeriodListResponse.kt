package com.uade.dda2.server.feature.enrollmentperiod.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Página de períodos de inscripción de una edición.")
data class EnrollmentPeriodListResponse(
    @field:Schema(description = "Períodos incluidos en la página actual.", accessMode = Schema.AccessMode.READ_ONLY)
    val content: List<EnrollmentPeriodListItemResponse>,
    @field:Schema(description = "Número de página, comenzando en cero.", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
    val page: Int,
    @field:Schema(description = "Cantidad máxima de elementos por página.", example = "20", accessMode = Schema.AccessMode.READ_ONLY)
    val size: Int,
    @field:Schema(description = "Cantidad total de períodos.", example = "4", accessMode = Schema.AccessMode.READ_ONLY)
    val totalElements: Long,
    @field:Schema(description = "Cantidad total de páginas.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    val totalPages: Int,
)
