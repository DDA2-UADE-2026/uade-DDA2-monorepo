package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramBenefitResponse
import com.uade.dda2.server.feature.program.entity.ProgramBenefit
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.error.ProgramBenefitErrors
import com.uade.dda2.server.feature.program.error.ProgramEditionErrors
import com.uade.dda2.server.feature.program.mapper.toEntity
import com.uade.dda2.server.feature.program.mapper.toResponse
import com.uade.dda2.server.feature.program.mapper.updateFrom
import com.uade.dda2.server.feature.program.repository.ProgramBenefitRepository
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.validator.AdminProgramBenefitValidator
import com.uade.dda2.server.feature.program.validator.AdminProgramEditionValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AdminProgramBenefitService(
    private val programEditionRepository: ProgramEditionRepository,
    private val programBenefitRepository: ProgramBenefitRepository,
    private val adminProgramBenefitValidator: AdminProgramBenefitValidator,
    private val adminProgramEditionValidator: AdminProgramEditionValidator,
) {

    @Transactional(readOnly = true)
    fun list(
        editionId: UUID,
    ): List<ProgramBenefitResponse> {
        findEdition(editionId)

        return programBenefitRepository
            .findAllByProgramEditionId(editionId)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun get(
        editionId: UUID,
        benefitId: UUID,
    ): ProgramBenefitResponse =
        findBenefit(
            editionId = editionId,
            benefitId = benefitId,
        ).toResponse()

    @Transactional
    fun create(
        editionId: UUID,
        request: CreateProgramBenefitRequest,
    ): ProgramBenefitResponse {
        val edition = findEdition(editionId)

        adminProgramEditionValidator.validateCanModify(edition)
        adminProgramBenefitValidator.validateCreate(request)

        val benefit = request.toEntity(
            programEdition = edition,
        )

        return programBenefitRepository
            .save(benefit)
            .toResponse()
    }

    @Transactional
    fun update(
        editionId: UUID,
        benefitId: UUID,
        request: UpdateProgramBenefitRequest,
    ): ProgramBenefitResponse {
        val benefit = findBenefit(
            editionId = editionId,
            benefitId = benefitId,
        )

        adminProgramEditionValidator.validateCanModify(benefit.programEdition)
        adminProgramBenefitValidator.validateUpdate(request)

        benefit.updateFrom(request)

        return programBenefitRepository
            .save(benefit)
            .toResponse()
    }

    @Transactional
    fun delete(
        editionId: UUID,
        benefitId: UUID,
    ) {
        val benefit = findBenefit(
            editionId = editionId,
            benefitId = benefitId,
        )

        adminProgramEditionValidator.validateCanModify(benefit.programEdition)
        programBenefitRepository.delete(benefit)
    }

    private fun findEdition(id: UUID): ProgramEdition =
        programEditionRepository
            .findById(id)
            .orElseThrow {
                ProgramEditionErrors.notFound(id)
            }

    private fun findBenefit(
        editionId: UUID,
        benefitId: UUID,
    ): ProgramBenefit =
        programBenefitRepository
            .findByIdAndProgramEditionId(
                id = benefitId,
                programEditionId = editionId,
            )
            ?: throw ProgramBenefitErrors.notFound(
                id = benefitId,
                programEditionId = editionId,
            )
}
