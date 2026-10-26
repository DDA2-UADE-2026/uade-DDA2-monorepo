package com.uade.dda2.server.feature.benefit.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.benefit.entity.*
import com.uade.dda2.server.feature.benefit.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.benefit.repository.*
import com.uade.dda2.server.feature.log.entity.*
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class BenefitGrantExpirationService(
    private val applications: ApplicationRepository,
    private val users: UserRepository,
    private val editions: ProgramEditionRepository,
    private val grants: BenefitGrantRepository,
    private val changes: BenefitGrantChangeRepository,
    private val closure: BenefitGrantClosureService,
    private val logs: LogService,
    private val json: JsonMapper,
    private val timeProperties: EnrollmentPeriodExpirationProperties,
    private val clock: Clock,
) {
    @Transactional
    fun expireApplication(applicationId: UUID): Int {
        val application = applications.findByIdForUpdate(applicationId) ?: return 0
        if (application.status != ApplicationStatus.APPROVED) return 0
        requireNotNull(users.findByIdForUpdate(requireNotNull(application.user.id)))
        val edition = requireNotNull(editions.findByIdForUpdate(requireNotNull(application.programEdition.id)))
        val changedAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
        val localTime = LocalDateTime.ofInstant(changedAt.toInstant(), timeProperties.zone())
        val expired = grants.findExpiredForUpdate(applicationId, EXPIRABLE_STATUSES, localTime.toLocalDate())
        if (expired.isEmpty()) return 0
        val events = expired.map { grant ->
            val previous = grant.status
            grant.status = BenefitGrantStatus.EXPIRED
            BenefitGrantChange(grant = grant, sequenceNumber = changes.lastSequence(requireNotNull(grant.id)) + 1,
                action = BenefitGrantChangeAction.EXPIRED, previousStatus = previous, status = BenefitGrantStatus.EXPIRED,
                previousStartDate = grant.startDate, previousEndDate = grant.endDate, startDate = grant.startDate,
                endDate = grant.endDate, reason = EXPIRATION_REASON, responsible = null, changedAt = changedAt)
        }
        val saved = changes.saveAllAndFlush(events)
        closure.closeIfFinished(application, edition, null, changedAt, requireNotNull(saved.last().id))
        application.updatedAt = localTime
        saved.forEach { event ->
            logs.record(user = null, action = LogAction.CREATE, entityType = LogEntityType.BENEFIT_GRANT_CHANGE,
                entityId = requireNotNull(event.id).toString(), newValues = json.writeValueAsString(event.toAuditSnapshot()))
        }
        return saved.size
    }

    companion object {
        val EXPIRABLE_STATUSES = setOf(BenefitGrantStatus.ACTIVE, BenefitGrantStatus.SUSPENDED)
        const val EXPIRATION_REASON = "Vencimiento automático de vigencia."
    }
}
