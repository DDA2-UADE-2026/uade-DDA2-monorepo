package com.uade.dda2.server.feature.integration.error

import com.uade.dda2.server.error.ApiException
import org.springframework.http.HttpStatus

enum class M1AuthFailure(val status: HttpStatus, val code: String, val publicMessage: String) {
    DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "M1_AUTH_DISABLED", "El ingreso con M1 no está habilitado."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "M1_AUTH_INVALID_REQUEST", "El correo y la contraseña son obligatorios."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "M1_AUTH_INVALID_EMAIL", "El correo de acceso debe tener entre 1 y 180 caracteres."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "M1_AUTH_INVALID_CREDENTIALS", "Correo o contraseña incorrectos."),
    ACCOUNT_FORBIDDEN(HttpStatus.FORBIDDEN, "M1_AUTH_ACCOUNT_FORBIDDEN", "M1 no permite el ingreso de esta cuenta."),
    ACTOR_NOT_ALLOWED(HttpStatus.FORBIDDEN, "M1_AUTH_ACTOR_NOT_ALLOWED", "Este ingreso está habilitado únicamente para ciudadanos."),
    LOCAL_ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, "M1_AUTH_LOCAL_ACCOUNT_INACTIVE", "La cuenta local está inactiva."),
    CITIZEN_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "M1_AUTH_CITIZEN_ROLE_REQUIRED", "La cuenta local no tiene asignado el rol CIUDADANO."),
    CITIZEN_ROLE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "M1_AUTH_CITIZEN_ROLE_UNAVAILABLE", "El rol CIUDADANO no está configurado."),
    IDENTITY_CONFLICT(HttpStatus.CONFLICT, "M1_AUTH_IDENTITY_CONFLICT", "La identidad ciudadana requiere revisión de su vinculación local."),
    PROVISIONING_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "M1_AUTH_PROVISIONING_FAILED", "No se pudo preparar la cuenta local. Reintentá el ingreso."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "M1_AUTH_RATE_LIMITED", "M1 alcanzó el límite de intentos. Reintentá más tarde."),
    INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "M1_AUTH_INVALID_RESPONSE", "M1 devolvió una respuesta de autenticación inválida."),
    UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "M1_AUTH_UPSTREAM_ERROR", "No se pudo completar la autenticación con M1."),
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "M1_AUTH_UNAVAILABLE", "El servicio de autenticación de M1 no está disponible."),
    TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "M1_AUTH_TIMEOUT", "M1 no respondió dentro del tiempo de espera."),
}

/** No conserva la causa original, URLs, cuerpos ni headers que puedan contener secretos. */
class M1AuthException(val failure: M1AuthFailure, val upstreamStatus: Int? = null) :
    ApiException(failure.status, failure.code, failure.publicMessage)
