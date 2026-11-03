package com.uade.dda2.server.feature.integration.contract

enum class M8EventDirection { SEND, RECEIVE }

enum class M8EventType(
    val wireName: String,
    val direction: M8EventDirection,
    val sourceModule: String,
    val payloadClass: Class<out M8Payload>,
) {
    SOCIAL_DOCUMENTATION_REQUESTED("socialDocumentationRequested", M8EventDirection.SEND, "desarrollo-social", SocialDocumentationRequestedData::class.java),
    SOCIAL_BENEFIT_UPDATED("socialBenefitUpdated", M8EventDirection.SEND, "desarrollo-social", SocialBenefitUpdatedData::class.java),
    UPDATE_TICKET_STATUS("updateTicketStatus", M8EventDirection.SEND, "desarrollo-social", UpdateTicketStatusData::class.java),
    TICKET_UPDATED("ticketUpdated", M8EventDirection.RECEIVE, "atencion-ciudadana", TicketUpdatedData::class.java),
    SOCIAL_DOCUMENTATION_DELIVERED("socialDocumentationDelivered", M8EventDirection.RECEIVE, "ciudadanos", SocialDocumentationDeliveredData::class.java),
    CITIZEN_UPDATED("citizenUpdated", M8EventDirection.RECEIVE, "ciudadanos", CitizenUpdatedData::class.java),
    DEBT_OVERDUE("debtOverdue", M8EventDirection.RECEIVE, "rentas", DebtOverdueData::class.java),
    DEBT_SETTLED("debtSettled", M8EventDirection.RECEIVE, "rentas", DebtSettledData::class.java),
    EXEMPTION_REQUESTED("exemptionRequested", M8EventDirection.RECEIVE, "rentas", ExemptionRequestedData::class.java),
    UPDATE_EXEMPTION_STATUS("updateExemptionStatus", M8EventDirection.RECEIVE, "rentas", UpdateExemptionStatusData::class.java);

    companion object {
        const val VERSION = "1.0"
        fun fromWireName(name: String): M8EventType? = entries.firstOrNull { it.wireName == name }
        fun fromPayload(payload: M8Payload): M8EventType = entries.single { it.payloadClass.isInstance(payload) }
    }
}
