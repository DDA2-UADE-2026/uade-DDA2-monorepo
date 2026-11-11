package com.uade.dda2.server.feature.program.validator

import com.uade.dda2.server.error.ApiException
import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramRequirementRequest
import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProgramRequirementBranchTest {
    @TestFactory
    fun `create and update enforce typed requirement values`() = ProgramRequirementType.entries.flatMap { type ->
        val valid = when (type) {
            ProgramRequirementType.MIN_AGE, ProgramRequirementType.RESIDENCY_YEARS -> listOf("0", " 18 ", "2147483647")
            ProgramRequirementType.MAX_INCOME -> listOf("0", " 0.01 ", "100000000000000000000.50")
            ProgramRequirementType.HAS_CHILDREN -> listOf("true", "FALSE", " True ")
        }
        val invalid = when (type) {
            ProgramRequirementType.MIN_AGE, ProgramRequirementType.RESIDENCY_YEARS -> listOf("-1", "1.5", "2147483648", "abc", "")
            ProgramRequirementType.MAX_INCOME -> listOf("-0.01", "abc", "", "1,5")
            ProgramRequirementType.HAS_CHILDREN -> listOf("1", "yes", "", "falsehood")
        }
        (valid.map { it to true } + invalid.map { it to false }).map { (value, accepted) ->
            dynamicTest("$type '$value' accepted=$accepted") {
                val validator = AdminProgramRequirementValidator()
                val actions = listOf<() -> Unit>(
                    { validator.validateCreate(CreateProgramRequirementRequest(type, value)) },
                    { validator.validateUpdate(UpdateProgramRequirementRequest(type, value)) },
                )
                actions.forEach { action ->
                    if (accepted) action()
                    else assertEquals("PROGRAM_REQUIREMENT_INVALID_VALUE", assertFailsWith<ApiException>(block = action).code)
                }
            }
        }
    }
}
