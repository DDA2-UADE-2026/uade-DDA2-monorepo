package com.uade.dda2.server.error

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        exception: ApiException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(exception.status)
            .body(
                ErrorResponse(
                    message = exception.message,
                    code = exception.code,
                    status = exception.status.value(),
                    path = request.requestURI,
                ),
            )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val fields = exception.bindingResult
            .allErrors
            .map {
                val field = (it as? FieldError)?.field ?: it.objectName
                FieldErrorResponse(field = field, message = it.defaultMessage ?: "Valor inválido.")
            }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    message = "La validación falló.",
                    code = "VALIDATION_ERROR",
                    status = HttpStatus.BAD_REQUEST.value(),
                    path = request.requestURI,
                    fields = fields,
                ),
            )
    }

    @ExceptionHandler(
        HandlerMethodValidationException::class,
        ConstraintViolationException::class,
    )
    fun handleMethodValidation(
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    message = "La validación falló.",
                    code = "VALIDATION_ERROR",
                    status = HttpStatus.BAD_REQUEST.value(),
                    path = request.requestURI,
                ),
            )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    message = "El cuerpo de la solicitud es inválido.",
                    code = "INVALID_REQUEST_BODY",
                    status = HttpStatus.BAD_REQUEST.value(),
                    path = request.requestURI,
                ),
            )

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSize(request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val isProgramImage = request.requestURI.matches(Regex("/api/admin/programs/[^/]+/image"))

        return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(
                ErrorResponse(
                    message = if (isProgramImage) "La imagen no puede superar 10 MB." else "El archivo no puede superar 10 MB.",
                    code = if (isProgramImage) "PROGRAM_IMAGE_FILE_TOO_LARGE" else "APPLICATION_DOCUMENT_FILE_TOO_LARGE",
                    status = HttpStatus.PAYLOAD_TOO_LARGE.value(),
                    path = request.requestURI,
                ),
            )
    }

    @ExceptionHandler(BadCredentialsException::class, AuthenticationException::class)
    fun handleAuthentication(
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorResponse(
                    message = "El nombre de usuario o la contraseña son inválidos.",
                    code = "AUTH_INVALID_CREDENTIALS",
                    status = HttpStatus.UNAUTHORIZED.value(),
                    path = request.requestURI,
                ),
            )

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    message = "La operación entra en conflicto con datos relacionados existentes.",
                    code = "DATA_INTEGRITY_VIOLATION",
                    status = HttpStatus.CONFLICT.value(),
                    path = request.requestURI,
                ),
            )
}
