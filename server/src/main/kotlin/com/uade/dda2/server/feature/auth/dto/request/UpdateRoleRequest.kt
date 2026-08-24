package com.uade.dda2.server.feature.auth.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateRoleRequest(
    @field:NotBlank(message = "name is required")
    @field:Size(min = 1, max = 50, message = "name must be at most 50 characters")
    val name: String = "",

    val permissions: List<String> = emptyList(),
)
