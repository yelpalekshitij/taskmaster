package com.taskmaster.user.tenant

import com.taskmaster.user.keycloak.KeycloakAdminService
import com.taskmaster.user.tenant.dto.*
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class TenantService(
    private val tenantRepository: TenantRepository,
    private val keycloakAdminService: KeycloakAdminService
) {
    private val log = LoggerFactory.getLogger(TenantService::class.java)

    @Transactional(readOnly = true)
    fun findAll(name: String?, active: Boolean?, domain: String?, pageable: Pageable): Page<TenantDto> {
        val spec = TenantSpecification.buildSpec(name, active, domain)
        return tenantRepository.findAll(spec, pageable).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    @Cacheable("tenants", key = "#id")
    fun findById(id: UUID): TenantDto {
        return tenantRepository.findById(id)
            .orElseThrow { NoSuchElementException("Tenant not found: $id") }
            .toDto()
    }

    fun create(request: CreateTenantRequest): TenantDto {
        require(!tenantRepository.existsByDomain(request.domain)) {
            "Tenant with domain '${request.domain}' already exists"
        }

        val tenant = Tenant(
            name = request.name,
            domain = request.domain,
            logoUrl = request.logoUrl
        )
        val saved = tenantRepository.save(tenant)
        log.info("Tenant created: id={}, domain={}", saved.id, saved.domain)

        // Create the first TENANT_ADMIN user in Keycloak
        keycloakAdminService.createTenantAdmin(
            tenantId = saved.id,
            email = request.adminEmail,
            username = request.adminUsername,
            firstName = request.adminFirstName,
            lastName = request.adminLastName,
            password = request.adminPassword
        )

        return saved.toDto()
    }

    @CacheEvict("tenants", key = "#id")
    fun update(id: UUID, request: UpdateTenantRequest): TenantDto {
        val tenant = tenantRepository.findById(id)
            .orElseThrow { NoSuchElementException("Tenant not found: $id") }

        request.name?.let { tenant.name = it }
        request.logoUrl?.let { tenant.logoUrl = it }
        request.active?.let { tenant.active = it }

        return tenantRepository.save(tenant).toDto()
    }

    @CacheEvict("tenants", key = "#id")
    fun deactivate(id: UUID) {
        val tenant = tenantRepository.findById(id)
            .orElseThrow { NoSuchElementException("Tenant not found: $id") }
        tenant.active = false
        tenantRepository.save(tenant)
        log.info("Tenant deactivated: {}", id)
    }

    @Transactional(readOnly = true)
    fun getStats(tenantId: UUID): TenantStatsDto {
        val tenant = tenantRepository.findById(tenantId)
            .orElseThrow { NoSuchElementException("Tenant not found: $tenantId") }
        val activeUsers = tenantRepository.countActiveUsers(tenantId)
        return TenantStatsDto(
            tenantId = tenant.id,
            tenantName = tenant.name,
            totalUsers = activeUsers,
            activeUsers = activeUsers
        )
    }

    private fun Tenant.toDto() = TenantDto(id, name, domain, logoUrl, active, createdAt)
}
