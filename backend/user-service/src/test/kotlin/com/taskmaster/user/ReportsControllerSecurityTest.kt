package com.taskmaster.user

import com.taskmaster.user.config.SecurityConfig
import com.taskmaster.user.reports.ReportsController
import com.taskmaster.user.tenant.Tenant
import com.taskmaster.user.tenant.TenantRepository
import com.taskmaster.user.user.UserRepository
import io.micrometer.tracing.Tracer
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(ReportsController::class)
@TestPropertySource(properties = [
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
])
@Import(SecurityConfig::class, ReportsControllerSecurityTest.MockRepositoriesConfig::class)
class ReportsControllerSecurityTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var tenantRepository: TenantRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    class MockRepositoriesConfig {
        @Bean
        fun tenantRepository(): TenantRepository = io.mockk.mockk(relaxed = true)

        @Bean
        fun userRepository(): UserRepository = io.mockk.mockk(relaxed = true)

        @Bean
        fun tracer(): Tracer = io.mockk.mockk(relaxed = true)
    }

    // --- /api/v1/reports/global ---

    @Test
    fun `global stats returns 401 when unauthenticated`() {
        mockMvc.get("/api/v1/reports/global")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `global stats returns 403 when authenticated as USER role`() {
        mockMvc.get("/api/v1/reports/global") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_USER")))
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `global stats returns 403 when authenticated as TENANT_ADMIN role`() {
        mockMvc.get("/api/v1/reports/global") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `global stats returns 200 with correct data for MASTER_ADMIN role`() {
        every { tenantRepository.count() } returns 5
        every { tenantRepository.countByActive(true) } returns 4
        every { userRepository.count() } returns 20
        every { userRepository.countByActive(true) } returns 18

        mockMvc.get("/api/v1/reports/global") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_MASTER_ADMIN")))
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalTenants") { value(5) }
            jsonPath("$.activeTenants") { value(4) }
            jsonPath("$.totalUsers") { value(20) }
            jsonPath("$.activeUsers") { value(18) }
        }
    }

    // --- /api/v1/reports/tenants ---

    @Test
    fun `tenant stats returns 401 when unauthenticated`() {
        mockMvc.get("/api/v1/reports/tenants")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `tenant stats returns 403 when authenticated as USER role`() {
        mockMvc.get("/api/v1/reports/tenants") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_USER")))
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `tenant stats returns 200 for MASTER_ADMIN role`() {
        val tenantId = UUID.randomUUID()
        val tenant = Tenant(id = tenantId, name = "Acme Corp", domain = "acme.com")
        every { tenantRepository.findAll() } returns listOf(tenant)
        every { userRepository.countByTenantId(tenantId) } returns 10
        every { userRepository.countByTenantIdAndActive(tenantId, true) } returns 8

        mockMvc.get("/api/v1/reports/tenants") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_MASTER_ADMIN")))
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].tenantName") { value("Acme Corp") }
            jsonPath("$[0].totalUsers") { value(10) }
            jsonPath("$[0].activeUsers") { value(8) }
        }
    }

    @Test
    fun `tenant stats returns 200 for TENANT_ADMIN role`() {
        val tenantId = UUID.randomUUID()
        val tenant = Tenant(id = tenantId, name = "Beta Inc", domain = "beta.com")
        every { tenantRepository.findAll() } returns listOf(tenant)
        every { userRepository.countByTenantId(tenantId) } returns 5
        every { userRepository.countByTenantIdAndActive(tenantId, true) } returns 5

        mockMvc.get("/api/v1/reports/tenants") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].tenantName") { value("Beta Inc") }
        }
    }
}
