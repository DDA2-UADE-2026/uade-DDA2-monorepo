package com.uade.dda2.server.feature.auth.dto.response

data class UserResponse(
    val id: Long,
    val username: String,
    val name: String,
    val email: String,
    val roles: List<String>,
    val permissions: List<String>,
)
