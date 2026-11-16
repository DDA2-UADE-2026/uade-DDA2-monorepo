package com.uade.dda2.server.feature.integration.contract

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface M8Payload

@Schema(description = "Envelope Core. El productor omite correlationId y causationId cuando no existen. Los slugs de origen son los del Core; los códigos Mx pertenecen al payload.")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class M8EventEnvelope<T : M8Payload>(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val eventId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val eventType: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val eventVersion: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val occurredAt: OffsetDateTime,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val sourceModule: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:Valid val data: T,
    @field:Schema(nullable = true)
    val correlationId: UUID? = null,
    @field:Schema(nullable = true)
    val causationId: UUID? = null,
)

enum class EventActorType { SYSTEM, EXTERNAL_USER, SELF_SERVICE, AREA_USER, CITIZEN, AGENT, AREA_RESPONSIBLE, ADMIN }

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class EventActorData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val type: EventActorType,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
        description = "UUID canónico del actor. SYSTEM usa null; empleados usan null hasta disponer de actorId de M1.")
    val id: UUID?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventAttachmentData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val fileName: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val contentType: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val url: String,
    @field:Schema(nullable = true)
    @field:Min(0) val sizeBytes: Long? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TicketEscalationData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val active: Boolean,
    @field:Schema(nullable = true)
    val reasonCode: String? = null,
    @field:Schema(nullable = true)
    val escalatedAt: OffsetDateTime? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CitizenAddressData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val propertyId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val street: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val number: String,
    @field:Schema(nullable = true)
    val primary: Boolean? = null,
    @field:Schema(nullable = true)
    val effectiveFrom: LocalDate? = null,
    @field:Schema(nullable = true)
    val effectiveUntil: LocalDate? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class InformationRequestData(
    @field:Schema(nullable = true)
    val messageForCitizen: String? = null,
    @field:Schema(nullable = true)
    val requiredBy: OffsetDateTime? = null,
    @field:Schema(nullable = true)
    val purpose: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class InformationResponseData(
    @field:Schema(nullable = true)
    val message: String? = null,
    @field:Schema(nullable = true)
    val purpose: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResolutionData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val type: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReasonCodeData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val reasonCode: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TicketReopenData(
    @field:Schema(nullable = true)
    val reason: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TicketDuplicateData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val mainTicketId: UUID,
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TicketDetailsData(
    @field:Schema(nullable = true)
    @field:Valid val routing: TicketRoutingData? = null,
    @field:Schema(nullable = true)
    @field:Valid val informationRequest: InformationRequestData? = null,
    @field:Schema(nullable = true)
    @field:Valid val informationResponse: InformationResponseData? = null,
    @field:Schema(nullable = true)
    @field:Valid val returnInfo: ReasonCodeData? = null,
    @field:Schema(nullable = true)
    @field:Valid val resolution: ResolutionData? = null,
    @field:Schema(nullable = true)
    @field:Valid val cancellation: ReasonCodeData? = null,
    @field:Schema(nullable = true)
    @field:Valid val reopen: TicketReopenData? = null,
    @field:Schema(nullable = true)
    @field:Valid val escalation: TicketEscalationData? = null,
    @field:Schema(nullable = true)
    @field:Valid val duplicate: TicketDuplicateData? = null,
    @field:Schema(nullable = true)
    @field:Valid val closure: ReasonCodeData? = null,
)
