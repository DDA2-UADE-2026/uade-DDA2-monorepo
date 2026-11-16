package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitStatus
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.application.repository.ApplicationVisitRepository
import com.uade.dda2.server.feature.auth.entity.CitizenSnapshot
import com.uade.dda2.server.feature.auth.repository.CitizenSnapshotRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantChange
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantChangeAction
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantStatus
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantChangeRepository
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.event.M8BusinessFact
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service
@Transactional(propagation = Propagation.MANDATORY)
class CitizenIncomingService(
    private val users: UserRepository,
    private val snapshots: CitizenSnapshotRepository,
    private val applications: ApplicationRepository,
    private val visits: ApplicationVisitRepository,
    private val editions: ProgramEditionRepository,
    private val grants: BenefitGrantRepository,
    private val changes: BenefitGrantChangeRepository,
    private val businessEvents: ApplicationEventPublisher,
    private val logs: LogService,
    private val json: JsonMapper,
    private val time: EnrollmentPeriodExpirationProperties,
    private val clock: Clock,
) {
    fun receive(message: M8EventEnvelope<CitizenUpdatedData>) {
        val user = users.findByExternalCitizenIdIgnoreCase(message.data.citizenId.toString()) ?: return
        val userId = requireNotNull(user.id)
        if (message.data.updateType == CitizenUpdateType.DECEASED) {
            deceased(userId, message)
            return
        }
        val locked = users.findByIdForUpdate(userId) ?: return
        val snapshot = snapshots.findById(userId).orElse(null) ?: snapshots.save(CitizenSnapshot(user = locked))
        val occurredAt = message.occurredAt.toInstant()
        val data = message.data.details
        when (message.data.updateType) {
            CitizenUpdateType.REGISTERED -> {
                if (snapshot.lastRegisteredAt?.let { occurredAt.isBefore(it) } != true) {
                    snapshot.fullName = "${data.name} ${data.lastname}"
                    snapshot.dni = data.dni
                    snapshot.lastRegisteredAt = occurredAt
                }
                if (snapshot.lastAddressAt?.let { occurredAt.isBefore(it) } != true) {
                    snapshot.address = data.address?.let { "${it.street} ${it.number}" }
                    snapshot.lastAddressAt = occurredAt
                }
            }
            CitizenUpdateType.ADDRESS_UPDATED -> if (snapshot.lastAddressAt?.let { occurredAt.isBefore(it) } != true) {
                snapshot.address = data.newAddress?.let { "${it.street} ${it.number}" }
                snapshot.lastAddressAt = occurredAt
            }
            CitizenUpdateType.BLOCKED -> if (snapshot.lastStateAt?.let { occurredAt.isBefore(it) } != true) {
                snapshot.blocked = true
                snapshot.blockReason = data.reason
                snapshot.reviewRequired = data.reviewRequired == true
                snapshot.lastStateAt = occurredAt
            }
            CitizenUpdateType.DECEASED -> Unit
        }
        auditCitizen(userId, message)
    }

    private fun deceased(userId: Long, message: M8EventEnvelope<CitizenUpdatedData>) {
        // Bloquear solicitudes en orden estable antes del usuario, igual que los servicios de beneficios.
        val ids = applications.findIdsByUserAndStatuses(userId, OPEN)
        val lockedApplications = ids.mapNotNull { applications.findByIdForUpdate(it) }
        val user = users.findByIdForUpdate(userId) ?: return
        // Una presentación que ganó la carrera se procesa en el siguiente intento de Core; no queda fuera del cierre.
        if (applications.findIdsByUserAndStatuses(userId, OPEN).any { it !in ids })
            throw CoreIntegrationException("M8_CITIZEN_APPLICATIONS_CHANGED")
        val snapshot = snapshots.findById(userId).orElse(null) ?: snapshots.save(CitizenSnapshot(user = user))
        if (snapshot.deceasedAt != null && snapshot.lastStateAt?.let { message.occurredAt.toInstant().isBefore(it) } == true) return
        snapshot.deceasedAt = requireNotNull(message.data.details.deceasedAt)
        snapshot.blocked = true
        snapshot.blockReason = DEATH_REASON
        snapshot.reviewRequired = true
        snapshot.lastStateAt = message.occurredAt.toInstant()
        val changedAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
        val localTime = LocalDateTime.ofInstant(changedAt.toInstant(), time.zone())
        val lockedEditions = lockedApplications.map { requireNotNull(it.programEdition.id) }.distinct().sorted()
            .associateWith { editions.findByIdForUpdate(it) ?: throw CoreIntegrationException("M8_EDITION_NOT_FOUND") }
        lockedApplications.filter { it.status in OPEN }.forEach { application ->
            val applicationId = requireNotNull(application.id)
            val activeGrants = grants.lockByApplicationAndStatuses(applicationId, setOf(BenefitGrantStatus.ACTIVE, BenefitGrantStatus.SUSPENDED))
            activeGrants.forEach { grant ->
                val previous = grant.status
                grant.status = BenefitGrantStatus.FINALIZED
                val change = changes.saveAndFlush(BenefitGrantChange(grant = grant,
                    sequenceNumber = changes.lastSequence(requireNotNull(grant.id)) + 1,
                    action = BenefitGrantChangeAction.FINALIZED, previousStatus = previous, status = BenefitGrantStatus.FINALIZED,
                    previousStartDate = grant.startDate, previousEndDate = grant.endDate, startDate = grant.startDate,
                    endDate = grant.endDate, reason = DEATH_REASON, responsible = null, changedAt = changedAt,
                    causationEventId = message.eventId, effectiveEndDate = snapshot.deceasedAt))
                businessEvents.publishEvent(M8BusinessFact.BenefitChanged(change, message.eventId))
                logs.record(null, LogAction.CREATE, LogEntityType.BENEFIT_GRANT_CHANGE, requireNotNull(change.id).toString(),
                    newValues = json.writeValueAsString(mapOf("reason" to DEATH_REASON, "causationId" to message.eventId)))
            }
            visits.findAllByApplicationIdAndStatus(applicationId, ApplicationVisitStatus.SCHEDULED).forEach { visit ->
                visit.status = ApplicationVisitStatus.CANCELLED
                visit.cancellationReason = DEATH_REASON
                visit.cancelledAt = changedAt
                visit.cancelledBy = null
                visit.cancelledByEventId = message.eventId
                logs.record(null, LogAction.UPDATE, LogEntityType.APPLICATION_VISIT, requireNotNull(visit.id).toString(),
                    newValues = json.writeValueAsString(mapOf("reason" to DEATH_REASON, "causationId" to message.eventId)))
            }
            if (application.status == ApplicationStatus.APPROVED) {
                val edition = requireNotNull(lockedEditions[application.programEdition.id])
                if (edition.currentEnrollment <= 0) throw CoreIntegrationException("M8_ENROLLMENT_INCONSISTENT")
                edition.currentEnrollment -= 1
            }
            val previous = application.status
            application.status = ApplicationStatus.CLOSED
            application.closedReason = DEATH_REASON
            application.closedAt = changedAt
            application.closedByEventId = message.eventId
            application.updatedAt = localTime
            logs.record(null, LogAction.UPDATE, LogEntityType.APPLICATION, applicationId.toString(),
                oldValues = json.writeValueAsString(mapOf("status" to previous)),
                newValues = json.writeValueAsString(mapOf("status" to ApplicationStatus.CLOSED, "reason" to DEATH_REASON, "causationId" to message.eventId)))
        }
        auditCitizen(userId, message)
    }

    private fun auditCitizen(userId: Long, message: M8EventEnvelope<CitizenUpdatedData>) {
        logs.record(null, LogAction.UPDATE, LogEntityType.CITIZEN_SNAPSHOT, userId.toString(),
            newValues = json.writeValueAsString(mapOf("updateType" to message.data.updateType, "eventId" to message.eventId)))
    }

    companion object {
        const val DEATH_REASON = "Fallecimiento del titular"
        val OPEN = ApplicationStatus.entries.filter { it !in setOf(ApplicationStatus.REJECTED, ApplicationStatus.CLOSED) }
    }
}
