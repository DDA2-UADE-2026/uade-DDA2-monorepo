package com.uade.dda2.server.config

import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.dto.CoreEnvelope
import com.uade.dda2.server.feature.integration.messaging.M8BusinessEventConsumer
import com.uade.dda2.server.feature.integration.service.CoreEventHandler
import com.uade.dda2.server.feature.integration.service.M8ContractCodec
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.core.business", name = ["enabled"], havingValue = "true")
class M8BusinessHandlersConfiguration {
    @Bean
    fun ticketUpdatedHandler(codec: M8ContractCodec, consumer: M8BusinessEventConsumer): CoreEventHandler =
        handler(M8EventType.TICKET_UPDATED) { consumer.ticketUpdated(codec.typed(it, TicketUpdatedData::class.java)) }
    @Bean
    fun documentationDeliveredHandler(codec: M8ContractCodec, consumer: M8BusinessEventConsumer): CoreEventHandler =
        handler(M8EventType.SOCIAL_DOCUMENTATION_DELIVERED) { consumer.documentationDelivered(codec.typed(it, SocialDocumentationDeliveredData::class.java)) }
    @Bean
    fun citizenUpdatedHandler(codec: M8ContractCodec, consumer: M8BusinessEventConsumer): CoreEventHandler =
        handler(M8EventType.CITIZEN_UPDATED) { consumer.citizenUpdated(codec.typed(it, CitizenUpdatedData::class.java)) }
    @Bean
    fun debtOverdueHandler(codec: M8ContractCodec, consumer: M8BusinessEventConsumer): CoreEventHandler =
        handler(M8EventType.DEBT_OVERDUE) { consumer.debtOverdue(codec.typed(it, DebtOverdueData::class.java)) }
    @Bean
    fun debtSettledHandler(codec: M8ContractCodec, consumer: M8BusinessEventConsumer): CoreEventHandler =
        handler(M8EventType.DEBT_SETTLED) { consumer.debtSettled(codec.typed(it, DebtSettledData::class.java)) }
    @Bean
    fun exemptionRequestedHandler(codec: M8ContractCodec, consumer: M8BusinessEventConsumer): CoreEventHandler =
        handler(M8EventType.EXEMPTION_REQUESTED) { consumer.exemptionRequested(codec.typed(it, ExemptionRequestedData::class.java)) }
    @Bean
    fun exemptionUpdatedHandler(codec: M8ContractCodec, consumer: M8BusinessEventConsumer): CoreEventHandler =
        handler(M8EventType.UPDATE_EXEMPTION_STATUS) { consumer.exemptionUpdated(codec.typed(it, UpdateExemptionStatusData::class.java)) }

    private fun handler(type: M8EventType, action: (CoreEnvelope) -> Unit): CoreEventHandler = object : CoreEventHandler {
        override val eventType = type.wireName
        override val eventVersion = M8EventType.VERSION
        override fun handle(envelope: CoreEnvelope) = action(envelope)
    }
}
