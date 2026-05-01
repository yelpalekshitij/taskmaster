package com.taskmaster.scheduler.controller

import com.taskmaster.scheduler.common.SecurityUtils
import com.taskmaster.scheduler.entity.ScheduledTask
import com.taskmaster.scheduler.service.SchedulerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class ScheduleTaskRequest(
    @field:NotNull val scheduledAt: Instant
)

data class ScheduledTaskResponse(
    val id: UUID,
    val taskId: UUID,
    val tenantId: UUID,
    val scheduledAt: Instant,
    val cronExpr: String?,
    val jobKey: String,
    val active: Boolean,
    val createdAt: Instant
)

@RestController
@RequestMapping("/api/v1/scheduler/tasks")
@Tag(name = "Scheduler", description = "Task scheduling endpoints")
class SchedulerController(
    private val schedulerService: SchedulerService
) {

    @PostMapping("/{taskId}/schedule")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Schedule a task to be executed at a given time")
    fun scheduleTask(
        @PathVariable taskId: UUID,
        @Valid @RequestBody request: ScheduleTaskRequest
    ): ResponseEntity<ScheduledTaskResponse> {
        val tenantId = SecurityUtils.getCurrentTenantIdAsUUID()
            ?: return ResponseEntity.status(400).build()

        val scheduledTask = schedulerService.scheduleTask(taskId, tenantId, request.scheduledAt)
        return ResponseEntity.ok(scheduledTask.toResponse())
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a scheduled task")
    fun cancelSchedule(@PathVariable taskId: UUID): ResponseEntity<Void> {
        schedulerService.cancelSchedule(taskId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all active scheduled tasks for the current tenant")
    fun listActiveSchedules(): ResponseEntity<List<ScheduledTaskResponse>> {
        val tenantId = SecurityUtils.getCurrentTenantIdAsUUID()
            ?: return ResponseEntity.status(400).build()

        val schedules = schedulerService.getActiveSchedules(tenantId)
        return ResponseEntity.ok(schedules.map { it.toResponse() })
    }

    private fun ScheduledTask.toResponse() = ScheduledTaskResponse(
        id = id,
        taskId = taskId,
        tenantId = tenantId,
        scheduledAt = scheduledAt,
        cronExpr = cronExpr,
        jobKey = jobKey,
        active = active,
        createdAt = createdAt
    )
}
