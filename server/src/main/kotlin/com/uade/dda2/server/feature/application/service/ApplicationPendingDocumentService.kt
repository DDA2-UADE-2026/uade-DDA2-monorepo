package com.uade.dda2.server.feature.application.service

import com.uade.dda2.server.feature.application.dto.response.PendingApplicationDocumentResponse
import com.uade.dda2.server.feature.application.dto.response.PendingDocumentReason
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import com.uade.dda2.server.feature.application.repository.ApplicationDocumentRepository
import com.uade.dda2.server.feature.program.repository.ProgramDocumentRequirementRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ApplicationPendingDocumentService(
    private val requirements: ProgramDocumentRequirementRepository,
    private val documents: ApplicationDocumentRepository,
) {
    fun calculate(application: Application): List<PendingApplicationDocumentResponse> =
        calculate(listOf(application))[requireNotNull(application.id)].orEmpty()

    fun calculate(applications: Collection<Application>): Map<UUID, List<PendingApplicationDocumentResponse>> {
        if (applications.isEmpty()) return emptyMap()
        val applicationIds = applications.map { requireNotNull(it.id) }
        val editionIds = applications.map { requireNotNull(it.programEdition.id) }.distinct()
        val requirementsByEdition = requirements.findAllByProgramEditionIdIn(editionIds)
            .asSequence()
            .filter { it.required }
            .sortedWith(compareBy({ it.name }, { it.code }))
            .groupBy { requireNotNull(it.programEdition.id) }
        val states = documents.findStatesByApplicationIdIn(applicationIds)
            .associateBy { it.applicationId to it.requirementId }

        return applications.associate { application ->
            val applicationId = requireNotNull(application.id)
            val editionId = requireNotNull(application.programEdition.id)
            applicationId to requirementsByEdition[editionId].orEmpty().mapNotNull { requirement ->
                val state = states[applicationId to requireNotNull(requirement.id)]
                when {
                    state == null -> PendingApplicationDocumentResponse(
                        requirementId = requireNotNull(requirement.id), code = requirement.code, name = requirement.name,
                        reason = PendingDocumentReason.MISSING, observation = null,
                    )
                    state.status == ApplicationDocumentStatus.OBSERVED -> PendingApplicationDocumentResponse(
                        requirementId = requireNotNull(requirement.id), code = requirement.code, name = requirement.name,
                        reason = PendingDocumentReason.OBSERVED, observation = state.observation,
                    )
                    else -> null
                }
            }
        }
    }
}
