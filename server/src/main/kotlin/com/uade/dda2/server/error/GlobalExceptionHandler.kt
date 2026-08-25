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
                FieldErrorResponse(field = field, message = it.defaultMessage ?: "Invalid value")
            }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    message = "Validation failed.",
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
                    message = "Validation failed.",
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
                    message = "Invalid request body.",
                    code = "INVALID_REQUEST_BODY",
                    status = HttpStatus.BAD_REQUEST.value(),
                    path = request.requestURI,
                ),
            )

    @ExceptionHandler(BadCredentialsException::class, AuthenticationException::class)
    fun handleAuthentication(
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorResponse(
                    message = "Invalid username or password.",
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
                    message = "The operation conflicts with existing related data.",
                    code = "DATA_INTEGRITY_VIOLATION",
                    status = HttpStatus.CONFLICT.value(),
                    path = request.requestURI,
                ),
            )
}
