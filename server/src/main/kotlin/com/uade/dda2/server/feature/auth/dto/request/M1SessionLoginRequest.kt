package com.uade.dda2.server.feature.auth.dto.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.uade.dda2.server.error.BadRequestException
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import tools.jackson.databind.JsonNode

@Schema(description = "Credenciales de Ciudadanos para obtener una sesión propia de M8.", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
data class M1SessionLoginRequest(
    @field:Schema(description = "Correo de acceso registrado en M1.", example = "ciudadano@example.invalid")
    @field:NotBlank(message = "El correo es obligatorio.")
    @field:Email(message = "El correo debe ser válido.")
    @field:Size(max = 180, message = "El correo no puede superar 180 caracteres.")
    val email: String = "",
    @field:Schema(description = "Contraseña de M1. Se envía sin modificar y no se guarda en M8.", accessMode = Schema.AccessMode.WRITE_ONLY)
    @field:NotBlank(message = "La contraseña es obligatoria.")
    val password: String = "",
) {
    @JsonAnySetter
    @Suppress("UNUSED_PARAMETER")
    fun rejectUnknownField(name: String, value: JsonNode?) {
        throw BadRequestException("M1_AUTH_INVALID_REQUEST", "Solo se admiten email y password para el ingreso con M1.")
    }

    override fun toString(): String = "M1SessionLoginRequest(<redacted>)"
}
