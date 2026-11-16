package com.uade.dda2.server.feature.integration.event

import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationResolution
import com.uade.dda2.server.feature.application.entity.ApplicationResolutionDecision
import com.uade.dda2.server.feature.application.entity.ApplicationVisit
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantChange
import java.util.UUID
import java.time.OffsetDateTime

sealed interface M8BusinessFact {
    data class ApplicationSubmitted(val application: Application) : M8BusinessFact
    data class BenefitChanged(val change: BenefitGrantChange, val causationId: UUID? = null) : M8BusinessFact
    data class WorkerAssigned(val application: Application) : M8BusinessFact
    data class VisitScheduled(val visit: ApplicationVisit) : M8BusinessFact
    data class ApplicationResolved(
        val application: Application,
        val decision: ApplicationResolutionDecision,
        val resolvedAt: OffsetDateTime,
    ) : M8BusinessFact {
        constructor(resolution: ApplicationResolution) : this(resolution.application, resolution.decision, resolution.resolvedAt)
    }
}
