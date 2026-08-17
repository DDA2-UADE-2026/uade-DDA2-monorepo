package com.uade.dda2.server.security

import com.uade.dda2.server.error.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class SecurityErrorResponseWriter(
    private val jsonMapper: JsonMapper,
) {
    fun write(
        request: HttpServletRequest,
        response: HttpServletResponse,
        status: HttpStatus,
        code: String,
        message: String,
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        jsonMapper.writeValue(
            response.outputStream,
            ErrorResponse(
                message = message,
                code = code,
                status = status.value(),
                path = request.requestURI,
            ),
        )
    }
}
