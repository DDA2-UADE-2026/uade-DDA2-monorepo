package com.uade.dda2.server.config

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration
import java.util.UUID

@ConfigurationProperties("app.core")
class CoreProperties {
    var enabled = false
    var checkOnStartup = false
    var module = "desarrollo-social"
    var apiBaseUrl = "https://core-uade-2026.duckdns.org/api/v1"
    var machineSecret = ""
    var queue = "q.desarrollo-social"
    var connectTimeout: Duration = Duration.ofSeconds(5)
    var readTimeout: Duration = Duration.ofSeconds(10)
    var publisher = Publisher()
    var consumer = Consumer()
    var probe = Probe()

    class Publisher {
        var enabled = false
        var batchSize = 20
        var pollInterval: Duration = Duration.ofSeconds(5)
        var confirmTimeout: Duration = Duration.ofSeconds(10)
        var leaseDuration: Duration = Duration.ofSeconds(60)
        var retryDelay: Duration = Duration.ofSeconds(5)
        var maxRetryDelay: Duration = Duration.ofMinutes(5)
    }

    class Consumer {
        var enabled = false
        var prefetch = 1
    }

    class Probe {
        var enabled = false
        var eventType = "socialCoreConnectivityProbe"
        var runId: UUID? = null
    }

    @PostConstruct
    fun validate() {
        if (!enabled) return
        require(module == "desarrollo-social") { "CORE_MODULE debe identificar a desarrollo-social." }
        val uri = URI.create(apiBaseUrl)
        require(uri.userInfo == null && uri.query == null && uri.fragment == null &&
            (uri.scheme == "https" || uri.scheme == "http" && uri.host in LOOPBACK_HOSTS)) {
            "CORE_API_BASE_URL requiere HTTPS; HTTP se admite solo en loopback para pruebas aisladas."
        }
        require(uri.host != null && uri.path.trimEnd('/') == "/api/v1") { "CORE_API_BASE_URL debe terminar en /api/v1." }
        // AMQPS tiene credenciales independientes. El secret HTTP se exige al pedir un token.
        require(queue.isNotBlank()) { "Falta CORE_QUEUE." }
        listOf(connectTimeout, readTimeout, publisher.pollInterval, publisher.confirmTimeout,
            publisher.leaseDuration, publisher.retryDelay, publisher.maxRetryDelay).forEach {
            require(!it.isNegative && !it.isZero) { "Los timeouts e intervalos de Core deben ser positivos." }
        }
        require(connectTimeout.toMillis() in 1..Int.MAX_VALUE.toLong() && readTimeout.toMillis() in 1..Int.MAX_VALUE.toLong()) {
            "Los timeouts de conexión y lectura deben poder representarse en milisegundos positivos."
        }
        require(publisher.leaseDuration > publisher.confirmTimeout.plusSeconds(1)) { "La reserva de outbox debe superar el timeout del confirm." }
        require(publisher.batchSize in 1..100 && consumer.prefetch in 1..100) { "Lote y prefetch de Core deben estar entre 1 y 100." }
        require(publisher.maxRetryDelay >= publisher.retryDelay) { "El máximo backoff debe superar el inicial." }
        require(!consumer.enabled || probe.enabled) { "Todavía solo está implementado el consumidor técnico; habilitar CORE_PROBE_ENABLED." }
        if (probe.enabled) {
            require(probe.runId != null) { "La prueba técnica requiere CORE_PROBE_RUN_ID explícito." }
            require(probe.eventType.isNotBlank() && probe.eventType.length <= 120) { "Tipo de prueba Core inválido." }
        }
    }

    companion object {
        val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "::1", "[::1]")
    }
}
