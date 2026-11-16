package com.uade.dda2.server.feature.integration.validator

import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import jakarta.validation.Validator
import org.springframework.stereotype.Component

@Component
class M8PayloadValidator(private val validator: Validator) {
    fun validate(payload: M8Payload) {
        valid(validator.validate(payload).isEmpty())
        when (payload) {
            is SocialBenefitUpdatedData -> {
                if (payload.status != SocialBenefitEventStatus.FINALIZED) valid(payload.endDate >= payload.startDate)
                valid(if (payload.status == SocialBenefitEventStatus.APPROVED) payload.reason == null else !payload.reason.isNullOrBlank())
            }
            is UpdateTicketStatusData -> validateTicketUpdate(payload)
            is TicketUpdatedData -> {
                // Broadcast ajeno o de reclasificación: solo se requieren los metadatos comunes.
                if (payload.responsibleAreaId != "M8" || isReclassification(payload)) return
                valid(payload.isAnonymous == (payload.citizenId == null))
                if (payload.updateType == "ROUTED") {
                    valid(payload.details?.routing != null)
                    valid(payload.details!!.routing!!.formData?.let { it.isNull || it.isObject } != false)
                }
                if (payload.updateType == "INFORMATION_PROVIDED") valid(
                    !payload.details?.informationResponse?.message.isNullOrBlank() || payload.attachments.isNotEmpty())
                val escalation = if (payload.updateType == "ESCALATION_CHANGED") {
                    valid(payload.details?.escalation != null)
                    payload.details?.escalation
                } else payload.details?.routing?.escalation
                if (escalation?.active == true) valid(escalation.reasonCode in setOf("CRITICAL_PRIORITY", "SLA_BREACHED", "MANUAL") && escalation.escalatedAt != null)
            }
            is CitizenUpdatedData -> validateCitizen(payload)
            is SocialDocumentationDeliveredData -> payload.files.flatMap { it.documentationRequests }.forEach {
                valid((it.status == ExternalDocumentationStatus.COMPLETED) == (it.documentId != null))
            }
            is ExemptionRequestedData -> valid(payload.requestedUntil >= payload.requestedFrom)
            is UpdateExemptionStatusData -> if (payload.status == ExemptionEventStatus.APPROVED) {
                valid(payload.exemptionId != null && payload.percentage != null && payload.validFrom != null && payload.validUntil != null)
                valid(payload.validUntil!! >= payload.validFrom!!)
            } else valid(!payload.reason.isNullOrBlank())
            else -> Unit
        }
    }

    fun isReclassification(payload: TicketUpdatedData): Boolean =
        payload.updateType in setOf("INFORMATION_REQUIRED", "INFORMATION_PROVIDED") &&
            (payload.details?.informationRequest?.purpose == "RECLASSIFICATION" || payload.details?.informationResponse?.purpose == "RECLASSIFICATION")

    private fun validateTicketUpdate(payload: UpdateTicketStatusData) {
        valid(payload.updatedBy.type in setOf(EventActorType.SYSTEM, EventActorType.EXTERNAL_USER))
        valid(payload.updatedBy.type != EventActorType.SYSTEM || payload.updatedBy.id == null)
        val details = payload.details
        // El modelo de entrada comparte details de M2; la salida solo incluye su variante operativa.
        val provided = listOfNotNull(details?.routing?.let { "routing" }, details?.informationRequest?.let { "informationRequest" },
            details?.informationResponse?.let { "informationResponse" }, details?.returnInfo?.let { "returnInfo" },
            details?.resolution?.let { "resolution" }, details?.cancellation?.let { "cancellation" },
            details?.reopen?.let { "reopen" }, details?.escalation?.let { "escalation" },
            details?.duplicate?.let { "duplicate" }, details?.closure?.let { "closure" })
        val allowed = when (payload.updateType) {
            TicketUpdateType.STARTED, TicketUpdateType.PROGRESS -> emptySet()
            TicketUpdateType.INFORMATION_REQUIRED -> setOf("informationRequest")
            TicketUpdateType.RETURNED -> setOf("returnInfo")
            TicketUpdateType.RESOLVED -> setOf("resolution")
            TicketUpdateType.REJECTED -> setOf("cancellation")
        }
        valid(provided.all { it in allowed } && details?.informationRequest?.purpose == null)
        when (payload.updateType) {
            TicketUpdateType.STARTED -> Unit
            TicketUpdateType.PROGRESS -> valid(payload.progress != null || !payload.publicMessage.isNullOrBlank() ||
                !payload.internalMessage.isNullOrBlank() || payload.attachments.isNotEmpty())
            TicketUpdateType.INFORMATION_REQUIRED -> valid(!details?.informationRequest?.messageForCitizen.isNullOrBlank())
            TicketUpdateType.RETURNED -> {
                valid(details?.returnInfo?.reasonCode in setOf("INVALID_INFORMATION", "REQUEST_TYPE_MISMATCH", "INSUFFICIENT_CONTEXT", "OTHER"))
                valid(!payload.publicMessage.isNullOrBlank() || !payload.internalMessage.isNullOrBlank())
            }
            TicketUpdateType.RESOLVED -> {
                valid(!payload.publicMessage.isNullOrBlank())
                valid(details?.resolution?.type in setOf("ACTION_COMPLETED", "REQUEST_FULFILLED", "INQUIRY_ANSWERED", "ACKNOWLEDGED", "NO_FURTHER_ACTION_REQUIRED"))
            }
            TicketUpdateType.REJECTED -> {
                valid(details?.cancellation?.reasonCode in setOf("OUT_OF_SCOPE", "DOES_NOT_APPLY", "INVALID_REQUEST_TYPE", "INVALID_DATA", "REJECTED_BY_AREA", "OTHER"))
                valid(!payload.publicMessage.isNullOrBlank() || !payload.internalMessage.isNullOrBlank())
            }
        }
    }

    private fun validateCitizen(payload: CitizenUpdatedData) {
        val d = payload.details
        when (payload.updateType) {
            CitizenUpdateType.REGISTERED -> valid(!d.dni.isNullOrBlank() && !d.cuil.isNullOrBlank() && !d.name.isNullOrBlank() &&
                !d.lastname.isNullOrBlank() && d.birthdate != null && !d.state.isNullOrBlank() && !d.nationality.isNullOrBlank() && d.address != null)
            CitizenUpdateType.ADDRESS_UPDATED -> valid(d.previousAddress != null && d.newAddress != null)
            CitizenUpdateType.BLOCKED -> valid(!d.reason.isNullOrBlank() && d.reviewRequired != null)
            CitizenUpdateType.DECEASED -> valid(d.deceasedAt != null && !d.source.isNullOrBlank())
        }
    }

    private fun valid(condition: Boolean) {
        if (!condition) throw CoreIntegrationException("M8_INVALID_PAYLOAD")
    }
}
