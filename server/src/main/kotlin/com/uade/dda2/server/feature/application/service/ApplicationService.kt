package com.uade.dda2.server.feature.application.service

import com.uade.dda2.server.config.EnrollmentPeriodExpirationProperties
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationRequest
import com.uade.dda2.server.feature.application.dto.response.ApplicationListResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationResponse
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.error.ApplicationErrors
import com.uade.dda2.server.feature.application.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.application.mapper.toResponse
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.application.validator.ApplicationValidator
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.HexFormat
import java.util.UUID

data class ApplicationSubmission(val application: ApplicationResponse, val replayed: Boolean)

@Service
class ApplicationService(
    private val applications: ApplicationRepository,
    private val users: UserRepository,
    private val periods: EnrollmentPeriodRepository,
    private val editions: ProgramEditionRepository,
    private val currentUser: CurrentUserService,
    private val validator: ApplicationValidator,
    private val logs: LogService,
    private val json: JsonMapper,
    private val timeProperties: EnrollmentPeriodExpirationProperties,
) {
    @Transactional
    fun submit(request: CreateApplicationRequest, idempotencyKey: String?): ApplicationSubmission {
        validator.validateIdempotencyKey(idempotencyKey)
        val principal = currentUser.principal()
        // Serialize submissions for this user, including different keys and different periods.
        val user = validator.validateUser(users.findByIdForUpdate(principal.id), principal, "applications:own:create")
        val requestHash = hashRequest(request)
        if (idempotencyKey != null) {
            applications.findByUserIdAndIdempotencyKey(principal.id, idempotencyKey)?.let { existing ->
                if (existing.requestHash != requestHash || existing.enrollmentPeriod.id != request.enrollmentPeriodId) {
                    throw ApplicationErrors.idempotencyConflict()
                }
                return ApplicationSubmission(existing.toResponse(), replayed = true)
            }
        }

        val editionId = periods.findProgramEditionIdById(request.enrollmentPeriodId)
            ?: throw ApplicationErrors.periodNotFound()
        // Same lock order as the administrative period operations: edition, then period.
        editions.findByIdForUpdate(editionId) ?: throw ApplicationErrors.periodNotFound()
        val period = periods.findByIdForUpdate(request.enrollmentPeriodId) ?: throw ApplicationErrors.periodNotFound()
        // PostgreSQL timestamps store microseconds; keep first and replay responses identical.
        val now = LocalDateTime.now(timeProperties.zone()).truncatedTo(ChronoUnit.MICROS)
        validator.validatePeriod(period, now.toLocalDate())
        if (applications.existsByUserIdAndEnrollmentPeriodId(principal.id, request.enrollmentPeriodId)) {
            throw ApplicationErrors.duplicatePeriod()
        }
        if (applications.existsByUserIdAndProgramEditionIdAndStatusNotIn(principal.id, editionId, ApplicationStatus.ALLOWS_NEW_PERIOD)) {
            throw ApplicationErrors.blockingApplication()
        }

        val application = applications.saveAndFlush(Application(
            user = user, programEdition = period.programEdition, enrollmentPeriod = period,
            submittedAt = now, idempotencyKey = idempotencyKey,
            requestHash = requestHash.takeIf { idempotencyKey != null },
        ))
        logs.record(user = user, action = LogAction.CREATE, entityType = LogEntityType.APPLICATION,
            entityId = requireNotNull(application.id).toString(),
            newValues = json.writeValueAsString(application.toAuditSnapshot()))
        return ApplicationSubmission(application.toResponse(), replayed = false)
    }

    @Transactional(readOnly = true)
    fun list(page: Int, size: Int): ApplicationListResponse {
        val userId = authorizedUserId()
        val results = applications.findAllByUserId(userId, PageRequest.of(page, size, Sort.by("applicationNumber").descending()))
        return ApplicationListResponse(results.content.map { it.toResponse() }, results.number, results.size,
            results.totalElements, results.totalPages)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): ApplicationResponse =
        (applications.findByIdAndUserId(id, authorizedUserId()) ?: throw ApplicationErrors.notFound()).toResponse()

    private fun authorizedUserId(): Long {
        val principal = currentUser.principal()
        validator.validateUser(users.findByIdWithRoles(principal.id), principal, "applications:own:view")
        return principal.id
    }

    private fun hashRequest(request: CreateApplicationRequest): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest("application:v1:${request.enrollmentPeriodId}".toByteArray(Charsets.UTF_8)),
    )
}
