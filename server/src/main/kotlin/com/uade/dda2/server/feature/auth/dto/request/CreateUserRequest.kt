package com.uade.dda2.server.feature.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Datos requeridos para crear un usuario.")
data class CreateUserRequest(
    @field:Schema(description = "Nombre único utilizado para iniciar sesión.", example = "maria.gomez")
    @field:NotBlank(message = "El nombre de usuario es obligatorio.")
    @field:Size(min = 1, max = 80, message = "El nombre de usuario no puede superar los 80 caracteres.")
    val username: String = "",

    @field:Schema(description = "Contraseña inicial, de 8 a 72 caracteres.", example = "ClaveSegura123!", accessMode = Schema.AccessMode.WRITE_ONLY)
    @field:NotBlank(message = "La contraseña es obligatoria.")
    @field:Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
    val password: String = "",

    @field:Schema(description = "Nombre completo del usuario.", example = "María Gómez")
    @field:NotBlank(message = "El nombre es obligatorio.")
    @field:Size(min = 1, max = 150, message = "El nombre no puede superar los 150 caracteres.")
    val name: String = "",

    @field:Schema(description = "Correo electrónico válido.", example = "maria.gomez@example.com", format = "email")
    @field:NotBlank(message = "El correo electrónico es obligatorio.")
    @field:Email(message = "El correo electrónico debe ser válido.")
    @field:Size(min = 1, max = 180, message = "El correo electrónico no puede superar los 180 caracteres.")
    val email: String = "",

    @field:Schema(description = "Indica si el usuario puede acceder al sistema.", example = "true")
    val active: Boolean = true,
    @field:Schema(description = "Nombres de los roles asignados.", example = "[\"OPERADOR\"]")
    val roles: List<String> = emptyList(),
)
