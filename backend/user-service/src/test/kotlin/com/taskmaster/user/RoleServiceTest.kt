package com.taskmaster.user

import com.taskmaster.user.role.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class RoleServiceTest {

    private val roleRepository = mockk<RoleRepository>()
    private val permissionRepository = mockk<PermissionRepository>()
    private val rolePermissionRepository = mockk<RolePermissionRepository>()
    private val roleService = RoleService(roleRepository, permissionRepository, rolePermissionRepository)

    @Test
    fun `findRolesForTenant returns system and tenant roles`() {
        val tenantId = UUID.randomUUID()
        val systemRole = Role(name = "USER", systemRole = true)
        val tenantRole = Role(name = "CUSTOM_ROLE", tenantId = tenantId)

        every { roleRepository.findByTenantIdOrSystemRoleTrue(tenantId) } returns listOf(systemRole, tenantRole)
        every { rolePermissionRepository.findPermissionsByRoleId(any()) } returns emptyList()

        val result = roleService.findRolesForTenant(tenantId)

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "USER" && it.systemRole })
        assertTrue(result.any { it.name == "CUSTOM_ROLE" && !it.systemRole })
    }

    @Test
    fun `findAllPermissions returns all permissions`() {
        val perms = listOf(
            Permission(name = "TASK_CREATE", description = "Create tasks"),
            Permission(name = "TASK_READ", description = "Read tasks")
        )
        every { permissionRepository.findAll() } returns perms

        val result = roleService.findAllPermissions()

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "TASK_CREATE" })
    }
}
