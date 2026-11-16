package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.feature.integration.dto.CoreLocalMetrics
import com.uade.dda2.server.feature.integration.entity.CoreOutboxStatus
import com.uade.dda2.server.feature.integration.repository.CoreInboxRepository
import com.uade.dda2.server.feature.integration.repository.CoreOutboxRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoreLocalMetricsService(private val outbox: CoreOutboxRepository, private val inbox: CoreInboxRepository) {
    @Transactional(readOnly = true)
    fun metrics() = CoreLocalMetrics(
        pending = outbox.countByStatus(CoreOutboxStatus.PENDING),
        inFlight = outbox.countByStatus(CoreOutboxStatus.IN_FLIGHT),
        brokerConfirmed = outbox.countByStatus(CoreOutboxStatus.BROKER_CONFIRMED),
        inboxProcessed = inbox.count(),
        oldestUnconfirmedAt = outbox.oldestCreatedAtByStatusIn(listOf(CoreOutboxStatus.PENDING, CoreOutboxStatus.IN_FLIGHT)),
    )
}
