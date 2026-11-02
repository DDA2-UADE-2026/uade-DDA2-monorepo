package com.uade.dda2.server.feature.integration.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.integration.dto.CoreConnectionStatusResponse
import com.uade.dda2.server.feature.integration.dto.CoreDashboardResponse
import com.uade.dda2.server.feature.integration.dto.CoreEventDetail
import com.uade.dda2.server.feature.integration.dto.CoreEventPage
import com.uade.dda2.server.feature.integration.dto.CoreEventStatus
import com.uade.dda2.server.feature.integration.dto.CoreLocalMetrics
import com.uade.dda2.server.feature.integration.dto.CorePublication
import com.uade.dda2.server.feature.integration.dto.CoreSubscription
import com.uade.dda2.server.feature.integration.service.CoreDashboardAccess.Companion.CORE_VIEW_PERMISSION
import com.uade.dda2.server.feature.integration.service.CoreDashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/integrations/core", produces = ["application/json"])
@Tag(
    name = "Integración Core",
    description = "Monitoreo de Desarrollo Social en Core. Requiere integrations:core:view en el rol activo. " +
        "Usa credenciales de máquina privadas del servidor y no modifica registros ni mensajes.",
)
@ApiResponse(
    responseCode = "200",
    description = "Consulta completada. En /status, revisar el estado de cada transporte en el cuerpo.",
    useReturnTypeSchema = true,
)
@ApiResponse(
    responseCode = "502",
    description = "Core respondió con un error HTTP o un contrato inesperado.",
    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
)
@ApiResponse(
    responseCode = "503",
    description = "Integración desactivada, secreto HTTP ausente o Core inaccesible.",
    content = [Content(schema = Schema(implementation = ErrorResponse::class))],
)
class CoreDashboardController(
    private val service: CoreDashboardService,
) {
    @PreAuthorize("hasAuthority('$CORE_VIEW_PERMISSION')")
    @GetMapping("/status")
    @Operation(
        operationId = "getCoreConnectionStatus",
        summary = "Comprobar acceso HTTP y AMQPS",
        description = "Realiza GET /dashboard autenticado y una consulta pasiva de la cola propia. " +
            "Devuelve el resultado de cada transporte, incluso ante fallo, con código y HTTP remoto cuando existen. " +
            "No publica, consume ni purga mensajes. Una respuesta local 200 no significa que ambos transportes estén UP. " +
            "No verifica el worker ni el recorrido de eventos.",
    )
    fun status(): CoreConnectionStatusResponse =
        service.status()

    @PreAuthorize("hasAuthority('$CORE_VIEW_PERMISSION')")
    @GetMapping("/dashboard")
    @Operation(
        operationId = "getCoreDashboard",
        summary = "Consultar las métricas del módulo en Core",
        description = "Combina el dashboard remoto del módulo autenticado con contadores locales acumulados de outbox/inbox. " +
            "windowHours afecta las métricas remotas que Core define por ventana; " +
            "no todos sus contadores se limitan a ese período. DELIVERED en Core y BROKER_CONFIRMED local " +
            "no acreditan procesamiento de negocio.",
    )
    fun dashboard(
        @Parameter(description = "Ventana remota en horas, de 1 a 720.", example = "24")
        @RequestParam(defaultValue = "24") windowHours: Int,
    ): CoreDashboardResponse =
        service.dashboard(windowHours)

    @PreAuthorize("hasAuthority('$CORE_VIEW_PERMISSION')")
    @GetMapping("/metrics")
    @Operation(
        operationId = "getCoreLocalMetrics",
        summary = "Consultar contadores locales de outbox e inbox",
        description = "Consulta solo nuestra base, incluso si Core está desactivado o inaccesible. " +
            "No hace solicitudes remotas ni devuelve payloads.",
    )
    fun metrics(): CoreLocalMetrics =
        service.metrics()

    @PreAuthorize("hasAuthority('$CORE_VIEW_PERMISSION')")
    @GetMapping("/publications")
    @Operation(
        operationId = "listCorePublications",
        summary = "Consultar nuestras publicaciones declaradas",
        description = "Filtra la respuesta global de Core por desarrollo-social. No crea ni reactiva publicaciones.",
    )
    fun publications(): List<CorePublication> =
        service.publications()

    @PreAuthorize("hasAuthority('$CORE_VIEW_PERMISSION')")
    @GetMapping("/subscriptions")
    @Operation(
        operationId = "listCoreSubscriptions",
        summary = "Consultar nuestras suscripciones",
        description = "Devuelve solo las suscripciones de desarrollo-social, su cola, estado y maxAttempts. No las modifica.",
    )
    fun subscriptions(): List<CoreSubscription> =
        service.subscriptions()

    @PreAuthorize("hasAuthority('$CORE_VIEW_PERMISSION')")
    @GetMapping("/events")
    @Operation(
        operationId = "listCoreEvents",
        summary = "Consultar eventos visibles para nuestro módulo",
        description = "Consulta el listado remoto paginado. Core limita el tráfico a nuestra identidad de máquina. " +
            "page comienza en 1, size admite de 1 a 200. El listado incluye metadatos, sin el sobre completo.",
    )
    fun events(
        @Parameter(description = "Número de página, comenzando en uno.", example = "1")
        @RequestParam(defaultValue = "1") page: Int,

        @Parameter(description = "Cantidad de elementos por página, entre 1 y 200.", example = "25")
        @RequestParam(defaultValue = "25") size: Int,

        @Parameter(description = "Nombre exacto del tipo de evento, de 1 a 120 caracteres.")
        @RequestParam(required = false) eventType: String?,

        @Parameter(description = "Estado de ingesta y ruteo del evento en Core.")
        @RequestParam(required = false) status: CoreEventStatus?,

        @Parameter(description = "UUID de correlación del trámite.")
        @RequestParam(required = false) correlationId: UUID?,
    ): CoreEventPage =
        service.events(
            page = page,
            size = size,
            eventType = eventType,
            status = status,
            correlationId = correlationId,
        )

    @PreAuthorize("hasAuthority('$CORE_VIEW_PERMISSION')")
    @GetMapping("/events/{eventId}")
    @Operation(
        operationId = "getCoreEvent",
        summary = "Consultar un evento por su eventId",
        description = "Consulta el UUID lógico del evento, no el ID interno de Core. " +
            "Incluye sobre y entregas; puede contener datos del trámite y requiere el permiso de integración. " +
            "Un 404 inmediato tras publicar puede indicar que el worker todavía no lo procesó.",
    )
    @ApiResponse(
        responseCode = "404",
        description = "Evento inexistente o no visible para Desarrollo Social.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun event(
        @Parameter(description = "UUID lógico del evento enviado o recibido.")
        @PathVariable eventId: UUID,
    ): CoreEventDetail =
        service.event(eventId)
}
