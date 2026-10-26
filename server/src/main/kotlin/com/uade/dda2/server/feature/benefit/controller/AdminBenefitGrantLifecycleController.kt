package com.uade.dda2.server.feature.benefit.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.benefit.dto.request.*
import com.uade.dda2.server.feature.benefit.dto.response.*
import com.uade.dda2.server.feature.benefit.service.BenefitGrantLifecycleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/applications/{id}/benefit-grants/{grantId}", produces = ["application/json"])
@Tag(name = "Otorgamientos de beneficios", description = "Asignacion, mantenimiento e historial administrativo de beneficios otorgados.")
@ApiResponse(responseCode = "400", description = "Cuerpo, motivo, checklist, UUID o fechas invalidos; campos adicionales no admitidos.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "401", description = "Sin acceso valido o usuario inexistente/inactivo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "403", description = "Sin permiso vigente en JWT y rol activo de base de datos.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "404", description = "Solicitud inexistente u otorgamiento inexistente o ajeno a ella.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
class AdminBenefitGrantLifecycleController(private val service: BenefitGrantLifecycleService) {
    @PostMapping("/renew")
    @PreAuthorize("hasAuthority('applications:management:benefits:manage')")
    @Operation(summary = "Renovar o reactivar un beneficio otorgado", description =
        "Exige solicitud APPROVED y otorgamiento ACTIVE o SUSPENDED cuya fecha de fin aun no paso, inclusive hoy. Conserva inicio y datos copiados. " +
        "El nuevo fin debe superar el actual, incluir hoy en la zona configurada y estar dentro de la edicion ACTIVE. " +
        "Exige todos los requisitos vigentes cumplidos, documentos obligatorios VALID y ninguna aprobacion incompatible. " +
        "Registra motivo, responsable, instante UTC y copias de los requisitos. No requiere convocatoria abierta ni otro cupo.")
    @ApiResponse(responseCode = "200", description = "Otorgamiento renovado en ACTIVE; historial y auditoria confirmados.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "409", description = "Solicitud no aprobada, otorgamiento vencido aunque aun no haya sido procesado, estado definitivo, edicion no activa, requisitos o documentos incumplidos o incompatibilidad.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun renew(@PathVariable id: UUID, @PathVariable grantId: UUID, @RequestBody request: RenewBenefitGrantRequest): BenefitGrantResponse =
        service.renew(id, grantId, request)

    @PostMapping("/suspend")
    @PreAuthorize("hasAuthority('applications:management:benefits:manage')")
    @Operation(summary = "Suspender un beneficio activo", description =
        "Exige solicitud APPROVED y otorgamiento ACTIVE. Conserva vigencia, datos copiados, estado de solicitud y cupo. " +
        "Registra motivo, responsable y fecha UTC. Admite edicion suspendida, cerrada o vencida, sin repetir elegibilidad.")
    @ApiResponse(responseCode = "200", description = "Otorgamiento SUSPENDED; historial y auditoria confirmados.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "409", description = "Solicitud no aprobada o transicion invalida/repetida.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun suspend(@PathVariable id: UUID, @PathVariable grantId: UUID, @RequestBody request: ChangeBenefitGrantStateRequest): BenefitGrantResponse =
        service.suspend(id, grantId, request)

    @PostMapping("/finalize")
    @PreAuthorize("hasAuthority('applications:management:benefits:manage')")
    @Operation(summary = "Finalizar definitivamente un beneficio", description =
        "Exige solicitud APPROVED y otorgamiento ACTIVE o SUSPENDED. Efecto inmediato, conservando las fechas pactadas. " +
        "Si no quedan otros otorgamientos ACTIVE/SUSPENDED, cierra la solicitud en CLOSED y libera exactamente una reserva. " +
        "Conserva la resolucion y trabajador asignado. Registra motivo, responsable e instante UTC; no admite reapertura.")
    @ApiResponse(responseCode = "200", description = "Otorgamiento FINALIZED; eventual cierre y liberacion del cupo confirmados.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "409", description = "Solicitud no aprobada, transicion invalida/repetida o contador de cupos inconsistente.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun finalize(@PathVariable id: UUID, @PathVariable grantId: UUID, @RequestBody request: ChangeBenefitGrantStateRequest): BenefitGrantResponse =
        service.finalize(id, grantId, request)

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('applications:management:view')")
    @Operation(summary = "Consultar historial completo de un otorgamiento", description =
        "Solo administracion. Orden ascendente por secuencia, incluyendo asignacion inicial, estados, vigencias, motivos, " +
        "responsables, fechas UTC y copias de los requisitos reevaluados. Incluye EXPIRED automatico con responsable ID null y nombre Sistema. No se expone a ciudadanos.")
    @ApiResponse(responseCode = "200", description = "Historial inmutable del otorgamiento.", useReturnTypeSchema = true)
    fun history(@PathVariable id: UUID, @PathVariable grantId: UUID): List<BenefitGrantChangeResponse> = service.history(id, grantId)
}
