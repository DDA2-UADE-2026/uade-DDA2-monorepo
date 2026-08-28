package com.uade.dda2.server.feature.enrollmentperiod.controller

import com.uade.dda2.server.feature.enrollmentperiod.dto.request.CreateEnrollmentPeriodRequest
import com.uade.dda2.server.feature.enrollmentperiod.dto.request.UpdateEnrollmentPeriodRequest
import com.uade.dda2.server.feature.enrollmentperiod.dto.response.EnrollmentPeriodListResponse
import com.uade.dda2.server.feature.enrollmentperiod.dto.response.EnrollmentPeriodResponse
import com.uade.dda2.server.feature.enrollmentperiod.service.AdminEnrollmentPeriodService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/programs/{programId}/editions/{editionId}/enrollment-periods")
@Validated
@Tag(
    name = "Períodos de inscripción",
    description = "Administración y ciclo de vida de las convocatorias asociadas a ediciones de programas.",
)
class AdminEnrollmentPeriodController(
    private val adminEnrollmentPeriodService: AdminEnrollmentPeriodService,
) {

    @PreAuthorize("hasAuthority('enrollment-periods:management:view')")
    @GetMapping
    @Operation(
        operationId = "listEnrollmentPeriods",
        summary = "Listar períodos de inscripción",
        description = "Devuelve una página de períodos pertenecientes a la edición indicada.",
    )
    fun list(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,

        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,

        @Parameter(description = "Número de página, comenzando en cero.", example = "0")
        @Min(value = 0, message = "La página no puede ser negativa.")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Cantidad de elementos por página, entre 1 y 100.", example = "20")
        @Min(value = 1, message = "El tamaño de página debe ser mayor a cero.")
        @Max(value = 100, message = "El tamaño de página no puede superar 100 elementos.")
        @RequestParam(defaultValue = "20") size: Int,
    ): EnrollmentPeriodListResponse =
        adminEnrollmentPeriodService.list(
            programId = programId,
            programEditionId = editionId,
            page = page,
            size = size,
        )

    @PreAuthorize("hasAuthority('enrollment-periods:management:view')")
    @GetMapping("/{enrollmentPeriodId}")
    @Operation(
        operationId = "getEnrollmentPeriod",
        summary = "Consultar un período de inscripción",
        description = "Devuelve el detalle del período y valida su pertenencia al programa y la edición indicados.",
    )
    fun get(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,

        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,

        @Parameter(description = "UUID del período de inscripción.")
        @PathVariable enrollmentPeriodId: UUID,
    ): EnrollmentPeriodResponse =
        adminEnrollmentPeriodService.get(
            programId = programId,
            programEditionId = editionId,
            enrollmentPeriodId = enrollmentPeriodId,
        )

    @PreAuthorize("hasAuthority('enrollment-periods:management:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        operationId = "createEnrollmentPeriod",
        summary = "Crear un período de inscripción",
        description = "Crea un período programado, contenido en las fechas de la edición y sin superponerse con otros períodos.",
    )
    fun create(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,

        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,

        @Valid @RequestBody request: CreateEnrollmentPeriodRequest,
    ): EnrollmentPeriodResponse =
        adminEnrollmentPeriodService.create(
            programId = programId,
            programEditionId = editionId,
            request = request,
        )

    @PreAuthorize("hasAuthority('enrollment-periods:management:edit')")
    @PutMapping("/{enrollmentPeriodId}")
    @Operation(
        operationId = "updateEnrollmentPeriod",
        summary = "Actualizar un período de inscripción",
        description = "Actualiza fechas y observaciones sin permitir modificar directamente el estado.",
    )
    fun update(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,

        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,

        @Parameter(description = "UUID del período de inscripción.")
        @PathVariable enrollmentPeriodId: UUID,

        @Valid @RequestBody request: UpdateEnrollmentPeriodRequest,
    ): EnrollmentPeriodResponse =
        adminEnrollmentPeriodService.update(
            programId = programId,
            programEditionId = editionId,
            enrollmentPeriodId = enrollmentPeriodId,
            request = request,
        )

    @PreAuthorize("hasAuthority('enrollment-periods:management:change-status')")
    @PostMapping("/{enrollmentPeriodId}/open")
    @Operation(
        operationId = "openEnrollmentPeriod",
        summary = "Abrir un período de inscripción",
        description = "Abre un período programado cuando la edición está activa, la fecha actual se encuentra dentro del rango y no existe otro período abierto para la edición.",
    )
    fun open(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,

        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,

        @Parameter(description = "UUID del período de inscripción.")
        @PathVariable enrollmentPeriodId: UUID,
    ): EnrollmentPeriodResponse =
        adminEnrollmentPeriodService.open(
            programId = programId,
            programEditionId = editionId,
            enrollmentPeriodId = enrollmentPeriodId,
        )

    @PreAuthorize("hasAuthority('enrollment-periods:management:change-status')")
    @PostMapping("/{enrollmentPeriodId}/suspend")
    @Operation(
        operationId = "suspendEnrollmentPeriod",
        summary = "Suspender un período de inscripción",
        description = "Suspende un período abierto e impide recibir nuevas solicitudes.",
    )
    fun suspend(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,

        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,

        @Parameter(description = "UUID del período de inscripción.")
        @PathVariable enrollmentPeriodId: UUID,
    ): EnrollmentPeriodResponse =
        adminEnrollmentPeriodService.suspend(
            programId = programId,
            programEditionId = editionId,
            enrollmentPeriodId = enrollmentPeriodId,
        )

    @PreAuthorize("hasAuthority('enrollment-periods:management:change-status')")
    @PostMapping("/{enrollmentPeriodId}/reopen")
    @Operation(
        operationId = "reopenEnrollmentPeriod",
        summary = "Reabrir un período de inscripción",
        description = "Reabre un período suspendido si aún se encuentra dentro de sus fechas, la edición continúa activa y no existe otro período abierto para la edición.",
    )
    fun reopen(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,

        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,

        @Parameter(description = "UUID del período de inscripción.")
        @PathVariable enrollmentPeriodId: UUID,
    ): EnrollmentPeriodResponse =
        adminEnrollmentPeriodService.reopen(
            programId = programId,
            programEditionId = editionId,
            enrollmentPeriodId = enrollmentPeriodId,
        )

    @PreAuthorize("hasAuthority('enrollment-periods:management:change-status')")
    @PostMapping("/{enrollmentPeriodId}/close")
    @Operation(
        operationId = "closeEnrollmentPeriod",
        summary = "Cerrar un período de inscripción",
        description = "Cierra de forma terminal un período abierto o suspendido.",
    )
    fun close(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,

        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,

        @Parameter(description = "UUID del período de inscripción.")
        @PathVariable enrollmentPeriodId: UUID,
    ): EnrollmentPeriodResponse =
        adminEnrollmentPeriodService.close(
            programId = programId,
            programEditionId = editionId,
            enrollmentPeriodId = enrollmentPeriodId,
        )
}
