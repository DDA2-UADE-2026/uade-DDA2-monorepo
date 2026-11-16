package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.CoreOutboxLease
import com.uade.dda2.server.feature.integration.entity.CoreOutboxStatus
import com.uade.dda2.server.feature.integration.repository.CoreOutboxRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
@ConditionalOnProperty(prefix = "app.core", name = ["enabled"], havingValue = "true")
class CoreOutboxStore(private val repository: CoreOutboxRepository, private val properties: CoreProperties, private val clock: Clock) {
    @Transactional
    fun reserveNext(): CoreOutboxLease? {
        val now = clock.instant()
        val row = repository.lockNext(now) ?: return null
        val token = UUID.randomUUID()
        row.status = CoreOutboxStatus.IN_FLIGHT
        row.leaseToken = token
        row.leaseUntil = now.plus(properties.publisher.leaseDuration)
        row.attempts += 1
        return CoreOutboxLease(row.eventId, token, row.payloadJson, row.traceId)
    }

    @Transactional
    fun confirm(lease: CoreOutboxLease): Boolean = repository.confirm(lease.eventId, lease.token, clock.instant()) == 1

    @Transactional
    fun retry(lease: CoreOutboxLease, code: String) {
        val row = repository.findById(lease.eventId).orElse(null) ?: return
        val multiplier = 1L shl (row.attempts - 1).coerceIn(0, 20)
        val initial = properties.publisher.retryDelay.toMillis()
        val maximum = properties.publisher.maxRetryDelay.toMillis()
        val delay = if (initial > maximum / multiplier) maximum else initial * multiplier
        repository.retry(lease.eventId, lease.token, clock.instant().plusMillis(delay), code.take(100))
    }
}
