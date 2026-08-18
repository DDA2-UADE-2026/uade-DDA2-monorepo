package com.uade.dda2.server.feature.notification.controller

import com.uade.dda2.server.feature.notification.service.SseHub
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
class NotificationController(
    private val sseHub: SseHub,
) {
    @GetMapping("/events/subscribe", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(): SseEmitter = sseHub.subscribe()
}
