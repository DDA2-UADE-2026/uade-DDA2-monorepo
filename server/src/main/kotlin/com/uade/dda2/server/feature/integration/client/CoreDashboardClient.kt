package com.uade.dda2.server.feature.integration.client

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.CoreModuleDashboard
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import org.springframework.http.HttpMethod
import tools.jackson.databind.json.JsonMapper

class CoreDashboardClient(private val api: CoreApiClient, private val json: JsonMapper,
    private val properties: CoreProperties) {
    fun dashboard(windowHours: Int): CoreModuleDashboard {
        require(windowHours in 1..720)
        val node = api.request(HttpMethod.GET, "/dashboard?windowHours=$windowHours")
        val result = try { json.treeToValue(node, CoreModuleDashboard::class.java) }
        catch (_: Exception) { throw CoreIntegrationException("CORE_INVALID_DASHBOARD_RESPONSE") }
        if (result.module != properties.module || result.windowHours != windowHours)
            throw CoreIntegrationException("CORE_DASHBOARD_IDENTITY_MISMATCH")
        return result
    }
}
