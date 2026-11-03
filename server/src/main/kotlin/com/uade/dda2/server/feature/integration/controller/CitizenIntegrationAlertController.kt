package com.uade.dda2.server.feature.integration.controller

import com.uade.dda2.server.feature.integration.dto.CitizenIntegrationAlertResponse
import com.uade.dda2.server.feature.integration.dto.SocialIntegrationPage
import com.uade.dda2.server.feature.integration.service.CitizenIntegrationAlertService
import io.swagger.v3.oas.annotations.Operation
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
@RequestMapping("/api/admin/integrations/core/citizen-alerts")
@Validated
@Tag(name = "Core — Alertas M1", description = "Restricciones de Ciudadanos que requieren revisión administrativa.")
class CitizenIntegrationAlertController(
    private val service: CitizenIntegrationAlertService,
) {
    @PreAuthorize("hasAuthority('users:view')")
    @GetMapping
    @Operation(summary = "Consultar titulares bloqueados o fallecidos")
    fun list(
        @Min(0) @RequestParam(defaultValue = "0") page: Int,
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") size: Int,
    ): SocialIntegrationPage<CitizenIntegrationAlertResponse> =
        service.list(page, size)
}
