package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramListResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramOptionResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramResponse
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.error.ProgramErrors
import com.uade.dda2.server.feature.program.mapper.toEntity
import com.uade.dda2.server.feature.program.mapper.toListResponse
import com.uade.dda2.server.feature.program.mapper.toOptionResponse
import com.uade.dda2.server.feature.program.mapper.toResponse
import com.uade.dda2.server.feature.program.mapper.updateFrom
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import com.uade.dda2.server.feature.program.validator.AdminProgramValidator
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class AdminProgramService(
    private val programRepository: ProgramRepository,
    private val adminProgramValidator: AdminProgramValidator,
    private val currentUserService: CurrentUserService,
) {

    @Transactional(readOnly = true)
    fun list(
        page: Int,
        size: Int,
    ): ProgramListResponse {
        val pageable = PageRequest.of(page, size)

        return programRepository
            .findAllByOrderByNameAsc(pageable)
            .toListResponse()
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): ProgramResponse =
        findProgram(id).toResponse()

    @Transactional(readOnly = true)
    fun options(): List<ProgramOptionResponse> =
        programRepository
            .findAllByOrderByNameAsc()
            .map { it.toOptionResponse() }

    @Transactional
    fun create(request: CreateProgramRequest): ProgramResponse {
        adminProgramValidator.validateCreate(request)

        val program = request.toEntity(
            createdBy = currentUserService.userReference(),
        )

        return try {
            programRepository
                .saveAndFlush(program)
                .toResponse()
        } catch (_: DataIntegrityViolationException) {
            throw ProgramErrors.nameAlreadyExists(program.name)
        }
    }

    @Transactional
    fun update(
        id: UUID,
        request: UpdateProgramRequest,
    ): ProgramResponse {
        val program = findProgram(id)

        adminProgramValidator.validateUpdate(
            program = program,
            request = request,
        )

        program.updateFrom(request)

        return try {
            programRepository
                .saveAndFlush(program)
                .toResponse()
        } catch (_: DataIntegrityViolationException) {
            throw ProgramErrors.nameAlreadyExists(program.name)
        }
    }

    @Transactional
    fun delete(id: UUID) {
        val program = findProgram(id)

        adminProgramValidator.validateDelete(program)

        programRepository.delete(program)
    }

    private fun findProgram(id: UUID): Program =
        programRepository
            .findById(id)
            .orElseThrow {
                ProgramErrors.notFound(id)
            }
}
