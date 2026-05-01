package com.taskmaster.user.role

import com.taskmaster.user.common.TenantContext
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class RoleDto(val id: UUID, val name: String, val description: String?, val systemRole: Boolean, val permissions: List<String>)
data class PermissionDto(val id: UUID, val name: String, val description: String?)
data class CreateRoleRequest(val name: String, val description: String?, val permissionIds: List<UUID> = emptyList())

@Service
@Transactional
class RoleService(
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val rolePermissionRepository: RolePermissionRepository
) {

    @Transactional(readOnly = true)
    @Cacheable("roles", key = "'tenant:' + #tenantId")
    fun findRolesForTenant(tenantId: UUID?): List<RoleDto> {
        val roles = if (tenantId == null) {
            roleRepository.findBySystemRoleTrue()
        } else {
            roleRepository.findByTenantIdOrSystemRoleTrue(tenantId)
        }
        return roles.map { role ->
            val perms = rolePermissionRepository.findPermissionsByRoleId(role.id)
            RoleDto(role.id, role.name, role.description, role.systemRole, perms.map { it.name })
        }
    }

    @Transactional(readOnly = true)
    @Cacheable("permissions")
    fun findAllPermissions(): List<PermissionDto> =
        permissionRepository.findAll().map { PermissionDto(it.id, it.name, it.description) }

    fun createRole(request: CreateRoleRequest): RoleDto {
        val tenantId = TenantContext.get()?.let { UUID.fromString(it) }
        val role = Role(name = request.name, description = request.description, tenantId = tenantId)
        val saved = roleRepository.save(role)

        request.permissionIds.forEach { permId ->
            rolePermissionRepository.save(RolePermission(RolePermissionId(saved.id, permId)))
        }

        val perms = rolePermissionRepository.findPermissionsByRoleId(saved.id)
        return RoleDto(saved.id, saved.name, saved.description, saved.systemRole, perms.map { it.name })
    }
}
