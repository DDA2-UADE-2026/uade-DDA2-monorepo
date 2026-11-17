package com.uade.dda2.server.security

enum class JwtAuthSource { LOCAL, M1 }

data class JwtPrincipal(
    val id: Long,
    val username: String?,
    val activeRole: String,
    val permissions: List<String>,
    val authSource: JwtAuthSource = JwtAuthSource.LOCAL,
)
