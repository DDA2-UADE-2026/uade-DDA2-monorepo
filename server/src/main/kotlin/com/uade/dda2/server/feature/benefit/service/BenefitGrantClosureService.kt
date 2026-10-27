package com.uade.dda2.server.feature.benefit.service

import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.benefit.error.BenefitGrantErrors
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BenefitGrantClosureService(
    private val grants: BenefitGrantRepository,
    private val logs: LogService,
    private val json: JsonMapper,
) {
    // Caller holds application, applicant and edition locks and has flushed all terminal changes.
    @Transactional(propagation = Propagation.MANDATORY)
    fun closeIfFinished(application: Application, edition: ProgramEdition, actor: User?, changedAt: OffsetDateTime, changeId: UUID) {
        if (application.status != ApplicationStatus.APPROVED || grants.countUnfinished(requireNotNull(application.id)) != 0L) return
        if (edition.currentEnrollment <= 0) throw BenefitGrantErrors.inconsistentEnrollment()
        edition.currentEnrollment -= 1
        application.status = ApplicationStatus.CLOSED
        logs.record(user = actor, action = LogAction.UPDATE, entityType = LogEntityType.APPLICATION,
            entityId = requireNotNull(application.id).toString(),
            oldValues = json.writeValueAsString(mapOf("status" to ApplicationStatus.APPROVED)),
            newValues = json.writeValueAsString(mapOf("status" to ApplicationStatus.CLOSED,
                "grantChangeId" to changeId, "responsibleUserId" to actor?.id, "changedAt" to changedAt,
                "editionId" to edition.id, "currentEnrollment" to edition.currentEnrollment)))
    }
}
