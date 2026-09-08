package com.uade.dda2.server.feature.program.mapper

import com.uade.dda2.server.feature.program.dto.admin.response.ProgramImageResponse
import com.uade.dda2.server.feature.program.entity.ProgramImage
import java.util.UUID

fun programImageUrl(imageId: UUID?): String? =
    imageId?.let { "/api/images/$it" }

fun ProgramImage.toResponse(): ProgramImageResponse =
    ProgramImageResponse(
        id = requireNotNull(id),
        programId = requireNotNull(program.id),
        originalName = originalName,
        contentType = contentType,
        sizeBytes = sizeBytes,
        url = requireNotNull(programImageUrl(id)),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
