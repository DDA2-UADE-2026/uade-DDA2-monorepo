package com.uade.dda2.server.feature.auth.dto.response

data class RoleResponse(
    val id: Long,
    val name: String,
    val permissions: List<String>,
)
