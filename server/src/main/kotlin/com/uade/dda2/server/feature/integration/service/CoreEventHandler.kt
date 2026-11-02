package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.feature.integration.dto.CoreEnvelope

interface CoreEventHandler {
    val eventType: String
    val eventVersion: String
    fun handle(envelope: CoreEnvelope)
}
