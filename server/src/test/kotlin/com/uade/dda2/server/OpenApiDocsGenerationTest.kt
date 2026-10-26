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
        val outputDir = File("build/openapi")
        outputDir.mkdirs()
        File(outputDir, "openapi.json").writeText(spec)
    }
}
