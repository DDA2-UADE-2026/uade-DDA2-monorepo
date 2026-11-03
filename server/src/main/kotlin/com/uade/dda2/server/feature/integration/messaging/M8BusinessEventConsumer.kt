package com.uade.dda2.server.feature.integration.messaging

import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.service.*
import io.github.springwolf.bindings.amqp.annotations.AmqpAsyncOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncListener
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(propagation = Propagation.MANDATORY)
class M8BusinessEventConsumer(
    private val policy: M8BusinessPolicy,
    private val tickets: SocialTicketIncomingService,
    private val citizens: CitizenIncomingService,
    private val documents: ExternalDocumentationService,
    private val revenue: RevenueIncomingService,
) {
    @AsyncListener(operation = AsyncOperation(channelName = "q.desarrollo-social",
        description = "Broadcast de M2. Filtra área M8 y reclasificación; compara updatedAt antes de aplicar efectos. Ack tras el commit de inbox y gestión.",
        message = AsyncMessage(name = "ticketUpdated", messageId = "ticketUpdated.v1")))
    @AmqpAsyncOperationBinding(ack = true)
    fun ticketUpdated(message: M8EventEnvelope<TicketUpdatedData>) { enabled(); tickets.receive(message) }

    @AsyncListener(operation = AsyncOperation(channelName = "q.desarrollo-social",
        description = "Referencias documentales de M1. COMPLETED es entrega, no validación administrativa; solicitud desconocida se confirma sin efectos.",
        message = AsyncMessage(name = "socialDocumentationDelivered", messageId = "socialDocumentationDelivered.v1")))
    @AmqpAsyncOperationBinding(ack = true)
    fun documentationDelivered(message: M8EventEnvelope<SocialDocumentationDeliveredData>) { enabled(); documents.receive(message) }

    @AsyncListener(operation = AsyncOperation(channelName = "q.desarrollo-social",
        description = "Actualiza ciudadanos ya vinculados. BLOCKED restringe solicitudes; DECEASED finaliza ACTIVE y SUSPENDED y cierra solicitudes abiertas con trazabilidad.",
        message = AsyncMessage(name = "citizenUpdated", messageId = "citizenUpdated.v1")))
    @AmqpAsyncOperationBinding(ack = true)
    fun citizenUpdated(message: M8EventEnvelope<CitizenUpdatedData>) { enabled(); citizens.receive(message) }

    @AsyncListener(operation = AsyncOperation(channelName = "q.desarrollo-social",
        description = "Registra deuda vencida de Rentas para consulta; no cambia elegibilidad social.",
        message = AsyncMessage(name = "debtOverdue", messageId = "debtOverdue.v1")))
    @AmqpAsyncOperationBinding(ack = true)
    fun debtOverdue(message: M8EventEnvelope<DebtOverdueData>) { enabled(); revenue.overdue(message) }

    @AsyncListener(operation = AsyncOperation(channelName = "q.desarrollo-social",
        description = "Solo MUNICIPAL_TAX, ciudadano no null y deuda conocida. Otros casos se confirman sin efectos.",
        message = AsyncMessage(name = "debtSettled", messageId = "debtSettled.v1")))
    @AmqpAsyncOperationBinding(ack = true)
    fun debtSettled(message: M8EventEnvelope<DebtSettledData>) { enabled(); revenue.settled(message) }

    @AsyncListener(operation = AsyncOperation(channelName = "q.desarrollo-social",
        description = "Registra el pedido; vinculación automática solo ante un único TAX_EXEMPTION activo. Los casos ambiguos quedan para selección administrativa.",
        message = AsyncMessage(name = "exemptionRequested", messageId = "exemptionRequested.v1")))
    @AmqpAsyncOperationBinding(ack = true)
    fun exemptionRequested(message: M8EventEnvelope<ExemptionRequestedData>) { enabled(); revenue.exemptionRequested(message) }

    @AsyncListener(operation = AsyncOperation(channelName = "q.desarrollo-social",
        description = "Registra APPROVED o REJECTED sin modificar beneficios ni datos propiedad de Rentas.",
        message = AsyncMessage(name = "updateExemptionStatus", messageId = "updateExemptionStatus.v1")))
    @AmqpAsyncOperationBinding(ack = true)
    fun exemptionUpdated(message: M8EventEnvelope<UpdateExemptionStatusData>) { enabled(); revenue.exemptionUpdated(message) }

    private fun enabled() {
        if (!policy.enabled()) throw CoreIntegrationException("M8_BUSINESS_DISABLED")
    }
}
