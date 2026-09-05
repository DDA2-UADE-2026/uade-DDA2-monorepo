package com.uade.dda2.server.feature.document.repository

import com.uade.dda2.server.feature.document.entity.Document
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DocumentRepository : JpaRepository<Document, UUID> {
    fun existsByUploadedById(userId: Long): Boolean
}
