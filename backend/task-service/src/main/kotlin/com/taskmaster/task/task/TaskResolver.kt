package com.taskmaster.task.task

import com.taskmaster.task.comment.TaskCommentRepository
import com.taskmaster.task.comment.dto.TaskCommentDto
import com.taskmaster.task.common.SecurityUtils
import com.taskmaster.task.common.TenantContext
import com.taskmaster.task.task.dto.CreateTaskInput
import com.taskmaster.task.task.dto.TaskDto
import com.taskmaster.task.task.dto.TaskFilter
import com.taskmaster.task.task.dto.TaskPage
import com.taskmaster.task.task.dto.UpdateTaskInput
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class TaskResolver(
    private val taskService: TaskService,
    private val taskCommentRepository: TaskCommentRepository
) {

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun tasks(
        @Argument filter: TaskFilter?,
        @Argument page: Int?,
        @Argument size: Int?
    ): TaskPage {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        return taskService.findAll(tenantId, filter, page ?: 0, size ?: 20)
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myTasks(@Argument page: Int?, @Argument size: Int?): TaskPage {
        val userId = UUID.fromString(SecurityUtils.getCurrentUserId()!!)
        val tenantId = UUID.fromString(TenantContext.getRequired())
        return taskService.findByAssignee(tenantId, userId, page ?: 0, size ?: 20)
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun task(@Argument id: String): TaskDto? {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        return taskService.findById(UUID.fromString(id), tenantId)
    }

    @MutationMapping
    @PreAuthorize("@sec.hasPermission('TASK_CREATE')")
    fun createTask(@Argument input: CreateTaskInput): TaskDto {
        val userId = UUID.fromString(SecurityUtils.getCurrentUserId()!!)
        val tenantId = UUID.fromString(TenantContext.getRequired())
        return taskService.createTask(tenantId, userId, input)
    }

    @MutationMapping
    @PreAuthorize("@sec.hasPermission('TASK_UPDATE')")
    fun updateTask(@Argument id: String, @Argument input: UpdateTaskInput): TaskDto {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        return taskService.updateTask(UUID.fromString(id), tenantId, input)
    }

    @MutationMapping
    @PreAuthorize("@sec.hasPermission('TASK_UPDATE')")
    fun updateTaskStatus(@Argument id: String, @Argument status: String): TaskDto {
        val userId = UUID.fromString(SecurityUtils.getCurrentUserId()!!)
        val tenantId = UUID.fromString(TenantContext.getRequired())
        return taskService.updateTaskStatus(UUID.fromString(id), tenantId, TaskStatus.valueOf(status), userId)
    }

    @MutationMapping
    @PreAuthorize("@sec.hasPermission('TASK_ASSIGN')")
    fun assignTask(@Argument id: String, @Argument userId: String): TaskDto {
        val assignedBy = UUID.fromString(SecurityUtils.getCurrentUserId()!!)
        val tenantId = UUID.fromString(TenantContext.getRequired())
        return taskService.assignTask(UUID.fromString(id), tenantId, UUID.fromString(userId), assignedBy)
    }

    @MutationMapping
    @PreAuthorize("@sec.hasPermission('TASK_DELETE')")
    fun deleteTask(@Argument id: String): Boolean {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        taskService.deleteTask(UUID.fromString(id), tenantId)
        return true
    }

    @SchemaMapping(typeName = "Task", field = "comments")
    fun comments(task: TaskDto): List<TaskCommentDto> {
        return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(task.id)
            .map { c ->
                TaskCommentDto(
                    id = c.id,
                    taskId = c.taskId,
                    userId = c.userId,
                    content = c.content,
                    createdAt = c.createdAt
                )
            }
    }
}
