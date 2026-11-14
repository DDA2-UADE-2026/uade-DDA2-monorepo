package com.uade.dda2.server.feature.integration.dto

import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import com.uade.dda2.server.feature.integration.contract.EventAttachmentData
import com.uade.dda2.server.feature.integration.contract.TicketDetailsData
import com.uade.dda2.server.feature.integration.contract.TicketUpdateType
import com.uade.dda2.server.feature.integration.entity.SocialTicketState
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import tools.jackson.databind.JsonNode
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class SocialIntegrationPage<T>(val content: List<T>, val page: Int, val size: Int,
    val totalElements: Long, val totalPages: Int)

data class SocialTicketResponse(val id: UUID, val publicId: String, val citizenId: UUID?, val anonymous: Boolean,
    val responsibleAreaId: String, val currentStatus: String, val currentPriority: String, val state: SocialTicketState,
    val applicationId: UUID?, val assignedWorkerId: Long?, val informationPending: Boolean, val requiresAttention: Boolean,
    val sourceUpdatedAt: Instant, val closedReason: String?)

data class SocialTicketDetailResponse(val ticket: SocialTicketResponse, @field:Schema(type = "object", implementation = Map::class, additionalProperties = Schema.AdditionalPropertiesValue.TRUE) val routing: JsonNode,
    val correlationId: UUID?, val responseCausationId: UUID, @field:Schema(type = "object", implementation = Map::class, additionalProperties = Schema.AdditionalPropertiesValue.TRUE, nullable = true) val informationResponse: JsonNode?, val reopenReason: String?, val attachments: List<EventAttachmentData>)

data class AssignSocialTicketRequest(@field:Positive val workerId: Long)
data class LinkSocialTicketApplicationRequest(val applicationId: UUID)

data class UpdateSocialTicketRequest(
    val updateType: TicketUpdateType,
    val publicMessage: String? = null,
    val internalMessage: String? = null,
    @field:Min(0) @field:Max(100) val progress: Int? = null,
    @field:Valid val details: TicketDetailsData? = null,
    @field:Valid val attachments: List<EventAttachmentData> = emptyList(),
)

data class SocialEventQueuedResponse(val eventId: UUID, val status: String = "PENDING")

data class RevenueDebtResponse(val id: UUID, val citizenId: UUID, val conceptCode: String,
    val outstandingAmount: BigDecimal, val dueDate: LocalDate, val settled: Boolean, val settledAt: Instant?,
    val remainingBalance: BigDecimal?, val outstandingBalance: BigDecimal?, val sourceUpdatedAt: Instant)

data class RevenueExemptionResponse(val id: UUID, val citizenId: UUID, val conceptCode: String,
    val requestReason: String?, val requestedPercentage: BigDecimal?, val requestedFrom: LocalDate?,
    val requestedUntil: LocalDate?, val benefitGrantId: UUID?, val ambiguousBenefit: Boolean, val status: String,
    val exemptionId: UUID?, val percentage: BigDecimal?, val validFrom: LocalDate?, val validUntil: LocalDate?, val resolutionReason: String?)

data class LinkExemptionBenefitRequest(val benefitGrantId: UUID)

data class ExternalDocumentationResponse(val requestId: UUID, val applicationId: UUID, val requirementId: UUID,
    val fileId: UUID, val fileNumber: String, val documentId: UUID?, val deliveryStatus: String,
    val reviewStatus: ApplicationDocumentStatus, val observation: String?, val reviewedByUserId: Long?, val reviewedAt: Instant?)

data class CitizenIntegrationAlertResponse(val userId: Long, val citizenId: String?, val blocked: Boolean,
    val reason: String?, val reviewRequired: Boolean, val deceasedAt: LocalDate?, val sourceUpdatedAt: Instant?)
