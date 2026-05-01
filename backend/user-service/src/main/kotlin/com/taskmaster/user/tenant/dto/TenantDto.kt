package com.taskmaster.user.tenant.dto

import java.time.Instant
import java.util.UUID

data class TenantDto(
    val id: UUID,
    val name: String,
    val domain: String,
    val logoUrl: String?,
    val active: Boolean,
    val createdAt: Instant
)

data class CreateTenantRequest(
    val name: String,
    val domain: String,
    val logoUrl: String? = null,
    val adminEmail: String,
    val adminUsername: String,
    val adminFirstName: String,
    val adminLastName: String,
    val adminPassword: String
)

data class UpdateTenantRequest(
    val name: String? = null,
    val logoUrl: String? = null,
    val active: Boolean? = null
)

data class TenantStatsDto(
    val tenantId: UUID,
    val tenantName: String,
    val totalUsers: Long,
    val activeUsers: Long
)
