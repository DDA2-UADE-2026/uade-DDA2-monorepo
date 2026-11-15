package com.uade.dda2.server.error

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import java.time.Instant

@Schema(description = "Respuesta estándar de error de la API.")
data class ErrorResponse(
    @field:Schema(description = "Mensaje legible que explica el error.", example = "No se encontró el recurso solicitado.")
    val message: String,
    @field:Schema(description = "Código estable y procesable del error.", example = "RESOURCE_NOT_FOUND")
    val code: String,
    @field:Schema(description = "Código de estado HTTP.", example = "400")
    val status: Int,
    @field:Schema(description = "Instante UTC en el que ocurrió el error.", example = "2026-08-26T14:30:00Z")
    val timestamp: Instant = Instant.now(),
    @field:Schema(description = "Ruta de la solicitud que produjo el error.", example = "/api/admin/programs/550e8400-e29b-41d4-a716-446655440000")
    val path: String,
    @field:Schema(description = "URI del tipo de problema según RFC 9457.", example = "about:blank")
    val type: String = "about:blank",
    @field:Schema(description = "Título HTTP del problema.", example = "Bad Request")
    val title: String = HttpStatus.valueOf(status).name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
    @field:Schema(description = "Detalle legible del problema.", example = "El cuerpo de la solicitud es inválido.")
    val detail: String = message,
    @field:Schema(description = "URI de la solicitud que produjo el problema.", example = "/api/admin/programs")
    val instance: String = path,
    @field:Schema(description = "Errores de validación asociados a campos, cuando corresponda.")
    val fields: List<FieldErrorResponse>? = null,
)

@Schema(description = "Detalle de un campo que no superó la validación.")
data class FieldErrorResponse(
    @field:Schema(description = "Nombre del campo inválido.", example = "name")
    val field: String,
    @field:Schema(description = "Motivo por el cual el valor fue rechazado.", example = "El nombre es obligatorio.")
    val message: String,
)
