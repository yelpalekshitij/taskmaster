package com.taskmaster.task.task

import com.taskmaster.task.task.dto.TaskFilter
import org.springframework.data.jpa.domain.Specification
import java.util.UUID

object TaskSpecification {

    fun byTenant(tenantId: UUID): Specification<Task> =
        Specification { root, _, cb -> cb.equal(root.get<UUID>("tenantId"), tenantId) }

    fun byStatus(status: TaskStatus): Specification<Task> =
        Specification { root, _, cb -> cb.equal(root.get<TaskStatus>("status"), status) }

    fun byPriority(priority: TaskPriority): Specification<Task> =
        Specification { root, _, cb -> cb.equal(root.get<TaskPriority>("priority"), priority) }

    fun byAssignedTo(assignedTo: UUID): Specification<Task> =
        Specification { root, _, cb -> cb.equal(root.get<UUID>("assignedTo"), assignedTo) }

    fun fromFilter(tenantId: UUID, filter: TaskFilter?): Specification<Task> {
        var spec = byTenant(tenantId)
        filter?.status?.let { spec = spec.and(byStatus(it)) }
        filter?.priority?.let { spec = spec.and(byPriority(it)) }
        filter?.assignedTo?.let { spec = spec.and(byAssignedTo(UUID.fromString(it))) }
        return spec
    }
}
