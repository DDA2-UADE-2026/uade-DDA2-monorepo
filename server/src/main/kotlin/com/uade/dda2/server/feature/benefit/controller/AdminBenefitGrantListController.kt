package com.uade.dda2.server.feature.benefit.controller

import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantStatus
import com.uade.dda2.server.feature.benefit.mapper.toCitizenResponse
import com.uade.dda2.server.feature.benefit.dto.response.CitizenBenefitGrantResponse
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
import com.uade.dda2.server.feature.benefit.validator.BenefitGrantValidator
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

data class AdminBenefitGrantListItem(val benefit: CitizenBenefitGrantResponse, val userId: Long, val userName: String)
data class AdminBenefitGrantListResponse(val content: List<AdminBenefitGrantListItem>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)

@RestController
@Validated
@RequestMapping("/api/admin/benefit-grants", produces = ["application/json"])
class AdminBenefitGrantListController(
    private val grants: BenefitGrantRepository,
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: BenefitGrantValidator,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('applications:management:view')")
    @Operation(operationId = "listAdminBenefitGrants", summary = "Listar beneficios otorgados")
    @Transactional(readOnly = true)
    fun list(@RequestParam(defaultValue = "0") @Min(0) page: Int,
             @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
             @RequestParam(required = false) status: BenefitGrantStatus?,
             @RequestParam(required = false) @Min(1) applicationNumber: Long?): AdminBenefitGrantListResponse {
        val principal = currentUser.principal()
        validator.validateActor(users.findByIdWithRoles(principal.id), principal, "applications:management:view")
        val results = grants.findAdminPage(status, applicationNumber, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt", "id")))
        return AdminBenefitGrantListResponse(results.content.map {
            AdminBenefitGrantListItem(it.toCitizenResponse(), requireNotNull(it.application.user.id), it.application.user.name)
        }, results.number, results.size, results.totalElements, results.totalPages)
    }
}
