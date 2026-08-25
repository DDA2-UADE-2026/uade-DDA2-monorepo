package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.program.dto.request.CreateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.request.UpdateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.response.ProgramRequirementResponse
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.ProgramRequirement
import com.uade.dda2.server.feature.program.error.ProgramEditionErrors
import com.uade.dda2.server.feature.program.error.ProgramRequirementErrors
import com.uade.dda2.server.feature.program.mapper.toEntity
import com.uade.dda2.server.feature.program.mapper.toResponse
import com.uade.dda2.server.feature.program.mapper.updateFrom
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramRequirementRepository
import com.uade.dda2.server.feature.program.validator.ProgramRequirementValidator
import com.uade.dda2.server.feature.program.validator.ProgramEditionValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProgramRequirementService(
    private val programEditionRepository: ProgramEditionRepository,
    private val programRequirementRepository: ProgramRequirementRepository,
    private val programRequirementValidator: ProgramRequirementValidator,
    private val programEditionValidator: ProgramEditionValidator,
) {

    @Transactional(readOnly = true)
    fun list(
        editionId: UUID,
    ): List<ProgramRequirementResponse> {
        findEdition(editionId)

        return programRequirementRepository
            .findAllByProgramEditionId(editionId)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun get(
        editionId: UUID,
        requirementId: UUID,
    ): ProgramRequirementResponse =
        findRequirement(
            editionId = editionId,
            requirementId = requirementId,
        ).toResponse()

    @Transactional
    fun create(
        editionId: UUID,
        request: CreateProgramRequirementRequest,
    ): ProgramRequirementResponse {
        val edition = findEdition(editionId)

        programEditionValidator.validateCanModify(edition)
        programRequirementValidator.validateCreate(request)

        val requirement = request.toEntity(
            programEdition = edition,
        )

        return programRequirementRepository
            .save(requirement)
            .toResponse()
    }

    @Transactional
    fun update(
        editionId: UUID,
        requirementId: UUID,
        request: UpdateProgramRequirementRequest,
    ): ProgramRequirementResponse {
        val requirement = findRequirement(
            editionId = editionId,
            requirementId = requirementId,
        )

        programEditionValidator.validateCanModify(requirement.programEdition)
        programRequirementValidator.validateUpdate(request)

        requirement.updateFrom(request)

        return programRequirementRepository
            .save(requirement)
            .toResponse()
    }

    @Transactional
    fun delete(
        editionId: UUID,
        requirementId: UUID,
    ) {
        val requirement = findRequirement(
            editionId = editionId,
            requirementId = requirementId,
        )

        programEditionValidator.validateCanModify(requirement.programEdition)
        programRequirementRepository.delete(requirement)
    }

    private fun findEdition(id: UUID): ProgramEdition =
        programEditionRepository
            .findById(id)
            .orElseThrow {
                ProgramEditionErrors.notFound(id)
            }

    private fun findRequirement(
        editionId: UUID,
        requirementId: UUID,
    ): ProgramRequirement =
        programRequirementRepository
            .findByIdAndProgramEditionId(
                id = requirementId,
                programEditionId = editionId,
            )
            ?: throw ProgramRequirementErrors.notFound(
                id = requirementId,
                programEditionId = editionId,
            )
}
