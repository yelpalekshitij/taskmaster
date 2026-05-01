package com.taskmaster.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.taskmaster.task.comment.TaskCommentRepository
import com.taskmaster.task.history.TaskHistoryService
import com.taskmaster.task.outbox.OutboxEvent
import com.taskmaster.task.outbox.OutboxEventRepository
import com.taskmaster.task.task.Task
import com.taskmaster.task.task.TaskPriority
import com.taskmaster.task.task.TaskRepository
import com.taskmaster.task.task.TaskService
import com.taskmaster.task.task.TaskStatus
import com.taskmaster.task.task.dto.CreateTaskInput
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

class TaskServiceTest {

    private val taskRepository = mockk<TaskRepository>()
    private val outboxRepository = mockk<OutboxEventRepository>()
    private val objectMapper = ObjectMapper()
    private val taskHistoryService = mockk<TaskHistoryService>(relaxed = true)
    // TaskCommentRepository needed by TaskService constructor; relaxed so no stubs needed
    private val taskCommentRepository = mockk<TaskCommentRepository>(relaxed = true)

    private val taskService = TaskService(
        taskRepository = taskRepository,
        outboxRepository = outboxRepository,
        objectMapper = objectMapper,
        taskHistoryService = taskHistoryService,
        taskCommentRepository = taskCommentRepository
    )

    @Test
    fun `createTask saves task and publishes outbox event when assignedTo is set`() {
        val tenantId = UUID.randomUUID()
        val createdBy = UUID.randomUUID()
        val assignedTo = UUID.randomUUID()

        val input = CreateTaskInput(
            title = "Test Task",
            description = "A description",
            priority = TaskPriority.HIGH,
            assignedTo = assignedTo
        )

        val savedTask = Task(
            title = input.title,
            description = input.description,
            priority = TaskPriority.HIGH,
            tenantId = tenantId,
            createdBy = createdBy,
            assignedTo = assignedTo
        )

        every { taskRepository.save(any()) } returns savedTask
        every { outboxRepository.save(any()) } answers { firstArg() }

        val result = taskService.createTask(tenantId, createdBy, input)

        assertNotNull(result)
        assertEquals("Test Task", result.title)

        val outboxSlot = slot<OutboxEvent>()
        verify { outboxRepository.save(capture(outboxSlot)) }
        assertEquals("TASK_ASSIGNED", outboxSlot.captured.eventType)
        assertEquals(savedTask.id, outboxSlot.captured.aggregateId)
    }

    @Test
    fun `createTask does not publish outbox event when assignedTo is null`() {
        val tenantId = UUID.randomUUID()
        val createdBy = UUID.randomUUID()

        val input = CreateTaskInput(
            title = "Unassigned Task",
            assignedTo = null
        )

        val savedTask = Task(
            title = input.title,
            tenantId = tenantId,
            createdBy = createdBy
        )

        every { taskRepository.save(any()) } returns savedTask

        val result = taskService.createTask(tenantId, createdBy, input)

        assertNotNull(result)
        verify(exactly = 0) { outboxRepository.save(any()) }
    }

    @Test
    fun `updateTaskStatus records history and publishes outbox event for assigned task`() {
        val tenantId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val assignedTo = UUID.randomUUID()
        val changedBy = UUID.randomUUID()

        val existingTask = Task(
            id = taskId,
            title = "Existing Task",
            status = TaskStatus.TODO,
            tenantId = tenantId,
            createdBy = UUID.randomUUID(),
            assignedTo = assignedTo
        )

        every { taskRepository.findByIdAndTenantId(taskId, tenantId) } returns existingTask
        every { taskRepository.save(any()) } returns existingTask
        every { outboxRepository.save(any()) } answers { firstArg() }

        val result = taskService.updateTaskStatus(taskId, tenantId, TaskStatus.IN_PROGRESS, changedBy)

        assertNotNull(result)
        assertEquals(TaskStatus.IN_PROGRESS, result.status)

        verify { taskHistoryService.record(taskId, changedBy, TaskStatus.TODO, TaskStatus.IN_PROGRESS, null) }

        val outboxSlot = slot<OutboxEvent>()
        verify { outboxRepository.save(capture(outboxSlot)) }
        assertEquals("TASK_UPDATED", outboxSlot.captured.eventType)
    }

    @Test
    fun `assignTask publishes TASK_ASSIGNED outbox event`() {
        val tenantId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val assignedTo = UUID.randomUUID()
        val assignedBy = UUID.randomUUID()

        val existingTask = Task(
            id = taskId,
            title = "Task to Assign",
            tenantId = tenantId,
            createdBy = UUID.randomUUID()
        )

        every { taskRepository.findByIdAndTenantId(taskId, tenantId) } returns existingTask
        every { taskRepository.save(any()) } returns existingTask
        every { outboxRepository.save(any()) } answers { firstArg() }

        val result = taskService.assignTask(taskId, tenantId, assignedTo, assignedBy)

        assertNotNull(result)

        val outboxSlot = slot<OutboxEvent>()
        verify { outboxRepository.save(capture(outboxSlot)) }
        assertEquals("TASK_ASSIGNED", outboxSlot.captured.eventType)
        assertEquals(taskId, outboxSlot.captured.aggregateId)
    }

    @Test
    fun `updateTaskStatus throws when task not found`() {
        val tenantId = UUID.randomUUID()
        val taskId = UUID.randomUUID()

        every { taskRepository.findByIdAndTenantId(taskId, tenantId) } returns null

        org.junit.jupiter.api.assertThrows<NoSuchElementException> {
            taskService.updateTaskStatus(taskId, tenantId, TaskStatus.DONE, UUID.randomUUID())
        }
    }
}
