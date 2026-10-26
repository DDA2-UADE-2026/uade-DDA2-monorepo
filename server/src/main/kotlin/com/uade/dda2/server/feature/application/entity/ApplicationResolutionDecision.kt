package com.uade.dda2.server.feature.application.entity

/** Decisiones con las que se resuelve una solicitud; cada una corresponde a un estado final o de espera. */
enum class ApplicationResolutionDecision(val status: ApplicationStatus) {
    APPROVED(ApplicationStatus.APPROVED),
    REJECTED(ApplicationStatus.REJECTED),
    WAITLISTED(ApplicationStatus.WAITLISTED),
}
