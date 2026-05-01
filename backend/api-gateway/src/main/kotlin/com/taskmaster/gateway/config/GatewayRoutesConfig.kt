package com.taskmaster.gateway.config

import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayRoutesConfig {

    @Bean
    fun routeLocator(builder: RouteLocatorBuilder): RouteLocator = builder.routes()

        // -------------------------------------------------------
        // User Service — REST endpoints
        // -------------------------------------------------------
        .route("user-service") { r ->
            r.path("/api/v1/users/**", "/api/v1/tenants/**", "/api/v1/roles/**")
                .uri("lb://user-service")
        }

        // -------------------------------------------------------
        // Task Service — REST endpoints
        // -------------------------------------------------------
        .route("task-service-rest") { r ->
            r.path("/api/v1/tasks/**")
                .uri("lb://task-service")
        }

        // -------------------------------------------------------
        // Task Service — GraphQL
        // -------------------------------------------------------
        .route("task-service-graphql") { r ->
            r.path("/graphql", "/graphiql")
                .uri("lb://task-service")
        }

        // -------------------------------------------------------
        // Notification Service
        // -------------------------------------------------------
        .route("notification-service") { r ->
            r.path("/api/v1/notifications/**")
                .uri("lb://notification-service")
        }

        // -------------------------------------------------------
        // Scheduler Service
        // -------------------------------------------------------
        .route("scheduler-service") { r ->
            r.path("/api/v1/scheduler/**")
                .uri("lb://scheduler-service")
        }

        // -------------------------------------------------------
        // Swagger / OpenAPI aggregation
        // Each route rewrites the gateway-scoped path to the
        // downstream service's canonical /v3/api-docs path.
        // -------------------------------------------------------
        .route("user-service-docs") { r ->
            r.path("/v3/api-docs/user-service")
                .filters { f -> f.rewritePath("/v3/api-docs/user-service", "/v3/api-docs") }
                .uri("lb://user-service")
        }
        .route("task-service-docs") { r ->
            r.path("/v3/api-docs/task-service")
                .filters { f -> f.rewritePath("/v3/api-docs/task-service", "/v3/api-docs") }
                .uri("lb://task-service")
        }
        .route("notification-service-docs") { r ->
            r.path("/v3/api-docs/notification-service")
                .filters { f -> f.rewritePath("/v3/api-docs/notification-service", "/v3/api-docs") }
                .uri("lb://notification-service")
        }
        .route("scheduler-service-docs") { r ->
            r.path("/v3/api-docs/scheduler-service")
                .filters { f -> f.rewritePath("/v3/api-docs/scheduler-service", "/v3/api-docs") }
                .uri("lb://scheduler-service")
        }

        .build()
}
