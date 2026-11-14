package com.uade.dda2.server.feature.integration.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

enum class CoreConnectionState { DISABLED, NOT_CONFIGURED, UP, DEGRADED, DOWN }

data class CoreHttpCheck(val state: CoreConnectionState, val upstreamStatus: Int? = null,
    val code: String? = null, val traceId: String? = null)

@Schema(description = "Readiness pública de Core según el cuerpo de GET /health/ready. Core puede responder HTTP 200 con " +
    "status degraded o down; state lo refleja. checks resume cada dependencia remota (database, broker). " +
    "No acredita que el worker de Core esté consumiendo.")
data class CoreReadinessCheck(val state: CoreConnectionState, val status: String? = null,
    val checks: Map<String, String> = emptyMap(), val upstreamStatus: Int? = null,
    val code: String? = null, val traceId: String? = null)

data class CoreAmqpCheck(val state: CoreConnectionState, val queue: String,
    val pendingMessages: Int? = null, val activeConsumers: Int? = null, val code: String? = null)

@Schema(description = "Comprobación activa de la readiness de Core, HTTP autenticado y AMQPS. No publica ni consume eventos; no es un monitor continuo.")
data class CoreConnectionStatusResponse(val checkedAt: Instant, val module: String, val enabled: Boolean,
    val machineSecretConfigured: Boolean, val publisherEnabled: Boolean, val consumerEnabled: Boolean,
    val readiness: CoreReadinessCheck, val http: CoreHttpCheck, val amqp: CoreAmqpCheck)

@Schema(description = "Contadores locales acumulados, independientes de la ventana de estadísticas de Core. BROKER_CONFIRMED no acredita procesamiento remoto.")
data class CoreLocalMetrics(val pending: Long, val inFlight: Long, val brokerConfirmed: Long,
    val inboxProcessed: Long, val oldestUnconfirmedAt: Instant?)

data class CoreDashboardResponse(val checkedAt: Instant, val local: CoreLocalMetrics, val core: CoreModuleDashboard)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreModuleDashboard(val module: String, val windowHours: Int, val generatedAt: OffsetDateTime,
    val published: CorePublishedMetrics, val received: CoreReceivedMetrics, val deadLetters: CoreDeadLetterMetrics,
    val subscriptions: CoreSubscriptionMetrics, val publications: CorePublicationMetrics,
    val volumeByHour: List<CoreHourlyVolume>, val processing: CoreProcessingMetrics)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CorePublishedMetrics(val total: Long, val inWindow: Long, val topTypes: List<CoreTypeVolume>)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreTypeVolume(val eventType: String, val total: Long)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreReceivedMetrics(val byStatus: Map<String, Long>, val total: Long,
    val delivered: Long, val pendingRetry: Long, val dead: Long)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreDeadLetterMetrics(val open: Long, val byReason: Map<String, Long>)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreSubscriptionMetrics(val total: Long, val active: Long, val eventTypes: List<String>)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CorePublicationMetrics(val declared: List<String>)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreHourlyVolume(val hour: OffsetDateTime, val total: Long, val rejected: Long)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreProcessingMetrics(val avgMs: Double, val maxMs: Double)

enum class CoreEventStatus { ROUTED, NO_SUBSCRIBERS, REJECTED, DUPLICATE }

@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreEventSummary(val id: UUID, val eventId: UUID, val eventType: String, val eventVersion: String,
    val sourceModule: String, val occurredAt: OffsetDateTime, val receivedAt: OffsetDateTime,
    val correlationId: UUID?, val status: String, val rejectionCode: String?, val ingestionChannel: String,
    val processingMs: Long?, val deliveryCount: Long, val deliveredCount: Long)

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Página de Core: page comienza en 1. El listado no incluye el payload del evento.")
data class CoreEventPage(val items: List<CoreEventSummary>, val total: Long,
    val page: Int, val size: Int, val pages: Int)
