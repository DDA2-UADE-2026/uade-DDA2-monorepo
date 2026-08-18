package com.uade.dda2.server.feature.notification.service

import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.notification.dto.SseEventPayload
import com.uade.dda2.server.feature.notification.event.ResourceUpdatedEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ResourceUpdatedSseListener(
    private val sseHub: SseHub,
    @Value("\${spring.application.name}") private val sourceModule: String,
) {
    // AFTER_COMMIT: si la transacción que originó el cambio hace rollback,
    // nunca se notifica un cambio que en realidad no ocurrió.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onResourceUpdated(event: ResourceUpdatedEvent) {
        if (event.action == LogAction.LOGIN) {
            return
        }

        sseHub.broadcast(
            SseEventPayload(
                eventType = "resource.updated",
                sourceModule = sourceModule,
                data = mapOf(
                    "resource" to event.entityType.tableName,
                    "resourceId" to event.entityId,
                    "action" to event.action.name,
                ),
            ),
        )
    }
}
