package com.uade.dda2.server.feature.application.repository

import com.uade.dda2.server.feature.application.entity.ApplicationResolution
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApplicationResolutionRepository : JpaRepository<ApplicationResolution, UUID> {
    @EntityGraph(attributePaths = ["resolvedBy", "requirementChecks"])
    fun findAllByApplicationIdOrderByResolvedAtAscIdAsc(applicationId: UUID): List<ApplicationResolution>

    fun existsByResolvedById(userId: Long): Boolean
}
