package com.uade.dda2.server.error

import java.time.Instant

data class ErrorResponse(
    val message: String,
    val code: String,
    val status: Int,
    val timestamp: Instant = Instant.now(),
    val path: String,
    val fields: List<FieldErrorResponse>? = null,
)

data class FieldErrorResponse(
    val field: String,
    val message: String,
)
