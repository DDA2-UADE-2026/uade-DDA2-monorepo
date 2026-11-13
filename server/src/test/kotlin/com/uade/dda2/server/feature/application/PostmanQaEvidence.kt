package com.uade.dda2.server.feature.application

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import org.springframework.test.web.servlet.MvcResult
import java.nio.file.Files
import java.nio.file.Path

/** Reads the real collection; exports only synthetic responses, never auth headers. */
object PostmanQaEvidence {
    private val json = JsonMapper.builder().build()
    fun request(name: String): JsonNode {
        val root = json.readTree(Files.readString(Path.of("docs/postman/program-feature.postman_collection.json")))
        fun find(node: JsonNode): JsonNode? {
            if (node.path("name").asText() == name && node.has("request")) return node
            for (child in node.path("item")) find(child)?.let { return it }
            return null
        }
        return requireNotNull(find(root)) { "Missing Postman request: $name" }
    }
    fun save(id: String, name: String, result: MvcResult) {
        val directory = Path.of("build/qa-postman")
        Files.createDirectories(directory)
        val evidence = mapOf("id" to id, "requestName" to name, "status" to result.response.status,
            "body" to json.readTree(result.response.contentAsString))
        Files.writeString(directory.resolve("$id.json"), json.writeValueAsString(evidence))
    }
}
