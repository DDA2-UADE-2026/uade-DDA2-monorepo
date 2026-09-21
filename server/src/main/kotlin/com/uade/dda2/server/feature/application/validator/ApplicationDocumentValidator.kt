package com.uade.dda2.server.feature.application.validator

import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.error.ApplicationDocumentErrors
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

data class ValidatedApplicationFile(
    val originalName: String,
    val contentType: String,
    val content: ByteArray,
)

@Component
class ApplicationDocumentValidator {
    fun validateMutable(application: Application) {
        if (application.status in FINAL_STATUSES) throw ApplicationDocumentErrors.finalApplication()
    }

    fun validateFile(file: MultipartFile): ValidatedApplicationFile {
        if (file.isEmpty || file.size == 0L) throw ApplicationDocumentErrors.emptyFile()
        if (file.size > MAX_FILE_SIZE) throw ApplicationDocumentErrors.fileTooLarge()

        val suppliedName = file.originalFilename?.trim()
        if (suppliedName.isNullOrBlank() || suppliedName.any(Char::isISOControl)) {
            throw ApplicationDocumentErrors.invalidFileName()
        }
        val originalName = suppliedName.substringAfterLast('/').substringAfterLast('\\').trim()
        if (originalName.isBlank() || originalName.length > 255 || originalName == "." || originalName == "..") {
            throw ApplicationDocumentErrors.invalidFileName()
        }

        val extension = originalName.substringAfterLast('.', "").lowercase()
        val declaredType = file.contentType?.lowercase()?.trim()
        val expectedType = when (extension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> throw ApplicationDocumentErrors.invalidFileType()
        }
        if (declaredType != expectedType) throw ApplicationDocumentErrors.invalidFileType()

        val content = file.bytes
        if (content.isEmpty()) throw ApplicationDocumentErrors.emptyFile()
        if (content.size.toLong() > MAX_FILE_SIZE) throw ApplicationDocumentErrors.fileTooLarge()
        val validSignature = when (expectedType) {
            "application/pdf" -> content.startsWith(byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D))
            "image/jpeg" -> content.startsWith(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
            "image/png" -> content.startsWith(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            else -> false
        }
        if (!validSignature) throw ApplicationDocumentErrors.invalidFileType()

        return ValidatedApplicationFile(originalName, expectedType, content)
    }

    fun normalizedObservation(status: ApplicationDocumentStatus, observation: String?): String? {
        if (status !in setOf(ApplicationDocumentStatus.VALID, ApplicationDocumentStatus.OBSERVED)) {
            throw ApplicationDocumentErrors.invalidReviewStatus()
        }
        val normalized = observation?.trim()?.takeIf(String::isNotEmpty)
        if (status == ApplicationDocumentStatus.OBSERVED && normalized == null) {
            throw ApplicationDocumentErrors.observationRequired()
        }
        if (status == ApplicationDocumentStatus.VALID && normalized != null) {
            throw ApplicationDocumentErrors.observationNotAllowed()
        }
        return normalized
    }

    private fun ByteArray.startsWith(signature: ByteArray): Boolean =
        size >= signature.size && signature.indices.all { this[it] == signature[it] }

    companion object {
        const val MAX_FILE_SIZE: Long = 10L * 1024L * 1024L
        private val FINAL_STATUSES = setOf(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED, ApplicationStatus.CLOSED)
    }
}
