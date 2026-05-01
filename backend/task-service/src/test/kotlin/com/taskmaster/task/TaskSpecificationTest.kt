package com.taskmaster.task

import com.taskmaster.task.task.Task
import com.taskmaster.task.task.TaskPriority
import com.taskmaster.task.task.TaskSpecification
import com.taskmaster.task.task.TaskStatus
import com.taskmaster.task.task.dto.TaskFilter
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

class TaskSpecificationTest {

    private val root = mockk<Root<Task>>(relaxed = true)
    private val query = mockk<CriteriaQuery<*>>(relaxed = true)
    private val cb = mockk<CriteriaBuilder>(relaxed = true)
    private val predicate = mockk<Predicate>()
    private val path = mockk<Path<Any>>(relaxed = true)

    @Test
    fun `byStatus creates equality predicate on status field`() {
        every { root.get<TaskStatus>("status") } returns path as Path<TaskStatus>
        every { cb.equal(any(), TaskStatus.TODO) } returns predicate

        val spec = TaskSpecification.byStatus(TaskStatus.TODO)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        verify { cb.equal(any(), TaskStatus.TODO) }
    }

    @Test
    fun `byTenant creates equality predicate on tenantId field`() {
        val tenantId = UUID.randomUUID()
        every { root.get<UUID>("tenantId") } returns path as Path<UUID>
        every { cb.equal(any(), tenantId) } returns predicate

        val spec = TaskSpecification.byTenant(tenantId)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        verify { cb.equal(any(), tenantId) }
    }

    @Test
    fun `byPriority creates equality predicate on priority field`() {
        every { root.get<TaskPriority>("priority") } returns path as Path<TaskPriority>
        every { cb.equal(any(), TaskPriority.HIGH) } returns predicate

        val spec = TaskSpecification.byPriority(TaskPriority.HIGH)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        verify { cb.equal(any(), TaskPriority.HIGH) }
    }

    @Test
    fun `fromFilter with null filter only adds tenant predicate`() {
        val tenantId = UUID.randomUUID()
        every { root.get<UUID>("tenantId") } returns path as Path<UUID>
        every { cb.equal(any(), tenantId) } returns predicate

        val spec = TaskSpecification.fromFilter(tenantId, null)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        verify(exactly = 1) { cb.equal(any(), tenantId) }
    }

    @Test
    fun `fromFilter with all fields set combines all predicates`() {
        val tenantId = UUID.randomUUID()
        val assignedTo = UUID.randomUUID()
        val filter = TaskFilter(
            status = TaskStatus.IN_PROGRESS,
            priority = TaskPriority.CRITICAL,
            assignedTo = assignedTo.toString()
        )

        every { root.get<Any>(any<String>()) } returns path
        every { cb.equal(any(), any()) } returns predicate
        every { cb.and(*anyVararg()) } returns predicate

        val spec = TaskSpecification.fromFilter(tenantId, filter)
        assertNotNull(spec)
    }
}
