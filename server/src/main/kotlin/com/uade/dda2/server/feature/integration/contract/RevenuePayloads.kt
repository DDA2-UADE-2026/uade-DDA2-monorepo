package com.uade.dda2.server.feature.integration.contract

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Rentas → M8. Nombre exacto debtOverdue. Se registra para consulta administrativa; no cambia elegibilidad social.")
@JsonIgnoreProperties(ignoreUnknown = true)
data class DebtOverdueData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val debtId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val citizenId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val conceptCode: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val outstandingAmount: BigDecimal,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val dueDate: LocalDate,
) : M8Payload

@Schema(description = "Rentas → M8. Procesar solo MUNICIPAL_TAX con citizenId no null y deuda conocida. Otros orígenes se confirman sin efectos; infractionId corresponde a TRAFFIC_FINE.")
@JsonIgnoreProperties(ignoreUnknown = true)
data class DebtSettledData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val debtId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) val citizenId: UUID?,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val conceptCode: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val originType: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val originId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val settledAt: OffsetDateTime,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val remainingBalance: BigDecimal,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val outstandingBalance: BigDecimal,
    @field:Schema(nullable = true)
    val infractionId: UUID? = null,
) : M8Payload

@Schema(description = "Rentas → M8. Registra el pedido y lo vincula con el beneficio TAX_EXEMPTION activo cuando la correspondencia es inequívoca.")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExemptionRequestedData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val requestId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val citizenId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val conceptCode: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val reason: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val requestedPercentage: BigDecimal,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val requestedFrom: LocalDate,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val requestedUntil: LocalDate,
) : M8Payload

enum class ExemptionEventStatus { APPROVED, REJECTED }

@Schema(description = "Rentas → M8. APPROVED exige exemptionId, percentage, validFrom y validUntil. REJECTED exige reason y no requiere identificador ni fechas de exención.")
@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateExemptionStatusData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val requestId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val citizenId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val status: ExemptionEventStatus,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val conceptCode: String,
    @field:Schema(nullable = true)
    val exemptionId: UUID? = null,
    @field:Schema(nullable = true)
    val percentage: BigDecimal? = null,
    @field:Schema(nullable = true)
    val validFrom: LocalDate? = null,
    @field:Schema(nullable = true)
    val validUntil: LocalDate? = null,
    @field:Schema(nullable = true)
    val reason: String? = null,
) : M8Payload
