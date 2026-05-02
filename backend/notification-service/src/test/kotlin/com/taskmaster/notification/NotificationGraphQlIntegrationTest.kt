package com.taskmaster.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.taskmaster.notification.kafka.DltConsumer
import com.taskmaster.notification.kafka.NotificationEventConsumer
import com.taskmaster.notification.notification.Notification
import com.taskmaster.notification.notification.NotificationRepository
import com.taskmaster.notification.notification.NotificationType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
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
class NotificationGraphQlIntegrationTest {

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

    @MockBean
    private lateinit var javaMailSender: JavaMailSender

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    private val tenantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    private fun gql(query: String) =
        post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(mapOf("query" to query)))
            .header("X-Tenant-Id", tenantId.toString())
            .with(jwt().jwt { j ->
                j.subject(userId.toString())
                    .claim("tenant_id", tenantId.toString())
            })

    @BeforeEach
    fun cleanUp() {
        notificationRepository.deleteAll()
    }

    // ── Query: notifications ──────────────────────────────────────────────────

    @Test
    fun `notifications returns empty when user has no notifications`() {
        mockMvc.perform(gql("{ notifications { totalElements content { id } } }"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.notifications.totalElements").value(0))
            .andExpect(jsonPath("$.data.notifications.content").isEmpty)
    }

    @Test
    fun `notifications returns only notifications belonging to authenticated user`() {
        val mine = saveNotification(userId, "My Notification")
        saveNotification(UUID.randomUUID(), "Someone Else")  // different user

        mockMvc.perform(gql("{ notifications { totalElements content { id title } } }"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.notifications.totalElements").value(1))
            .andExpect(jsonPath("$.data.notifications.content[0].id").value(mine.id.toString()))
            .andExpect(jsonPath("$.data.notifications.content[0].title").value("My Notification"))
    }

    @Test
    fun `notifications returns error without authentication`() {
        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("query" to "{ notifications { totalElements } }")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errors").isArray)
    }

    // ── Query: unreadCount ────────────────────────────────────────────────────

    @Test
    fun `unreadCount returns count of unread notifications`() {
        saveNotification(userId, "Unread 1", read = false)
        saveNotification(userId, "Unread 2", read = false)
        saveNotification(userId, "Already Read", read = true)

        mockMvc.perform(gql("{ unreadCount }"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.unreadCount").value(2))
    }

    @Test
    fun `unreadCount returns 0 when all notifications are read`() {
        saveNotification(userId, "Read", read = true)

        mockMvc.perform(gql("{ unreadCount }"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.unreadCount").value(0))
    }

    // ── Mutation: markNotificationRead ────────────────────────────────────────

    @Test
    fun `markNotificationRead sets read=true in database`() {
        val notif = saveNotification(userId, "Unread", read = false)

        mockMvc.perform(gql("""mutation { markNotificationRead(id: "${notif.id}") { id read } }"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.markNotificationRead.read").value(true))

        mockMvc.perform(gql("{ unreadCount }"))
            .andExpect(jsonPath("$.data.unreadCount").value(0))
    }

    @Test
    fun `markNotificationRead returns error for notification belonging to another user`() {
        val other = UUID.randomUUID()
        val notif = saveNotification(other, "Not Mine")

        mockMvc.perform(gql("""mutation { markNotificationRead(id: "${notif.id}") { id } }"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errors").isArray)
            .andExpect(jsonPath("$.errors[0].message").exists())
    }

    @Test
    fun `markNotificationRead returns error for non-existent notification`() {
        mockMvc.perform(gql("""mutation { markNotificationRead(id: "${UUID.randomUUID()}") { id } }"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errors").isArray)
    }

    // ── Mutation: markAllNotificationsRead ────────────────────────────────────

    @Test
    fun `markAllNotificationsRead marks all user notifications as read`() {
        saveNotification(userId, "First", read = false)
        saveNotification(userId, "Second", read = false)
        saveNotification(userId, "Third", read = false)

        mockMvc.perform(gql("mutation { markAllNotificationsRead }"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.markAllNotificationsRead").value(true))

        mockMvc.perform(gql("{ unreadCount }"))
            .andExpect(jsonPath("$.data.unreadCount").value(0))
    }

    @Test
    fun `markAllNotificationsRead does not affect other users notifications`() {
        val other = UUID.randomUUID()
        saveNotification(other, "Other User Notif", read = false)
        saveNotification(userId, "Mine", read = false)

        mockMvc.perform(gql("mutation { markAllNotificationsRead }"))
            .andExpect(status().isOk)

        val remaining = notificationRepository.countByUserIdAndReadFalse(other)
        assert(remaining == 1L) { "Other user's unread count should remain 1, was $remaining" }
    }

    // ── Mutation: deleteNotification ──────────────────────────────────────────

    @Test
    fun `deleteNotification removes notification from database`() {
        val notif = saveNotification(userId, "To Delete")

        mockMvc.perform(gql("""mutation { deleteNotification(id: "${notif.id}") }"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.deleteNotification").value(true))

        mockMvc.perform(gql("{ notifications { totalElements } }"))
            .andExpect(jsonPath("$.data.notifications.totalElements").value(0))
    }

    @Test
    fun `deleteNotification returns error when notification belongs to another user`() {
        val other = UUID.randomUUID()
        val notif = saveNotification(other, "Not Mine")

        mockMvc.perform(gql("""mutation { deleteNotification(id: "${notif.id}") }"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errors").isArray)

        assert(notificationRepository.findById(notif.id).isPresent) {
            "Notification should not have been deleted"
        }
    }

    @Test
    fun `deleteNotification returns error for non-existent id`() {
        mockMvc.perform(gql("""mutation { deleteNotification(id: "${UUID.randomUUID()}") }"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errors").isArray)
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
            message = "Test message for $title",
            read = read
        )
    )
}
