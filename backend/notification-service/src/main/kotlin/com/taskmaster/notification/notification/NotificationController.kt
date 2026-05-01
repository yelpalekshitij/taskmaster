package com.taskmaster.notification.notification

import com.taskmaster.notification.common.SecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class SendNotificationRequest(
    val userId: UUID,
    val tenantId: UUID,
    val type: String,
    val title: String,
    val message: String,
    val referenceId: UUID? = null
)

data class NotificationResponse(
    val id: UUID,
    val userId: UUID,
    val tenantId: UUID,
    val type: NotificationType,
    val title: String,
    val message: String,
    val referenceId: UUID?,
    val read: Boolean,
    val createdAt: Instant
)

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notification management endpoints")
class NotificationController(
    private val notificationRepository: NotificationRepository
) {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List notifications for current user (paginated)")
    fun getNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<NotificationResponse>> {
        val userId = UUID.fromString(
            SecurityUtils.getCurrentUserId() ?: return ResponseEntity.status(401).build()
        )
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        return ResponseEntity.ok(notifications.map { it.toResponse() })
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark a single notification as read")
    @Transactional
    fun markRead(@PathVariable id: UUID): ResponseEntity<NotificationResponse> {
        val userId = UUID.fromString(
            SecurityUtils.getCurrentUserId() ?: return ResponseEntity.status(401).build()
        )
        val notification = notificationRepository.findById(id)
            .orElse(null) ?: return ResponseEntity.notFound().build()

        if (notification.userId != userId) {
            return ResponseEntity.status(403).build()
        }

        notification.read = true
        val saved = notificationRepository.save(notification)
        return ResponseEntity.ok(saved.toResponse())
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read for current user")
    @Transactional
    fun markAllRead(): ResponseEntity<Void> {
        val userId = UUID.fromString(
            SecurityUtils.getCurrentUserId() ?: return ResponseEntity.status(401).build()
        )
        notificationRepository.markAllReadByUserId(userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Internal endpoint: directly persist a notification (called via Feign from other services)")
    @Transactional
    fun sendDirect(@RequestBody request: SendNotificationRequest): ResponseEntity<NotificationResponse> {
        val type = runCatching { NotificationType.valueOf(request.type) }
            .getOrDefault(NotificationType.SYSTEM)

        val notification = Notification(
            userId = request.userId,
            tenantId = request.tenantId,
            type = type,
            title = request.title,
            message = request.message,
            referenceId = request.referenceId
        )
        val saved = notificationRepository.save(notification)
        return ResponseEntity.ok(saved.toResponse())
    }

    private fun Notification.toResponse() = NotificationResponse(
        id = id,
        userId = userId,
        tenantId = tenantId,
        type = type,
        title = title,
        message = message,
        referenceId = referenceId,
        read = read,
        createdAt = createdAt
    )
}
