package com.uade.dda2.server.feature.program.validator

import com.uade.dda2.server.feature.program.dto.request.CreateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.request.UpdateProgramRequirementRequest
import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType
import com.uade.dda2.server.feature.program.error.ProgramRequirementErrors
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class ProgramRequirementValidator {

    fun validateCreate(request: CreateProgramRequirementRequest) {
        validateValue(
            type = request.type,
            value = request.value,
        )
    }

    fun validateUpdate(request: UpdateProgramRequirementRequest) {
        validateValue(
            type = request.type,
            value = request.value,
        )
    }

    private fun validateValue(
        type: ProgramRequirementType,
        value: String,
    ) {
        val normalizedValue = value.trim()

        val valid = when (type) {
            ProgramRequirementType.MIN_AGE ->
                isValidNonNegativeInteger(normalizedValue)

            ProgramRequirementType.MAX_INCOME ->
                isValidNonNegativeDecimal(normalizedValue)

            ProgramRequirementType.RESIDENCY_YEARS ->
                isValidNonNegativeInteger(normalizedValue)

            ProgramRequirementType.HAS_CHILDREN ->
                isValidBoolean(normalizedValue)
        }

        if (!valid) {
            throw ProgramRequirementErrors.invalidValue(
                type = type,
                value = value,
            )
        }
    }

    private fun isValidNonNegativeInteger(value: String): Boolean {
        val number = value.toIntOrNull() ?: return false

        return number >= 0
    }

    private fun isValidNonNegativeDecimal(value: String): Boolean {
        val number = value.toBigDecimalOrNull() ?: return false

        return number >= BigDecimal.ZERO
    }

    private fun isValidBoolean(value: String): Boolean =
        value.equals("true", ignoreCase = true) ||
                value.equals("false", ignoreCase = true)
}