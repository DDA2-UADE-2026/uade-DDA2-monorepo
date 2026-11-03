package com.uade.dda2.server.feature.integration.client

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.CoreModuleDashboard
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import org.springframework.http.HttpMethod
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode

class CoreDashboardClient(private val api: CoreApiClient, private val json: JsonMapper,
    private val properties: CoreProperties) {
    fun dashboard(windowHours: Int): CoreModuleDashboard {
        require(windowHours in 1..720)
        val node = api.request(HttpMethod.GET, "/dashboard?windowHours=$windowHours")
        val result = try {
            val deadLetters = node.get("deadLetters") as? ObjectNode
            val byReason = deadLetters?.get("byReason")
            if (byReason?.isArray == true) {
                // Core entrega una lista; la API de Desarrollo Social expone un mapa por motivo.
                val reasons = json.treeToValue(byReason, Array<DeadLetterReason>::class.java)
                deadLetters.set("byReason", json.valueToTree(reasons.associate { it.reasonCode to it.total }))
            }
            json.treeToValue(node, CoreModuleDashboard::class.java)
        } catch (_: Exception) {
            throw CoreIntegrationException("CORE_INVALID_DASHBOARD_RESPONSE")
        }
        if (result.module != properties.module || result.windowHours != windowHours)
            throw CoreIntegrationException("CORE_DASHBOARD_IDENTITY_MISMATCH")
        return result
    }

    private data class DeadLetterReason(val reasonCode: String, val total: Long)
}
