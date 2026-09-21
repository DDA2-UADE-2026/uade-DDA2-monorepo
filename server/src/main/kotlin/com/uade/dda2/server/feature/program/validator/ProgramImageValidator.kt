package com.uade.dda2.server.feature.program.validator

import com.uade.dda2.server.feature.program.error.ProgramImageErrors
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

data class ValidatedProgramImage(
    val originalName: String,
    val contentType: String,
    val content: ByteArray,
)

@Component
class ProgramImageValidator {
    fun validate(file: MultipartFile): ValidatedProgramImage {
        if (file.isEmpty || file.size == 0L) throw ProgramImageErrors.emptyFile()
        if (file.size > MAX_FILE_SIZE) throw ProgramImageErrors.fileTooLarge()

        val suppliedName = file.originalFilename?.trim()
        if (suppliedName.isNullOrBlank() || suppliedName.any(Char::isISOControl)) {
            throw ProgramImageErrors.invalidFileName()
        }

        val originalName = suppliedName.substringAfterLast('/').substringAfterLast('\\').trim()
        if (originalName.isBlank() || originalName.length > 255 || originalName == "." || originalName == "..") {
            throw ProgramImageErrors.invalidFileName()
        }

        val extension = originalName.substringAfterLast('.', "").lowercase()
        val declaredType = file.contentType?.lowercase()?.trim()
        val expectedType = when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> throw ProgramImageErrors.invalidFileType()
        }
        if (declaredType != expectedType) throw ProgramImageErrors.invalidFileType()

        val content = file.bytes
        if (content.isEmpty()) throw ProgramImageErrors.emptyFile()
        if (content.size.toLong() > MAX_FILE_SIZE) throw ProgramImageErrors.fileTooLarge()

        val validSignature = when (expectedType) {
            "image/jpeg" -> content.startsWith(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
            "image/png" -> content.startsWith(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            else -> false
        }
        if (!validSignature) throw ProgramImageErrors.invalidFileType()

        return ValidatedProgramImage(
            originalName = originalName,
            contentType = expectedType,
            content = content,
        )
    }

    private fun ByteArray.startsWith(signature: ByteArray): Boolean =
        size >= signature.size && signature.indices.all { this[it] == signature[it] }

    companion object {
        const val MAX_FILE_SIZE: Long = 10L * 1024L * 1024L
    }
}
