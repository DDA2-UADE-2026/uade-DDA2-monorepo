package com.uade.dda2.server.feature.integration.dto

import java.util.UUID

data class CoreOutboxLease(val eventId: UUID, val token: UUID, val payloadJson: String, val traceId: String)
data class CoreQueueState(val messageCount: Int, val consumerCount: Int)
data class CoreMessageMetadata(val messageId: String? = null, val eventIdHeader: String? = null,
    val targetModule: String? = null, val attempt: Int? = null, val traceId: String? = null)
enum class CoreInboxResult { PROCESSED, DUPLICATE }
