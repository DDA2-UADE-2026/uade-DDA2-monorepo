package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.feature.auth.repository.CitizenSnapshotRepository
import com.uade.dda2.server.feature.integration.dto.CitizenIntegrationAlertResponse
import com.uade.dda2.server.feature.integration.dto.SocialIntegrationPage
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CitizenIntegrationAlertService(
    private val snapshots: CitizenSnapshotRepository,
    private val access: SocialIntegrationAccess,
) {
    @Transactional(readOnly = true)
    fun list(page: Int, size: Int): SocialIntegrationPage<CitizenIntegrationAlertResponse> {
        access.check("users:view")
        val result = snapshots.findIntegrationAlerts(PageRequest.of(page, size, Sort.by("userId")))
        return SocialIntegrationPage(result.content.map { CitizenIntegrationAlertResponse(requireNotNull(it.userId),
            it.user.externalCitizenId, it.blocked, it.blockReason, it.reviewRequired, it.deceasedAt, it.lastStateAt) },
            result.number, result.size, result.totalElements, result.totalPages)
    }
}
