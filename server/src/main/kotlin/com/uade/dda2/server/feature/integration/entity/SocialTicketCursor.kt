package com.uade.dda2.server.feature.integration.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Solo metadatos para descartar broadcasts viejos; nunca guarda el contenido de tickets ajenos. */
@Entity
@Table(name = "social_ticket_cursor")
class SocialTicketCursor(
    @Id var id: UUID,
    @Column(name = "source_updated_at", nullable = false) var sourceUpdatedAt: Instant,
    @Column(name = "last_event_id", nullable = false) var lastEventId: UUID,
)
