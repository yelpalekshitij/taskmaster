package com.taskmaster.task

import com.taskmaster.task.comment.TaskCommentRepository
import com.taskmaster.task.common.TenantContext
import com.taskmaster.task.task.TaskResolver
import com.taskmaster.task.task.TaskService
import com.taskmaster.task.task.TaskStatus
import com.taskmaster.task.task.dto.CreateTaskInput
import com.taskmaster.task.task.dto.TaskDto
import com.taskmaster.task.task.dto.TaskPage
import com.taskmaster.task.task.dto.UpdateTaskInput
import com.taskmaster.task.task.TaskPriority
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.UUID

class TaskResolverTest {

    private val taskService = mockk<TaskService>()
    private val taskCommentRepository = mockk<TaskCommentRepository>(relaxed = true)
    private val resolver = TaskResolver(taskService, taskCommentRepository)

    private val tenantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    private val jwt: Jwt = Jwt.withTokenValue("test.token")
        .header("alg", "RS256")
        .subject(userId.toString())
        .claim("tenant_id", tenantId.toString())
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build()

    private fun makeTaskDto(id: UUID = UUID.randomUUID()) = TaskDto(
        id = id,
        title = "Test Task",
        description = null,
        status = TaskStatus.TODO,
        priority = TaskPriority.MEDIUM,
        dueDate = null,
        scheduledDate = null,
        tenantId = tenantId,
        assignedTo = null,
        createdBy = userId,
        tags = emptySet(),
        comments = emptyList(),
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private fun emptyPage() = TaskPage(
        content = emptyList(),
        totalElements = 0,
        totalPages = 0,
        pageNumber = 0,
        pageSize = 20
    )

    @BeforeEach
    fun setUp() {
        TenantContext.set(tenantId.toString())
    }

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    // ── Query: tasks ────────────────────────────────────────────────────────────

    @Test
    fun `tasks delegates to service with correct tenantId, default page and size`() {
        val expected = emptyPage()
        every { taskService.findAll(tenantId, null, 0, 20) } returns expected

        val result = resolver.tasks(null, null, null)

        assertEquals(expected, result)
        verify { taskService.findAll(tenantId, null, 0, 20) }
    }

    @Test
    fun `tasks passes provided page and size to service`() {
        val expected = emptyPage()
        every { taskService.findAll(tenantId, null, 2, 50) } returns expected

        val result = resolver.tasks(null, 2, 50)

        assertEquals(expected, result)
    }

    // ── Query: myTasks ──────────────────────────────────────────────────────────

    @Test
    fun `myTasks delegates to service with userId from JWT`() {
        val expected = emptyPage()
        every { taskService.findByAssignee(tenantId, userId, 0, 20) } returns expected

        val result = resolver.myTasks(null, null, jwt)

        assertEquals(expected, result)
        verify { taskService.findByAssignee(tenantId, userId, 0, 20) }
    }

    @Test
    fun `myTasks passes provided page and size to service`() {
        val expected = emptyPage()
        every { taskService.findByAssignee(tenantId, userId, 1, 10) } returns expected

        val result = resolver.myTasks(1, 10, jwt)

        assertEquals(expected, result)
    }

    // ── Query: task ─────────────────────────────────────────────────────────────

    @Test
    fun `task returns TaskDto when found`() {
        val id = UUID.randomUUID()
        val expected = makeTaskDto(id)
        every { taskService.findById(id, tenantId) } returns expected

        val result = resolver.task(id.toString())

        assertEquals(expected, result)
    }

    @Test
    fun `task returns null when not found`() {
        val id = UUID.randomUUID()
        every { taskService.findById(id, tenantId) } returns null

        val result = resolver.task(id.toString())

        assertNull(result)
    }

    // ── Mutation: createTask ────────────────────────────────────────────────────

    @Test
    fun `createTask delegates to service with tenantId and userId from JWT`() {
        val input = CreateTaskInput(title = "New Task", priority = TaskPriority.HIGH)
        val expected = makeTaskDto()
        every { taskService.createTask(tenantId, userId, input) } returns expected

        val result = resolver.createTask(input, jwt)

        assertEquals(expected, result)
        verify { taskService.createTask(tenantId, userId, input) }
    }

    // ── Mutation: updateTask ────────────────────────────────────────────────────

    @Test
    fun `updateTask delegates to service with parsed id and tenantId`() {
        val id = UUID.randomUUID()
        val input = UpdateTaskInput(title = "Updated")
        val expected = makeTaskDto(id)
        every { taskService.updateTask(id, tenantId, input) } returns expected

        val result = resolver.updateTask(id.toString(), input)

        assertEquals(expected, result)
    }

    // ── Mutation: updateTaskStatus ──────────────────────────────────────────────

    @Test
    fun `updateTaskStatus passes status enum and userId to service`() {
        val id = UUID.randomUUID()
        val expected = makeTaskDto(id).copy(status = TaskStatus.IN_PROGRESS)
        every { taskService.updateTaskStatus(id, tenantId, TaskStatus.IN_PROGRESS, userId) } returns expected

        val result = resolver.updateTaskStatus(id.toString(), "IN_PROGRESS", jwt)

        assertEquals(TaskStatus.IN_PROGRESS, result.status)
    }

    @Test
    fun `updateTaskStatus throws IllegalArgumentException for unknown status`() {
        val id = UUID.randomUUID()

        assertThrows<IllegalArgumentException> {
            resolver.updateTaskStatus(id.toString(), "INVALID_STATUS", jwt)
        }
    }

    // ── Mutation: assignTask ────────────────────────────────────────────────────

    @Test
    fun `assignTask delegates to service with correct parameters`() {
        val taskId = UUID.randomUUID()
        val assigneeId = UUID.randomUUID()
        val expected = makeTaskDto(taskId).copy(assignedTo = assigneeId)
        every { taskService.assignTask(taskId, tenantId, assigneeId, userId) } returns expected

        val result = resolver.assignTask(taskId.toString(), assigneeId.toString(), jwt)

        assertEquals(expected, result)
        verify { taskService.assignTask(taskId, tenantId, assigneeId, userId) }
    }

    // ── Mutation: deleteTask ────────────────────────────────────────────────────

    @Test
    fun `deleteTask calls service and returns true`() {
        val id = UUID.randomUUID()
        every { taskService.deleteTask(id, tenantId) } returns Unit

        val result = resolver.deleteTask(id.toString())

        assertEquals(true, result)
        verify { taskService.deleteTask(id, tenantId) }
    }

    // ── TenantContext ───────────────────────────────────────────────────────────

    @Test
    fun `tasks throws when TenantContext is not set`() {
        TenantContext.clear()

        assertThrows<IllegalStateException> {
            resolver.tasks(null, null, null)
        }
    }
}
