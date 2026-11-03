package com.uade.dda2.server.feature.integration.contract

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate
import java.util.UUID

enum class ExternalDocumentationStatus { PENDING, COMPLETED }

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalDocumentationRequestData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val requestId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val requirementId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val documentType: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val status: ExternalDocumentationStatus,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
        description = "UUID en M1 si COMPLETED; null si PENDING. La entrega no implica validación administrativa.")
    val documentId: UUID?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalFileData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val fileId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val number: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val initiatingArea: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank val status: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:Valid val documentationRequests: List<ExternalDocumentationRequestData>,
)

@Schema(description = "Ciudadanos → M8. Correlación=applicationId, causationId=eventId del pedido. Controlar ciudadano, programa y edición. Solicitud desconocida: ack sin efectos.")
@JsonIgnoreProperties(ignoreUnknown = true)
data class SocialDocumentationDeliveredData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val citizenId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val applicationId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val programId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val programEditionId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:Valid val files: List<ExternalFileData>,
) : M8Payload

enum class CitizenUpdateType { REGISTERED, ADDRESS_UPDATED, BLOCKED, DECEASED }

@JsonIgnoreProperties(ignoreUnknown = true)
data class CitizenDetailsData(
    @field:Schema(nullable = true)
    val dni: String? = null,
    @field:Schema(nullable = true)
    val cuil: String? = null,
    @field:Schema(nullable = true)
    val name: String? = null,
    @field:Schema(nullable = true)
    val lastname: String? = null,
    @field:Schema(nullable = true)
    val birthdate: LocalDate? = null,
    @field:Schema(nullable = true)
    val state: String? = null,
    @field:Schema(nullable = true)
    val nationality: String? = null,
    @field:Schema(nullable = true)
    @field:Valid val address: CitizenAddressData? = null,
    @field:Schema(nullable = true)
    @field:Valid val previousAddress: CitizenAddressData? = null,
    @field:Schema(nullable = true)
    @field:Valid val newAddress: CitizenAddressData? = null,
    @field:Schema(nullable = true)
    val reason: String? = null,
    @field:Schema(nullable = true)
    val reviewRequired: Boolean? = null,
    @field:Schema(nullable = true)
    val deceasedAt: LocalDate? = null,
    @field:Schema(nullable = true)
    val source: String? = null,
)

@Schema(description = "Ciudadanos → M8. Solo usuarios ya vinculados por el UUID canónico. REGISTERED actualiza snapshot; ADDRESS_UPDATED domicilio; BLOCKED impide nuevas solicitudes; DECEASED finaliza beneficios y cierra solicitudes abiertas.")
@JsonIgnoreProperties(ignoreUnknown = true)
data class CitizenUpdatedData(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val citizenId: UUID,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val updateType: CitizenUpdateType,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:Valid val details: CitizenDetailsData,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:Valid val updatedBy: EventActorData,
) : M8Payload
