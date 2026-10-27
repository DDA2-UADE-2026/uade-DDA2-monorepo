package com.uade.dda2.server.feature.application.validator

import com.uade.dda2.server.feature.application.dto.request.CreateApplicationVisitRescheduleRequest
import com.uade.dda2.server.feature.application.dto.request.ReviewApplicationVisitRescheduleRequest
import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleDecision
import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleRequestStatus
import com.uade.dda2.server.feature.application.error.ApplicationVisitRescheduleRequestErrors
import org.springframework.stereotype.Component

data class ValidatedApplicationVisitRescheduleReview(
    val status: ApplicationVisitRescheduleRequestStatus,
    val comment: String?,
)

@Component
class ApplicationVisitRescheduleRequestValidator {
    fun validateCreate(request: CreateApplicationVisitRescheduleRequest): String {
        val reason = request.reason?.trim()
        if (reason.isNullOrEmpty() || reason.length > 1000) {
            throw ApplicationVisitRescheduleRequestErrors.invalidData(
                "El motivo debe tener entre 1 y 1000 caracteres.",
            )
        }
        return reason
    }

    fun validateReview(request: ReviewApplicationVisitRescheduleRequest): ValidatedApplicationVisitRescheduleReview {
        val decision = request.status
            ?: throw ApplicationVisitRescheduleRequestErrors.invalidData("El estado de revision es obligatorio.")
        val comment = optionalComment(request.comment)
        return when (decision) {
            ApplicationVisitRescheduleDecision.APPROVED -> {
                if (comment == null && request.comment != null) {
                    throw ApplicationVisitRescheduleRequestErrors.invalidData(
                        "El comentario debe tener entre 1 y 1000 caracteres cuando se informa.",
                    )
                }
                ValidatedApplicationVisitRescheduleReview(
                    status = ApplicationVisitRescheduleRequestStatus.APPROVED,
                    comment = comment,
                )
            }

            ApplicationVisitRescheduleDecision.REJECTED -> {
                if (request.startsAt != null || request.durationMinutes != null || request.location != null) {
                    throw ApplicationVisitRescheduleRequestErrors.invalidData(
                        "El rechazo no admite startsAt, durationMinutes ni location.",
                    )
                }
                if (comment == null) {
                    throw ApplicationVisitRescheduleRequestErrors.invalidData(
                        "El comentario es obligatorio al rechazar y debe tener entre 1 y 1000 caracteres.",
                    )
                }
                ValidatedApplicationVisitRescheduleReview(
                    status = ApplicationVisitRescheduleRequestStatus.REJECTED,
                    comment = comment,
                )
            }
        }
    }

    private fun optionalComment(value: String?): String? {
        if (value == null) return null
        val comment = value.trim()
        if (comment.isEmpty() || comment.length > 1000) return null
        return comment
    }
}
