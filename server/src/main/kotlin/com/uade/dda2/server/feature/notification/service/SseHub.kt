package com.uade.dda2.server.feature.notification.service

import com.uade.dda2.server.feature.notification.dto.SseEventPayload
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CopyOnWriteArrayList

private val EMITTER_TIMEOUT_MILLIS = java.time.Duration.ofMinutes(15).toMillis()

/**
 * Registro en memoria de conexiones SSE activas. Un único proceso, sin estado
 * compartido entre instancias: alcanza para esta app (una instancia detrás de
 * un único backend). Si en algún momento se escala horizontalmente, esto tendría
 * que reemplazarse por un fan-out vía el propio broker de eventos.
 */
@Service
class SseHub {
    private val logger = LoggerFactory.getLogger(SseHub::class.java)
    private val emitters = CopyOnWriteArrayList<SseEmitter>()

    fun subscribe(): SseEmitter {
        val emitter = SseEmitter(EMITTER_TIMEOUT_MILLIS)
        emitters.add(emitter)

        emitter.onCompletion { emitters.remove(emitter) }
        emitter.onTimeout { emitters.remove(emitter) }
        emitter.onError { emitters.remove(emitter) }

        return emitter
    }

    // Se manda siempre como evento "message" (sin .name()): el cliente
    // distingue el tipo leyendo payload.eventType, así se pueden sumar
    // nuevos tipos de evento (ej. los que lleguen de Kafka) sin tocar
    // el código de conexión del front.
    fun broadcast(payload: SseEventPayload) {
        emitters.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .id(payload.eventId)
                        .data(payload),
                )
            } catch (ex: Exception) {
                logger.debug("Descartando SSE emitter roto: {}", ex.message)
                emitters.remove(emitter)
            }
        }
    }

    @Scheduled(fixedRate = 15_000)
    fun heartbeat() {
        emitters.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"))
            } catch (ex: Exception) {
                logger.debug("Descartando SSE emitter roto en heartbeat: {}", ex.message)
                emitters.remove(emitter)
            }
        }
    }
}
