package com.uade.dda2.server.feature.activity.service

import com.uade.dda2.server.feature.activity.dto.response.ActivityEnrollmentResponse
import com.uade.dda2.server.feature.activity.dto.response.CitizenActivityListResponse
import com.uade.dda2.server.feature.activity.dto.response.CitizenActivityResponse
import com.uade.dda2.server.feature.activity.entity.Activity
import com.uade.dda2.server.feature.activity.entity.ActivityEnrollment
import com.uade.dda2.server.feature.activity.entity.ActivityStatus
import com.uade.dda2.server.feature.activity.error.ActivityErrors
import com.uade.dda2.server.feature.activity.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.activity.mapper.toCitizenListResponse
import com.uade.dda2.server.feature.activity.mapper.toCitizenResponse
import com.uade.dda2.server.feature.activity.mapper.toResponse
import com.uade.dda2.server.feature.activity.repository.ActivityEnrollmentRepository
import com.uade.dda2.server.feature.activity.repository.ActivityRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class CitizenActivityService(
    private val activities: ActivityRepository,
    private val enrollments: ActivityEnrollmentRepository,
    private val currentUser: CurrentUserService,
    private val logs: LogService,
    private val json: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun list(page: Int, size: Int): CitizenActivityListResponse {
        val result = activities.findAllByStatusOrderByStartDateAsc(ActivityStatus.OPEN, PageRequest.of(page, size))
        val ids = result.content.map { requireNotNull(it.id) }
        val counts = if (ids.isEmpty()) {
            emptyMap()
        } else {
            enrollments.countByActivityIds(ids).associate { it.activityId to it.enrollmentCount }
        }
        return result.toCitizenListResponse(counts)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): CitizenActivityResponse {
        val activity = activities.findByIdAndStatus(id, ActivityStatus.OPEN)
            ?: throw ActivityErrors.notAvailable(id)
        return activity.toCitizenResponse(enrollments.countByActivityId(id))
    }

    @Transactional
    fun enroll(activityId: UUID): ActivityEnrollmentResponse {
        // Locking the activity serializes capacity checks and prevents overbooking.
        val activity = activities.findByIdForUpdate(activityId) ?: throw ActivityErrors.notFound(activityId)
        validateOpen(activity)

        val citizen = currentUser.userReference()
        val citizenId = requireNotNull(citizen.id)
        if (enrollments.existsByActivityIdAndCitizenId(activityId, citizenId)) {
            throw ActivityErrors.alreadyEnrolled()
        }
        if (enrollments.countByActivityId(activityId) >= activity.capacity) {
            throw ActivityErrors.capacityFull()
        }

        val enrollment = enrollments.saveAndFlush(
            ActivityEnrollment(activity = activity, citizen = citizen),
        )
        logs.record(
            user = citizen,
            action = LogAction.CREATE,
            entityType = LogEntityType.ACTIVITY_ENROLLMENT,
            entityId = requireNotNull(enrollment.id).toString(),
            newValues = json.writeValueAsString(enrollment.toAuditSnapshot()),
        )
        return enrollment.toResponse()
    }

    private fun validateOpen(activity: Activity) {
        if (activity.status != ActivityStatus.OPEN) throw ActivityErrors.notOpen()
    }
}
