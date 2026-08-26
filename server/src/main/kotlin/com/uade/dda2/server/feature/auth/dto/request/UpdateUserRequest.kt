package com.uade.dda2.server.feature.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Datos requeridos para actualizar un usuario.")
data class UpdateUserRequest(
    @field:Schema(description = "Nombre único utilizado para iniciar sesión.", example = "maria.gomez")
    @field:NotBlank(message = "El nombre de usuario es obligatorio.")
    @field:Size(min = 1, max = 80, message = "El nombre de usuario no puede superar los 80 caracteres.")
    val username: String = "",

    @field:Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
    val password: String? = null,

    @field:NotBlank(message = "El nombre es obligatorio.")
    @field:Size(min = 1, max = 150, message = "El nombre no puede superar los 150 caracteres.")
    val name: String = "",

    @field:NotBlank(message = "El correo electrónico es obligatorio.")
    @field:Email(message = "El correo electrónico debe ser válido.")
    @field:Size(min = 1, max = 180, message = "El correo electrónico no puede superar los 180 caracteres.")
    val email: String = "",

    @field:Schema(description = "Indica si el usuario puede acceder al sistema.", example = "true")
    val active: Boolean = true,
    @field:Schema(description = "Lista completa de roles que conservará el usuario.", example = "[\"OPERADOR\", \"SUPERVISOR\"]")
    val roles: List<String> = emptyList(),
)
    @field:Schema(description = "Nueva contraseña; si se omite, se conserva la actual.", example = "NuevaClave123!", accessMode = Schema.AccessMode.WRITE_ONLY, nullable = true)
    @field:Schema(description = "Nombre completo del usuario.", example = "María Gómez")
    @field:Schema(description = "Correo electrónico válido.", example = "maria.gomez@example.com", format = "email")
