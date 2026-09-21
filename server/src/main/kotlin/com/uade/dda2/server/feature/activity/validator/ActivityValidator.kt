package com.uade.dda2.server.feature.activity.validator

import com.uade.dda2.server.feature.activity.dto.request.CreateActivityRequest
import com.uade.dda2.server.feature.activity.dto.request.UpdateActivityRequest
import com.uade.dda2.server.feature.activity.entity.Activity
import com.uade.dda2.server.feature.activity.entity.ActivityStatus
import com.uade.dda2.server.feature.activity.error.ActivityErrors
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ActivityValidator {
    fun validateCreate(request: CreateActivityRequest) {
        validateDates(request.startDate, request.endDate)
    }

    fun validateUpdate(activity: Activity, request: UpdateActivityRequest) {
        if (activity.status != ActivityStatus.DRAFT) {
            throw ActivityErrors.cannotEdit(activity.status)
        }
        validateDates(request.startDate, request.endDate)
    }

    fun validatePublish(activity: Activity) {
        validateTransition(activity, ActivityStatus.DRAFT, ActivityStatus.OPEN)
    }

    fun validateClose(activity: Activity) {
        validateTransition(activity, ActivityStatus.OPEN, ActivityStatus.CLOSED)
    }

    private fun validateDates(startDate: LocalDate, endDate: LocalDate) {
        if (endDate.isBefore(startDate)) {
            throw ActivityErrors.invalidDateRange()
        }
    }

    private fun validateTransition(
        activity: Activity,
        expectedStatus: ActivityStatus,
        newStatus: ActivityStatus,
    ) {
        if (activity.status != expectedStatus) {
            throw ActivityErrors.invalidStatusTransition(activity.status, newStatus)
        }
    }
}
