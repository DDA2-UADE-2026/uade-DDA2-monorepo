package com.uade.dda2.server.feature.center.validator

import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.center.error.CenterErrors
import org.springframework.stereotype.Component

@Component
class ProfessionalAssignmentValidator {
    fun validateEligible(professional: User) {
        val hasProfessionalRole = professional.roles.any { it.name.equals("PROFESIONAL_CENTRO", ignoreCase = true) }
        if (!professional.active || !hasProfessionalRole) throw CenterErrors.professionalNotEligible()
    }
}
