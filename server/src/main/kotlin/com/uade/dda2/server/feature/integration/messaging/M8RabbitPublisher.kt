package com.uade.dda2.server.feature.integration.messaging

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.service.CoreOutboxService
import com.uade.dda2.server.feature.integration.service.M8ContractCodec
import io.github.springwolf.bindings.amqp.annotations.AmqpAsyncOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class M8RabbitPublisher(
    private val core: CoreProperties,
    private val codec: M8ContractCodec,
    private val outbox: ObjectProvider<CoreOutboxService>,
) {
    @AsyncPublisher(operation = AsyncOperation(channelName = "muni.inbox",
        description = "Pedir documentos a Ciudadanos. Outbox transaccional, routing key vacía; Core administra la topología.",
        message = AsyncMessage(name = "socialDocumentationRequested", messageId = "socialDocumentationRequested.v1")))
    @AmqpAsyncOperationBinding(deliveryMode = 2, mandatory = true)
    @Transactional(propagation = Propagation.MANDATORY)
    fun documentationRequested(message: M8EventEnvelope<SocialDocumentationRequestedData>): UUID = enqueue(message)

    @AsyncPublisher(operation = AsyncOperation(channelName = "muni.inbox",
        description = "Informar a Rentas un cambio de un beneficio otorgado. Confirm del broker no significa aceptación ni procesamiento remoto.",
        message = AsyncMessage(name = "socialBenefitUpdated", messageId = "socialBenefitUpdated.v1")))
    @AmqpAsyncOperationBinding(deliveryMode = 2, mandatory = true)
    @Transactional(propagation = Propagation.MANDATORY)
    fun benefitUpdated(message: M8EventEnvelope<SocialBenefitUpdatedData>): UUID = enqueue(message)

    @AsyncPublisher(operation = AsyncOperation(channelName = "muni.inbox",
        description = "Responder al ticketUpdated de M2 usando su correlación y evento causal. Nunca transmite un JWT de usuario.",
        message = AsyncMessage(name = "updateTicketStatus", messageId = "updateTicketStatus.v1")))
    @AmqpAsyncOperationBinding(deliveryMode = 2, mandatory = true)
    @Transactional(propagation = Propagation.MANDATORY)
    fun ticketUpdated(message: M8EventEnvelope<UpdateTicketStatusData>): UUID = enqueue(message)

    private fun enqueue(message: M8EventEnvelope<out M8Payload>): UUID {
        if (!core.enabled || !core.business.enabled) throw CoreIntegrationException("M8_BUSINESS_DISABLED")
        val type = M8EventType.fromPayload(message.data)
        if (type.direction != M8EventDirection.SEND || type.wireName != message.eventType)
            throw CoreIntegrationException("M8_INVALID_EVENT_CONTRACT")
        return (outbox.ifAvailable ?: throw CoreIntegrationException("CORE_DISABLED")).enqueue(codec.envelope(message))
    }
}
