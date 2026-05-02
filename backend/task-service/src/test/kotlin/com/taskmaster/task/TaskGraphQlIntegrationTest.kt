package com.taskmaster.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.taskmaster.task.outbox.OutboxRelay
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.CacheManager
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
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
class TaskGraphQlIntegrationTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun cacheManager(): CacheManager = NoOpCacheManager()
    }

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
    private lateinit var outboxRelay: OutboxRelay

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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
                    .claim("realm_access", mapOf("roles" to listOf("MASTER_ADMIN")))
            }.authorities(SimpleGrantedAuthority("ROLE_MASTER_ADMIN")))

    // ── Query: tasks ──────────────────────────────────────────────────────────

    @Test
    fun `tasks returns empty list for new tenant`() {
        mockMvc.perform(gql("{ tasks { totalElements content { id } } }"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.tasks.totalElements").value(0))
            .andExpect(jsonPath("$.data.tasks.content").isEmpty)
    }

    @Test
    fun `tasks returns error when called without authentication`() {
        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(mapOf("query" to "{ tasks { totalElements } }")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errors").isArray)
            .andExpect(jsonPath("$.errors[0].message").exists())
    }

    @Test
    fun `tasks filtered by status returns only matching tasks`() {
        createTask("Todo Task", "MEDIUM")
        val inProgressId = createTask("In-Progress Task", "MEDIUM")

        mockMvc.perform(gql("""mutation { updateTaskStatus(id: "$inProgressId", status: IN_PROGRESS) { id } }"""))
            .andExpect(status().isOk)

        mockMvc.perform(gql("{ tasks(filter: { status: TODO }) { totalElements content { title } } }"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.tasks.totalElements").value(1))
            .andExpect(jsonPath("$.data.tasks.content[0].title").value("Todo Task"))
    }

    // ── Query: task ───────────────────────────────────────────────────────────

    @Test
    fun `task returns task by id`() {
        val id = createTask("Find Me", "LOW")

        mockMvc.perform(gql("""{ task(id: "$id") { id title priority } }"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.task.id").value(id))
            .andExpect(jsonPath("$.data.task.title").value("Find Me"))
            .andExpect(jsonPath("$.data.task.priority").value("LOW"))
    }

    @Test
    fun `task returns null for unknown id`() {
        mockMvc.perform(gql("""{ task(id: "${UUID.randomUUID()}") { id } }"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.task").doesNotExist())
    }

    // ── Query: myTasks ────────────────────────────────────────────────────────

    @Test
    fun `myTasks returns only tasks assigned to authenticated user`() {
        val myId = createTask("My Task", "MEDIUM")
        val otherId = createTask("Other Task", "MEDIUM")
        val stranger = UUID.randomUUID()

        mockMvc.perform(gql("""mutation { assignTask(id: "$myId", userId: "$userId") { id } }"""))
        mockMvc.perform(gql("""mutation { assignTask(id: "$otherId", userId: "$stranger") { id } }"""))

        mockMvc.perform(gql("{ myTasks { totalElements content { title } } }"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.myTasks.totalElements").value(1))
            .andExpect(jsonPath("$.data.myTasks.content[0].title").value("My Task"))
    }

    // ── Mutation: createTask ──────────────────────────────────────────────────

    @Test
    fun `createTask persists task and it appears in subsequent query`() {
        mockMvc.perform(gql("""
            mutation { createTask(input: { title: "Persisted Task", priority: HIGH }) { id title status priority } }
        """.trimIndent()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.createTask.title").value("Persisted Task"))
            .andExpect(jsonPath("$.data.createTask.status").value("TODO"))
            .andExpect(jsonPath("$.data.createTask.priority").value("HIGH"))

        mockMvc.perform(gql("{ tasks { totalElements content { title } } }"))
            .andExpect(jsonPath("$.data.tasks.totalElements").value(1))
            .andExpect(jsonPath("$.data.tasks.content[0].title").value("Persisted Task"))
    }

    @Test
    fun `createTask with tags stores tags correctly`() {
        mockMvc.perform(gql("""
            mutation { createTask(input: { title: "Tagged Task", priority: LOW, tags: ["backend", "urgent"] }) { tags } }
        """.trimIndent()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.createTask.tags").isArray)
            .andExpect(jsonPath("$.data.createTask.tags.length()").value(2))
    }

    // ── Mutation: updateTask ──────────────────────────────────────────────────

    @Test
    fun `updateTask changes title and description in database`() {
        val id = createTask("Original", "MEDIUM")

        mockMvc.perform(gql("""
            mutation { updateTask(id: "$id", input: { title: "Updated", description: "New desc" }) { title description } }
        """.trimIndent()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.updateTask.title").value("Updated"))
            .andExpect(jsonPath("$.data.updateTask.description").value("New desc"))

        mockMvc.perform(gql("""{ task(id: "$id") { title description } }"""))
            .andExpect(jsonPath("$.data.task.title").value("Updated"))
            .andExpect(jsonPath("$.data.task.description").value("New desc"))
    }

    // ── Mutation: updateTaskStatus ────────────────────────────────────────────

    @Test
    fun `updateTaskStatus transitions from TODO to IN_PROGRESS`() {
        val id = createTask("Status Task", "MEDIUM")

        mockMvc.perform(gql("""mutation { updateTaskStatus(id: "$id", status: IN_PROGRESS) { id status } }"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.updateTaskStatus.status").value("IN_PROGRESS"))

        mockMvc.perform(gql("""{ task(id: "$id") { status } }"""))
            .andExpect(jsonPath("$.data.task.status").value("IN_PROGRESS"))
    }

    @Test
    fun `updateTaskStatus returns error for invalid status value`() {
        val id = createTask("Bad Status", "MEDIUM")

        mockMvc.perform(gql("""mutation { updateTaskStatus(id: "$id", status: "INVALID") { id } }"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errors").isArray)
    }

    // ── Mutation: assignTask ──────────────────────────────────────────────────

    @Test
    fun `assignTask sets assignedTo field in database`() {
        val id = createTask("To Assign", "MEDIUM")
        val assignee = UUID.randomUUID()

        mockMvc.perform(gql("""mutation { assignTask(id: "$id", userId: "$assignee") { assignedTo } }"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.assignTask.assignedTo").value(assignee.toString()))

        mockMvc.perform(gql("""{ task(id: "$id") { assignedTo } }"""))
            .andExpect(jsonPath("$.data.task.assignedTo").value(assignee.toString()))
    }

    // ── Mutation: deleteTask ──────────────────────────────────────────────────

    @Test
    fun `deleteTask removes task from database`() {
        val id = createTask("To Delete", "MEDIUM")

        mockMvc.perform(gql("{ tasks { totalElements } }"))
            .andExpect(jsonPath("$.data.tasks.totalElements").value(1))

        mockMvc.perform(gql("""mutation { deleteTask(id: "$id") }"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.deleteTask").value(true))

        mockMvc.perform(gql("{ tasks { totalElements } }"))
            .andExpect(jsonPath("$.data.tasks.totalElements").value(0))
    }

    // ── Tenant isolation ──────────────────────────────────────────────────────

    @Test
    fun `task created in one tenant is not visible to another tenant`() {
        createTask("Tenant A Task", "MEDIUM")

        val otherTenant = UUID.randomUUID()
        mockMvc.perform(
            post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("query" to "{ tasks { totalElements } }")))
                .header("X-Tenant-Id", otherTenant.toString())
                .with(jwt().jwt { j ->
                    j.subject(UUID.randomUUID().toString())
                        .claim("tenant_id", otherTenant.toString())
                        .claim("realm_access", mapOf("roles" to listOf("MASTER_ADMIN")))
                }.authorities(SimpleGrantedAuthority("ROLE_MASTER_ADMIN")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.tasks.totalElements").value(0))
    }

    // ── addComment ────────────────────────────────────────────────────────────

    @Test
    fun `addComment persists comment and returns it`() {
        val taskId = createTask("Commented Task", "MEDIUM")

        mockMvc.perform(gql("""
            mutation { addComment(taskId: "$taskId", content: "Great work!") { id content userId } }
        """.trimIndent()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.addComment.content").value("Great work!"))
            .andExpect(jsonPath("$.data.addComment.userId").value(userId.toString()))

        mockMvc.perform(gql("""{ task(id: "$taskId") { comments { content } } }"""))
            .andExpect(jsonPath("$.data.task.comments[0].content").value("Great work!"))
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun createTask(title: String, priority: String): String {
        val result = mockMvc.perform(gql("""
            mutation { createTask(input: { title: "$title", priority: $priority }) { id } }
        """.trimIndent())).andReturn()
        return objectMapper.readTree(result.response.contentAsString)
            .at("/data/createTask/id").asText()
    }
}
