package com.uade.dda2.server.feature.auth.service

import com.uade.dda2.server.feature.auth.dto.response.LoginResponse

// El JWT nunca se expone en un DTO de respuesta: este login solo entrega
// cookies. El token viaja únicamente hasta el controller, que lo usa para
// setear la cookie httpOnly y lo descarta.
data class LoginResult(
    val token: String,
    val response: LoginResponse,
)
