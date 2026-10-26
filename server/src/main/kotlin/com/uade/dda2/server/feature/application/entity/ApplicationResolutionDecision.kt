package com.uade.dda2.server.feature.application.entity

enum class ApplicationResolutionDecision {
    APPROVED, REJECTED, WAITLISTED;

    fun applicationStatus(): ApplicationStatus = ApplicationStatus.valueOf(name)
}
