package com.uade.dda2.server.feature.program.dto.admin.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Metadatos de la imagen de portada de un programa; nunca contiene sus bytes.")
data class ProgramImageResponse(
    @field:Schema(description = "UUID de la imagen.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "UUID del programa asociado.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val programId: UUID,
    @field:Schema(description = "Nombre original del archivo.", example = "portada.png", accessMode = Schema.AccessMode.READ_ONLY)
    val originalName: String,
    @field:Schema(description = "Tipo MIME validado de la imagen.", example = "image/png", accessMode = Schema.AccessMode.READ_ONLY)
    val contentType: String,
    @field:Schema(description = "Tamaño de la imagen en bytes.", example = "245812", accessMode = Schema.AccessMode.READ_ONLY)
    val sizeBytes: Long,
    @field:Schema(description = "URL pública de la imagen.", example = "/api/images/750e8400-e29b-41d4-a716-446655440002", accessMode = Schema.AccessMode.READ_ONLY)
    val url: String,
    @field:Schema(description = "Fecha y hora de creación.", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    val createdAt: LocalDateTime,
    @field:Schema(description = "Fecha y hora de la última actualización.", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    val updatedAt: LocalDateTime,
)
