package com.uade.dda2.server.feature.intervention.mapper

import com.uade.dda2.server.feature.intervention.dto.response.InterventionPublicationResponse
import com.uade.dda2.server.feature.intervention.entity.InterventionFollowUpPublication

fun InterventionFollowUpPublication.toPublicationResponse() = InterventionPublicationResponse(
    requireNotNull(id), summary, visible, publishedAt, updatedAt, requireNotNull(updatedBy.id), updatedBy.name,
)

fun InterventionFollowUpPublication.toPublicationAuditSnapshot(): Map<String, Any?> = mapOf(
    "publicationId" to id, "followUpId" to followUp.id, "visible" to visible,
    "publishedAt" to publishedAt, "updatedAt" to updatedAt, "updatedByUserId" to updatedBy.id,
)
