package com.taskmaster.task.task

import com.taskmaster.task.common.TenantContext
import com.taskmaster.task.common.SecurityUtils
import com.taskmaster.task.task.dto.CreateTaskInput
import com.taskmaster.task.task.dto.TaskDto
import com.taskmaster.task.task.dto.TaskFilter
import com.taskmaster.task.task.dto.TaskPage
import com.taskmaster.task.task.dto.UpdateTaskInput
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "Task management REST API (admin use)")
class TaskController(private val taskService: TaskService) {

    @GetMapping
    @Operation(summary = "List tasks with optional filters (paginated)")
    @PreAuthorize("@sec.hasPermission('TASK_READ') or @sec.isMasterAdmin()")
    fun listTasks(
        @RequestParam(required = false) status: TaskStatus?,
        @RequestParam(required = false) priority: TaskPriority?,
        @RequestParam(required = false) assignedTo: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): TaskPage {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        val filter = TaskFilter(status = status, priority = priority, assignedTo = assignedTo?.toString())
        return taskService.findAll(tenantId, filter, page, size)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    @PreAuthorize("@sec.hasPermission('TASK_READ') or @sec.isMasterAdmin()")
    fun getTask(@PathVariable id: UUID): ResponseEntity<TaskDto> {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        val task = taskService.findById(id, tenantId)
        return if (task != null) ResponseEntity.ok(task) else ResponseEntity.notFound().build()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new task")
    @PreAuthorize("@sec.hasPermission('TASK_CREATE') or @sec.isMasterAdmin()")
    fun createTask(@Valid @RequestBody input: CreateTaskInput): TaskDto {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        val userId = UUID.fromString(SecurityUtils.getCurrentUserId()!!)
        return taskService.createTask(tenantId, userId, input)
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update task fields")
    @PreAuthorize("@sec.hasPermission('TASK_UPDATE') or @sec.isMasterAdmin()")
    fun updateTask(
        @PathVariable id: UUID,
        @Valid @RequestBody input: UpdateTaskInput
    ): TaskDto {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        return taskService.updateTask(id, tenantId, input)
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status")
    @PreAuthorize("@sec.hasPermission('TASK_UPDATE') or @sec.isMasterAdmin()")
    fun updateTaskStatus(
        @PathVariable id: UUID,
        @Parameter(description = "New status value") @RequestParam status: TaskStatus
    ): TaskDto {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        val userId = UUID.fromString(SecurityUtils.getCurrentUserId()!!)
        return taskService.updateTaskStatus(id, tenantId, status, userId)
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign task to a user")
    @PreAuthorize("@sec.hasPermission('TASK_ASSIGN') or @sec.isMasterAdmin()")
    fun assignTask(
        @PathVariable id: UUID,
        @Parameter(description = "User ID to assign to") @RequestParam userId: UUID
    ): TaskDto {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        val assignedBy = UUID.fromString(SecurityUtils.getCurrentUserId()!!)
        return taskService.assignTask(id, tenantId, userId, assignedBy)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task")
    @PreAuthorize("@sec.hasPermission('TASK_DELETE') or @sec.isMasterAdmin()")
    fun deleteTask(@PathVariable id: UUID) {
        val tenantId = UUID.fromString(TenantContext.getRequired())
        taskService.deleteTask(id, tenantId)
    }
}
