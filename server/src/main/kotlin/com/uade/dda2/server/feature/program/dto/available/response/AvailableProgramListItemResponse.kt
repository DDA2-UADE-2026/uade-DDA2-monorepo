package com.uade.dda2.server.feature.program.dto.available.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

@Schema(description = "Resumen de un programa disponible para mostrar en un listado ciudadano.")
data class AvailableProgramListItemResponse(
    @field:Schema(description = "UUID del programa.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "Nombre del programa.", example = "Becas de formación laboral", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
    @field:Schema(description = "Objetivo del programa.", example = "Mejorar la empleabilidad de jóvenes.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val objective: String?,
    @field:Schema(description = "Cantidad de ediciones disponibles.", example = "2", accessMode = Schema.AccessMode.READ_ONLY)
    val availableEditions: Int,
    @field:Schema(description = "Fecha de inicio de la edición disponible más próxima.", example = "2026-09-01", format = "date", accessMode = Schema.AccessMode.READ_ONLY)
    val nextEditionStartDate: LocalDate,
    @field:Schema(description = "Fecha de finalización de la edición disponible más próxima.", example = "2026-12-15", format = "date", accessMode = Schema.AccessMode.READ_ONLY)
    val nextEditionEndDate: LocalDate,
)
