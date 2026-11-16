package com.uade.dda2.server.feature.integration.repository

import com.uade.dda2.server.feature.integration.entity.CoreOutbox
import com.uade.dda2.server.feature.integration.entity.CoreOutboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface CoreOutboxRepository : JpaRepository<CoreOutbox, UUID> {
    @Modifying
    @Query(value = """INSERT INTO core_outbox (event_id,event_type,event_version,correlation_id,occurred_at,
        payload_json,payload_hash,trace_id,status,attempts,next_attempt_at,created_at)
        VALUES (:eventId,:eventType,:eventVersion,:correlationId,:occurredAt,:payloadJson,:payloadHash,:traceId,
        'PENDING',0,:now,:now) ON CONFLICT DO NOTHING""", nativeQuery = true)
    fun insertPending(eventId: UUID, eventType: String, eventVersion: String, correlationId: UUID?, occurredAt: Instant,
        payloadJson: String, payloadHash: String, traceId: String, now: Instant): Int

    @Query(value = """SELECT * FROM core_outbox WHERE
        (status='PENDING' AND next_attempt_at<=:now) OR (status='IN_FLIGHT' AND lease_until<=:now)
        ORDER BY created_at,event_id LIMIT 1 FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    fun lockNext(now: Instant): CoreOutbox?

    @Modifying
    @Query("""update CoreOutbox o set o.status=com.uade.dda2.server.feature.integration.entity.CoreOutboxStatus.BROKER_CONFIRMED,
        o.confirmedAt=:now, o.leaseToken=null, o.leaseUntil=null, o.lastError=null
        where o.eventId=:eventId and o.leaseToken=:token and o.leaseUntil>:now
        and o.status=com.uade.dda2.server.feature.integration.entity.CoreOutboxStatus.IN_FLIGHT""")
    fun confirm(eventId: UUID, token: UUID, now: Instant): Int

    @Modifying
    @Query("""update CoreOutbox o set o.status=com.uade.dda2.server.feature.integration.entity.CoreOutboxStatus.PENDING,
        o.nextAttemptAt=:next, o.lastError=:error, o.leaseToken=null, o.leaseUntil=null
        where o.eventId=:eventId and o.leaseToken=:token
        and o.status=com.uade.dda2.server.feature.integration.entity.CoreOutboxStatus.IN_FLIGHT""")
    fun retry(eventId: UUID, token: UUID, next: Instant, error: String): Int

    fun countByStatus(status: CoreOutboxStatus): Long

    @Query("select min(o.createdAt) from CoreOutbox o where o.status in :statuses")
    fun oldestCreatedAtByStatusIn(statuses: Collection<CoreOutboxStatus>): Instant?
}
