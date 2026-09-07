package com.uade.dda2.server.feature.application.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.application.dto.request.ReviewApplicationDocumentRequest
import com.uade.dda2.server.feature.application.dto.response.ApplicationDocumentContent
import com.uade.dda2.server.feature.application.dto.response.ApplicationDocumentMutation
import com.uade.dda2.server.feature.application.dto.response.ApplicationDocumentResponse
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationDocument
import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import com.uade.dda2.server.feature.application.error.ApplicationDocumentErrors
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import com.uade.dda2.server.feature.application.repository.ApplicationDocumentMetadataProjection
import com.uade.dda2.server.feature.application.repository.ApplicationDocumentRepository
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.application.validator.ApplicationDocumentValidator
import com.uade.dda2.server.feature.application.validator.ApplicationValidator
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.document.entity.Document
import com.uade.dda2.server.feature.document.repository.DocumentRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.error.ProgramDocumentRequirementErrors
import com.uade.dda2.server.feature.program.repository.ProgramDocumentRequirementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ApplicationDocumentService(
    private val applications: ApplicationRepository,
    private val applicationDocuments: ApplicationDocumentRepository,
    private val documents: DocumentRepository,
    private val requirements: ProgramDocumentRequirementRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val applicationValidator: ApplicationValidator,
    private val documentValidator: ApplicationDocumentValidator,
    private val logs: LogService,
    private val json: JsonMapper,
    private val timeProperties: EnrollmentPeriodExpirationProperties,
) {
    @Transactional(readOnly = true)
    fun listOwn(applicationId: UUID): List<ApplicationDocumentResponse> {
        val actor = authorized("applications:own:documents:view")
        applications.findByIdAndUserId(applicationId, requireNotNull(actor.id)) ?: throw ApplicationErrors.notFound()
        return metadata(applicationId, administrative = false)
    }

    @Transactional
    fun putOwn(applicationId: UUID, requirementId: UUID, file: MultipartFile): ApplicationDocumentMutation {
        val actor = authorized("applications:own:documents:manage")
        val application = applications.findByIdAndUserIdForUpdate(applicationId, requireNotNull(actor.id))
            ?: throw ApplicationErrors.notFound()
        documentValidator.validateMutable(application)
        val requirement = requirements.findByIdAndProgramEditionId(requirementId, requireNotNull(application.programEdition.id))
            ?: throw ProgramDocumentRequirementErrors.wrongEdition(requirementId)
        val validated = documentValidator.validateFile(file)
        val now = now()
        val stored = documents.saveAndFlush(Document(
            originalName = validated.originalName,
            contentType = validated.contentType,
            sizeBytes = validated.content.size.toLong(),
            content = validated.content,
            uploadedBy = actor,
            createdAt = now,
        ))

        val existing = applicationDocuments.findEntityByApplicationIdAndRequirementId(applicationId, requirementId)
        val created = existing == null
        val applicationDocument: ApplicationDocument
        val oldSnapshot: String?
        val oldDocument: Document?
        if (existing == null) {
            applicationDocument = ApplicationDocument(
                application = application,
                requirement = requirement,
                document = stored,
                uploadedAt = now,
            )
            oldSnapshot = null
            oldDocument = null
        } else {
            applicationDocument = existing
            oldSnapshot = snapshotJson(existing)
            oldDocument = existing.document
            existing.document = stored
            existing.status = ApplicationDocumentStatus.PENDING
            existing.observation = null
            existing.reviewedBy = null
            existing.reviewedAt = null
            existing.uploadedAt = now
        }
        val saved = applicationDocuments.saveAndFlush(applicationDocument)
        if (oldDocument != null) {
            documents.delete(oldDocument)
            documents.flush()
        }
        touch(application, now)
        logs.record(
            user = actor,
            action = if (created) LogAction.CREATE else LogAction.UPDATE,
            entityType = LogEntityType.APPLICATION_DOCUMENT,
            entityId = requireNotNull(saved.id).toString(),
            oldValues = oldSnapshot,
            newValues = snapshotJson(saved),
        )
        return ApplicationDocumentMutation(response(applicationId, requireNotNull(saved.id), false), created)
    }

    @Transactional
    fun deleteOwn(applicationId: UUID, applicationDocumentId: UUID) {
        val actor = authorized("applications:own:documents:manage")
        val application = applications.findByIdAndUserIdForUpdate(applicationId, requireNotNull(actor.id))
            ?: throw ApplicationErrors.notFound()
        documentValidator.validateMutable(application)
        val link = applicationDocuments.findEntityByIdAndApplicationId(applicationDocumentId, applicationId)
            ?: throw ApplicationDocumentErrors.notFound()
        val oldSnapshot = snapshotJson(link)
        val oldDocument = link.document
        applicationDocuments.delete(link)
        applicationDocuments.flush()
        documents.delete(oldDocument)
        documents.flush()
        touch(application, now())
        logs.record(actor, LogAction.DELETE, LogEntityType.APPLICATION_DOCUMENT, applicationDocumentId.toString(), oldValues = oldSnapshot)
    }

    @Transactional(readOnly = true)
    fun contentOwn(applicationId: UUID, applicationDocumentId: UUID): ApplicationDocumentContent {
        val actor = authorized("applications:own:documents:view")
        applications.findByIdAndUserId(applicationId, requireNotNull(actor.id)) ?: throw ApplicationErrors.notFound()
        return content(applicationId, applicationDocumentId)
    }

    @Transactional(readOnly = true)
    fun listAdmin(applicationId: UUID): List<ApplicationDocumentResponse> {
        authorized("applications:management:documents:view")
        if (!applications.existsById(applicationId)) throw ApplicationErrors.notFound()
        return metadata(applicationId, administrative = true)
    }

    @Transactional(readOnly = true)
    fun contentAdmin(applicationId: UUID, applicationDocumentId: UUID): ApplicationDocumentContent {
        authorized("applications:management:documents:view")
        if (!applications.existsById(applicationId)) throw ApplicationErrors.notFound()
        return content(applicationId, applicationDocumentId)
    }

    @Transactional
    fun reviewAdmin(applicationId: UUID, applicationDocumentId: UUID, request: ReviewApplicationDocumentRequest): ApplicationDocumentResponse {
        val actor = authorized("applications:management:documents:review")
        val application = applications.findByIdForUpdate(applicationId) ?: throw ApplicationErrors.notFound()
        documentValidator.validateMutable(application)
        val link = applicationDocuments.findEntityByIdAndApplicationId(applicationDocumentId, applicationId)
            ?: throw ApplicationDocumentErrors.notFound()
        if (link.status != ApplicationDocumentStatus.PENDING) throw ApplicationDocumentErrors.invalidReviewStatus()
        val observation = documentValidator.normalizedObservation(request.status, request.observation)
        val oldSnapshot = snapshotJson(link)
        val now = now()
        link.status = request.status
        link.observation = observation
        link.reviewedBy = actor
        link.reviewedAt = now
        applicationDocuments.saveAndFlush(link)
        touch(application, now)
        logs.record(actor, LogAction.UPDATE, LogEntityType.APPLICATION_DOCUMENT, applicationDocumentId.toString(), oldSnapshot, snapshotJson(link))
        return response(applicationId, applicationDocumentId, true)
    }

    private fun authorized(permission: String): User {
        val principal = currentUser.principal()
        return applicationValidator.validateUser(users.findByIdWithRoles(principal.id), principal, permission)
    }

    private fun metadata(applicationId: UUID, administrative: Boolean): List<ApplicationDocumentResponse> =
        applicationDocuments.findMetadataByApplicationId(applicationId).map { it.toResponse(administrative) }

    private fun response(applicationId: UUID, id: UUID, administrative: Boolean): ApplicationDocumentResponse =
        (applicationDocuments.findMetadataByIdAndApplicationId(id, applicationId) ?: throw ApplicationDocumentErrors.notFound())
            .toResponse(administrative)

    private fun ApplicationDocumentMetadataProjection.toResponse(administrative: Boolean) = ApplicationDocumentResponse(
        id = id, applicationId = applicationId, requirementId = requirementId, requirementCode = requirementCode,
        requirementName = requirementName, required = required, documentId = documentId, originalName = originalName,
        contentType = contentType, sizeBytes = sizeBytes, status = status, observation = observation,
        uploadedAt = uploadedAt, reviewedByUserId = reviewedByUserId, reviewedAt = reviewedAt,
        contentUrl = if (administrative) "/api/admin/applications/$applicationId/documents/$id/content"
            else "/api/applications/$applicationId/documents/$id/content",
    )

    private fun content(applicationId: UUID, id: UUID): ApplicationDocumentContent {
        val projection = applicationDocuments.findContentByIdAndApplicationId(id, applicationId)
            ?: throw ApplicationDocumentErrors.notFound()
        return ApplicationDocumentContent(projection.originalName, projection.contentType, projection.sizeBytes, projection.content)
    }

    private fun touch(application: Application, at: LocalDateTime) {
        application.updatedAt = at
        applications.save(application)
    }

    private fun snapshotJson(link: ApplicationDocument): String = json.writeValueAsString(mapOf(
        "id" to link.id?.toString(),
        "applicationId" to link.application.id?.toString(),
        "requirementId" to link.requirement.id?.toString(),
        "documentId" to link.document.id?.toString(),
        "originalName" to link.document.originalName,
        "contentType" to link.document.contentType,
        "sizeBytes" to link.document.sizeBytes,
        "status" to link.status.name,
        "observation" to link.observation,
        "uploadedAt" to link.uploadedAt.toString(),
        "reviewedByUserId" to link.reviewedBy?.id,
        "reviewedAt" to link.reviewedAt?.toString(),
    ))

    private fun now(): LocalDateTime = LocalDateTime.now(timeProperties.zone()).truncatedTo(ChronoUnit.MICROS)
}
