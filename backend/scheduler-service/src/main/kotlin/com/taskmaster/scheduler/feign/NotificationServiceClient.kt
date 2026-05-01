package com.taskmaster.scheduler.feign

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

data class SendNotificationRequest(
    val userId: UUID,
    val tenantId: UUID,
    val type: String,
    val title: String,
    val message: String,
    val referenceId: UUID? = null
)

@FeignClient(name = "notification-service")
interface NotificationServiceClient {

    @PostMapping("/api/v1/notifications/send")
    fun sendDirectNotification(@RequestBody request: SendNotificationRequest)
}
