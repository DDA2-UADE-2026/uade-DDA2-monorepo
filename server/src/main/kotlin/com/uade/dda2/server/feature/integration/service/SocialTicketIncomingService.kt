package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.feature.integration.contract.M8EventEnvelope
import com.uade.dda2.server.feature.integration.contract.TicketUpdatedData
import com.uade.dda2.server.feature.integration.entity.SocialTicket
import com.uade.dda2.server.feature.integration.entity.SocialTicketCursor
import com.uade.dda2.server.feature.integration.entity.SocialTicketState
import com.uade.dda2.server.feature.integration.repository.SocialTicketCursorRepository
import com.uade.dda2.server.feature.integration.repository.SocialTicketRepository
import com.uade.dda2.server.feature.integration.validator.M8PayloadValidator
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.Clock

@Service
@Transactional(propagation = Propagation.MANDATORY)
class SocialTicketIncomingService(
    private val tickets: SocialTicketRepository,
    private val cursors: SocialTicketCursorRepository,
    private val validator: M8PayloadValidator,
    private val json: JsonMapper,
    private val logs: LogService,
    private val clock: Clock,
) {
    fun receive(message: M8EventEnvelope<TicketUpdatedData>) {
        val data = message.data
        if (validator.isReclassification(data)) return
        val updatedAt = data.updatedAt.toInstant()
        val cursor = cursors.lockById(data.ticketId)
        if (cursor != null && updatedAt.isBefore(cursor.sourceUpdatedAt)) return
        if (cursor == null) cursors.saveAndFlush(SocialTicketCursor(data.ticketId, updatedAt, message.eventId))
        else { cursor.sourceUpdatedAt = updatedAt; cursor.lastEventId = message.eventId }

        var ticket = tickets.lockById(data.ticketId)
        // Comparar la fecha antes de cerrar una gestión: un broadcast viejo no puede reasignarla.
        if (data.responsibleAreaId != "M8") {
            if (ticket != null) {
                ticket.state = SocialTicketState.CLOSED
                ticket.responsibleAreaId = data.responsibleAreaId
                ticket.closedReason = "Ticket reasignado a otra área."
                ticket.informationPending = false
                ticket.requiresAttention = false
                ticket.sourceUpdatedAt = updatedAt
                ticket.lastEventId = message.eventId
                ticket.updatedAt = clock.instant()
                audit(ticket, message)
            }
            return
        }
        if (ticket == null) {
            if (data.updateType != "ROUTED") return
            ticket = tickets.save(SocialTicket(id = data.ticketId, publicId = data.publicId, citizenId = data.citizenId,
                anonymous = data.isAnonymous, lastEventId = message.eventId, responseCausationId = message.eventId,
                routeEventId = message.eventId, sourceUpdatedAt = updatedAt, snapshotJson = json.writeValueAsString(data.details?.routing),
                correlationId = message.correlationId, createdAt = clock.instant(), updatedAt = clock.instant()))
        }
        ticket.publicId = data.publicId
        ticket.citizenId = data.citizenId
        ticket.anonymous = data.isAnonymous
        ticket.responsibleAreaId = data.responsibleAreaId
        ticket.currentStatus = data.currentStatus
        ticket.currentPriority = data.currentPriority
        ticket.sourceUpdatedAt = updatedAt
        ticket.lastEventId = message.eventId
        ticket.updatedAt = clock.instant()
        when (data.updateType) {
            "ROUTED" -> {
                ticket.snapshotJson = json.writeValueAsString(data.details?.routing)
                ticket.attachmentsJson = json.writeValueAsString(data.attachments)
                ticket.correlationId = message.correlationId ?: ticket.correlationId
                ticket.responseCausationId = message.eventId
                if (data.details?.routing?.reassignment == true) {
                    ticket.routeEventId = message.eventId
                    ticket.startedRouteEventId = null
                    ticket.state = SocialTicketState.RECEIVED
                    ticket.assignedWorker = null
                    ticket.informationPending = false
                    ticket.informationRequiredBy = null
                    ticket.informationResponseJson = null
                    ticket.closedReason = null
                    ticket.requiresAttention = true
                }
                ticket.responseTrigger = "ROUTED"
            }
            "INFORMATION_PROVIDED" -> {
                if (ticket.state !in SocialTicketEventService.TERMINAL) {
                    ticket.informationResponseJson = json.writeValueAsString(mapOf("informationResponse" to data.details?.informationResponse, "attachments" to data.attachments))
                    ticket.correlationId = message.correlationId ?: ticket.correlationId
                    ticket.informationPending = false
                    ticket.informationRequiredBy = null
                    ticket.state = SocialTicketState.IN_PROGRESS
                    ticket.responseCausationId = message.eventId
                    ticket.responseTrigger = "INFORMATION_PROVIDED"
                    ticket.requiresAttention = true
                }
            }
            "REOPENED" -> {
                ticket.correlationId = message.correlationId ?: ticket.correlationId
                ticket.state = SocialTicketState.IN_PROGRESS
                ticket.closedReason = null
                ticket.reopenReason = data.details?.reopen?.reason
                ticket.responseCausationId = message.eventId
                ticket.responseTrigger = "REOPENED"
                ticket.informationPending = false
                ticket.requiresAttention = true
            }
            "CANCELLED", "DUPLICATE_LINKED", "CLOSED" -> {
                ticket.state = SocialTicketState.CLOSED
                ticket.closedReason = data.updateType
                ticket.informationPending = false
                ticket.informationRequiredBy = null
                ticket.requiresAttention = false
            }
            "ESCALATION_CHANGED" -> {
                val previous = json.readTree(ticket.snapshotJson)
                val wasActive = previous["escalation"]?.get("active")?.asBoolean() == true
                val snapshot = json.createObjectNode().apply { setAll(previous.properties().associate { it.key to it.value }) }
                snapshot.set("escalation", json.valueToTree(data.details?.escalation))
                ticket.snapshotJson = json.writeValueAsString(snapshot)
                if (data.details?.escalation?.active == true && !wasActive) ticket.requiresAttention = true
            }
            else -> Unit
        }
        audit(ticket, message)
    }

    private fun audit(ticket: SocialTicket, message: M8EventEnvelope<TicketUpdatedData>) {
        logs.record(null, LogAction.UPDATE, LogEntityType.SOCIAL_TICKET, ticket.id.toString(), newValues = json.writeValueAsString(
            mapOf("eventId" to message.eventId, "updateType" to message.data.updateType, "state" to ticket.state)))
    }
}
