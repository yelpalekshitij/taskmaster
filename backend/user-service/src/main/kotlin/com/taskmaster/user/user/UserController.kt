package com.taskmaster.user.user

import com.taskmaster.user.common.TenantContext
import com.taskmaster.user.user.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management")
class UserController(private val userService: UserService) {

    @GetMapping
    @PreAuthorize("hasRole('MASTER_ADMIN') or hasRole('TENANT_ADMIN')")
    @Operation(summary = "List users for the current tenant")
    fun getUsers(@PageableDefault(size = 20) pageable: Pageable): ResponseEntity<Page<UserDto>> {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        return ResponseEntity.ok(userService.findByTenant(tenantId, pageable))
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user")
    fun getCurrentUser(): ResponseEntity<UserDto> =
        ResponseEntity.ok(userService.findCurrentUser())

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('MASTER_ADMIN') or hasRole('TENANT_ADMIN') or @sec.isSameUser(#userId)")
    @Operation(summary = "Get user by ID")
    fun getUser(@PathVariable userId: UUID): ResponseEntity<UserDto> =
        ResponseEntity.ok(userService.findById(userId))

    @PostMapping
    @PreAuthorize("hasRole('MASTER_ADMIN') or hasRole('TENANT_ADMIN')")
    @Operation(summary = "Create a new user in the current tenant")
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserDto> {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(tenantId, request))
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('MASTER_ADMIN') or hasRole('TENANT_ADMIN') or @sec.isSameUser(#userId)")
    @Operation(summary = "Update user profile or role assignments")
    fun updateUser(
        @PathVariable userId: UUID,
        @Valid @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserDto> =
        ResponseEntity.ok(userService.update(userId, request))
}
