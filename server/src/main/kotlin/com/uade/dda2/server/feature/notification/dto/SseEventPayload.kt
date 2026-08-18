package com.uade.dda2.server.feature.notification.dto

import java.time.Instant
import java.util.UUID

/**
 * Formato común de evento (TPO, Sección 8): tipo, id, fecha, módulo de origen y datos.
 * Se reutiliza para eventos internos (ej. recurso actualizado) y, más adelante,
 * para reenviar por el mismo canal los eventos asincrónicos consumidos de Kafka.
 */
data class SseEventPayload(
    val eventType: String,
    val eventId: String = UUID.randomUUID().toString(),
    val occurredAt: Instant = Instant.now(),
    val sourceModule: String,
    val data: Map<String, Any?>,
)
