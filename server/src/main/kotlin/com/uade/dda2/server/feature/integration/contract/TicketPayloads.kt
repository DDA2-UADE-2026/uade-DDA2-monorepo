package com.uade.dda2.server.feature.integration.contract

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class TicketLocationData(
    @field:Schema(nullable = true) val addressLine: String? = null,
    @field:Schema(nullable = true) val street: String? = null,
    @field:Schema(nullable = true) val streetNumber: String? = null,
    @field:Schema(nullable = true) val neighborhoodId: UUID? = null,
    @field:Schema(nullable = true) val reference: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TicketRoutingData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val requestType: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val category: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val subcategory: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val ticketType: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val summary: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val description: String,
    @field:Schema(type = "object", implementation = Map::class, additionalProperties = Schema.AdditionalPropertiesValue.TRUE, nullable = true,
        description = "Respuestas dinámicas del formulario como objeto JSON.")
    val formData: JsonNode? = null,
    @field:Schema(nullable = true)
    @field:Valid val location: TicketLocationData? = null,
    @field:Schema(nullable = true)
    val resolutionDueAt: OffsetDateTime? = null,
    @field:Schema(nullable = true)
    @field:Valid val escalation: TicketEscalationData? = null,
    val reassignment: Boolean = false,
)

@Schema(description = "Atención Ciudadana → M8, broadcast. Orden por updatedAt antes de modificar una gestión existente. Otro responsibleAreaId o reclasificación: ack sin contenido. ROUTED admite citizenId=null cuando isAnonymous=true.")
@JsonIgnoreProperties(ignoreUnknown = true)
data class TicketUpdatedData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val ticketId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val publicId: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    val citizenId: UUID?,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @get:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @get:JsonProperty("isAnonymous") val isAnonymous: Boolean,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val responsibleAreaId: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val updateType: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val currentStatus: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val currentPriority: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
    @field:Schema(nullable = true)
    @field:Valid val details: TicketDetailsData? = null,
    @field:Schema(nullable = true)
    val publicMessage: String? = null,
    @field:Schema(nullable = true)
    val progress: Int? = null,
    @field:Valid val attachments: List<EventAttachmentData> = emptyList(),
) : M8Payload
