package com.uade.dda2.server

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.JsonNode
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class OpenApiDocsGenerationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var json: JsonMapper

    @Test
    fun `dumps the OpenAPI spec to build openapi openapi json`() {
        val spec =
            mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString

        val schemas = json.readTree(spec).get("components").get("schemas")
        val states = schemas.get("BenefitGrantResponse").get("properties").get("status").get("enum")
        val actions = schemas.get("BenefitGrantChangeResponse").get("properties").get("action").get("enum")
        val responsibleTypes = schemas.get("BenefitGrantChangeResponse").get("properties").get("responsibleUserId").get("type")
        assertContains((0 until states.size()).map { states[it].stringValue() }, "EXPIRED")
        assertContains((0 until actions.size()).map { actions[it].stringValue() }, "EXPIRED")
        assertContains((0 until responsibleTypes.size()).map { responsibleTypes[it].stringValue() }, "null")
        val citizenProperties = schemas.get("CitizenBenefitGrantResponse").get("properties")
        assertEquals(14, citizenProperties.size())
        val citizenStates = citizenProperties.get("status").get("enum")
        assertEquals(setOf("ACTIVE", "SUSPENDED", "FINALIZED", "EXPIRED"),
            (0 until citizenStates.size()).map { citizenStates[it].stringValue() }.toSet())
        for (field in listOf("description", "amount")) {
            val nullableTypes = citizenProperties.get(field).get("type")
            assertContains((0 until nullableTypes.size()).map { nullableTypes[it].stringValue() }, "null")
        }
        val messageTypes = schemas.get("CitizenBenefitGrantListResponse").get("properties").get("message").get("type")
        assertContains((0 until messageTypes.size()).map { messageTypes[it].stringValue() }, "null")
        val parameters = json.readTree(spec).get("paths").get("/api/benefit-grants").get("get").get("parameters")
        assertEquals(setOf("page", "size"), (0 until parameters.size()).map { parameters[it].get("name").stringValue() }.toSet())
        val paths = json.readTree(spec).get("paths")
        assertM1PostmanContracts(paths)
        val m1Login = paths.get("/auth/session/m1/login").get("post")
        val m1File = paths["/api/admin/applications/{applicationId}/external-documents/{requestId}/file"]["get"]
        assertTrue(m1File["security"].size() > 0)
        for (code in listOf("200", "400", "401", "403", "404", "409", "502", "503", "504"))
            assertTrue(m1File["responses"].has(code), "M1 file $code")
        assertEquals("binary", m1File["responses"]["200"]["content"]["application/octet-stream"]["schema"]["format"].asString())
        for (code in listOf("409", "502", "503", "504"))
            assertTrue(m1File["responses"][code]["content"].has("application/problem+json"))
        assertEquals("sessionM1Login", m1Login["operationId"].asString())
        assertTrue(m1Login["security"] == null || m1Login["security"].isEmpty)
        for (code in listOf("200", "400", "401", "403", "409", "429", "502", "503", "504"))
            assertTrue(m1Login["responses"].has(code), "M1 login $code")
        assertTrue(m1Login["responses"]["200"]["headers"].has("Set-Cookie"))
        val m1Csrf = m1Login["parameters"].first { it["name"].asString() == "X-XSRF-TOKEN" }
        assertTrue(m1Csrf["required"].asBoolean())
        val m1Request = schemas["M1SessionLoginRequest"]
        assertEquals(setOf("email", "password"), m1Request["properties"].properties().map { it.key }.toSet())
        assertTrue(m1Request["properties"]["password"]["writeOnly"].asBoolean())
        assertEquals(180, m1Request["properties"]["email"]["maxLength"].asInt())
        assertEquals("#/components/schemas/CookieSessionResponse", m1Login["responses"]["200"]["content"]["*/*"]["schema"]["\$ref"].asString())
        for (suffix in listOf("status", "dashboard", "metrics", "publications", "subscriptions", "events", "events/{eventId}")) {
            val operation = paths.get("/api/admin/integrations/core/$suffix").get("get")
            for (code in listOf("200", "401", "403", "502", "503")) assertTrue(operation.get("responses").has(code), "Core $suffix $code")
            assertTrue(operation.get("security").size() > 0)
        }
        val coreConnectionProperties = schemas.get("CoreConnectionStatusResponse").get("properties")
        assertTrue(coreConnectionProperties.has("http"))
        assertTrue(coreConnectionProperties.has("amqp"))
        assertTrue(!coreConnectionProperties.has("accessToken") && !coreConnectionProperties.has("machineSecret"))
        for (path in listOf("/api/intervention-plans", "/api/intervention-plans/{planId}",
            "/api/intervention-plans/{planId}/actions/{actionId}/follow-ups")) {
            for (code in listOf("200", "400", "401", "403")) assertTrue(paths.get(path).get("get").get("responses").has(code))
        }
        val publicationPath = paths.get("/api/applications/{id}/intervention-plan/actions/{actionId}/follow-ups/{followUpId}/citizen-publication")
        for (method in listOf("put", "delete")) {
            for (code in listOf("400", "401", "403", "404", "409")) assertTrue(publicationPath.get(method).get("responses").has(code))
        }
        for (code in listOf("200", "201")) assertTrue(publicationPath.get("put").get("responses").has(code))
        assertTrue(publicationPath.get("delete").get("responses").has("204"))
        val publicFields = mapOf(
            "CitizenInterventionPlanSummaryResponse" to listOf("id", "applicationId", "applicationNumber", "programId", "programName", "programEditionId", "programEditionName", "createdAt", "status"),
            "CitizenInterventionPlanResponse" to listOf("id", "applicationId", "applicationNumber", "programId", "programName", "programEditionId", "programEditionName", "createdAt", "status", "objectives"),
            "CitizenInterventionObjectiveResponse" to listOf("id", "description", "actions"),
            "CitizenInterventionActionResponse" to listOf("id", "description", "startDate", "dueDate", "status"),
            "CitizenInterventionFollowUpResponse" to listOf("id", "actionId", "occurredOn", "status", "summary", "publishedAt"),
        )
        for ((name, fields) in publicFields) {
            val props = schemas.get(name).get("properties")
            assertEquals(fields.size, props.size(), name)
            fields.forEach { assertTrue(props.has(it), "$name.$it") }
        }
        for (name in listOf("CitizenInterventionPlanListResponse", "CitizenInterventionFollowUpListResponse")) {
            val types = schemas.get(name).get("properties").get("message").get("type")
            assertContains((0 until types.size()).map { types[it].stringValue() }, "null")
        }
        val publicationBranches = schemas.get("InterventionFollowUpResponse").get("properties").get("citizenPublication").get("anyOf")
        assertEquals("#/components/schemas/InterventionPublicationResponse", publicationBranches[0].get("\$ref").stringValue())
        assertEquals("null", publicationBranches[1].get("type").stringValue())
        val resultTypes = schemas.get("InterventionFollowUpResponse").get("properties").get("result").get("type")
        assertContains((0 until resultTypes.size()).map { resultTypes[it].stringValue() }, "null")
        val outputDir = File("build/openapi")
        outputDir.mkdirs()
        File(outputDir, "openapi.json").writeText(spec)
    }

    private fun assertM1PostmanContracts(paths: JsonNode) {
        for (name in listOf("m1-session", "m1-documents")) {
            val collection = json.readTree(File("docs/postman/$name.postman_collection.json").readText())
            assertEquals("http://localhost:8080", collection["variable"].first { it["key"].asString() == "baseUrl" }["value"].asString())
            collection["variable"].forEach { variable ->
                if (Regex("password|token|secret", RegexOption.IGNORE_CASE).containsMatchIn(variable["key"].asString()))
                    assertTrue(variable["value"].asString().isEmpty(), "No exportar credenciales en $name")
            }
            collection["item"].forEach { item ->
                val request = item["request"]
                val url = request["url"].asString()
                assertTrue(url.startsWith("{{baseUrl}}/"), "Los requests deben dirigirse a M8")
                val path = Regex("\\{\\{(\\w+)}}").replace(url.removePrefix("{{baseUrl}}")) { "{${it.groupValues[1]}}" }
                assertTrue(paths[path]?.has(request["method"].asString().lowercase()) == true, "Contrato ausente: $name $path")
            }
            if (name == "m1-documents") {
                assertEquals("{{m8AdminAccessToken}}", collection["auth"]["bearer"][0]["value"].asString())
                assertEquals("false", collection["variable"].first { it["key"].asString() == "m1FileProxyReadEnabled" }["value"].asString())
                assertTrue(collection["item"].all { it["request"]["method"].asString() == "GET" })
            }
        }
    }
}
