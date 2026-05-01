package com.taskmaster.user.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    fun findByTenantId(tenantId: UUID, pageable: Pageable): Page<User>
    fun findByKeycloakId(keycloakId: String): Optional<User>
    fun existsByEmailAndTenantId(email: String, tenantId: UUID): Boolean
    fun countByTenantIdAndActive(tenantId: UUID, active: Boolean): Long
}
