package com.uade.dda2.server.feature.integration.controller

import com.uade.dda2.server.feature.integration.dto.LinkExemptionBenefitRequest
import com.uade.dda2.server.feature.integration.dto.RevenueDebtResponse
import com.uade.dda2.server.feature.integration.dto.RevenueExemptionResponse
import com.uade.dda2.server.feature.integration.dto.SocialIntegrationPage
import com.uade.dda2.server.feature.integration.service.RevenueManagementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/integrations/core/revenue")
@Validated
@Tag(name = "Core — Rentas M8", description = "Consulta administrativa de deuda y exenciones; no modifica elegibilidad ni beneficios.")
class RevenueIntegrationController(
    private val service: RevenueManagementService,
) {
    @PreAuthorize("hasAuthority('revenue:management:view')")
    @GetMapping("/debts")
    @Operation(summary = "Consultar deudas recibidas de Rentas")
    fun debts(
        @RequestParam(required = false) citizenId: UUID?,
        @Min(0) @RequestParam(defaultValue = "0") page: Int,
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") size: Int,
    ): SocialIntegrationPage<RevenueDebtResponse> =
        service.debts(citizenId, page, size)

    @PreAuthorize("hasAuthority('revenue:management:view')")
    @GetMapping("/exemptions")
    @Operation(summary = "Consultar solicitudes de exención y resoluciones")
    fun exemptions(
        @RequestParam(required = false) citizenId: UUID?,
        @Min(0) @RequestParam(defaultValue = "0") page: Int,
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") size: Int,
    ): SocialIntegrationPage<RevenueExemptionResponse> =
        service.exemptions(citizenId, page, size)

    @PreAuthorize("hasAuthority('revenue:management:manage')")
    @PutMapping("/exemptions/{requestId}/benefit")
    @Operation(summary = "Vincular una exención a un beneficio", description = "Permite resolver coincidencias ambiguas entre beneficios TAX_EXEMPTION activos del mismo ciudadano.")
    fun link(
        @PathVariable requestId: UUID,
        @Valid @RequestBody request: LinkExemptionBenefitRequest,
    ): RevenueExemptionResponse =
        service.link(requestId, request)
}
