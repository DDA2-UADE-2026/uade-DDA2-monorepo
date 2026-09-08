package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.program.dto.ProgramImageContent
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramImageResponse
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramImage
import com.uade.dda2.server.feature.program.error.ProgramErrors
import com.uade.dda2.server.feature.program.error.ProgramImageErrors
import com.uade.dda2.server.feature.program.mapper.toResponse
import com.uade.dda2.server.feature.program.repository.ProgramImageRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import com.uade.dda2.server.feature.program.validator.ProgramImageValidator
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.UUID

@Service
class ProgramImageService(
    private val programRepository: ProgramRepository,
    private val programImageRepository: ProgramImageRepository,
    private val programImageValidator: ProgramImageValidator,
) {
    @Transactional
    fun create(programId: UUID, file: MultipartFile): ProgramImageResponse {
        val program = findProgram(programId)
        if (programImageRepository.existsByProgramId(programId)) {
            throw ProgramImageErrors.alreadyExists(programId)
        }

        val validated = programImageValidator.validate(file)
        val now = LocalDateTime.now()
        val image = ProgramImage(
            program = program,
            originalName = validated.originalName,
            contentType = validated.contentType,
            sizeBytes = validated.content.size.toLong(),
            content = validated.content,
            createdAt = now,
            updatedAt = now,
        )
        program.updatedAt = now

        return try {
            programImageRepository.saveAndFlush(image).toResponse()
        } catch (_: DataIntegrityViolationException) {
            throw ProgramImageErrors.alreadyExists(programId)
        }
    }

    @Transactional
    fun update(programId: UUID, file: MultipartFile): ProgramImageResponse {
        val program = findProgram(programId)
        val image = programImageRepository.findByProgramId(programId)
            ?: throw ProgramImageErrors.notFound(programId)
        val validated = programImageValidator.validate(file)
        val now = LocalDateTime.now()

        image.originalName = validated.originalName
        image.contentType = validated.contentType
        image.sizeBytes = validated.content.size.toLong()
        image.content = validated.content
        image.updatedAt = now
        program.updatedAt = now

        return programImageRepository.saveAndFlush(image).toResponse()
    }

    @Transactional
    fun delete(programId: UUID) {
        val program = findProgram(programId)
        if (!programImageRepository.existsByProgramId(programId)) {
            throw ProgramImageErrors.notFound(programId)
        }
        val now = LocalDateTime.now()

        programImageRepository.deleteByProgramId(programId)
        program.updatedAt = now
    }

    @Transactional(readOnly = true)
    fun content(imageId: UUID): ProgramImageContent {
        val image = programImageRepository.findById(imageId)
            .orElseThrow { ProgramImageErrors.notFound() }

        return ProgramImageContent(
            originalName = image.originalName,
            contentType = image.contentType,
            sizeBytes = image.sizeBytes,
            content = image.content,
        )
    }

    private fun findProgram(id: UUID): Program =
        programRepository.findById(id).orElseThrow { ProgramErrors.notFound(id) }
}
