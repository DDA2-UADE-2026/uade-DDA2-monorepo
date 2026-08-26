package com.uade.dda2.server.config

import com.uade.dda2.server.error.ErrorResponse
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val JWT_SECURITY_SCHEME = "bearer-jwt"

@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("API de Desarrollo Social")
                    .description(
                        "API REST para administrar autenticación, usuarios, roles, permisos y programas sociales.",
                    )
                    .version("1.0.0")
                    .contact(Contact().name("Equipo de Desarrollo Social")),
            )
            .components(
                Components().addSecuritySchemes(
                    JWT_SECURITY_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT obtenido al iniciar sesión. Ingrese únicamente el token, sin el prefijo Bearer."),
                ),
            )

    @Bean
    fun commonOpenApiResponses(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            openApi.paths?.forEach { (path, pathItem) ->
                pathItem.readOperationsMap().forEach { (method, operation) ->
                    actuatorDescriptions[path]?.let { (summary, description) ->
                        operation.summary = summary
                        operation.description = description
                    }
                    if (path !in publicPaths) {
                        operation.security = listOf(SecurityRequirement().addList(JWT_SECURITY_SCHEME))
                        operation.responses.addApiResponse("401", errorResponse("No autenticado."))
                        operation.responses.addApiResponse("403", errorResponse("No posee permisos para realizar la operación."))
                    }
                    if (operation.requestBody != null || operation.parameters.orEmpty().isNotEmpty()) {
                        operation.responses.addApiResponse("400", errorResponse("La solicitud es inválida."))
                    }
                    if (operation.parameters.orEmpty().any { it.`in` == "path" }) {
                        operation.responses.addApiResponse("404", errorResponse("No se encontró el recurso solicitado."))
                    }
                    if (method in setOf(PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT, PathItem.HttpMethod.PATCH, PathItem.HttpMethod.DELETE)) {
                        operation.responses.addApiResponse("409", errorResponse("La operación entra en conflicto con el estado actual de los datos."))
                    }
                    operation.responses.addApiResponse("500", errorResponse("Ocurrió un error interno inesperado."))
                }
            }
        }

    private fun errorResponse(description: String): ApiResponse =
        ApiResponse()
            .description(description)
            .content(
                Content().addMediaType(
                    "application/json",
                    MediaType().schema(Schema<ErrorResponse>().`$ref`("#/components/schemas/ErrorResponse")),
                ),
            )

    private val publicPaths = setOf(
        "/auth/login",
        "/actuator/health",
    )

    private val actuatorDescriptions = mapOf(
        "/actuator" to ("Consultar Actuator" to "Devuelve los enlaces de descubrimiento de los endpoints operativos disponibles."),
        "/actuator/health" to ("Consultar el estado de salud" to "Informa el estado de salud actual de la aplicación."),
        "/actuator/info" to ("Consultar información de la aplicación" to "Devuelve la información operativa publicada por la aplicación."),
    )

}
