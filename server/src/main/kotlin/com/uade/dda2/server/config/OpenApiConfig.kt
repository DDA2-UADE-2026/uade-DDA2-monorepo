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
            openApi.components?.schemas.orEmpty().forEach { (name, schema) ->
                if (schema.description.isNullOrBlank()) {
                    schema.description = schemaDescriptions[name] ?: "Estructura de datos $name."
                }
                schema.properties.orEmpty().forEach { (propertyName, propertySchema) ->
                    if (propertySchema.description.isNullOrBlank()) {
                        propertySchema.description = propertyDescriptions[propertyName] ?: "Valor de $propertyName."
                    }
                }
            }
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

    private val schemaDescriptions = mapOf(
        "LoginRequest" to "Credenciales requeridas para iniciar sesión.",
        "LoginResponse" to "Sesión autenticada, token y permisos concedidos.",
        "MeResponse" to "Perfil del usuario autenticado.",
        "CreateUserRequest" to "Datos requeridos para crear un usuario.",
        "UpdateUserRequest" to "Datos requeridos para actualizar un usuario.",
        "UserResponse" to "Datos públicos de un usuario autenticado.",
        "UserManagementResponse" to "Detalle administrativo de un usuario.",
        "CreateRoleRequest" to "Datos requeridos para crear un rol.",
        "UpdateRoleRequest" to "Datos requeridos para actualizar un rol.",
        "RoleResponse" to "Rol y permisos asociados.",
        "PermissionResponse" to "Permiso disponible en el sistema.",
        "CreateProgramRequest" to "Datos requeridos para crear un programa.",
        "UpdateProgramRequest" to "Datos requeridos para actualizar un programa.",
        "ProgramResponse" to "Detalle completo de un programa social.",
        "ProgramListResponse" to "Página de programas sociales.",
        "ProgramEditionResponse" to "Detalle completo de una edición de programa.",
        "ProgramEditionListResponse" to "Página de ediciones de un programa.",
        "ProgramBenefitResponse" to "Beneficio asociado a una edición.",
        "ProgramRequirementResponse" to "Requisito asociado a una edición.",
        "ProgramIncompatibilityResponse" to "Relación de incompatibilidad entre programas.",
        "ErrorResponse" to "Respuesta estándar de error de la API.",
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

    private val propertyDescriptions = mapOf(
        "id" to "Identificador único.",
        "username" to "Nombre de usuario utilizado para iniciar sesión.",
        "password" to "Contraseña del usuario.",
        "name" to "Nombre descriptivo.",
        "email" to "Correo electrónico.",
        "active" to "Indica si el registro está activo.",
        "roles" to "Roles asignados al usuario.",
        "permissions" to "Permisos concedidos.",
        "token" to "Token JWT de acceso.",
        "expiresIn" to "Tiempo de validez del token, expresado en segundos.",
        "user" to "Datos del usuario.",
        "objective" to "Objetivo del programa.",
        "content" to "Elementos de la página actual.",
        "page" to "Número de página, comenzando en cero.",
        "size" to "Cantidad de elementos solicitados por página.",
        "totalElements" to "Cantidad total de elementos.",
        "totalPages" to "Cantidad total de páginas.",
        "programId" to "UUID del programa.",
        "programName" to "Nombre del programa.",
        "programEditionId" to "UUID de la edición del programa.",
        "startDate" to "Fecha de inicio.",
        "endDate" to "Fecha de finalización.",
        "maxCapacity" to "Capacidad máxima de participantes.",
        "currentEnrollment" to "Cantidad actual de participantes inscriptos.",
        "status" to "Estado actual.",
        "benefitType" to "Tipo de beneficio.",
        "type" to "Tipo de requisito.",
        "description" to "Descripción complementaria.",
        "amount" to "Monto del beneficio.",
        "value" to "Valor exigido por el requisito.",
        "createdBy" to "Usuario que creó el registro.",
        "createdAt" to "Fecha y hora de creación.",
        "updatedAt" to "Fecha y hora de la última actualización.",
        "message" to "Mensaje legible que explica el error.",
        "code" to "Código estable y procesable del error.",
        "timestamp" to "Instante UTC en el que ocurrió el error.",
        "path" to "Ruta de la solicitud que produjo el error.",
        "fields" to "Errores de validación asociados a campos.",
    )
}
