package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.error.ApiException
import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.NotFoundException
import com.uade.dda2.server.feature.integration.client.CoreBrokerClient
import com.uade.dda2.server.feature.integration.client.CoreDashboardClient
import com.uade.dda2.server.feature.integration.client.CoreEventClient
import com.uade.dda2.server.feature.integration.client.CoreRegistryClient
import com.uade.dda2.server.feature.integration.dto.*
import com.uade.dda2.server.feature.integration.error.CoreHttpException
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.UUID

@Service
class CoreDashboardService(private val properties: CoreProperties, private val access: CoreDashboardAccess,
    private val local: CoreLocalMetricsService, private val dashboards: ObjectProvider<CoreDashboardClient>,
    private val registries: ObjectProvider<CoreRegistryClient>, private val events: ObjectProvider<CoreEventClient>,
    private val brokers: ObjectProvider<CoreBrokerClient>, private val clock: Clock) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun status(): CoreConnectionStatusResponse {
        access.check()
        val checkedAt = clock.instant()
        val http = when {
            !properties.enabled -> CoreHttpCheck(CoreConnectionState.DISABLED)
            properties.machineSecret.isBlank() -> CoreHttpCheck(CoreConnectionState.NOT_CONFIGURED, code = "CORE_HTTP_SECRET_MISSING")
            else -> try {
                client(dashboards).dashboard(24)
                logger.info("CORE_HTTP_CONNECTED module={} endpoint=/dashboard", properties.module)
                CoreHttpCheck(CoreConnectionState.UP, upstreamStatus = 200)
            } catch (exception: Exception) {
                val code = (exception as? CoreIntegrationException)?.code ?: "CORE_HTTP_CHECK_FAILED"
                val failure = exception as? CoreHttpException
                logger.warn("CORE_HTTP_CONNECTION_FAILED module={} upstreamStatus={} code={}", properties.module, failure?.status, code)
                CoreHttpCheck(CoreConnectionState.DOWN, failure?.status, code,
                    failure?.traceId?.takeIf { it.matches(Regex("[A-Za-z0-9_.:-]{1,100}")) })
            }
        }
        val amqp = if (!properties.enabled) CoreAmqpCheck(CoreConnectionState.DISABLED, properties.queue)
        else try {
            val queue = client(brokers).inspectQueue()
            CoreAmqpCheck(CoreConnectionState.UP, properties.queue, queue.messageCount, queue.consumerCount)
        } catch (exception: Exception) {
            CoreAmqpCheck(CoreConnectionState.DOWN, properties.queue,
                code = (exception as? CoreIntegrationException)?.code ?: "CORE_AMQP_CHECK_FAILED")
        }
        return CoreConnectionStatusResponse(checkedAt, properties.module, properties.enabled,
            properties.machineSecret.isNotBlank(), properties.enabled && properties.publisher.enabled,
            properties.enabled && properties.consumer.enabled, http, amqp)
    }

    fun dashboard(windowHours: Int): CoreDashboardResponse {
        access.check()
        if (windowHours !in 1..720) throw BadRequestException("CORE_INVALID_WINDOW", "windowHours debe estar entre 1 y 720.")
        val remote = remote { client(dashboards).dashboard(windowHours) }
        return CoreDashboardResponse(clock.instant(), local.metrics(), remote)
    }

    fun metrics(): CoreLocalMetrics {
        access.check()
        return local.metrics()
    }

    fun publications(): List<CorePublication> {
        access.check()
        return remote { client(registries).publications() }
    }

    fun subscriptions(): List<CoreSubscription> {
        access.check()
        return remote { client(registries).subscriptions() }
    }

    fun events(page: Int, size: Int, eventType: String?, status: CoreEventStatus?, correlationId: UUID?): CoreEventPage {
        access.check()
        if (page < 1 || size !in 1..200 || eventType != null && (eventType.isBlank() || eventType.length > 120))
            throw BadRequestException("CORE_INVALID_EVENT_FILTER", "page debe ser positivo, size entre 1 y 200 y eventType debe tener de 1 a 120 caracteres.")
        return remote { client(events).events(page, size, eventType, status, correlationId) }
    }

    fun event(eventId: UUID): CoreEventDetail {
        access.check()
        return remote { client(events).event(eventId) }
            ?: throw NotFoundException("CORE_EVENT_NOT_FOUND", "Core no encontró un evento visible con ese eventId.")
    }

    private fun <T : Any> client(provider: ObjectProvider<T>): T {
        if (!properties.enabled) throw CoreIntegrationException("CORE_DISABLED")
        return provider.ifAvailable ?: throw CoreIntegrationException("CORE_CLIENT_UNAVAILABLE")
    }

    private fun <T> remote(action: () -> T): T = try { action() }
    catch (exception: CoreHttpException) {
        // El 401/403 de Core pertenece a la credencial de máquina, no a la sesión del usuario local.
        throw ApiException(HttpStatus.BAD_GATEWAY, exception.code, "La API de Core respondió con HTTP ${exception.status} (${exception.code}).")
    } catch (exception: CoreIntegrationException) {
        val status = if (exception.code.startsWith("CORE_INVALID_") || exception.code == "CORE_DASHBOARD_IDENTITY_MISMATCH")
            HttpStatus.BAD_GATEWAY else HttpStatus.SERVICE_UNAVAILABLE
        throw ApiException(status, exception.code, "La consulta de Core no está disponible (${exception.code}).")
    } catch (_: Exception) {
        throw ApiException(HttpStatus.BAD_GATEWAY, "CORE_INVALID_RESPONSE", "No se pudo interpretar la respuesta de Core.")
    }
}
