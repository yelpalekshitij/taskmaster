package com.taskmaster.user.role

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface RoleRepository : JpaRepository<Role, UUID> {
    fun findByTenantIdOrSystemRoleTrue(tenantId: UUID?): List<Role>
    fun findBySystemRoleTrue(): List<Role>
}

interface PermissionRepository : JpaRepository<Permission, UUID> {
    fun findByName(name: String): Permission?
}

interface RolePermissionRepository : JpaRepository<RolePermission, RolePermissionId> {
    @Query("SELECT p FROM Permission p JOIN RolePermission rp ON rp.id.permissionId = p.id WHERE rp.id.roleId = :roleId")
    fun findPermissionsByRoleId(roleId: UUID): List<Permission>

    fun deleteByIdRoleId(roleId: UUID)
}

interface UserRoleRepository : JpaRepository<UserRole, UserRoleId> {
    @Query("SELECT r FROM Role r JOIN UserRole ur ON ur.id.roleId = r.id WHERE ur.id.userId = :userId")
    fun findRolesByUserId(userId: UUID): List<Role>

    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.id.userId = :userId")
    fun deleteByUserId(userId: UUID)
}
