package com.taskmaster.user.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class UserDto(
    val id: UUID,
    val keycloakId: String,
    val email: String,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val tenantId: UUID,
    val active: Boolean,
    val roles: List<String> = emptyList(),
    val createdAt: Instant
)

data class CreateUserRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank @field:Size(min = 3, max = 50) val username: String,
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:NotBlank @field:Size(min = 8) val password: String,
    val roleIds: List<UUID> = emptyList()
)

data class UpdateUserRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val active: Boolean? = null,
    val roleIds: List<UUID>? = null
)
