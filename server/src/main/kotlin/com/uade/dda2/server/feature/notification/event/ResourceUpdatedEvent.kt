package com.uade.dda2.server.feature.notification.event

import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType

/**
 * Publicado en proceso (Spring ApplicationEventPublisher) cada vez que LogService
 * registra una mutación. Lo consume ResourceUpdatedSseListener para avisar por SSE
 * a los clientes conectados que un recurso cambió, sin acoplar los servicios de
 * negocio al transporte SSE.
 */
data class ResourceUpdatedEvent(
    val entityType: LogEntityType,
    val entityId: String,
    val action: LogAction,
)
