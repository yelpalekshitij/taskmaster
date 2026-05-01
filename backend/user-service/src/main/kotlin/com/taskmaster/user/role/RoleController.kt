package com.taskmaster.user.role

import com.taskmaster.user.common.TenantContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles & Permissions", description = "Role and permission management")
class RoleController(private val roleService: RoleService) {

    @GetMapping
    @PreAuthorize("hasRole('MASTER_ADMIN') or hasRole('TENANT_ADMIN')")
    @Operation(summary = "List roles available to current tenant")
    fun getRoles(): ResponseEntity<List<RoleDto>> {
        val tenantId = TenantContext.get()?.let { if (it.isBlank()) null else UUID.fromString(it) }
        return ResponseEntity.ok(roleService.findRolesForTenant(tenantId))
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('MASTER_ADMIN') or hasRole('TENANT_ADMIN')")
    @Operation(summary = "List all available permissions")
    fun getPermissions(): ResponseEntity<List<PermissionDto>> =
        ResponseEntity.ok(roleService.findAllPermissions())

    @PostMapping
    @PreAuthorize("hasRole('MASTER_ADMIN') or hasRole('TENANT_ADMIN')")
    @Operation(summary = "Create a custom role for the current tenant")
    fun createRole(@RequestBody request: CreateRoleRequest): ResponseEntity<RoleDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request))
}
