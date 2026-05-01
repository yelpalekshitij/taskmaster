package com.taskmaster.task.task

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.UUID

interface TaskRepository : JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Task?

    fun findByTenantId(tenantId: UUID, pageable: Pageable): Page<Task>

    fun findByAssignedToAndTenantId(assignedTo: UUID, tenantId: UUID, pageable: Pageable): Page<Task>

    fun existsByIdAndTenantId(id: UUID, tenantId: UUID): Boolean
}
