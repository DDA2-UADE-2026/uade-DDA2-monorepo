package com.uade.dda2.server.feature.benefit.validator

import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.benefit.dto.request.RenewBenefitGrantRequest
import com.uade.dda2.server.feature.benefit.entity.*
import com.uade.dda2.server.feature.benefit.error.BenefitGrantErrors
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.ProgramRequirement
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

data class ValidatedBenefitRenewal(val reason: String, val endDate: LocalDate, val checks: Map<UUID, Boolean>)

@Component
class BenefitGrantLifecycleValidator {
    fun reason(value: String?): String {
        val trimmed = value?.trim()
        if (trimmed.isNullOrEmpty() || trimmed.length > 1000)
            throw BenefitGrantErrors.invalidData("El motivo debe tener entre 1 y 1000 caracteres.")
        return trimmed
    }

    fun validateStage(grant: BenefitGrant, action: BenefitGrantChangeAction) {
        if (grant.application.status != ApplicationStatus.APPROVED) throw BenefitGrantErrors.applicationNotApproved()
        if (grant.status in setOf(BenefitGrantStatus.FINALIZED, BenefitGrantStatus.EXPIRED) ||
            (action == BenefitGrantChangeAction.SUSPENDED && grant.status != BenefitGrantStatus.ACTIVE) ||
            action in setOf(BenefitGrantChangeAction.ASSIGNED, BenefitGrantChangeAction.EXPIRED)) throw BenefitGrantErrors.invalidTransition()
    }

    fun validateRenewal(request: RenewBenefitGrantRequest, grant: BenefitGrant, edition: ProgramEdition,
        today: LocalDate, requirements: List<ProgramRequirement>): ValidatedBenefitRenewal {
        if (grant.endDate < today) throw BenefitGrantErrors.invalidTransition()
        val reason = reason(request.reason)
        val end = request.endDate ?: throw BenefitGrantErrors.invalidData("La fecha de fin es obligatoria.")
        if (end <= grant.endDate || end < today || end > edition.endDate || grant.startDate < edition.startDate || grant.startDate > today)
            throw BenefitGrantErrors.invalidData("El nuevo fin debe extender el actual, incluir hoy y permanecer dentro de la edicion.")
        val checks = request.requirementChecks ?: throw BenefitGrantErrors.invalidData("requirementChecks es obligatorio.")
        if (checks.any { it == null || it.requirementId == null || it.fulfilled == null } ||
            checks.map { it?.requirementId }.distinct().size != checks.size ||
            checks.map { it?.requirementId }.toSet() != requirements.map { it.id }.toSet())
            throw BenefitGrantErrors.invalidData("Se requiere un resultado por requisito vigente, sin duplicados ni requisitos ajenos.")
        if (edition.status != ProgramEditionStatus.ACTIVE) throw BenefitGrantErrors.editionNotActive()
        val results = checks.filterNotNull().associate { requireNotNull(it.requirementId) to requireNotNull(it.fulfilled) }
        if (results.values.any { !it }) throw BenefitGrantErrors.requirementsNotMet()
        return ValidatedBenefitRenewal(reason, end, results)
    }
}
