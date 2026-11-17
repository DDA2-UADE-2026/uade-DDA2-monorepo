package com.uade.dda2.server.feature.integration.error

import com.uade.dda2.server.error.ApiException
import org.springframework.http.HttpStatus

enum class M1DocumentFailure(val status: HttpStatus, val message: String) {
    DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "La descarga documental de M1 no está habilitada."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "M1 no dispone del archivo solicitado."),
    ACCESS_DENIED(HttpStatus.BAD_GATEWAY, "M1 no autorizó el acceso de servicio al archivo."),
    TOO_LARGE(HttpStatus.BAD_GATEWAY, "El archivo de M1 supera el límite permitido por el proxy."),
    INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "M1 devolvió una respuesta documental inválida."),
    UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "M1 no pudo entregar el archivo."),
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "El servicio documental de M1 no está disponible."),
    TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "M1 no entregó el archivo dentro del plazo permitido."),
}

class M1DocumentException(val failure: M1DocumentFailure) : ApiException(
    failure.status, "M1_DOCUMENT_${failure.name}", failure.message,
)
