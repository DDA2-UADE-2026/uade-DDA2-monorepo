package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.integration.contract.EventActorData
import com.uade.dda2.server.feature.integration.contract.EventActorType
import com.uade.dda2.server.feature.integration.contract.TicketUpdateType
import com.uade.dda2.server.feature.integration.contract.UpdateTicketStatusData
import com.uade.dda2.server.feature.integration.dto.*
import com.uade.dda2.server.feature.integration.entity.SocialTicket
import com.uade.dda2.server.feature.integration.entity.SocialTicketState
import com.uade.dda2.server.feature.integration.repository.SocialTicketRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class SocialTicketManagementService(
    private val tickets: SocialTicketRepository,
    private val applications: ApplicationRepository,
    private val users: UserRepository,
    private val access: SocialIntegrationAccess,
    private val policy: M8BusinessPolicy,
    private val events: SocialTicketEventService,
    private val json: JsonMapper,
    private val logs: LogService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun list(state: SocialTicketState?, attention: Boolean?, page: Int, size: Int): SocialIntegrationPage<SocialTicketResponse> {
        access.check("tickets:management:view")
        val result = tickets.findAdministrativePage(state, attention, PageRequest.of(page, size,
            Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))))
        return SocialIntegrationPage(result.content.map { response(it) }, result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): SocialTicketDetailResponse {
        access.check("tickets:management:view")
        return detail(tickets.findById(id).orElseThrow { notFound() })
    }

    @Transactional
    fun assign(id: UUID, request: AssignSocialTicketRequest): SocialTicketDetailResponse {
        val actor = access.check("tickets:management:manage")
        enabled()
        val ticket = tickets.lockById(id) ?: throw notFound()
        val worker = users.findByIdWithRoles(request.workerId)?.takeIf { it.active }
            ?: throw NotFoundException("M8_TICKET_WORKER_NOT_FOUND", "No se encontró el administrativo activo.")
        if (worker.roles.none { role -> role.permissions.any { it.name == "tickets:management:manage" } })
            throw ConflictException("M8_TICKET_WORKER_NOT_ELIGIBLE", "El administrativo no tiene permisos para gestionar tickets.")
        if (ticket.state in SocialTicketEventService.TERMINAL || ticket.responsibleAreaId != "M8") throw notManageable()
        ticket.assignedWorker = worker
        events.startIfNeeded(ticket, actor)
        ticket.updatedAt = clock.instant()
        return detail(ticket)
    }

    @Transactional
    fun linkApplication(id: UUID, request: LinkSocialTicketApplicationRequest): SocialTicketDetailResponse {
        val actor = access.check("tickets:management:manage")
        enabled()
        // El mismo orden que los callbacks de solicitudes: application antes de ticket.
        val application = applications.findByIdForUpdate(request.applicationId)
            ?: throw NotFoundException("APPLICATION_NOT_FOUND", "No se encontró la solicitud.")
        val ticket = tickets.lockById(id) ?: throw notFound()
        if (ticket.state in SocialTicketEventService.TERMINAL || ticket.responsibleAreaId != "M8") throw notManageable()
        if (ticket.anonymous || ticket.citizenId != policy.citizenId(application.user))
            throw ConflictException("M8_TICKET_CITIZEN_MISMATCH", "El ticket y la solicitud deben pertenecer al mismo ciudadano vinculado.")
        if (ticket.applicationId != null && ticket.applicationId != application.id ||
            application.originTicketId != null && application.originTicketId != id.toString())
            throw ConflictException("M8_TICKET_APPLICATION_CONFLICT", "El ticket o la solicitud ya tienen otro vínculo.")
        ticket.applicationId = application.id
        application.originTicketId = id.toString()
        ticket.updatedAt = clock.instant()
        logs.record(actor, LogAction.UPDATE, LogEntityType.SOCIAL_TICKET, id.toString(),
            newValues = json.writeValueAsString(mapOf("applicationId" to application.id)))
        application.assignedWorker?.let { ticket.assignedWorker = it; events.startIfNeeded(ticket, actor) }
        return detail(ticket)
    }

    @Transactional
    fun update(id: UUID, request: UpdateSocialTicketRequest): SocialEventQueuedResponse {
        val actor = access.check("tickets:management:manage")
        enabled()
        val ticket = tickets.lockById(id) ?: throw notFound()
        val data = UpdateTicketStatusData(id, request.updateType, EventActorData(EventActorType.EXTERNAL_USER, null),
            OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC), request.publicMessage, request.internalMessage,
            request.progress, request.details, request.attachments)
        if (request.updateType == TicketUpdateType.STARTED) ticket.assignedWorker = ticket.assignedWorker ?: actor
        val eventId = try { events.send(ticket, data, actor) }
        catch (exception: CoreIntegrationException) {
            if (exception.code != "M8_INVALID_PAYLOAD") throw exception
            throw BadRequestException("M8_INVALID_PAYLOAD", "La actualización no cumple las reglas de su updateType. Consultá el contrato AsyncAPI.")
        }
        return SocialEventQueuedResponse(eventId)
    }

    private fun response(ticket: SocialTicket) = SocialTicketResponse(ticket.id, ticket.publicId, ticket.citizenId, ticket.anonymous,
        ticket.responsibleAreaId, ticket.currentStatus, ticket.currentPriority, ticket.state, ticket.applicationId,
        ticket.assignedWorker?.id, ticket.informationPending, ticket.requiresAttention, ticket.sourceUpdatedAt, ticket.closedReason)

    private fun detail(ticket: SocialTicket) = SocialTicketDetailResponse(response(ticket), json.readTree(ticket.snapshotJson),
        ticket.correlationId, ticket.responseCausationId, ticket.informationResponseJson?.let { json.readTree(it) }, ticket.reopenReason,
        json.readTree(ticket.attachmentsJson).asSequence().map { json.treeToValue(it, com.uade.dda2.server.feature.integration.contract.EventAttachmentData::class.java) }.toList())

    private fun enabled() {
        if (!policy.enabled()) throw ConflictException("M8_BUSINESS_DISABLED", "La integración de negocio con Core está desactivada.")
    }
    private fun notFound() = NotFoundException("M8_TICKET_NOT_FOUND", "No se encontró un ticket de Desarrollo Social.")
    private fun notManageable() = ConflictException("M8_TICKET_NOT_MANAGEABLE", "El ticket no tiene una gestión activa en M8.")
}
