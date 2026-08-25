package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.program.dto.request.CreateProgramEditionRequest
import com.uade.dda2.server.feature.program.dto.request.UpdateProgramEditionRequest
import com.uade.dda2.server.feature.program.dto.response.ProgramEditionListResponse
import com.uade.dda2.server.feature.program.dto.response.ProgramEditionResponse
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import com.uade.dda2.server.feature.program.error.ProgramEditionErrors
import com.uade.dda2.server.feature.program.error.ProgramErrors
import com.uade.dda2.server.feature.program.mapper.toEntity
import com.uade.dda2.server.feature.program.mapper.toListResponse
import com.uade.dda2.server.feature.program.mapper.toResponse
import com.uade.dda2.server.feature.program.mapper.updateFrom
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import com.uade.dda2.server.feature.program.validator.ProgramEditionValidator
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ProgramEditionService(
    private val programRepository: ProgramRepository,
    private val programEditionRepository: ProgramEditionRepository,
    private val programEditionValidator: ProgramEditionValidator,
    private val currentUserService: CurrentUserService,
) {

    @Transactional(readOnly = true)
    fun list(
        programId: UUID,
        page: Int,
        size: Int,
    ): ProgramEditionListResponse {
        findProgram(programId)

        val pageable = PageRequest.of(page, size)

        return programEditionRepository
            .findAllByProgramIdOrderByStartDateDesc(
                programId = programId,
                pageable = pageable,
            )
            .toListResponse()
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): ProgramEditionResponse =
        findEdition(id).toResponse()

    @Transactional
    fun create(
        programId: UUID,
        request: CreateProgramEditionRequest,
    ): ProgramEditionResponse {
        val program = findProgram(programId)

        programEditionValidator.validateCreate(
            program = program,
            request = request,
        )

        val edition = request.toEntity(
            program = program,
            createdBy = currentUserService.userReference(),
        )

        return try {
            programEditionRepository
                .saveAndFlush(edition)
                .toResponse()
        } catch (_: DataIntegrityViolationException) {
            throw ProgramEditionErrors.nameAlreadyExists(
                programId = programId,
                name = edition.name,
            )
        }
    }

    @Transactional
    fun update(
        id: UUID,
        request: UpdateProgramEditionRequest,
    ): ProgramEditionResponse {
        val edition = findEdition(id)

        programEditionValidator.validateUpdate(
            edition = edition,
            request = request,
        )

        edition.updateFrom(request)

        return try {
            programEditionRepository
                .saveAndFlush(edition)
                .toResponse()
        } catch (_: DataIntegrityViolationException) {
            throw ProgramEditionErrors.nameAlreadyExists(
                programId = requireNotNull(edition.program.id),
                name = edition.name,
            )
        }
    }

    @Transactional
    fun activate(id: UUID): ProgramEditionResponse =
        changeStatus(
            id = id,
            status = ProgramEditionStatus.ACTIVE,
        )

    @Transactional
    fun suspend(id: UUID): ProgramEditionResponse =
        changeStatus(
            id = id,
            status = ProgramEditionStatus.SUSPENDED,
        )

    @Transactional
    fun close(id: UUID): ProgramEditionResponse =
        changeStatus(
            id = id,
            status = ProgramEditionStatus.CLOSED,
        )

    @Transactional
    fun delete(id: UUID) {
        val edition = findEdition(id)

        programEditionValidator.validateDelete(edition)

        programEditionRepository.delete(edition)
    }

    private fun changeStatus(
        id: UUID,
        status: ProgramEditionStatus,
    ): ProgramEditionResponse {
        val edition = findEdition(id)

        programEditionValidator.validateStatusTransition(
            edition = edition,
            newStatus = status,
        )

        edition.status = status

        return programEditionRepository
            .save(edition)
            .toResponse()
    }

    private fun findProgram(id: UUID): Program =
        programRepository
            .findById(id)
            .orElseThrow {
                ProgramErrors.notFound(id)
            }

    private fun findEdition(id: UUID): ProgramEdition =
        programEditionRepository
            .findById(id)
            .orElseThrow {
                ProgramEditionErrors.notFound(id)
            }
}
