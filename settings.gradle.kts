pluginManagement {
    repositories {
        maven { url = uri("https://repo.spring.io/milestone") }
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "TaskMaster"

include(
    "service-registry",
    "config-server",
    "api-gateway",
    "user-service",
    "task-service",
    "notification-service",
    "scheduler-service",
    "admin-server"
)

// Map module names to their location under backend/
listOf(
    "service-registry", "config-server", "api-gateway",
    "user-service", "task-service", "notification-service",
    "scheduler-service", "admin-server"
).forEach { name ->
    project(":$name").projectDir = file("backend/$name")
}
