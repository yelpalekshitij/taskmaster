package com.taskmaster.notification.notification

import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Controller
import java.util.UUID

data class NotificationDto(
    val id: UUID,
    val type: String,
    val title: String,
    val message: String,
    val read: Boolean,
    val createdAt: String,
    val referenceId: UUID?
)

data class NotificationPageDto(
    val content: List<NotificationDto>,
    val totalElements: Long,
    val totalPages: Int,
    val pageNumber: Int,
    val pageSize: Int
)

@Controller
class NotificationResolver(private val notificationRepository: NotificationRepository) {

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun notifications(
        @Argument page: Int?,
        @Argument size: Int?,
        @AuthenticationPrincipal jwt: Jwt
    ): NotificationPageDto {
        val userId = UUID.fromString(jwt.subject)
        val pageable = PageRequest.of(page ?: 0, size ?: 20, Sort.by("createdAt").descending())
        val result = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        return NotificationPageDto(
            content = result.content.map { it.toDto() },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            pageNumber = result.number,
            pageSize = result.size
        )
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun unreadCount(@AuthenticationPrincipal jwt: Jwt): Int {
        val userId = UUID.fromString(jwt.subject)
        return notificationRepository.countByUserIdAndReadFalse(userId).toInt()
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional
    fun markNotificationRead(@Argument id: String, @AuthenticationPrincipal jwt: Jwt): NotificationDto {
        val userId = UUID.fromString(jwt.subject)
        val notification = notificationRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Notification not found: $id") }
        if (notification.userId != userId) throw SecurityException("Access denied")
        notification.read = true
        return notificationRepository.save(notification).toDto()
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional
    fun markAllNotificationsRead(@AuthenticationPrincipal jwt: Jwt): Boolean {
        val userId = UUID.fromString(jwt.subject)
        notificationRepository.markAllReadByUserId(userId)
        return true
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional
    fun deleteNotification(@Argument id: String, @AuthenticationPrincipal jwt: Jwt): Boolean {
        val userId = UUID.fromString(jwt.subject)
        val notification = notificationRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Notification not found: $id") }
        if (notification.userId != userId) throw SecurityException("Access denied")
        notificationRepository.delete(notification)
        return true
    }

    private fun Notification.toDto() = NotificationDto(
        id = id,
        type = type.name,
        title = title,
        message = message,
        read = read,
        createdAt = createdAt.toString(),
        referenceId = referenceId
    )
}
