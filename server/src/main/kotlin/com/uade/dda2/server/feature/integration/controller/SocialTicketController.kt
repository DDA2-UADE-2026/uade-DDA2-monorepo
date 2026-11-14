package com.uade.dda2.server.feature.integration.controller

import com.uade.dda2.server.feature.integration.dto.AssignSocialTicketRequest
import com.uade.dda2.server.feature.integration.dto.LinkSocialTicketApplicationRequest
import com.uade.dda2.server.feature.integration.dto.SocialEventQueuedResponse
import com.uade.dda2.server.feature.integration.dto.SocialIntegrationPage
import com.uade.dda2.server.feature.integration.dto.SocialTicketDetailResponse
import com.uade.dda2.server.feature.integration.dto.SocialTicketResponse
import com.uade.dda2.server.feature.integration.dto.UpdateSocialTicketRequest
import com.uade.dda2.server.feature.integration.entity.SocialTicketState
import com.uade.dda2.server.feature.integration.service.SocialTicketManagementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
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

@RestController
@RequestMapping("/api/admin/integrations/core/tickets")
@Validated
@Tag(name = "Core — Tickets M8", description = "Gestión de tickets derivados por M2 a Desarrollo Social.")
class SocialTicketController(
    private val service: SocialTicketManagementService,
) {
    @PreAuthorize("hasAuthority('tickets:management:view')")
    @GetMapping
    @Operation(summary = "Listar tickets de M8")
    fun list(
        @RequestParam(required = false) state: SocialTicketState?,
        @RequestParam(required = false) attention: Boolean?,
        @Min(0) @RequestParam(defaultValue = "0") page: Int,
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") size: Int,
    ): SocialIntegrationPage<SocialTicketResponse> =
        service.list(state, attention, page, size)

    @PreAuthorize("hasAuthority('tickets:management:view')")
    @GetMapping("/{ticketId}")
    @Operation(summary = "Consultar el ticket y su snapshot operativo")
    fun get(
        @PathVariable ticketId: UUID,
    ): SocialTicketDetailResponse =
        service.get(ticketId)

    @PreAuthorize("hasAuthority('tickets:management:manage')")
    @PutMapping("/{ticketId}/assignee")
    @Operation(summary = "Asignar un administrativo", description = "Encola STARTED una sola vez por derivación ROUTED.")
    fun assign(
        @PathVariable ticketId: UUID,
        @Valid @RequestBody request: AssignSocialTicketRequest,
    ): SocialTicketDetailResponse =
        service.assign(ticketId, request)

    @PreAuthorize("hasAuthority('tickets:management:manage')")
    @PutMapping("/{ticketId}/application")
    @Operation(summary = "Vincular la solicitud social del mismo ciudadano")
    fun linkApplication(
        @PathVariable ticketId: UUID,
        @Valid @RequestBody request: LinkSocialTicketApplicationRequest,
    ): SocialTicketDetailResponse =
        service.linkApplication(ticketId, request)

    @PreAuthorize("hasAuthority('tickets:management:manage')")
    @PostMapping("/{ticketId}/updates")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Encolar una actualización operativa", description = "Respuesta 202: guardada en outbox, pendiente de confirmación del broker. El backend genera actor, fecha, evento y causalidad. Ver las reglas por updateType en AsyncAPI.")
    fun update(
        @PathVariable ticketId: UUID,
        @Valid @RequestBody request: UpdateSocialTicketRequest,
    ): SocialEventQueuedResponse =
        service.update(ticketId, request)
}
