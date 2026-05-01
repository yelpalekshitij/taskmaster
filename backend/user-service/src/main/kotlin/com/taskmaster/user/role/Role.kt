package com.taskmaster.user.role

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "roles")
class Role(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String,

    @Column(name = "tenant_id")
    val tenantId: UUID? = null,

    var description: String? = null,

    @Column(name = "system_role")
    val systemRole: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "permissions")
class Permission(
    @Id
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false)
    val name: String,

    var description: String? = null
)

@Embeddable
data class RolePermissionId(
    @Column(name = "role_id") val roleId: UUID,
    @Column(name = "permission_id") val permissionId: UUID
) : java.io.Serializable

@Entity
@Table(name = "role_permissions")
class RolePermission(
    @EmbeddedId val id: RolePermissionId
)

@Embeddable
data class UserRoleId(
    @Column(name = "user_id") val userId: UUID,
    @Column(name = "role_id") val roleId: UUID
) : java.io.Serializable

@Entity
@Table(name = "user_roles")
class UserRole(
    @EmbeddedId val id: UserRoleId
)
