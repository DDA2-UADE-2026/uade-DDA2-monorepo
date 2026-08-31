package com.uade.dda2.server.security

data class JwtPrincipal(
    val id: Long,
    val username: String?,
    val activeRole: String,
    val permissions: List<String>,
)
