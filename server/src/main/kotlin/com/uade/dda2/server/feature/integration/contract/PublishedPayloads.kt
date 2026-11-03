package com.uade.dda2.server.feature.integration.contract

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RequiredDocumentData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val requirementId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val type: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val name: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val required: Boolean,
    @field:Schema(nullable = true)
    val description: String? = null,
)

@Schema(description = "M8 → Ciudadanos. Se emite al crear una solicitud cuya edición exige documentación. correlationId=applicationId.")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SocialDocumentationRequestedData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val citizenId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val applicationId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val programId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val programEditionId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:Valid @field:Size(max = 10) val requiredDocuments: List<RequiredDocumentData>,
    @field:Schema(nullable = true)
    val applicationNumber: String? = null,
    @field:Schema(nullable = true)
    val notes: String? = null,
) : M8Payload

enum class SocialBenefitEventStatus { APPROVED, SUSPENDED, FINALIZED }
enum class SocialBenefitEventType { TAX_EXEMPTION, HOUSING_SUBSIDY, FOOD_ASSISTANCE, UTILITY_SUBSIDY }

@Schema(description = "M8 → Rentas. Un evento por benefit_grant; benefitId estable. ACTIVE se publica como APPROVED y EXPIRED como FINALIZED. No existe REJECTED. reason es null en APPROVED y obligatorio en SUSPENDED/FINALIZED.")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class SocialBenefitUpdatedData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val benefitId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val citizenId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val applicationId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val programId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val programEditionId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val programName: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val benefitTypeId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val type: SocialBenefitEventType,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
        description = "La clave amount siempre se envía; null para beneficios no monetarios.")
    val amount: BigDecimal?,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val status: SocialBenefitEventStatus,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val startDate: LocalDate,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val endDate: LocalDate,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
    @field:Schema(nullable = true)
    val description: String? = null,
    @field:Schema(nullable = true)
    val reason: String? = null,
) : M8Payload

enum class TicketUpdateType { STARTED, PROGRESS, INFORMATION_REQUIRED, RETURNED, RESOLVED, REJECTED }

@Schema(description = "M8 → Atención Ciudadana. STARTED solo una vez por derivación; PROGRESS requiere contenido; INFORMATION_REQUIRED requiere informationRequest; RETURNED returnInfo; RESOLVED resolution y publicMessage; REJECTED cancellation. Correlación y causa provienen del ticketUpdated al que se responde.")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateTicketStatusData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val ticketId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val updateType: TicketUpdateType,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:Valid val updatedBy: EventActorData,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val updateOccurredAt: OffsetDateTime,
    @field:Schema(nullable = true)
    val publicMessage: String? = null,
    @field:Schema(nullable = true)
    val internalMessage: String? = null,
    @field:Schema(nullable = true)
    @field:Min(0) @field:Max(100) val progress: Int? = null,
    @field:Schema(nullable = true)
    @field:Valid val details: TicketDetailsData? = null,
    @field:Valid val attachments: List<EventAttachmentData> = emptyList(),
) : M8Payload
