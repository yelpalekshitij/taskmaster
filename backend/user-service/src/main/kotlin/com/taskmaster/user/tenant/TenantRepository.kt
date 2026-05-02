package com.taskmaster.user.tenant

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface TenantRepository : JpaRepository<Tenant, UUID>, JpaSpecificationExecutor<Tenant> {

    fun findByDomain(domain: String): Tenant?

    fun findByActive(active: Boolean, pageable: Pageable): Page<Tenant>

    fun existsByDomain(domain: String): Boolean

    fun countByActive(active: Boolean): Long

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.active = true")
    fun countActiveUsers(tenantId: UUID): Long
}
