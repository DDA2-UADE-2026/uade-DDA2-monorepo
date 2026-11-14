package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.integration.event.M8BusinessFact
import com.uade.dda2.server.feature.application.entity.ApplicationResolutionDecision
import com.uade.dda2.server.feature.application.entity.ApplicationVisit
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.entity.SocialTicket
import com.uade.dda2.server.feature.integration.entity.SocialTicketState
import com.uade.dda2.server.feature.integration.messaging.M8RabbitPublisher
import com.uade.dda2.server.feature.integration.repository.SocialTicketRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
@Transactional(propagation = Propagation.MANDATORY)
class SocialTicketEventService(
    private val tickets: SocialTicketRepository,
    private val publisher: M8RabbitPublisher,
    private val messages: M8MessageFactory,
    private val logs: LogService,
    private val json: JsonMapper,
    private val time: EnrollmentPeriodExpirationProperties,
    private val clock: Clock,
) {
    fun workerAssigned(application: Application) {
        val ticket = tickets.lockByApplicationId(requireNotNull(application.id)) ?: return
        if (ticket.state in TERMINAL || ticket.responsibleAreaId != "M8") return
        ticket.assignedWorker = application.assignedWorker
        ticket.updatedAt = clock.instant()
        val actor = application.assignedWorker ?: return
        startIfNeeded(ticket, actor)
    }

    fun startIfNeeded(ticket: SocialTicket, actor: User): UUID? {
        if (ticket.state in TERMINAL || ticket.responsibleAreaId != "M8") throw closed()
        if (ticket.responseTrigger != "ROUTED" || ticket.startedRouteEventId == ticket.routeEventId) return null
        val eventId = send(ticket, UpdateTicketStatusData(ticket.id, TicketUpdateType.STARTED,
            EventActorData(EventActorType.EXTERNAL_USER, null), now(), publicMessage = "Desarrollo Social está analizando tu pedido.", progress = 10), actor)
        ticket.startedRouteEventId = ticket.routeEventId
        return eventId
    }

    fun visitScheduled(visit: ApplicationVisit) {
        val ticket = tickets.lockByApplicationId(requireNotNull(visit.application.id)) ?: return
        if (ticket.state in TERMINAL || ticket.responsibleAreaId != "M8") return
        if (ticket.startedRouteEventId == null && ticket.responseTrigger == "ROUTED") return
        val start = visit.startsAt.atZoneSameInstant(time.zone())
        val end = visit.endsAt.atZoneSameInstant(time.zone())
        val day = start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val from = start.format(DateTimeFormatter.ofPattern("HH:mm"))
        val to = end.format(DateTimeFormatter.ofPattern("HH:mm"))
        send(ticket, UpdateTicketStatusData(ticket.id, TicketUpdateType.PROGRESS, EventActorData(EventActorType.SYSTEM, null), now(),
            publicMessage = "Programamos una visita o entrevista social para el $day entre $from y $to.", progress = 50), null)
    }

    fun applicationResolved(resolution: M8BusinessFact.ApplicationResolved) {
        val ticket = tickets.lockByApplicationId(requireNotNull(resolution.application.id)) ?: return
        if (ticket.state in TERMINAL || ticket.responsibleAreaId != "M8") return
        val (type, text) = when (resolution.decision) {
            ApplicationResolutionDecision.APPROVED -> "REQUEST_FULFILLED" to "Tu solicitud fue aprobada. Te avisaremos cuando el beneficio quede asignado."
            ApplicationResolutionDecision.REJECTED -> "NO_FURTHER_ACTION_REQUIRED" to "Tu solicitud fue evaluada y no cumple los requisitos del programa."
            ApplicationResolutionDecision.WAITLISTED -> return
        }
        send(ticket, UpdateTicketStatusData(ticket.id, TicketUpdateType.RESOLVED, EventActorData(EventActorType.SYSTEM, null),
            resolution.resolvedAt, publicMessage = text, progress = 100, details = TicketDetailsData(resolution = ResolutionData(type))), null)
    }

    fun send(ticket: SocialTicket, data: UpdateTicketStatusData, actor: User?): UUID {
        if (ticket.id != data.ticketId || ticket.state in TERMINAL || ticket.responsibleAreaId != "M8") throw closed()
        if (data.updateType == TicketUpdateType.STARTED &&
            (ticket.responseTrigger != "ROUTED" || ticket.startedRouteEventId == ticket.routeEventId))
            throw ConflictException("M8_TICKET_ALREADY_STARTED", "STARTED se publica una sola vez por derivación.")
        if (data.updateType == TicketUpdateType.PROGRESS && ticket.startedRouteEventId == null && ticket.responseTrigger == "ROUTED")
            throw ConflictException("M8_TICKET_NOT_STARTED", "Un administrativo debe tomar el ticket antes de informar avances.")
        if (data.updateType == TicketUpdateType.INFORMATION_REQUIRED && ticket.informationPending)
            throw ConflictException("M8_TICKET_INFORMATION_PENDING", "El ticket ya tiene un pedido de información activo.")
        if (ticket.informationPending && data.updateType != TicketUpdateType.INFORMATION_REQUIRED)
            throw ConflictException("M8_TICKET_WAITING_INFORMATION", "M2 debe comunicar la respuesta o reapertura antes de continuar la gestión.")
        val eventId = publisher.ticketUpdated(messages.create(data, ticket.correlationId, ticket.responseCausationId, data.updateOccurredAt))
        ticket.state = when (data.updateType) {
            TicketUpdateType.STARTED, TicketUpdateType.PROGRESS -> SocialTicketState.IN_PROGRESS
            TicketUpdateType.INFORMATION_REQUIRED -> SocialTicketState.WAITING_INFORMATION
            TicketUpdateType.RETURNED -> SocialTicketState.RETURNED
            TicketUpdateType.RESOLVED -> SocialTicketState.RESOLVED
            TicketUpdateType.REJECTED -> SocialTicketState.CLOSED
        }
        if (data.updateType == TicketUpdateType.STARTED) ticket.startedRouteEventId = ticket.routeEventId
        if (data.updateType == TicketUpdateType.INFORMATION_REQUIRED) {
            ticket.informationPending = true
            ticket.informationRequiredBy = data.details?.informationRequest?.requiredBy
        }
        if (data.updateType in setOf(TicketUpdateType.RETURNED, TicketUpdateType.RESOLVED, TicketUpdateType.REJECTED)) {
            ticket.informationPending = false
            ticket.informationRequiredBy = null
        }
        ticket.requiresAttention = false
        ticket.updatedAt = clock.instant()
        logs.record(actor, LogAction.UPDATE, LogEntityType.SOCIAL_TICKET, ticket.id.toString(),
            newValues = json.writeValueAsString(mapOf("updateType" to data.updateType, "eventId" to eventId, "state" to ticket.state)))
        return eventId
    }

    private fun now(): OffsetDateTime = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
    private fun closed() = ConflictException("M8_TICKET_NOT_MANAGEABLE", "El ticket no tiene una gestión activa en Desarrollo Social.")

    companion object {
        val TERMINAL = setOf(SocialTicketState.CLOSED, SocialTicketState.RETURNED, SocialTicketState.RESOLVED)
    }
}
