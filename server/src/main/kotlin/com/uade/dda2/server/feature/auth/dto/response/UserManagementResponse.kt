package com.uade.dda2.server.feature.auth.dto.response

import java.time.Instant

data class UserManagementResponse(
    val id: Long,
    val username: String,
    val name: String,
    val email: String,
    val active: Boolean,
    val roles: List<String>,
    val permissions: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant?,
)
