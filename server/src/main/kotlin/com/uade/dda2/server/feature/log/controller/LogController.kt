package com.uade.dda2.server.feature.log.controller

import com.uade.dda2.server.feature.log.dto.response.LogResponse
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/logs")
@Validated
@Tag(name = "Logs de auditoría", description = "Consulta general de eventos auditados, por entidad o por usuario.")
class LogController(
    private val logService: LogService,
) {
    @PreAuthorize("hasAuthority('logs:view')")
    @GetMapping
    @Operation(
        operationId = "listLogs",
        summary = "Consultar todos los logs",
        description = "Devuelve todos los eventos de auditoría, ordenados desde el más reciente.",
    )
    fun findAll(): List<LogResponse> =
        logService.findAll()

    @PreAuthorize("hasAuthority('logs:view')")
    @GetMapping("/entities/{entityType}/{entityId}")
    @Operation(
        operationId = "listLogsByEntity",
        summary = "Consultar logs de una entidad",
        description = "Devuelve el historial de auditoría de una entidad, ordenado desde el evento más reciente.",
    )
    fun findByEntity(
        @Parameter(description = "Tipo de entidad auditada.", example = "USER")
        @PathVariable entityType: LogEntityType,

        @Parameter(description = "Identificador de la entidad auditada.", example = "12")
        @NotBlank(message = "El identificador de entidad es obligatorio.")
        @PathVariable entityId: String,
    ): List<LogResponse> =
        logService.findByEntity(
            entityType = entityType,
            entityId = entityId,
        )

    @PreAuthorize("hasAuthority('logs:view')")
    @GetMapping("/users/{userId}")
    @Operation(
        operationId = "listLogsByUser",
        summary = "Consultar logs de un usuario",
        description = "Devuelve los eventos originados por un usuario, ordenados desde el más reciente.",
    )
    fun findByUser(
        @Parameter(description = "ID del usuario que originó los eventos.", example = "12")
        @Positive(message = "El ID del usuario debe ser positivo.")
        @PathVariable userId: Long,
    ): List<LogResponse> =
        logService.findByUser(userId)
}
