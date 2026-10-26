package com.uade.dda2.server.feature.benefit.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.benefit.dto.response.CitizenBenefitGrantListResponse
import com.uade.dda2.server.feature.benefit.service.CitizenBenefitGrantService
import com.uade.dda2.server.feature.benefit.service.OWN_BENEFIT_VIEW_PERMISSION
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
@RequestMapping("/api/benefit-grants", produces = ["application/json"])
@Tag(name = "Beneficios propios", description = "Consulta de todos los beneficios otorgados al titular autenticado.")
class CitizenBenefitGrantController(private val service: CitizenBenefitGrantService) {
    @GetMapping
    @PreAuthorize("hasAuthority('$OWN_BENEFIT_VIEW_PERMISSION')")
    @Operation(operationId = "listOwnBenefitGrants", summary = "Consultar mis beneficios asignados", description =
        "Requiere applications:own:benefits:view vigente en JWT y rol activo de base de datos. " +
        "El titular se obtiene de la autenticacion, sin seleccionar otro usuario. Incluye ACTIVE, SUSPENDED, FINALIZED y EXPIRED, " +
        "tambien de solicitudes cerradas y ediciones inactivas. Muestra datos copiados al otorgar y estado/vigencia actuales. " +
        "Orden por assignedAt e id descendentes. Si no hay beneficios, content=[] y message=No tenés beneficios asignados.; " +
        "si existen, message=null, incluso en una pagina fuera de rango. No modifica datos ni fuerza vencimientos.")
    @ApiResponse(responseCode = "200", description = "Pagina de beneficios propios, tambien cuando esta vacia.",
        content = [Content(schema = Schema(implementation = CitizenBenefitGrantListResponse::class), examples = [
            ExampleObject(name = "beneficiosPropios", summary = "Otorgamientos propios, con monto y con importe nulo", value = OWN_BENEFITS_EXAMPLE),
            ExampleObject(name = "sinBeneficios", summary = "Titular sin beneficios asignados", value = EMPTY_BENEFITS_EXAMPLE),
        ])])
    @ApiResponse(responseCode = "400", description = "Paginacion invalida.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "401", description = "Sin autenticacion valida o usuario inexistente/inactivo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "403", description = "Sin permiso vigente en JWT y rol activo actual.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun list(
        @Parameter(description = "Numero de pagina, comenzando en cero.", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "Elementos por pagina, entre 1 y 100.", example = "20")
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): CitizenBenefitGrantListResponse = service.list(page, size)
}

private const val EMPTY_BENEFITS_EXAMPLE = """{
  "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0,
  "message": "No tenés beneficios asignados."
}"""

private const val OWN_BENEFITS_EXAMPLE = """{
  "content": [
    {
      "id": "3cb52e5b-d02b-4894-a91e-32f9a8213888",
      "applicationId": "2aa19e54-6f3f-4eef-99b1-8e1af9d8db26", "applicationNumber": 15432,
      "programBenefitId": "750e8400-e29b-41d4-a716-446655440002",
      "programId": "550e8400-e29b-41d4-a716-446655440000", "programName": "Apoyo alimentario",
      "programEditionId": "650e8400-e29b-41d4-a716-446655440001", "programEditionName": "Edicion 2026",
      "benefitType": "FOOD_ASSISTANCE", "description": "Asistencia alimentaria mensual.", "amount": 25000.00,
      "status": "ACTIVE", "startDate": "2026-10-01", "endDate": "2026-12-31"
    },
    {
      "id": "80a0b155-c17f-4e03-b7ac-47ad8659d076",
      "applicationId": "aff51e91-1992-495c-a094-616f915485ba", "applicationNumber": 15431,
      "programBenefitId": "9e82457c-df91-4945-bc80-251f14b65c72",
      "programId": "2c14b4de-e04d-4800-92ec-12710f82ef55", "programName": "Apoyo habitacional",
      "programEditionId": "ec136809-2e54-43f0-b609-c44677e9fbdb", "programEditionName": "Edicion 2026",
      "benefitType": "TAX_EXEMPTION", "description": null, "amount": null,
      "status": "FINALIZED", "startDate": "2026-09-01", "endDate": "2026-09-30"
    }
  ],
  "page": 0, "size": 20, "totalElements": 2, "totalPages": 1, "message": null
}"""
