package com.uade.dda2.server.feature.application.entity

enum class ApplicationStatus {
    DRAFT, SUBMITTED, IN_VALIDATION, PENDING_DOCUMENTATION, IN_EVALUATION,
    IN_VISIT, APPROVED, REJECTED, WAITLISTED, CLOSED;

    companion object {
        val ALLOWS_NEW_PERIOD = setOf(REJECTED, CLOSED)
    }
}
