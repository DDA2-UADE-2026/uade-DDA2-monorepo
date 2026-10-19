package com.uade.dda2.server.feature.benefit.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.benefit.dto.request.CreateBenefitGrantRequest
import com.uade.dda2.server.feature.benefit.dto.response.BenefitGrantResponse
import com.uade.dda2.server.feature.benefit.service.BenefitGrantService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/applications/{id}/benefit-grants", produces = ["application/json"])
@Tag(name = "Otorgamientos de beneficios", description = "Asignacion individual y consulta administrativa de beneficios otorgados.")
@ApiResponse(responseCode = "401", description = "Sin acceso valido o usuario inexistente/inactivo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "403", description = "Sin permiso vigente en JWT y rol activo de base de datos.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "404", description = "Solicitud inexistente; en el alta tambien beneficio inexistente o ajeno a la edicion.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
class AdminBenefitGrantController(private val service: BenefitGrantService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('applications:management:benefits:assign')")
    @Operation(summary = "Otorgar un beneficio a una solicitud aprobada", description =
        "Exige solicitud APPROVED y edicion ACTIVE. La vigencia es inclusiva, debe incluir hoy en la zona configurada y estar " +
        "dentro de la edicion. El beneficio debe pertenecer a esa edicion. Copia tipo, descripcion y monto del catalogo, sin " +
        "aceptar personalizaciones. Revalida aprobaciones incompatibles con fechas de edicion superpuestas. Obtiene titular, " +
        "responsable y fecha del servidor y crea en ACTIVE. Admite distintos beneficios por solicitud, sin repetir el mismo. " +
        "Conserva APPROVED, los datos de resolucion y el cupo ya ocupado. No reevalua requisitos ni documentos.")
    @ApiResponse(responseCode = "201", description = "Otorgamiento registrado y auditado.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Cuerpo, UUID, fechas o campos extra invalidos; vigencia que no incluye hoy o excede la edicion.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "409", description = "Solicitud no aprobada, edicion no activa, beneficio ya otorgado o aprobacion incompatible.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun create(@PathVariable id: UUID, @RequestBody request: CreateBenefitGrantRequest): BenefitGrantResponse = service.create(id, request)

    @GetMapping
    @PreAuthorize("hasAuthority('applications:management:view')")
    @Operation(summary = "Consultar beneficios otorgados a una solicitud", description =
        "Solo administracion. Orden por assignedAt e id ascendentes. Una solicitud sin otorgamientos devuelve []. " +
        "Los datos copiados reflejan el momento del otorgamiento y no siguen las modificaciones del catalogo.")
    @ApiResponse(responseCode = "200", description = "Otorgamientos de la solicitud.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Identificador invalido.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun list(@PathVariable id: UUID): List<BenefitGrantResponse> = service.list(id)
}
