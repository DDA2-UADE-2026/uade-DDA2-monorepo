package com.uade.dda2.server.feature.auth.dto.response

data class LoginResponse(
    val token: String,
    val expiresIn: Long,
    val user: UserResponse,
    val permissions: List<String>,
)
