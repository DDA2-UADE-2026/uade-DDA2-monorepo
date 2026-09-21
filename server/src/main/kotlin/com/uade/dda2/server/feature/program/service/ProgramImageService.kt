package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.dto.ProgramImageContent
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramImageResponse
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramImage
import com.uade.dda2.server.feature.program.error.ProgramErrors
import com.uade.dda2.server.feature.program.error.ProgramImageErrors
import com.uade.dda2.server.feature.program.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.program.mapper.toResponse
import com.uade.dda2.server.feature.program.repository.ProgramImageRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import com.uade.dda2.server.feature.program.validator.ProgramImageValidator
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.util.UUID

@Service
class ProgramImageService(
    private val programRepository: ProgramRepository,
    private val programImageRepository: ProgramImageRepository,
    private val programImageValidator: ProgramImageValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
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

        val saved = try {
            programImageRepository.saveAndFlush(image)
        } catch (_: DataIntegrityViolationException) {
            throw ProgramImageErrors.alreadyExists(programId)
        }

        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.CREATE,
            entityType = LogEntityType.PROGRAM_IMAGE,
            entityId = requireNotNull(saved.id).toString(),
            newValues = json(saved.toAuditSnapshot()),
        )

        return saved.toResponse()
    }

    @Transactional
    fun update(programId: UUID, file: MultipartFile): ProgramImageResponse {
        val program = findProgram(programId)
        val image = programImageRepository.findByProgramId(programId)
            ?: throw ProgramImageErrors.notFound(programId)
        val validated = programImageValidator.validate(file)
        val now = LocalDateTime.now()

        val oldValues = json(image.toAuditSnapshot())
        image.originalName = validated.originalName
        image.contentType = validated.contentType
        image.sizeBytes = validated.content.size.toLong()
        image.content = validated.content
        image.updatedAt = now
        program.updatedAt = now

        val saved = programImageRepository.saveAndFlush(image)
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.UPDATE,
            entityType = LogEntityType.PROGRAM_IMAGE,
            entityId = requireNotNull(saved.id).toString(),
            oldValues = oldValues,
            newValues = json(saved.toAuditSnapshot()),
        )

        return saved.toResponse()
    }

    @Transactional
    fun delete(programId: UUID) {
        val program = findProgram(programId)
        val image = programImageRepository.findByProgramId(programId)
            ?: throw ProgramImageErrors.notFound(programId)
        val now = LocalDateTime.now()

        val oldValues = json(image.toAuditSnapshot())

        programImageRepository.deleteByProgramId(programId)
        program.updatedAt = now

        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.DELETE,
            entityType = LogEntityType.PROGRAM_IMAGE,
            entityId = requireNotNull(image.id).toString(),
            oldValues = oldValues,
        )
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

    private fun json(value: Any): String =
        requireNotNull(jsonMapper.writeValueAsString(value))
}
