package com.taskmaster.scheduler.feign

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@FeignClient(name = "task-service")
interface TaskServiceClient {

    @PatchMapping("/api/v1/tasks/{taskId}/status")
    fun updateTaskStatus(
        @PathVariable taskId: UUID,
        @RequestParam status: String,
        @RequestHeader("X-Tenant-Id") tenantId: String
    )
}
