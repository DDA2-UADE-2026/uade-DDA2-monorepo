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
}
