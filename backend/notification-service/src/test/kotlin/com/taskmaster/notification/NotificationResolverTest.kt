package com.taskmaster.notification

import com.taskmaster.notification.notification.Notification
import com.taskmaster.notification.notification.NotificationRepository
import com.taskmaster.notification.notification.NotificationResolver
import com.taskmaster.notification.notification.NotificationType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.Optional
import java.util.UUID

class NotificationResolverTest {

    private val notificationRepository = mockk<NotificationRepository>()
    private val resolver = NotificationResolver(notificationRepository)

    private val userId = UUID.randomUUID()
    private val jwt: Jwt = Jwt.withTokenValue("test.token")
        .header("alg", "RS256")
        .subject(userId.toString())
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build()

    private fun makeNotification(
        id: UUID = UUID.randomUUID(),
        owner: UUID = userId
    ) = Notification(
        id = id,
        userId = owner,
        tenantId = UUID.randomUUID(),
        type = NotificationType.TASK_ASSIGNED,
        title = "Task Assigned",
        message = "You were assigned a task.",
        read = false,
        referenceId = null,
        createdAt = Instant.now()
    )

    // ── Query: notifications ─────────────────────────────────────────────────

    @Test
    fun `notifications returns paginated result for authenticated user`() {
        val notification = makeNotification()
        val page = PageImpl(listOf(notification))
        every { notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, any<Pageable>()) } returns page

        val result = resolver.notifications(0, 20, jwt)

        assertEquals(1, result.totalElements)
        assertEquals(1, result.content.size)
        assertEquals(notification.id, result.content[0].id)
        assertEquals("TASK_ASSIGNED", result.content[0].type)
        assertEquals(false, result.content[0].read)
    }

    @Test
    fun `notifications uses defaults when page and size are null`() {
        val page = PageImpl(emptyList<Notification>())
        every { notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, any<Pageable>()) } returns page

        val result = resolver.notifications(null, null, jwt)

        assertEquals(0, result.totalElements)
        assertEquals(0, result.content.size)
    }

    // ── Query: unreadCount ───────────────────────────────────────────────────

    @Test
    fun `unreadCount returns the count from repository`() {
        every { notificationRepository.countByUserIdAndReadFalse(userId) } returns 7L

        val result = resolver.unreadCount(jwt)

        assertEquals(7, result)
        verify { notificationRepository.countByUserIdAndReadFalse(userId) }
    }

    @Test
    fun `unreadCount returns 0 when no unread notifications`() {
        every { notificationRepository.countByUserIdAndReadFalse(userId) } returns 0L

        assertEquals(0, resolver.unreadCount(jwt))
    }

    // ── Mutation: markNotificationRead ───────────────────────────────────────

    @Test
    fun `markNotificationRead sets read=true and returns updated notification`() {
        val id = UUID.randomUUID()
        val notification = makeNotification(id)
        every { notificationRepository.findById(id) } returns Optional.of(notification)
        every { notificationRepository.save(notification) } returns notification

        val result = resolver.markNotificationRead(id.toString(), jwt)

        assertTrue(result.read)
        verify { notificationRepository.save(notification) }
    }

    @Test
    fun `markNotificationRead throws NoSuchElementException when not found`() {
        val id = UUID.randomUUID()
        every { notificationRepository.findById(id) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            resolver.markNotificationRead(id.toString(), jwt)
        }
    }

    @Test
    fun `markNotificationRead throws SecurityException when notification belongs to another user`() {
        val id = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val notification = makeNotification(id, owner = otherUserId)
        every { notificationRepository.findById(id) } returns Optional.of(notification)

        assertThrows<SecurityException> {
            resolver.markNotificationRead(id.toString(), jwt)
        }
    }

    // ── Mutation: markAllNotificationsRead ───────────────────────────────────

    @Test
    fun `markAllNotificationsRead calls repository and returns true`() {
        every { notificationRepository.markAllReadByUserId(userId) } returns Unit

        val result = resolver.markAllNotificationsRead(jwt)

        assertTrue(result)
        verify { notificationRepository.markAllReadByUserId(userId) }
    }

    // ── Mutation: deleteNotification ─────────────────────────────────────────

    @Test
    fun `deleteNotification removes the notification and returns true`() {
        val id = UUID.randomUUID()
        val notification = makeNotification(id)
        every { notificationRepository.findById(id) } returns Optional.of(notification)
        every { notificationRepository.delete(notification) } returns Unit

        val result = resolver.deleteNotification(id.toString(), jwt)

        assertTrue(result)
        verify { notificationRepository.delete(notification) }
    }

    @Test
    fun `deleteNotification throws NoSuchElementException when not found`() {
        val id = UUID.randomUUID()
        every { notificationRepository.findById(id) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            resolver.deleteNotification(id.toString(), jwt)
        }
    }

    @Test
    fun `deleteNotification throws SecurityException when notification belongs to another user`() {
        val id = UUID.randomUUID()
        val notification = makeNotification(id, owner = UUID.randomUUID())
        every { notificationRepository.findById(id) } returns Optional.of(notification)

        assertThrows<SecurityException> {
            resolver.deleteNotification(id.toString(), jwt)
        }
    }
}
