package com.uade.dda2.server.feature.benefit.service

import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.benefit.dto.response.CitizenBenefitGrantListResponse
import com.uade.dda2.server.feature.benefit.mapper.toCitizenResponse
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
import com.uade.dda2.server.feature.benefit.validator.BenefitGrantValidator
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

const val OWN_BENEFIT_VIEW_PERMISSION = "applications:own:benefits:view"

@Service
class CitizenBenefitGrantService(
    private val grants: BenefitGrantRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: BenefitGrantValidator,
) {
    @Transactional(readOnly = true)
    fun list(page: Int, size: Int): CitizenBenefitGrantListResponse {
        val principal = currentUser.principal()
        val actor = validator.validateActor(users.findByIdWithRoles(principal.id), principal, OWN_BENEFIT_VIEW_PERMISSION)
        val results = grants.findAllByApplicationUserId(requireNotNull(actor.id),
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt", "id")))
        return CitizenBenefitGrantListResponse(results.content.map { it.toCitizenResponse() }, results.number, results.size,
            results.totalElements, results.totalPages, if (results.totalElements == 0L) EMPTY_MESSAGE else null)
    }

    companion object { const val EMPTY_MESSAGE = "No tenés beneficios asignados." }
}
