package com.taskmaster.scheduler.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduledTaskRepository : JpaRepository<ScheduledTask, UUID> {

    fun findByTaskId(taskId: UUID): ScheduledTask?

    fun findByTenantIdAndActiveTrue(tenantId: UUID): List<ScheduledTask>
}
