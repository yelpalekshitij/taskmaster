package com.taskmaster.user.tenant

import com.taskmaster.user.tenant.dto.*
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
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants", description = "Tenant management (MASTER_ADMIN only)")
class TenantController(private val tenantService: TenantService) {

    @GetMapping
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    @Operation(summary = "List all tenants with pagination and filters")
    fun getAllTenants(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) domain: String?,
        @PageableDefault(size = 20, sort = ["name"]) pageable: Pageable
    ): ResponseEntity<Page<TenantDto>> =
        ResponseEntity.ok(tenantService.findAll(name, active, domain, pageable))

    @GetMapping("/{tenantId}")
    @PreAuthorize("hasRole('MASTER_ADMIN') or @sec.isTenantOwner(#tenantId)")
    @Operation(summary = "Get tenant by ID")
    fun getTenant(@PathVariable tenantId: UUID): ResponseEntity<TenantDto> =
        ResponseEntity.ok(tenantService.findById(tenantId))

    @PostMapping
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    @Operation(summary = "Create a new tenant and provision its first TENANT_ADMIN")
    fun createTenant(@Valid @RequestBody request: CreateTenantRequest): ResponseEntity<TenantDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(tenantService.create(request))

    @PatchMapping("/{tenantId}")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    @Operation(summary = "Update tenant details")
    fun updateTenant(
        @PathVariable tenantId: UUID,
        @Valid @RequestBody request: UpdateTenantRequest
    ): ResponseEntity<TenantDto> =
        ResponseEntity.ok(tenantService.update(tenantId, request))

    @DeleteMapping("/{tenantId}")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    @Operation(summary = "Deactivate a tenant")
    fun deactivateTenant(@PathVariable tenantId: UUID): ResponseEntity<Void> {
        tenantService.deactivate(tenantId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{tenantId}/stats")
    @PreAuthorize("hasRole('MASTER_ADMIN') or @sec.isTenantOwner(#tenantId)")
    @Operation(summary = "Get tenant usage statistics")
    fun getTenantStats(@PathVariable tenantId: UUID): ResponseEntity<TenantStatsDto> =
        ResponseEntity.ok(tenantService.getStats(tenantId))
}
