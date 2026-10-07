package com.uade.dda2.server.feature.appointment.mapper

import com.uade.dda2.server.feature.appointment.dto.response.CommunityAttentionResponse
import com.uade.dda2.server.feature.appointment.entity.CommunityAttention

fun CommunityAttention.toResponse(): CommunityAttentionResponse = CommunityAttentionResponse(
    result = result,
    attendedOn = attendedOn,
    description = description,
    version = version,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
