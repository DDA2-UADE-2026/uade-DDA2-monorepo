package com.uade.dda2.server.feature.auth.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateUserRequest(
    @field:NotBlank(message = "username is required")
    @field:Size(max = 80, message = "username must be at most 80 characters")
    val username: String = "",

    @field:Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
    val password: String? = null,

    @field:NotBlank(message = "name is required")
    @field:Size(max = 150, message = "name must be at most 150 characters")
    val name: String = "",

    @field:NotBlank(message = "email is required")
    @field:Email(message = "email must be valid")
    @field:Size(max = 180, message = "email must be at most 180 characters")
    val email: String = "",

    val active: Boolean = true,
    val roles: List<String> = emptyList(),
)
