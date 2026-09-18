package com.uade.dda2.server.feature.activity.service

import com.uade.dda2.server.feature.activity.dto.request.UpdateActivityAttendanceRequest
import com.uade.dda2.server.feature.activity.dto.response.ProfessionalActivityEnrollmentListResponse
import com.uade.dda2.server.feature.activity.dto.response.ProfessionalActivityEnrollmentResponse
import com.uade.dda2.server.feature.activity.dto.response.ProfessionalActivityListResponse
import com.uade.dda2.server.feature.activity.entity.Activity
import com.uade.dda2.server.feature.activity.entity.ActivityStatus
import com.uade.dda2.server.feature.activity.error.ActivityErrors
import com.uade.dda2.server.feature.activity.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.activity.mapper.toProfessionalListResponse
import com.uade.dda2.server.feature.activity.mapper.toProfessionalEnrollmentListResponse
import com.uade.dda2.server.feature.activity.mapper.toProfessionalResponse
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class ProfessionalActivityService(
    private val activities: ActivityRepository,
    private val enrollments: ActivityEnrollmentRepository,
    private val currentUser: CurrentUserService,
    private val logs: LogService,
    private val json: JsonMapper,
) {
    private val attendanceStatuses = listOf(ActivityStatus.OPEN, ActivityStatus.CLOSED)

    @Transactional(readOnly = true)
    fun listActivities(page: Int, size: Int): ProfessionalActivityListResponse {
        val result = activities.findAllByStatusInOrderByStartDateDesc(attendanceStatuses, PageRequest.of(page, size))
        val ids = result.content.map { requireNotNull(it.id) }
        val counts = if (ids.isEmpty()) {
            emptyMap()
        } else {
            enrollments.countByActivityIds(ids).associate { it.activityId to it.enrollmentCount }
        }
        return result.toProfessionalListResponse(counts)
    }

    @Transactional(readOnly = true)
    fun listEnrollments(activityId: UUID, page: Int, size: Int): ProfessionalActivityEnrollmentListResponse {
        validateAttendanceActivity(findActivity(activityId))
        return enrollments.findAllByActivityId(activityId, PageRequest.of(page, size)).toProfessionalEnrollmentListResponse()
    }

    @Transactional
    fun updateAttendance(
        activityId: UUID,
        enrollmentId: UUID,
        request: UpdateActivityAttendanceRequest,
    ): ProfessionalActivityEnrollmentResponse {
        val activity = activities.findByIdForUpdate(activityId) ?: throw ActivityErrors.notFound(activityId)
        validateAttendanceActivity(activity)
        val enrollment = enrollments.findByIdAndActivityIdForUpdate(enrollmentId, activityId)
            ?: throw ActivityErrors.enrollmentNotFound(enrollmentId)
        val oldValues = json.writeValueAsString(enrollment.toAuditSnapshot())
        val professional = currentUser.userReference()

        enrollment.attendance = request.attendance
        enrollment.attendanceRecordedBy = professional
        enrollment.attendanceRecordedAt = OffsetDateTime.now(ZoneOffset.UTC)
        enrollments.saveAndFlush(enrollment)

        logs.record(
            user = professional,
            action = LogAction.UPDATE,
            entityType = LogEntityType.ACTIVITY_ENROLLMENT,
            entityId = requireNotNull(enrollment.id).toString(),
            oldValues = oldValues,
            newValues = json.writeValueAsString(enrollment.toAuditSnapshot()),
        )
        return enrollment.toProfessionalResponse()
    }

    private fun findActivity(id: UUID): Activity =
        activities.findById(id).orElseThrow { ActivityErrors.notFound(id) }

    private fun validateAttendanceActivity(activity: Activity) {
        if (activity.status == ActivityStatus.DRAFT) throw ActivityErrors.attendanceNotAllowed()
    }
}
