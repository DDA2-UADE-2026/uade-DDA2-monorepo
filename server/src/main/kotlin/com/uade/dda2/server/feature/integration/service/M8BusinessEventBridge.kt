package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantStatus
import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.event.M8BusinessFact
import com.uade.dda2.server.feature.integration.messaging.M8RabbitPublisher
import com.uade.dda2.server.feature.program.repository.ProgramDocumentRequirementRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class M8BusinessEventBridge(
    private val policy: M8BusinessPolicy,
    private val documents: ProgramDocumentRequirementRepository,
    private val publisher: M8RabbitPublisher,
    private val ticketEvents: SocialTicketEventService,
    private val time: EnrollmentPeriodExpirationProperties,
    private val messages: M8MessageFactory,
) {
    // EventListener síncrono: el hecho local y la outbox participan de la misma transacción.
    @EventListener
    fun applicationSubmitted(fact: M8BusinessFact.ApplicationSubmitted) {
        if (!policy.enabled()) return
        val application = fact.application
        policy.validateNewApplication(application.user)
        val catalog = documents.findAllByProgramEditionIdOrderByNameAsc(requireNotNull(application.programEdition.id))
        if (catalog.isEmpty()) return
        if (catalog.size > 10) throw ConflictException("M8_DOCUMENT_REQUIREMENTS_LIMIT", "La edición supera los diez requisitos admitidos por el contrato de Ciudadanos.")
        val applicationId = requireNotNull(application.id)
        val data = SocialDocumentationRequestedData(
            citizenId = policy.citizenId(application.user), applicationId = applicationId,
            programId = requireNotNull(application.programEdition.program.id), programEditionId = requireNotNull(application.programEdition.id),
            requiredDocuments = catalog.map { RequiredDocumentData(requireNotNull(it.id), it.code, it.name, it.required, it.description) },
            applicationNumber = application.applicationNumber?.toString(), notes = application.enrollmentPeriod.notes,
        )
        publisher.documentationRequested(messages.create(data, applicationId))
    }

    @EventListener
    fun benefitChanged(fact: M8BusinessFact.BenefitChanged) {
        if (!policy.enabled()) return
        val change = fact.change
        val grant = change.grant
        val application = grant.application
        val status = when (change.status) {
            BenefitGrantStatus.ACTIVE -> SocialBenefitEventStatus.APPROVED
            BenefitGrantStatus.SUSPENDED -> SocialBenefitEventStatus.SUSPENDED
            BenefitGrantStatus.FINALIZED, BenefitGrantStatus.EXPIRED -> SocialBenefitEventStatus.FINALIZED
        }
        val endDate = change.effectiveEndDate ?: when (change.status) {
            BenefitGrantStatus.FINALIZED -> change.changedAt.atZoneSameInstant(time.zone()).toLocalDate()
            else -> change.endDate
        }
        val data = SocialBenefitUpdatedData(
            benefitId = requireNotNull(grant.id), citizenId = policy.citizenId(application.user), applicationId = requireNotNull(application.id),
            programId = requireNotNull(application.programEdition.program.id), programEditionId = requireNotNull(application.programEdition.id),
            programName = application.programEdition.name, benefitTypeId = requireNotNull(grant.programBenefit.id),
            type = SocialBenefitEventType.valueOf(grant.benefitType.name), amount = grant.amount, status = status,
            startDate = change.startDate, endDate = endDate, updatedAt = change.changedAt, description = grant.description,
            reason = if (status == SocialBenefitEventStatus.APPROVED) null else change.reason,
        )
        publisher.benefitUpdated(messages.create(data, requireNotNull(application.id), fact.causationId ?: change.causationEventId, change.changedAt))
    }

    @EventListener
    fun workerAssigned(fact: M8BusinessFact.WorkerAssigned) {
        if (policy.enabled()) ticketEvents.workerAssigned(fact.application)
    }

    @EventListener
    fun visitScheduled(fact: M8BusinessFact.VisitScheduled) {
        if (policy.enabled()) ticketEvents.visitScheduled(fact.visit)
    }

    @EventListener
    fun applicationResolved(fact: M8BusinessFact.ApplicationResolved) {
        if (policy.enabled()) ticketEvents.applicationResolved(fact)
    }

}
