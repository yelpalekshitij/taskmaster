package com.taskmaster.gateway.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.properties.SwaggerUiConfigParameters
import org.springdoc.core.properties.SwaggerUiConfigProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("TaskMaster API Gateway")
                .description(
                    "Centralized API documentation aggregating all TaskMaster microservices. " +
                        "Use the selector above to switch between service documentation."
                )
                .version("1.0.0")
        )
        .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
        .components(
            Components().addSecuritySchemes(
                "bearerAuth",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Keycloak JWT — obtain from /realms/taskmaster-app or /realms/taskmaster-admin")
            )
        )

    /**
     * Registers each downstream microservice as a named Swagger UI group.
     * The swagger-ui will display a drop-down allowing users to switch between services.
     * Each group name must match the route path segment defined in GatewayRoutesConfig
     * (e.g. "/v3/api-docs/user-service").
     *
     * SwaggerUiConfigParameters requires the auto-configured SwaggerUiConfigProperties
     * to be injected — springdoc registers it as a bean automatically.
     */
    @Bean
    fun swaggerUiConfigParameters(swaggerUiConfigProperties: SwaggerUiConfigProperties): SwaggerUiConfigParameters {
        val parameters = SwaggerUiConfigParameters(swaggerUiConfigProperties)
        listOf("user-service", "task-service", "notification-service", "scheduler-service")
            .forEach { service -> parameters.addGroup(service) }
        return parameters
    }
}
