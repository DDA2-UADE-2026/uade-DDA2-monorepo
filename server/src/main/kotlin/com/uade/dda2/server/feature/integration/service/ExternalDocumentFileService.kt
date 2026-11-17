package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.feature.integration.client.M1DocumentClient
import com.uade.dda2.server.feature.integration.client.M1DocumentFile
import com.uade.dda2.server.feature.integration.error.M1DocumentException
import com.uade.dda2.server.feature.integration.error.M1DocumentFailure
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ExternalDocumentFileService(
    private val documents: ExternalDocumentationService,
    private val clients: ObjectProvider<M1DocumentClient>,
) {
    // La autorización y lectura de la referencia terminan antes de esperar a M1.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun download(applicationId: UUID, requestId: UUID): M1DocumentFile {
        val documentId = documents.fileDocumentId(applicationId, requestId)
        val client = clients.getIfAvailable() ?: throw M1DocumentException(M1DocumentFailure.DISABLED)
        return client.download(documentId)
    }
}
