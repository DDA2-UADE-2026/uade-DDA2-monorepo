package com.uade.dda2.server.feature.program.validator

import com.uade.dda2.server.feature.program.dto.request.CreateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.request.UpdateProgramBenefitRequest
import com.uade.dda2.server.feature.program.error.ProgramBenefitErrors
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class ProgramBenefitValidator {

    fun validateCreate(request: CreateProgramBenefitRequest) {
        validateAmount(request.amount)
    }

    fun validateUpdate(request: UpdateProgramBenefitRequest) {
        validateAmount(request.amount)
    }

    private fun validateAmount(amount: BigDecimal?) {
        if (amount != null && amount < BigDecimal.ZERO) {
            throw ProgramBenefitErrors.invalidAmount()
        }
    }
}