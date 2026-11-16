package com.uade.dda2.server

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class AsyncApiDocsGenerationTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper

    @Test
    fun `genera AsyncAPI desde los diez contratos tipados sin conectar al broker`() {
        val spec = mvc.perform(get("/springwolf/docs")).andExpect(status().isOk).andReturn().response.contentAsString
        File("build/asyncapi").mkdirs()
        File("build/asyncapi/asyncapi.json").writeText(spec)
        val root = json.readTree(spec)
        assertTrue(root["asyncapi"].asString().startsWith("3."))
        val operations = root["operations"].properties().map { it.value }
        assertEquals(3, operations.count { it["action"].asString() == "send" })
        assertEquals(7, operations.count { it["action"].asString() == "receive" })
        for (name in listOf("SocialDocumentationRequestedData", "SocialBenefitUpdatedData", "UpdateTicketStatusData",
            "TicketUpdatedData", "SocialDocumentationDeliveredData", "CitizenUpdatedData", "DebtOverdueData",
            "DebtSettledData", "ExemptionRequestedData", "UpdateExemptionStatusData")) assertTrue(spec.contains(name), name)
        val schemas = root["components"]["schemas"]
        val envelopes = schemas.properties().filter { it.value["properties"]?.has("eventId") == true }
        assertEquals(10, envelopes.size, "Cada sobre debe conservar el tipo concreto de data")
        envelopes.forEach { entry ->
            val properties = entry.value["properties"]
            assertTrue(properties.has("occurredAt") && !properties.has("occuredAt"))
            assertTrue(properties["data"].has("\$ref"), entry.key)
            val required = entry.value["required"].asSequence().map { it.asString() }.toSet()
            assertTrue(required.containsAll(listOf("eventId", "eventType", "eventVersion", "occurredAt", "sourceModule", "data")), entry.key)
        }
        val formData = schemas["TicketRoutingData"]["properties"]["formData"]
        assertTrue(!formData.toString().contains("JsonNode"))
        assertTrue(schemas["TicketLocationData"]["properties"].has("neighborhoodId"))
        val ticketRequired = schemas["TicketUpdatedData"]["required"].asSequence().map { it.asString() }.toSet()
        assertTrue("isAnonymous" in ticketRequired)
        mvc.perform(get("/springwolf/asyncapi-ui.html")).andExpect(status().isOk)
        assertTrue(!spec.contains("module_desarrollo-social") && !spec.contains("machineSecret"))
        mvc.perform(post("/springwolf/amqp/publish").with(user("admin"))).andExpect(status().isForbidden)
    }
}
