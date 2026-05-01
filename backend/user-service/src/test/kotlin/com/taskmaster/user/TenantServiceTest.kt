package com.taskmaster.user

import com.taskmaster.user.keycloak.KeycloakAdminService
import com.taskmaster.user.tenant.Tenant
import com.taskmaster.user.tenant.TenantRepository
import com.taskmaster.user.tenant.TenantService
import com.taskmaster.user.tenant.dto.CreateTenantRequest
import com.taskmaster.user.tenant.dto.UpdateTenantRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import java.util.UUID

class TenantServiceTest {

    private val tenantRepository = mockk<TenantRepository>()
    private val keycloakAdminService = mockk<KeycloakAdminService>(relaxed = true)
    private val tenantService = TenantService(tenantRepository, keycloakAdminService)

    @Test
    fun `create tenant succeeds when domain is unique`() {
        val request = CreateTenantRequest(
            name = "Acme Corp",
            domain = "acme.com",
            adminEmail = "admin@acme.com",
            adminUsername = "acme-admin",
            adminFirstName = "Admin",
            adminLastName = "User",
            adminPassword = "Password@123"
        )
        val tenant = Tenant(name = request.name, domain = request.domain)

        every { tenantRepository.existsByDomain(request.domain) } returns false
        every { tenantRepository.save(any()) } returns tenant

        val result = tenantService.create(request)

        assertNotNull(result)
        assertEquals("Acme Corp", result.name)
        verify { keycloakAdminService.createTenantAdmin(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `create tenant fails when domain already exists`() {
        val request = CreateTenantRequest(
            name = "Acme Corp",
            domain = "existing.com",
            adminEmail = "admin@acme.com",
            adminUsername = "admin",
            adminFirstName = "Admin",
            adminLastName = "User",
            adminPassword = "Password@123"
        )
        every { tenantRepository.existsByDomain("existing.com") } returns true

        assertThrows<IllegalArgumentException> {
            tenantService.create(request)
        }
    }

    @Test
    fun `update tenant patches only provided fields`() {
        val tenantId = UUID.randomUUID()
        val tenant = Tenant(id = tenantId, name = "Old Name", domain = "test.com")
        val request = UpdateTenantRequest(name = "New Name")

        every { tenantRepository.findById(tenantId) } returns Optional.of(tenant)
        every { tenantRepository.save(any()) } answers { firstArg() }

        val result = tenantService.update(tenantId, request)

        assertEquals("New Name", result.name)
        assertEquals("test.com", result.domain)
    }

    @Test
    fun `findById throws when tenant not found`() {
        val id = UUID.randomUUID()
        every { tenantRepository.findById(id) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            tenantService.findById(id)
        }
    }
}
