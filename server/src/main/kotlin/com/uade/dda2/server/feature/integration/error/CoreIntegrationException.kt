package com.uade.dda2.server.feature.integration.error

// Deliberadamente no incorpora cuerpos HTTP, sobres ni mensajes de excepciones con secretos.
open class CoreIntegrationException(val code: String) : RuntimeException(code)

class CoreHttpException(val status: Int, code: String, val traceId: String? = null) : CoreIntegrationException(code)
