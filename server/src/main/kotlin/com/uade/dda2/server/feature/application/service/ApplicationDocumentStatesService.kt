package com.uade.dda2.server.feature.application.service

import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import com.uade.dda2.server.feature.application.repository.ApplicationDocumentRepository
import com.uade.dda2.server.feature.application.repository.ApplicationDocumentStateProjection
import com.uade.dda2.server.feature.integration.repository.ExternalDocumentationReceiptRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ApplicationDocumentStatesService(
    private val local: ApplicationDocumentRepository,
    private val external: ExternalDocumentationReceiptRepository,
) {
    fun findStatesByApplicationIdIn(applicationIds: Collection<UUID>): List<ApplicationDocumentStateProjection> {
        if (applicationIds.isEmpty()) return emptyList()
        val states = local.findStatesByApplicationIdIn(applicationIds).associateBy { it.applicationId to it.requirementId }.toMutableMap()
        val receipts = external.findAllByApplicationIdIn(applicationIds)
            .groupBy { it.applicationId to it.requirementId }
        receipts.forEach { (key, group) ->
            if (key !in states) {
                val receipt = group.maxWith(compareBy({ it.sourceUpdatedAt }, { it.requestId.toString() }))
                if (receipt.status == "COMPLETED") states[key] = ExternalState(receipt.applicationId, receipt.requirementId,
                    receipt.reviewStatus, receipt.observation)
            }
        }
        return states.values.toList()
    }

    private data class ExternalState(
        override val applicationId: UUID,
        override val requirementId: UUID,
        override val status: ApplicationDocumentStatus,
        override val observation: String?,
    ) : ApplicationDocumentStateProjection
}
