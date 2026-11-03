package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import com.uade.dda2.server.feature.application.dto.request.ReviewApplicationDocumentRequest
import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.dto.ExternalDocumentationResponse
import com.uade.dda2.server.feature.integration.entity.ExternalDocumentationReceipt
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.repository.CoreOutboxRepository
import com.uade.dda2.server.feature.integration.repository.ExternalDocumentationReceiptRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.repository.ProgramDocumentRequirementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.util.UUID

@Service
class ExternalDocumentationService(
    private val applications: ApplicationRepository,
    private val users: UserRepository,
    private val requirements: ProgramDocumentRequirementRepository,
    private val receipts: ExternalDocumentationReceiptRepository,
    private val outbox: CoreOutboxRepository,
    private val policy: M8BusinessPolicy,
    private val access: SocialIntegrationAccess,
    private val logs: LogService,
    private val json: JsonMapper,
    private val clock: Clock,
) {
    @Transactional
    fun receive(message: M8EventEnvelope<SocialDocumentationDeliveredData>) {
        val p = message.data
        val application = applications.findByIdForUpdate(p.applicationId) ?: return
        val user = users.findByIdForUpdate(requireNotNull(application.user.id)) ?: throw CoreIntegrationException("M8_DOCUMENT_IDENTITY_MISMATCH")
        if (policy.citizenId(user) != p.citizenId || application.programEdition.id != p.programEditionId ||
            application.programEdition.program.id != p.programId) throw CoreIntegrationException("M8_DOCUMENT_IDENTITY_MISMATCH")
        val request = message.causationId?.let { outbox.findById(it).orElse(null) }
            ?: throw CoreIntegrationException("M8_DOCUMENT_REQUEST_UNKNOWN")
        if (request.eventType != M8EventType.SOCIAL_DOCUMENTATION_REQUESTED.wireName || request.correlationId != p.applicationId)
            throw CoreIntegrationException("M8_DOCUMENT_REQUEST_UNKNOWN")
        val requestedIds: Set<String> = json.readTree(request.payloadJson)["data"]["requiredDocuments"]
            .asSequence().map { it["requirementId"].asString() }.toSet()
        p.files.forEach { file -> file.documentationRequests.forEach { document ->
            val requirement = requirements.findByIdAndProgramEditionId(document.requirementId, p.programEditionId)
                ?: throw CoreIntegrationException("M8_DOCUMENT_REQUIREMENT_UNKNOWN")
            if (document.requirementId.toString() !in requestedIds || requirement.code != document.documentType)
                throw CoreIntegrationException("M8_DOCUMENT_REQUIREMENT_MISMATCH")
            val existing = receipts.findById(document.requestId).orElse(null)
            if (existing != null && (existing.applicationId != p.applicationId || existing.requirementId != document.requirementId))
                throw CoreIntegrationException("M8_DOCUMENT_REQUEST_CONFLICT")
            if (existing == null) receipts.save(ExternalDocumentationReceipt(document.requestId, p.applicationId, document.requirementId,
                file.fileId, file.number, document.documentId, document.status.name, message.eventId, message.occurredAt.toInstant()))
            else if (!message.occurredAt.toInstant().isBefore(existing.sourceUpdatedAt)) {
                if (existing.documentId != document.documentId || existing.status != document.status.name) {
                    existing.reviewStatus = ApplicationDocumentStatus.PENDING
                    existing.observation = null
                    existing.reviewedBy = null
                    existing.reviewedAt = null
                }
                existing.fileId = file.fileId
                existing.fileNumber = file.number
                existing.documentId = document.documentId
                existing.status = document.status.name
                existing.lastEventId = message.eventId
                existing.sourceUpdatedAt = message.occurredAt.toInstant()
            }
        } }
        logs.record(null, LogAction.UPDATE, LogEntityType.EXTERNAL_DOCUMENTATION, p.applicationId.toString(),
            newValues = json.writeValueAsString(mapOf("eventId" to message.eventId, "applicationId" to p.applicationId)))
    }

    @Transactional(readOnly = true)
    fun list(applicationId: UUID): List<ExternalDocumentationResponse> {
        access.check("applications:management:documents:view")
        if (!applications.existsById(applicationId)) throw NotFoundException("APPLICATION_NOT_FOUND", "No se encontró la solicitud.")
        return receipts.findAllByApplicationId(applicationId).map { response(it) }
    }

    @Transactional
    fun review(applicationId: UUID, requestId: UUID, request: ReviewApplicationDocumentRequest): ExternalDocumentationResponse {
        val actor = access.check("applications:management:documents:review")
        val application = applications.findByIdForUpdate(applicationId) ?: throw NotFoundException("APPLICATION_NOT_FOUND", "No se encontró la solicitud.")
        if (application.status !in ApplicationStatus.EVALUATION_ALLOWED + ApplicationStatus.IN_VISIT)
            throw ConflictException("APPLICATION_DOCUMENT_NOT_MUTABLE", "La solicitud ya no admite revisión documental.")
        val receipt = receipts.findById(requestId).orElse(null)?.takeIf { it.applicationId == applicationId }
            ?: throw NotFoundException("M8_DOCUMENT_NOT_FOUND", "No se encontró la referencia documental.")
        val observation = request.observation?.trim()?.takeIf { it.isNotEmpty() }
        if (receipt.status != "COMPLETED" || receipt.documentId == null || receipt.reviewStatus != ApplicationDocumentStatus.PENDING ||
            request.status !in setOf(ApplicationDocumentStatus.VALID, ApplicationDocumentStatus.OBSERVED) ||
            (request.status == ApplicationDocumentStatus.VALID && observation != null) ||
            (request.status == ApplicationDocumentStatus.OBSERVED && observation == null))
            throw ConflictException("M8_DOCUMENT_REVIEW_INVALID", "Solo puede revisarse una entrega COMPLETED pendiente; OBSERVED exige observación y VALID no la admite.")
        receipt.reviewStatus = request.status
        receipt.observation = observation
        receipt.reviewedBy = actor
        receipt.reviewedAt = clock.instant()
        logs.record(actor, LogAction.UPDATE, LogEntityType.EXTERNAL_DOCUMENTATION, requestId.toString(),
            newValues = json.writeValueAsString(mapOf("reviewStatus" to request.status, "documentId" to receipt.documentId)))
        return response(receipt)
    }
    private fun response(receipt: ExternalDocumentationReceipt) = ExternalDocumentationResponse(receipt.requestId,
        receipt.applicationId, receipt.requirementId, receipt.fileId, receipt.fileNumber, receipt.documentId, receipt.status,
        receipt.reviewStatus, receipt.observation, receipt.reviewedBy?.id, receipt.reviewedAt)
}
