package com.taskmaster.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.taskmaster.notification.kafka.DltConsumer
import com.taskmaster.notification.kafka.NotificationEventConsumer
import com.taskmaster.notification.notification.Notification
import com.taskmaster.notification.notification.NotificationRepository
import com.taskmaster.notification.notification.NotificationType
import com.taskmaster.notification.notification.SendNotificationRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Testcontainers
@Transactional
class NotificationControllerIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:15")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @MockBean
    private lateinit var notificationEventConsumer: NotificationEventConsumer

    @MockBean
    private lateinit var dltConsumer: DltConsumer

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    private val tenantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    private fun userJwt() = jwt().jwt { j ->
        j.subject(userId.toString())
            .claim("tenant_id", tenantId.toString())
    }

    @BeforeEach
    fun cleanUp() {
        notificationRepository.deleteAll()
    }

    // ── GET /api/v1/notifications ─────────────────────────────────────────────

    @Test
    fun `GET notifications returns 200 with paginated user notifications`() {
        saveNotification(userId, "First")
        saveNotification(userId, "Second")
        saveNotification(UUID.randomUUID(), "Stranger")  // should not appear

        mockMvc.perform(
            get("/api/v1/notifications")
                .header("X-Tenant-Id", tenantId.toString())
                .with(userJwt())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(2))
    }

    @Test
    fun `GET notifications returns 401 without authentication`() {
        mockMvc.perform(
            get("/api/v1/notifications")
                .header("X-Tenant-Id", tenantId.toString())
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET notifications respects page and size query parameters`() {
        repeat(5) { saveNotification(userId, "Notif $it") }

        mockMvc.perform(
            get("/api/v1/notifications?page=0&size=2")
                .header("X-Tenant-Id", tenantId.toString())
                .with(userJwt())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(5))
    }

    // ── PATCH /api/v1/notifications/{id}/read ─────────────────────────────────

    @Test
    fun `PATCH mark read returns 200 and sets read=true`() {
        val notif = saveNotification(userId, "Unread", read = false)

        mockMvc.perform(
            patch("/api/v1/notifications/${notif.id}/read")
                .header("X-Tenant-Id", tenantId.toString())
                .with(userJwt())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.read").value(true))
            .andExpect(jsonPath("$.id").value(notif.id.toString()))

        assert(notificationRepository.findById(notif.id).get().read)
    }

    @Test
    fun `PATCH mark read returns 404 for non-existent notification`() {
        mockMvc.perform(
            patch("/api/v1/notifications/${UUID.randomUUID()}/read")
                .header("X-Tenant-Id", tenantId.toString())
                .with(userJwt())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PATCH mark read returns 403 when notification belongs to another user`() {
        val other = UUID.randomUUID()
        val notif = saveNotification(other, "Not Mine")

        mockMvc.perform(
            patch("/api/v1/notifications/${notif.id}/read")
                .header("X-Tenant-Id", tenantId.toString())
                .with(userJwt())
        )
            .andExpect(status().isForbidden)

        assert(!notificationRepository.findById(notif.id).get().read) {
            "Notification should still be unread"
        }
    }

    @Test
    fun `PATCH mark read returns 401 without authentication`() {
        val notif = saveNotification(userId, "Unread")

        mockMvc.perform(
            patch("/api/v1/notifications/${notif.id}/read")
                .header("X-Tenant-Id", tenantId.toString())
        )
            .andExpect(status().isUnauthorized)
    }

    // ── PATCH /api/v1/notifications/read-all ─────────────────────────────────

    @Test
    fun `PATCH read-all returns 204 and marks all notifications as read`() {
        saveNotification(userId, "One", read = false)
        saveNotification(userId, "Two", read = false)
        saveNotification(userId, "Three", read = false)

        mockMvc.perform(
            patch("/api/v1/notifications/read-all")
                .header("X-Tenant-Id", tenantId.toString())
                .with(userJwt())
        )
            .andExpect(status().isNoContent)

        assert(notificationRepository.countByUserIdAndReadFalse(userId) == 0L)
    }

    @Test
    fun `PATCH read-all does not mark other users notifications as read`() {
        val other = UUID.randomUUID()
        saveNotification(other, "Other", read = false)

        mockMvc.perform(
            patch("/api/v1/notifications/read-all")
                .header("X-Tenant-Id", tenantId.toString())
                .with(userJwt())
        )
            .andExpect(status().isNoContent)

        assert(notificationRepository.countByUserIdAndReadFalse(other) == 1L) {
            "Other user's notification should remain unread"
        }
    }

    // ── POST /api/v1/notifications/send ───────────────────────────────────────

    @Test
    fun `POST send creates notification in database`() {
        val targetUser = UUID.randomUUID()
        val request = SendNotificationRequest(
            userId = targetUser,
            tenantId = tenantId,
            type = "TASK_ASSIGNED",
            title = "You have a new task",
            message = "Task 'Fix bug' was assigned to you"
        )

        mockMvc.perform(
            post("/api/v1/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Tenant-Id", tenantId.toString())
                .with(userJwt())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("You have a new task"))
            .andExpect(jsonPath("$.type").value("TASK_ASSIGNED"))
            .andExpect(jsonPath("$.userId").value(targetUser.toString()))

        assert(notificationRepository.findByUserIdOrderByCreatedAtDesc(targetUser, org.springframework.data.domain.Pageable.unpaged()).totalElements == 1L)
    }

    @Test
    fun `POST send falls back to SYSTEM type for unknown notification type`() {
        val request = SendNotificationRequest(
            userId = userId,
            tenantId = tenantId,
            type = "UNKNOWN_TYPE",
            title = "Unknown",
            message = "Test"
        )

        mockMvc.perform(
            post("/api/v1/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Tenant-Id", tenantId.toString())
                .with(userJwt())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("SYSTEM"))
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun saveNotification(
        owner: UUID,
        title: String,
        read: Boolean = false
    ): Notification = notificationRepository.save(
        Notification(
            userId = owner,
            tenantId = tenantId,
            type = NotificationType.TASK_ASSIGNED,
            title = title,
            message = "Message for $title",
            read = read
        )
    )
}
